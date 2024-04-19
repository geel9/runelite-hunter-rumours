package com.geel.hunterrumours;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.kit.KitType;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.npcoverlay.HighlightedNpc;
import net.runelite.client.game.npcoverlay.NpcOverlayService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import java.awt.*;
import java.util.*;

import net.runelite.client.util.Text;

@PluginDescriptor(
        name = "Hunter Rumours",
        description = "Tracks your current hunter rumour, as well as the saved rumours of all masters",
        tags = {"hunter", "hunters", "guild", "rumour", "rumor", "contract", "task", "varlamore"}
)
@Slf4j
public class HunterRumoursPlugin extends Plugin {
    public static final Map<Hunter, Rumour> hunterRumours = new HashMap<>() {
        {
            for (var hunter : Hunter.allValues()) {
                put(hunter, Rumour.NONE);
            }
        }
    };

    public Hunter currentHunter = Hunter.NONE;
    public Rumour currentDetachedRumour = Rumour.NONE;
    private boolean currentRumourFinished = false;
    private BackToBackState backToBackState = BackToBackState.UNKNOWN;
    private final Set<HunterRumourWorldMapPoint> currentMapPoints = new HashSet<>();
    boolean backToBackDialogOpened = false; // Tracking variable to hook into back-to-back dialog opening

    @Getter
    private int numRumoursCompleted = 0; // Number of rumours completed

    @Getter
    boolean hasFullHunterKit = false; // Whether the user is currently wearing the full hunter guild kit

    @Inject
    private HunterRumoursConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private NpcOverlayService npcOverlayService;

    @Inject
    private InfoBoxManager infoBoxManager;

    @Inject
    private ItemManager itemManager;

    @Inject
    private HunterRumoursOverlay overlay;

    @Inject
    private ConfigManager configManager;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ChatParser chatParser;

    @Inject
    private WorldMapPointManager worldMapPointManager;

    @Provides
    HunterRumoursConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(HunterRumoursConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        overlayManager.add(overlay);
        npcOverlayService.registerHighlighter(this::highlighterFn);
        clientThread.invoke(this::loadFromConfig);
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(overlay);
        if (infoBox != null) {
            infoBoxManager.removeInfoBox(infoBox);
        }
        npcOverlayService.unregisterHighlighter(this::highlighterFn);
        npcOverlayService.rebuild();
        clientThread.invoke(this::resetParams);
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            clientThread.invoke(this::loadFromConfig);
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged) {
        if (!configChanged.getGroup().equals(HunterRumoursConfig.GROUP)) {
            return;
        }

        clientThread.invoke(this::loadFromConfig);
        clientThread.invoke(npcOverlayService::rebuild);
    }

    @Subscribe
    public void onCommandExecuted(CommandExecuted event) {
        if (event.getCommand().equals("resetrumours")) {
            clientThread.invoke(this::resetConfig);
        }
    }

    @Subscribe
    public void onPlayerChanged(PlayerChanged event) {
        // We only care about ourselves
        if (event.getPlayer().getId() != client.getLocalPlayer().getId()) {
            return;
        }

        // Parse out hunter equipment
        var player = event.getPlayer();
        var comp = player.getPlayerComposition();
        var head = comp.getEquipmentId(KitType.HEAD);
        var top = comp.getEquipmentId(KitType.TORSO);
        var legs = comp.getEquipmentId(KitType.LEGS);
        var boots = comp.getEquipmentId(KitType.BOOTS);

        var isHead = head == ItemID.GUILD_HUNTER_HEADWEAR;
        var isTop = top == ItemID.GUILD_HUNTER_TOP;
        var isLegs = legs == ItemID.GUILD_HUNTER_LEGS;
        var isBoots = boots == ItemID.GUILD_HUNTER_BOOTS;

        boolean hasFullKit = isHead && isTop && isLegs && isBoots;
        if (hasFullKit != hasFullHunterKit) {
            hasFullHunterKit = hasFullKit;
            refreshAllDisplays();
        }
    }


    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        if (!isInBurrows()) {
            return;
        }

        if (event.getGroupId() == InterfaceID.DIALOG_OPTION) {
            backToBackDialogOpened = true;
        }
    }

    @Subscribe
    public void onPostClientTick(PostClientTick event) {
        // If the back-to-back chat dialog just opened, we will load the widget and parse it to
        // A) determine if what's being offered is enabling or disabling of back-to-back and
        // B) hook in to the dialog to detect when the user makes a choice and update our internal state
        if (!backToBackDialogOpened) {
            return;
        }

        backToBackDialogOpened = false;

        var widget = client.getWidget(ComponentID.DIALOG_OPTION_OPTIONS);
        if (widget == null) {
            return;
        }

        var children = widget.getChildren();
        if (children == null || children.length != 5) {
            return;
        }

        var title = children[0];
        var option1 = children[1];
        var option2 = children[2];

        if (title == null || option1 == null || option2 == null) {
            return;
        }

        boolean isPromptingToDisable = title.getText().contains("Disable back-to-back rumours?");
        boolean isPromptingToEnable = title.getText().contains("Enable back-to-back rumours?");

        if ((!isPromptingToDisable && !isPromptingToEnable) || !option1.getText().equals("Yes") || !option2.getText().equals("No")) {
            return;
        }

        BackToBackState ifYesState = isPromptingToEnable ? BackToBackState.ENABLED : BackToBackState.DISABLED;

        // Add a key listener to the title (a widget we know doesn't have a listener), to detect the '1'
        // key being pressed. We have to do this because the `onClickListener` doesn't fire if a number key is pressed
        // to activate the menu entry.
        title.setOnKeyListener((JavaScriptCallback) ev -> {
            if (ev.getTypedKeyChar() == '1') {
                setBackToBackState(ifYesState, true);
            }
        });
        title.setHasListener(true);

        // Capture player choice if they click 'Yes'.
        // We don't need to know if they click 'No' because we... don't care at all.
        option1.setOnClickListener((JavaScriptCallback) ev -> {
            setBackToBackState(ifYesState, true);
        });
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        handleBurrowsHunterDialog(event);
        handleQuetzalWhistleChatMessage(event);
        handleRumourFinishedChatMessage(event);
        handleBackToBackChatMessage(event);
        handleNumRumoursCompletedChatMessage(event);
    }

    /**
     * Sets the back-to-back state in memory and in config.
     * <p>
     * If this is due to a user's choice (eg they changed it), puts a message in chat indicating their current back-to-back status
     */
    public void setBackToBackState(BackToBackState backToBackState, boolean isFromUserChoice) {
        configManager.setRSProfileConfiguration(HunterRumoursConfig.GROUP, "backtoback", backToBackState);
        this.backToBackState = backToBackState;

        // If the user changed the current status, put a message in chat
        if (isFromUserChoice && config.backToBackMessage()) {
            Color color = backToBackState == BackToBackState.ENABLED ? Color.GREEN : Color.RED;
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Back-to-back: " + ColorUtil.wrapWithColorTag(backToBackState.getNiceName(), color), "");
        }
    }

    /**
     * Sets whether the user has completed their current rumour
     */
    public void setHunterRumourState(boolean hasFinishedCurrentRumour) {
        configManager.setRSProfileConfiguration(HunterRumoursConfig.GROUP, "current.rumour.finished", hasFinishedCurrentRumour);
        this.currentRumourFinished = hasFinishedCurrentRumour;
    }

    /**
     * Gets whether the user has completed their current rumour
     */
    public boolean getHunterRumourState() {
        return this.currentRumourFinished;
    }

    /**
     * Sets the number of rumours completed by the player
     */
    public void setNumRumoursCompleted(int numRumoursCompleted) {
        configManager.setRSProfileConfiguration(HunterRumoursConfig.GROUP, "rumours.completed", numRumoursCompleted);
        this.numRumoursCompleted = numRumoursCompleted;
    }

    /**
     * Sets the Hunter whose Rumour the user is currently assigned
     */
    public void setCurrentHunter(Hunter hunter) {
        configManager.setRSProfileConfiguration(HunterRumoursConfig.GROUP, "current.hunter", hunter);
        currentHunter = hunter;
    }

    /**
     * Sets the current Rumour for the given Hunter, even if they're not the user's current Hunter
     */
    public void setHunterRumour(Hunter hunter, Rumour rumour) {
        hunterRumours.put(hunter, rumour);
        configManager.setRSProfileConfiguration(HunterRumoursConfig.GROUP, "hunter." + hunter.getNpcId(), rumour);
    }

    /**
     * Sets the user's current "detached rumour".
     * <p>
     * A detached rumour is one that we know of from the Quetzal Whistle's "Rumour" functionality,
     * but we do not know which Hunter assigned it, since the game message does not include this information.
     */
    public void setDetachedRumour(Rumour rumour) {
        currentDetachedRumour = rumour;
        configManager.setRSProfileConfiguration(HunterRumoursConfig.GROUP, "current.detached.rumour", rumour);
    }

    /**
     * @return An array of the hunters that the user has enabled via plugin config.
     */
    public Hunter[] getEnabledHunters() {
        return Arrays.stream(Hunter.allValues()).filter(this::isHunterEnabled).toArray(Hunter[]::new);
    }

    /**
     * @return True if the user has enabled the given hunter's Tier to be tracked/displayed via plugin config.
     */
    public boolean isHunterEnabled(Hunter hunter) {
        switch (hunter.getTier()) {
            case MASTER:
                return config.includeMasterHunters();
            case EXPERT:
                return config.includeExpertHunters();
            case ADEPT:
                return config.includeAdeptHunters();
            case NOVICE:
                return config.includeNoviceHunters();
            case NONE:
            default:
                return false;
        }
    }

    /**
     * @return The current back-to-back state
     */
    public BackToBackState getBackToBackState() {
        return backToBackState;
    }


    /**
     * @return The Rumour that (we think) the player is currently assigned.
     */
    public Rumour getCurrentRumour() {
        // If there's no current Hunter, defer to the detached Rumour (if present).
        if (currentHunter == Hunter.NONE) {
            return currentDetachedRumour == null ? Rumour.NONE : currentDetachedRumour;
        }

        // Otherwise, if we know the current Hunter, always use it.
        return hunterRumours.get(currentHunter);
    }

    /**
     * @return True if the player is currently located within the Hunter Burrows
     */
    public boolean isInBurrows() {
        Player local = client.getLocalPlayer();
        if (local == null) {
            return false;
        }

        WorldPoint location = local.getWorldLocation();
        if (location.getPlane() != 0) {
            return false;
        }

        int x = location.getX();
        int y = location.getY();

        return x >= 1549 && x <= 1565 && y >= 9449 && y <= 9464;
    }

    /**
     * Handles a chat message indicating that the current Rumour has been completed.
     * <p>
     * Ignores any chat messages that are not relevant.
     */
    private void handleRumourFinishedChatMessage(ChatMessage event) {
        String message = event.getMessage();
        if (event.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }

        // Ensure that this is the right chat message
        if (!Text.standardize(message).equalsIgnoreCase("You find a rare piece of the creature! You should take it back to the Hunter Guild.")) {
            return;
        }

        setHunterRumourState(true);
        refreshAllDisplays();
    }

    /**
     * Handles the chat message that occurs when the player clicks "Rumour" on their Quetzal Whistle.
     * <p>
     * Attempts to extract the current Rumour from the message.
     * <p>
     * Ignores any chat messages that are not relevant.
     */
    private void handleQuetzalWhistleChatMessage(ChatMessage event) {
        String message = event.getMessage();
        if (event.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }

        // Ensure that this is the right chat message
        if (!message.contains("Your current rumour target is")) {
            return;
        }

        // Determine which Rumour the message is referencing -- if none, bail out.
        Rumour referencedRumour = chatParser.getReferencedRumour(message);
        if (referencedRumour == Rumour.NONE) {
            return;
        }

        // If the referenced Rumour is what we already thought the Rumour was, then return early.
        if (referencedRumour == getCurrentRumour()) {
            return;
        }

        // The referenced rumour is not what we thought the current Rumour was. This means that we have a state
        // mismatch. We know the current Rumour now, but not the right Hunter. Therefore, we need to
        // save the current Rumour as "detached", and clear some state relating to the current Hunter.
        setDetachedRumour(referencedRumour);

        // If we have a notion of a current Hunter, clean that up, because we were probably wrong.
        if (currentHunter != Hunter.NONE) {
            setHunterRumour(currentHunter, Rumour.NONE);
            setCurrentHunter(Hunter.NONE);
            setHunterRumourState(false);
        }

        refreshAllDisplays();
    }


    /**
     * Handles the chat message from Guild Scribe Verity, in immediate response to the user clicking `Rumour-settings`
     * <p>
     * Attempts to extract the current Back-to-back state from the chat message.
     * <p>
     * Ignores any chat messages that are not relevant.
     */
    private void handleBackToBackChatMessage(ChatMessage event) {
        // If not in hunter master area, can't be for rumours
        if (!isInBurrows()) {
            return;
        }

        // If not NPC dialog, can't be for rumours
        if (event.getType() != ChatMessageType.DIALOG) {
            return;
        }

        // Filter for the specific message we're looking for
        if (!event.getMessage().startsWith("Guild Scribe Verity|Would you like me to filter the rumour information a"))
            return;

        // Update our back-to-back internal state with what we can glean from the message
        setBackToBackState(event.getMessage().contains("little less") ? BackToBackState.DISABLED : BackToBackState.ENABLED, false);
    }

    /**
     * Handles the chat message indicating how many Hunter Rumours you have completed
     */
    private void handleNumRumoursCompletedChatMessage(ChatMessage event) {

        // If not NPC dialog, can't be for rumours
        if (event.getType() != ChatMessageType.GAMEMESSAGE) {
            return;
        }

        // If not in Burrows, can't be a rumour-complete message
        if (!isInBurrows()) {
            return;
        }

        //  You have completed <col=ff3045>62</col> rumours for the Hunter Guild.
        String prefix = "You have completed <col=ff3045>";
        String suffix = "</col> rumours for the Hunter Guild.";
        String message = event.getMessage();

        // Filter for the specific message we're looking for
        if (!message.startsWith(prefix) || !message.endsWith(suffix)) {
            return;
        }

        // I do so much to avoid regex
        int numDigits = message.length() - (prefix.length() + suffix.length());
        String justTheDigits = message.substring(prefix.length(), prefix.length() + numDigits);
        int numRumours = Integer.parseInt(justTheDigits);

        setNumRumoursCompleted(numRumours);
    }

    /**
     * Handles a chat message from a Hunter relating to Rumours.
     * <p>
     * Attempts to figure out the state of things (which Hunter we're assigned to; which Rumour they've assigned).
     * <p>
     * Ignores any chat messages that are not relevant.
     */
    private void handleBurrowsHunterDialog(ChatMessage event) {
        String dialogMessage = event.getMessage();

        // If not in hunter master area, can't be for rumours
        if (!isInBurrows()) {
            return;
        }

        // If not NPC dialog, can't be for rumours
        if (event.getType() != ChatMessageType.DIALOG) {
            return;
        }

        // Figure out which Hunter we're speaking to -- assuming we are
        Hunter hunter = chatParser.getSpeakingHunter(dialogMessage);
        if (hunter == Hunter.NONE) {
            return;
        }

        // The chat message comes in prefixed with the NPC name and "|" -- strip that out
        String npcNamePrefix = client.getNpcDefinition(hunter.getNpcId()).getName() + "|";
        String actualMessage = dialogMessage.replace(npcNamePrefix, "").toLowerCase(Locale.ROOT);

        // Determine if it's a "rumour complete" message
        if (actualMessage.contains("would you like another rumour?")) {
            setHunterRumourState(false);
            setHunterRumour(currentHunter, Rumour.NONE);
            refreshAllDisplays();
            return;
        }

        // Determine what hunter and rumour are being talked about in this message
        Hunter dialogHunter = chatParser.getReferencedHunter(actualMessage);
        Rumour dialogRumour = chatParser.getReferencedRumour(actualMessage);

        boolean hasHunter = dialogHunter != Hunter.NONE;
        boolean hasRumour = dialogRumour != Rumour.NONE;
        boolean isNoviceReassignmentOffer = dialogMessage.toLowerCase().contains("would you prefer that one, or a new one entirely");

        // No hunter and no rumour means we ignore this dialog message
        if (!hasHunter && !hasRumour) {
            return;
        }

        // If no hunter is referenced in the dialog, then we assume the rumour is for the current speaker
        if (!hasHunter) {
            dialogHunter = hunter;
        }

        setHunterRumourState(false);
        setHunterRumour(dialogHunter, dialogRumour);

        // Set the current hunter to whoever we now know the current hunter to be.
        // This is either the hunter we're talking to (if the hunter mentioned a target but _not_ another hunter),
        // or a hunter being referenced (if the hunter mentioned both a target _and_ another hunter).
        // The one exception is if the novice hunter is offering a new reassignment offer, where he'll
        // also say a target but not another hunter (but that doesn't mean he's now the current hunter).
        if (!isNoviceReassignmentOffer) {
            setCurrentHunter(dialogHunter);
            setDetachedRumour(Rumour.NONE);
        }

        if (config.currentRumourMessage()) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Current rumour: " + ColorUtil.wrapWithColorTag(dialogRumour.getFullName(), Color.RED) + " | Current hunter: " + ColorUtil.wrapWithColorTag(currentHunter.getCommonName(), Color.RED), "");
        }

        refreshAllDisplays();
    }

    private RumourInfoBox infoBox = null;

    private void refreshAllDisplays() {
        npcOverlayService.rebuild();
        handleInfoBox();
        handleWorldMap();
    }

    /**
     * Manages the InfoBox for the current Rumour -- adds/removes as necessary
     */
    private void handleInfoBox() {
        if (infoBox != null) {
            infoBoxManager.removeInfoBox(infoBox);
            infoBox = null;
        }

        if (!config.showInfoBox()) {
            return;
        }

        var rumour = getCurrentRumour();
        if (rumour == Rumour.NONE) {
            return;
        }

        infoBox = new RumourInfoBox(rumour, this, itemManager);
        infoBoxManager.addInfoBox(infoBox);
    }

    /**
     * Manages the World Map Locations -- adds/removes world map points as necessary
     */
    private void handleWorldMap() {
        for (HunterRumourWorldMapPoint location : currentMapPoints) {
            worldMapPointManager.remove(location);
        }
        currentMapPoints.clear();

        if (!config.showWorldMapLocations()) {
            return;
        }

        var rumour = getCurrentRumour();
        if (rumour == Rumour.NONE) {
            return;
        }

        Set<RumourLocation> locations = config.compactWorldMap()
                ? RumourLocation.getCollapsedLocationsForRumour(rumour)
                : RumourLocation.getLocationsForRumour(rumour);

        for (RumourLocation location :
                locations) {
            HunterRumourWorldMapPoint worldMapPoint = new HunterRumourWorldMapPoint(location.getWorldPoint(), itemManager, location);
            currentMapPoints.add(worldMapPoint);
            worldMapPointManager.add(worldMapPoint);
        }
    }

    /**
     * Callback registered with npcOverlayService -- determines if an NPC should be highlighted, and if so, how.
     * <p>
     * Used to highlight Hunters and the current Hunter Target.
     */
    private HighlightedNpc highlighterFn(NPC npc) {
        Hunter hunter = Hunter.fromNpcId(npc.getId());

        // Highlight the current Hunter if relevant
        if (hunter != Hunter.NONE && isHunterEnabled(hunter)) {
            Rumour hunterRumour = hunterRumours.get(hunter);
            boolean isUnknown = hunterRumour == Rumour.NONE;
            boolean isKnown = !isUnknown;
            boolean isCurrent = hunter == currentHunter;

            Color highlightColor = Color.WHITE;
            if (isCurrent && config.highlightCurrentHunter()) {
                highlightColor = config.currentHunterHighlightColor();
            } else if (isKnown && config.highlightKnownHunters()) {
                highlightColor = config.knownHunterHighlightColor();
            } else if (isUnknown && config.highlightUnknownHunters()) {
                highlightColor = config.unknownHunterHighlightColor();
            } else {
                return null;
            }

            return HighlightedNpc
                    .builder()
                    .npc(npc)
                    .highlightColor(highlightColor)
                    .borderWidth(2)
                    .outline(true)
                    .build();
        }

        // Highlight Rumour Target (hunter creature) if relevant
        if (config.highlightHunterNPCs()) {
            Rumour currentRumour = getCurrentRumour();
            if (currentRumour == Rumour.NONE
                    || currentRumour.getNpcId() == 0
                    || currentRumourFinished
                    || npc.getId() != currentRumour.getNpcId()
            ) {
                return null;
            }

            return HighlightedNpc.builder()
                    .npc(npc)
                    .highlightColor(config.hunterNPCHighlightColor())
                    .borderWidth(2)
                    .outline(true)
                    .build();
        }

        return null;
    }

    /**
     * Loads all state (current hunter, rumours, etc.) from config
     */
    private void loadFromConfig() {
        // Load number of Rumours completed
        Integer loadedNumRumours = configManager.getRSProfileConfiguration(HunterRumoursConfig.GROUP, "rumours.completed", Integer.class);
        if (loadedNumRumours == null) {
            loadedNumRumours = 0;
        }
        this.numRumoursCompleted = loadedNumRumours;

        // Load current hunter
        Hunter loadedCurrentHunter = configManager.getRSProfileConfiguration("hunterrumours", "current.hunter", Hunter.class);
        if (loadedCurrentHunter == null) {
            loadedCurrentHunter = Hunter.NONE;
        }

        this.currentHunter = loadedCurrentHunter;

        // Load all hunter rumours
        for (var hunter : Hunter.allValues()) {
            Rumour rumour = configManager.getRSProfileConfiguration("hunterrumours", "hunter." + hunter.getNpcId(), Rumour.class);
            if (rumour == null) {
                rumour = Rumour.NONE;
            }

            hunterRumours.put(hunter, rumour);
        }

        // Load detached rumour
        Rumour loadedDetachedRumour = configManager.getRSProfileConfiguration(HunterRumoursConfig.GROUP, "current.detached.rumour", Rumour.class);
        if (loadedDetachedRumour == null) {
            loadedDetachedRumour = Rumour.NONE;
        }
        currentDetachedRumour = loadedDetachedRumour;

        // Load has finished current rumour
        try {
            currentRumourFinished = configManager.getRSProfileConfiguration(HunterRumoursConfig.GROUP, "current.rumour.finished", boolean.class);
        } catch (NullPointerException ex) {
            // Catch null pointer execption and set to false so it doesn't happen again
            setHunterRumourState(false);
        }

        // Load back-to-back state
        BackToBackState loadedState = configManager.getRSProfileConfiguration(HunterRumoursConfig.GROUP, "backtoback", BackToBackState.class);
        if (loadedState == null) {
            loadedState = BackToBackState.UNKNOWN;
        }
        this.backToBackState = loadedState;

        refreshAllDisplays();
    }

    /**
     * Wipes plugin config and in-memory game state
     */
    private void resetConfig() {
        configManager.unsetRSProfileConfiguration(HunterRumoursConfig.GROUP, "current.hunter");
        configManager.unsetRSProfileConfiguration(HunterRumoursConfig.GROUP, "current.detached.rumour");
        configManager.unsetRSProfileConfiguration(HunterRumoursConfig.GROUP, "current.rumour.finished");
        configManager.unsetRSProfileConfiguration(HunterRumoursConfig.GROUP, "backtoback");

        for (var hunter : Hunter.allValues()) {
            configManager.unsetRSProfileConfiguration("hunterrumours", "hunter." + hunter.getNpcId());
        }

        resetParams();

        // Reset overlay
        refreshAllDisplays();
    }

    /**
     * Resets internal game state. Doesn't touch persistent config.
     */
    private void resetParams() {
        for (var hunter : hunterRumours.keySet()) {
            hunterRumours.replace(hunter, Rumour.NONE);
        }

        currentHunter = Hunter.NONE;
        currentDetachedRumour = Rumour.NONE;
        backToBackState = BackToBackState.UNKNOWN;
        hasFullHunterKit = false;

        refreshAllDisplays();
    }
}
