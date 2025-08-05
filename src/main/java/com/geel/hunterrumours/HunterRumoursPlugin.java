package com.geel.hunterrumours;

import com.geel.hunterrumours.enums.*;
import com.google.errorprone.annotations.Var;
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
import net.runelite.api.widgets.Widget;
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
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.awt.*;
import java.util.*;

@PluginDescriptor(
        name = "Hunter Rumours",
        description = "Tracks your current hunter rumour, as well as the saved rumours of all masters",
        tags = {"hunter", "hunters", "guild", "rumour", "rumor", "contract", "task", "varlamore"}
)
@Slf4j
public class HunterRumoursPlugin extends Plugin {
    // Varbit value corresponding to the Tier-1 relic Animal Wrangler.
    // This is used because this relic gives double hunter XP for chinchompas, which we need to account for to track kills.
    public static int RELIC_ANIMAL_WRANGLER = 3;

    public static final Map<Hunter, Rumour> hunterRumours = new HashMap<>() {
        {
            for (var hunter : Hunter.allValues()) {
                put(hunter, Rumour.NONE);
            }
        }
    };

    public Hunter currentHunter = Hunter.NONE;
    private boolean currentRumourFinished = false;
    private BackToBackState backToBackState = BackToBackState.UNKNOWN;
    private final Set<HunterRumourWorldMapPoint> currentMapPoints = new HashSet<>();
    private int previousHunterExp = -1; // Tracks Hunter experience -- used to detect XP drops indicating a creature was caught

    @Getter
    private int hunterKitItems = 0;

    @Inject
    @Getter
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
    private int latestInteractionTime = -1;

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

        removeInfoBox();
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
        clientThread.invoke(this::refreshAllDisplays);
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

        var items = 0;

        if (isHead) {
            items++;
        }

        if (isTop) {
            items++;
        }

        if (isLegs) {
            items++;
        }

        if (isBoots) {
            items++;
        }

        if (items != hunterKitItems) {
            hunterKitItems = items;
            if (items > 0) {
                updateLatestInteractionTime();
            }
            refreshAllDisplays();
        }
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        if (event.getGroupId() == InterfaceID.FAIRY_RING_PANEL && config.autoJumpFairyring()) {
            clientThread.invokeLater(this::handleFairyRingPanel);
        }

        if (event.getGroupId() == InterfaceID.DIALOG_OPTION && isInBurrows()) {
            clientThread.invokeLater(this::handleBackToBackDialog);
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        handleBurrowsHunterDialog(event);
        handleQuetzalWhistleChatMessage(event);
        handleBackToBackChatMessage(event);

        // Handle the "rumour finished" chat message at the END of the current tick.
        // This is to ensure that we process the caught creature XP drop and increment the caught count
        // before we display stats about the rumour on completion.
        // Without this, we (sometimes?) handle the rumour finished before we handle the creature being caught,
        // causing us to show incorrect statistics.
        clientThread.invokeAtTickEnd(() -> {
            handleRumourFinishedChatMessage(event);
        });
    }

    @Subscribe
    protected void onStatChanged(StatChanged event) {
        if (event.getSkill() != Skill.HUNTER) {
            return;
        }

        final int currentXp = event.getXp();

        // If previous XP is -1, just update to the current XP.
        if (previousHunterExp == -1) {
            previousHunterExp = currentXp;
            return;
        }

        int xpDiff = (currentXp - previousHunterExp) / getLeaguesXpMultiplier();
        if (xpDiff <= 0) {
            return;
        }

        // RAGING ECHOES LEAGUE: We need to account for the possibility that the player just caught a chinchompa
        // while using the Animal Wrangler relic, which will give them yet double the XP.
        // This is a bit gross. I write good code in my actual job.
        // Mostly.
        if (hasDoubleChinchompaExperience()) {
            final int preChincompaXpDiff = xpDiff;

            if (Arrays.stream(Creature.GREY_CHINCHOMPA.getPossibleXpDrops()).anyMatch(possibleXpDrop -> possibleXpDrop == (preChincompaXpDiff / 2))
                    || Arrays.stream(Creature.RED_CHINCHOMPA.getPossibleXpDrops()).anyMatch(possibleXpDrop -> possibleXpDrop == (preChincompaXpDiff / 2))) {
                // If we just caught a chincompa, just divide xpDiff by 2 here so that the below code will correctly identify
                // the catch as a chinchompa. Smart moves from a smart engineer (me).
                xpDiff /= 2;
            }
        }

        // Create a final int because Java requires that for lambda captures
        final int finalXpDiff = xpDiff;

        // Find the creature that corresponds to the XP drop and mark them as being fucking dead
        if (Arrays.stream(getCurrentRumour().getTargetCreature().getPossibleXpDrops()).anyMatch(possibleXpDrop -> possibleXpDrop == finalXpDiff)) {
            incrementCaughtCreatures();
            refreshAllDisplays();
        }

        previousHunterExp = currentXp;
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        handleInfoBox();

        if (!shouldWorldMapLocationsBeShown()) {
            for (HunterRumourWorldMapPoint location : currentMapPoints) {
                worldMapPointManager.remove(location);
            }

            currentMapPoints.clear();
        }
    }

    /**
     * Sets the back-to-back state in memory and in config.
     * <p>
     * If this is due to a user's choice (eg they changed it), puts a message in chat indicating their current back-to-back status
     */
    public void setBackToBackState(BackToBackState backToBackState, boolean isFromUserChoice) {
        updateLatestInteractionTime();
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
        updateLatestInteractionTime();
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
     * Sets the Hunter whose Rumour the user is currently assigned
     */
    public void setCurrentHunter(Hunter hunter) {
        updateLatestInteractionTime();
        configManager.setRSProfileConfiguration(HunterRumoursConfig.GROUP, "current.hunter", hunter);
        currentHunter = hunter;
    }

    /**
     * Sets the current Rumour for the given Hunter, even if they're not the user's current Hunter
     */
    public void setHunterRumour(Hunter hunter, Rumour rumour) {
        updateLatestInteractionTime();
        hunterRumours.put(hunter, rumour);
        configManager.setRSProfileConfiguration(HunterRumoursConfig.GROUP, "hunter." + hunter.getNpcId(), rumour);
    }

    /**
     * @return An array of the hunters that the user has enabled via plugin config.
     */
    public Hunter[] getEnabledHunters() {
        return Arrays.stream(Hunter.allValues()).filter(this::isHunterEnabled).toArray(Hunter[]::new);
    }

    /**
     * Increments the currently caught creatures by one and sets the value.
     */
    public void incrementCaughtCreatures() {
        updateLatestInteractionTime();
        setCaughtCreatures(getCaughtRumourCreatures() + 1);
    }

    /**
     * Sets the currently caught creatures.
     *
     * @param caughtCreatures the amount of creatures currently caught
     */
    private void setCaughtCreatures(int caughtCreatures) {
        configManager.setRSProfileConfiguration(HunterRumoursConfig.GROUP, "current.rumour.caught", caughtCreatures);
    }

    /**
     * @return The current amount of hunter creatures you've caught.
     */
    public int getCaughtRumourCreatures() {
        try {
            final int caughtCreatures = configManager.getRSProfileConfiguration(HunterRumoursConfig.GROUP, "current.rumour.caught", int.class);
            return caughtCreatures;
        } catch (NullPointerException ex) {
            return 0;
        }
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
        // If there's no current Hunter, we don't have a rumour??? obviously?????
        if (currentHunter == Hunter.NONE) {
            return Rumour.NONE;
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

    private int getLeaguesXpMultiplier() {
        // This code is specific to Raging Echoes league, so in case I don't patch this before future leagues, don't consider them
        // Make sure current date is before January 23, 2025
        if (System.currentTimeMillis() > (1737608400L * 1000L)) {
            return 1;
        }

        var tier1 = client.getVarbitValue(Varbits.LEAGUE_RELIC_1);
        var tier2 = client.getVarbitValue(Varbits.LEAGUE_RELIC_2);
        var tier5 = client.getVarbitValue(Varbits.LEAGUE_RELIC_5);
        var tier7 = client.getVarbitValue(Varbits.LEAGUE_RELIC_7);

        if (tier1 == 0) {
            return 1; // No relic -- 1x
        }

        if (tier2 == 0) {
            return 5; // Only t1 relic -- 5x
        }

        if (tier5 == 0) {
            return 8; // Only t1 and t2 relics -- 8x
        }

        if (tier7 == 0) {
            return 12; // Only t1, t2, and t5 relics -- 10x
        }

        return 16; // Has t7 relic -- 16x
    }

    private boolean hasDoubleChinchompaExperience() {
        return client.getVarbitValue(Varbits.LEAGUE_RELIC_1) == RELIC_ANIMAL_WRANGLER;
    }

    /**
     * Called when the fairy ring dialog is opened.
     * Responsible for scrolling to the relevant rumour code and highlighting it, if relevant.
     */
    private void handleFairyRingPanel() {
        // TODO: Look into fairy ring plugin and steal its scroll code, which is allegedly much simpler than mine.

        // Ensure we have a current, non-completed rumour
        var currentRumour = getCurrentRumour();
        if (currentRumour == Rumour.NONE) {
            return;
        }

        if (getHunterRumourState()) {
            return;
        }

        // Find the first-declared location for this rumour
        var locationGroups = RumourLocation.getGroupedLocationsForRumour(currentRumour);
        var firstLocationWithFairyRing = locationGroups.filter(g -> g.getValue().get(0).getFairyRingCode().length() == 3).findFirst();

        if (firstLocationWithFairyRing.isEmpty()) {
            return;
        }

        var fairyRingCode = firstLocationWithFairyRing.get().getValue().get(0).getFairyRingCode();

        // If we haven't interacted with hunter rumours in the last 2 minutes, bail out
        if (!interactedRecently(200)) {
            return;
        }

        // Find all the necessary widgets
        Widget panelList = client.getWidget(ComponentID.FAIRY_RING_PANEL_LIST);
        Widget favoritesList = client.getWidget(ComponentID.FAIRY_RING_PANEL_FAVORITES);
        Widget scrollBar = client.getWidget(ComponentID.FAIRY_RING_PANEL_SCROLLBAR);

        if (panelList == null || scrollBar == null || favoritesList == null) {
            return;
        }

        Widget scrollBarContainer = null, scrollBarHandle = null, scrollBarHandleTop = null,
                scrollBarHandleBottom = null, scrollBarUpButton = null, scrollBarDownButton = null;
        for (var scrollChild : scrollBar.getDynamicChildren()) {
            // This is stupid
            switch (scrollChild.getSpriteId()) {
                case SpriteID.SCROLLBAR_ARROW_DOWN:
                    scrollBarDownButton = scrollChild;
                    break;
                case SpriteID.SCROLLBAR_ARROW_UP:
                    scrollBarUpButton = scrollChild;
                    break;
                case SpriteID.SCROLLBAR_THUMB_MIDDLE:
                    scrollBarHandle = scrollChild;
                    break;
                case SpriteID.SCROLLBAR_THUMB_TOP:
                    scrollBarHandleTop = scrollChild;
                    break;
                case SpriteID.SCROLLBAR_THUMB_BOTTOM:
                    scrollBarHandleBottom = scrollChild;
                    break;
                case SpriteID.SCROLLBAR_THUMB_MIDDLE_DARK:
                    scrollBarContainer = scrollChild; // Weird reuse of a sprite for the inset slider container
                    break;
            }
        }

        // Lol
        if (scrollBarContainer == null || scrollBarHandle == null || scrollBarHandleTop == null
                || scrollBarHandleBottom == null || scrollBarUpButton == null || scrollBarDownButton == null) {
            return;
        }

        // Construct a list of all widgets that are the fairy ring code texts
        // Yes, this is slightly inefficient with memory. You should see what other plugins do!
        var codeWidgets = new ArrayList<Widget>();

        // Add in all children from the big list
        codeWidgets.addAll(Arrays.asList(panelList.getDynamicChildren()));

        // Add in all children from the favorites list
        codeWidgets.addAll(Arrays.asList(favoritesList.getStaticChildren()));

        // Find the widget corresponding to the fairy ring code
        Widget foundCodeWidget = null;
        for (var codeWidget : codeWidgets) {
            if (!codeWidget.getText().replace(" ", "").contentEquals(fairyRingCode)) {
                continue;
            }

            foundCodeWidget = codeWidget;
            break;
        }

        // If no widget found, bail out
        if (foundCodeWidget == null) {
            return;
        }

        // Scroll to the code entry and highlight it
        int panelScrollY = Math.min(foundCodeWidget.getRelativeY(), panelList.getScrollHeight() - panelList.getHeight());
        panelList.setScrollY(panelScrollY);
        panelList.revalidateScroll();
        foundCodeWidget.setTextColor(0x00FF00);
        foundCodeWidget.setText("(Rumour) " + foundCodeWidget.getText());

        // Determine scrollbar placement -- has to be done manually, I think, because just setting the panel
        // scroll value doesn't actually adjust its scrollbar (which makes sense)
        double codeEntryPlacement = (double) foundCodeWidget.getRelativeY() / (double) panelList.getScrollHeight();
        int maxHandleY = scrollBarContainer.getHeight() - 4; // Not sure where the 4 comes from... just padding?
        int handleY = (int) ((double) scrollBarContainer.getHeight() * codeEntryPlacement) + scrollBarUpButton.getHeight();
        handleY = Math.min(handleY, maxHandleY);
        int handleBottomY = handleY + (scrollBarHandle.getHeight() - scrollBarHandleBottom.getHeight());

        scrollBarHandle.setOriginalY(handleY);
        scrollBarHandleTop.setOriginalY(handleY);
        scrollBarHandleBottom.setOriginalY(handleBottomY);
        scrollBarHandle.revalidateScroll();
        scrollBarHandleTop.revalidateScroll();
        scrollBarHandleBottom.revalidateScroll();
    }

    /**
     * Called when the dialog options to enable/disable back-to-back rumours are shown.
     * Hooks in to dialog choice and updates state accordingly.
     */
    private void handleBackToBackDialog() {
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

        if ((!isPromptingToDisable && !isPromptingToEnable)) {
            return;
        }

        BackToBackState ifYesState = isPromptingToEnable ? BackToBackState.ENABLED : BackToBackState.DISABLED;

        // HACK(ish): if the user is 2quick2fast, they can select the option before this callback fires,
        // causing us to fail to attach a listener / cause the text to not be what we expect. So handle
        // that here.
        if (option1.getText().equals("Please wait...")) {
            setBackToBackState(ifYesState, true);
            return;
        }

        // Options should be "Yes" and "No"
        if (!option1.getText().equals("Yes") || !option2.getText().equals("No")) {
            return;
        }

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

        if (config.endOfRumourMessage()) {
            final int caughtCreatures = getCaughtRumourCreatures();
            final int pityThreshold = getCurrentRumour().getTrap().calculatePityRateForItems(hunterKitItems);
            final int percentage = 100 * caughtCreatures / pityThreshold;

            Color color;
            if (percentage >= 75) {
                color = config.endOfRumourMessageUnluckyColor();
            } else if (percentage >= 50) {
                color = config.endOfRumourMessageNormalColor();
            } else {
                color = config.endOfRumourMessageLuckyColor();
            }

            client.addChatMessage(ChatMessageType.GAMEMESSAGE,
                    "Hunter Rumours",
                    "Hunter Rumours: Rumour finished " +
                            ColorUtil.wrapWithColorTag(String.valueOf(percentage), color) +
                            "% of the way towards pity rate of " + pityThreshold,
                    "");
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

        // Update last interaction time -- checking the whistle indicates a clear intent to participate
        // in Hunter Rumours.
        updateLatestInteractionTime();

        // Determine which Rumour the message is referencing -- if none, bail out.
        Rumour referencedRumour = chatParser.getReferencedRumour(message);
        if (referencedRumour == Rumour.NONE) {
            return;
        }

        // Determine which Hunter the message is referencing -- if none, bail out.
        Hunter referencedHunter = chatParser.getReferencedHunter(message);
        if (referencedHunter == Hunter.NONE) {
            return;
        }

        // If the Hunter or Rumour is different than what we thought it was, update state.
        if (referencedRumour != getCurrentRumour() || referencedHunter != currentHunter) {
            // TODO: too much manual state shit, abstract this away
            setHunterRumour(referencedHunter, referencedRumour);
            setCurrentHunter(referencedHunter);
            setHunterRumourState(false);
            setCaughtCreatures(0);
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
        if (!event.getMessage().startsWith("Guild Scribe Verity|Would you like me to filter the rumour information a")) {
            return;
        }

        // Update our back-to-back internal state with what we can glean from the message
        setBackToBackState(event.getMessage().contains("little less") ? BackToBackState.DISABLED : BackToBackState.ENABLED, false);
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
        if (actualMessage.contains("would you like another rumour?") ||
                actualMessage.contains("here's your reward.") ||
                actualMessage.contains("another one done?")) {
            setHunterRumour(currentHunter, Rumour.NONE);
            setHunterRumourState(false);
            setCaughtCreatures(0);
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
        }

        if (config.currentRumourMessage()) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Current rumour: " + ColorUtil.wrapWithColorTag(dialogRumour.getFullName(), Color.RED) + " | Current hunter: " + ColorUtil.wrapWithColorTag(currentHunter.getCommonName(), Color.RED), "");
        }

        refreshAllDisplays();
    }

    private RumourInfoBox infoBox = null;

    private void refreshAllDisplays() {
        npcOverlayService.rebuild();
        handleWorldMap();

        // Remove the infobox -- then re-enable it if necessary.
        removeInfoBox();
        handleInfoBox();
    }

    /**
     * Manages the InfoBox for the current Rumour -- adds/removes as necessary
     */
    private void handleInfoBox() {
        var isShowing = infoBox != null;
        var shouldShow = shouldInfoBoxBeShown();

        if (isShowing && !shouldShow) {
            removeInfoBox();
        } else if (shouldShow && !isShowing) {
            infoBox = new RumourInfoBox(getCurrentRumour(), this, itemManager);
            infoBoxManager.addInfoBox(infoBox);
        }
    }

    private void removeInfoBox() {
        if (infoBox != null) {
            infoBoxManager.removeInfoBox(infoBox);
            infoBox = null;
        }
    }

    /**
     * Manages the World Map Locations -- adds/removes world map points as necessary
     */
    private void handleWorldMap() {
        for (HunterRumourWorldMapPoint location : currentMapPoints) {
            worldMapPointManager.remove(location);
        }

        currentMapPoints.clear();

        if (!shouldWorldMapLocationsBeShown()) {
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
        if (config.highlightHunterNPCs() != HunterRumoursConfig.HighlightType.NONE) {
            Rumour currentRumour = getCurrentRumour();
            if (currentRumour == Rumour.NONE
                    || currentRumour.getTargetCreature().getNpcId() == 0
                    || currentRumourFinished
                    || npc.getId() != currentRumour.getTargetCreature().getNpcId()
            ) {
                return null;
            }

            HighlightedNpc.HighlightedNpcBuilder highlightedNpcBuilder = HighlightedNpc.builder()
                    .npc(npc)
                    .highlightColor(config.hunterNPCHighlightColor())
                    .borderWidth(2);

            switch (config.highlightHunterNPCs()) {
                case TILE:
                    highlightedNpcBuilder.tile(true);
                    break;
                case OUTLINE:
                    highlightedNpcBuilder.outline(true);
                    break;
                case BOTH:
                    highlightedNpcBuilder.tile(true);
                    highlightedNpcBuilder.outline(true);
            }

            return highlightedNpcBuilder.build();
        }

        return null;
    }

    /**
     * Loads all state (current hunter, rumours, etc.) from config
     */
    private void loadFromConfig() {
        // Fetch current hunter XP
        int hunterExperience = client.getSkillExperience(Skill.HUNTER);
        if (hunterExperience > 0) {
            previousHunterExp = hunterExperience;
        }

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

        // Load has finished current rumour
        try {
            currentRumourFinished = configManager.getRSProfileConfiguration(HunterRumoursConfig.GROUP, "current.rumour.finished", boolean.class);
        } catch (NullPointerException ex) {
            currentRumourFinished = false;
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
     * Records the current tick as the "latest interaction time" -- used to track how long it's been since the player has engaged in Hunter Rumour-related behavior
     */
    private void updateLatestInteractionTime() {
        latestInteractionTime = client.getTickCount();
    }

    /**
     * Computes whether the player has interacted with Hunter Rumours in the last N ticks
     */
    private boolean interactedRecently(int tickThreshold) {
        return latestInteractionTime != -1 && (client.getTickCount() - latestInteractionTime <= tickThreshold);
    }

    /**
     * Checks the configurations of the info box, returns true if the info box should be disabled.
     */
    private boolean shouldInfoBoxBeShown() {
        // If infobox isn't enabled, it obviously should not be shown
        if (!config.showInfoBox()) {
            return false;
        }

        // If "force info box" is enabled, then the infobox should always be shown (if infobox itself is enabled)
        if (config.forceShowInfoBox()) {
            return true;
        }

        // If we have no active rumour, don't show the info box.
        var rumour = getCurrentRumour();
        if (rumour == Rumour.NONE) {
            return false;
        }

        // Infobox should be disabled if it's been long enough since the last interaction time
        return interactedRecently(config.infoBoxDisableTimer() * 100);
    }

    /**
     * Checks the configurations of the world map locations, returns true if the world map locations should be disabled.
     */
    private boolean shouldWorldMapLocationsBeShown() {
        // If world map locations aren't enabled, obviously they should not be enabled!
        if (!config.showWorldMapLocations()) {
            return false;
        }

        // If "force show world map locations" is enabled, then we should never disable world map locations.
        if (config.forceShowWorldMapLocations()) {
            return true;
        }

        // World map locations should be disabled if it's been long enough since the last interaction time
        return interactedRecently(config.worldMapLocationsDisableTimer() * 100);
    }

    /**
     * Wipes plugin config and in-memory game state
     */
    private void resetConfig() {
        configManager.unsetRSProfileConfiguration(HunterRumoursConfig.GROUP, "current.hunter");
        configManager.unsetRSProfileConfiguration(HunterRumoursConfig.GROUP, "current.rumour.finished");
        configManager.unsetRSProfileConfiguration(HunterRumoursConfig.GROUP, "backtoback");
        configManager.unsetRSProfileConfiguration(HunterRumoursConfig.GROUP, "current.rumour.caught");

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
        backToBackState = BackToBackState.UNKNOWN;
        hunterKitItems = 0;

        refreshAllDisplays();
    }
}
