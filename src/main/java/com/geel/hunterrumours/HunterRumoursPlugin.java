package com.geel.hunterrumours;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
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
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.*;
import java.util.*;

@PluginDescriptor(
	name = "Hunter Rumours",
	description = "Tracks your current hunter rumour, as well as the saved rumours of all masters",
	tags = {"hunter", "hunters", "guild", "rumour", "rumor", "contract", "task", "varlamore"}
)
@Slf4j
public class HunterRumoursPlugin extends Plugin
{
	public static final Map<Hunter, Rumour> hunterRumours = new HashMap<>()
	{
		{
			for (var hunter : Hunter.allValues())
			{
				put(hunter, Rumour.NONE);
			}
		}
	};

	public Hunter currentHunter = Hunter.NONE;
	public Rumour currentDetachedRumour = Rumour.NONE;

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
	private WorldMapPointManager worldMapPointManager;

	private BufferedImage mapArrow;
	private final Set<HunterRumourWorldMapPoint> currentMapPoints = new HashSet<>();

	@Provides
	HunterRumoursConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HunterRumoursConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
		npcOverlayService.registerHighlighter(this::highlighterFn);
		loadFromConfig();
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		npcOverlayService.unregisterHighlighter(this::highlighterFn);
		resetParams();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			loadFromConfig();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!configChanged.getGroup().equals(HunterRumoursConfig.GROUP))
		{
			return;
		}

		loadFromConfig();
		handleInfoBox();
		handleWorldMap();
		npcOverlayService.rebuild();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		handleBurrowsHunterDialog(event);
		handleQuetzalWhistleChatMessage(event);
	}

	private void handleQuetzalWhistleChatMessage(ChatMessage event)
	{
		String message = event.getMessage();

		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		if (!message.contains("Your current rumour target is"))
		{
			return;
		}

		Rumour referencedRumour = getReferencedRumour(message);
		if (referencedRumour == Rumour.NONE)
		{
			return;
		}

		if (referencedRumour != getCurrentRumour())
		{
			setDetachedRumour(referencedRumour);
		}

		// If we have a current hunter, and the current rumour disagrees with that hunter's rumour,
		// then we know that something is wrong -- reset the current hunter, and reset the rumour of the "current hunter"
		if (currentHunter == Hunter.NONE)
		{
			return;
		}

		Rumour currentRumour = hunterRumours.get(currentHunter);
		if (currentRumour == referencedRumour)
		{
			// Current rumour matches the whistle rumour -- everything's fine
			return;
		}

		// Current rumour DOES NOT match the whistle rumour -- reset things
		hunterRumours.put(currentHunter, Rumour.NONE);
		currentHunter = Hunter.NONE;
	}

	private void handleBurrowsHunterDialog(ChatMessage event)
	{
		String dialogMessage = event.getMessage();

		// If not in hunter master area, can't be for rumours
		if (!isInBurrows())
		{
			return;
		}

		// If not NPC dialog, can't be for rumours
		if (event.getType() != ChatMessageType.DIALOG)
		{
			return;
		}

		// Figure out which Hunter we're speaking to -- assuming we are
		Hunter hunter = getSpeakingHunter(dialogMessage);
		if (hunter == Hunter.NONE)
		{
			return;
		}

		String npcNamePrefix = client.getNpcDefinition(hunter.getNpcId()).getName() + "|";
		String actualMessage = dialogMessage.replace(npcNamePrefix, "").toLowerCase(Locale.ROOT);

		// Determine what hunter and rumour are being talked about in this message
		Hunter dialogHunter = getReferencedHunter(actualMessage);
		Rumour dialogRumour = getReferencedRumour(actualMessage);

		boolean hasHunter = dialogHunter != Hunter.NONE;
		boolean hasRumour = dialogRumour != Rumour.NONE;
		boolean isNoviceReassignmentOffer = dialogMessage.toLowerCase().contains("would you prefer that one, or a new one entirely");

		// No hunter and no rumour means we ignore this dialog message
		if (!hasHunter && !hasRumour)
		{
			return;
		}

		// If no hunter is referenced in the dialog, then we assume the rumour is for the current speaker
		if (!hasHunter)
		{
			dialogHunter = hunter;
		}

		setHunterRumour(dialogHunter, dialogRumour);

		// Set the current hunter to whoever we now know the current hunter to be.
		// This is either the hunter we're talking to (if the hunter mentioned a target but _not_ another hunter),
		// or a hunter being referenced (if the hunter mentioned both a target _and_ another hunter).
		// The one exception is if the novice hunter is offering a new reassignment offer, where he'll
		// also say a target but not another hunter (but that doesn't mean he's now the current hunter).
		if (!isNoviceReassignmentOffer)
		{
			setCurrentHunter(dialogHunter);
			setDetachedRumour(Rumour.NONE);
		}

		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Current rumour: " + ColorUtil.wrapWithColorTag(dialogRumour.getFullName(), Color.RED) + " | Current hunter: " + ColorUtil.wrapWithColorTag(currentHunter.getCommonName(), Color.RED), "");
		npcOverlayService.rebuild();
		handleInfoBox();
		handleWorldMap();
	}

	public Hunter[] getEnabledHunters()
	{
		return Arrays.stream(Hunter.allValues()).filter(this::isHunterEnabled).toArray(Hunter[]::new);
	}

	public void setCurrentHunter(Hunter hunter)
	{
		configManager.setRSProfileConfiguration(HunterRumoursConfig.GROUP, "current.hunter", hunter);
		currentHunter = hunter;
	}

	public void setHunterRumour(Hunter hunter, Rumour rumour)
	{
		hunterRumours.put(hunter, rumour);
		configManager.setRSProfileConfiguration(HunterRumoursConfig.GROUP, "hunter." + hunter.getNpcId(), rumour);
	}

	public void setDetachedRumour(Rumour rumour)
	{
		currentDetachedRumour = rumour;
		configManager.setRSProfileConfiguration(HunterRumoursConfig.GROUP, "current.detached.rumour", rumour);
	}

	public boolean isHunterEnabled(Hunter hunter)
	{
		switch (hunter.getTier())
		{
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

	private RumourInfoBox infoBox = null;

	private void handleInfoBox()
	{
		if (infoBox != null)
		{
			infoBoxManager.removeInfoBox(infoBox);
			infoBox = null;
		}

		if (!config.showInfoBox())
		{
			return;
		}

		var rumour = getCurrentRumour();
		if (rumour == Rumour.NONE)
		{
			return;
		}

		infoBox = new RumourInfoBox(rumour, this, itemManager);
		infoBoxManager.addInfoBox(infoBox);
	}

	private void handleWorldMap()
	{
		if (!config.showWorldMapLocations())
		{
			return;
		}

		for (HunterRumourWorldMapPoint location : currentMapPoints)
		{
			worldMapPointManager.remove(location);
		}
		currentMapPoints.clear();

		var rumour = getCurrentRumour();
		if (rumour == Rumour.NONE)
		{
			return;
		}

		Set<RumourLocation> locations = RumourLocation.getLocationsForRumour(rumour);
		for (RumourLocation location :
			locations)
		{
			HunterRumourWorldMapPoint worldMapPoint = new HunterRumourWorldMapPoint(location.getWorldPoint(), this, location);
			currentMapPoints.add(worldMapPoint);
			worldMapPointManager.add(worldMapPoint);
		}
	}

	private HighlightedNpc highlighterFn(NPC npc)
	{
		Hunter hunter = Hunter.fromNpcId(npc.getId());

		if (hunter != Hunter.NONE && isHunterEnabled(hunter))
		{
			Rumour hunterRumour = hunterRumours.get(hunter);
			boolean isUnknown = hunterRumour == Rumour.NONE;
			boolean isKnown = !isUnknown;
			boolean isCurrent = hunter == currentHunter;

			Color highlightColor = Color.WHITE;
			if (isCurrent && config.highlightCurrentHunter())
			{
				highlightColor = config.currentHunterHighlightColor();
			}
			else if (isKnown && config.highlightKnownHunters())
			{
				highlightColor = config.knownHunterHighlightColor();
			}
			else if (isUnknown && config.highlightUnknownHunters())
			{
				highlightColor = config.unknownHunterHighlightColor();
			}
			else
			{
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

		if (config.highlightHunterNPCs())
		{
			Rumour currentRumour = getCurrentRumour();
			if (npc.getId() != currentRumour.getNpcId())
			{
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

	BufferedImage getMapArrow()
	{
		if (mapArrow != null)
		{
			return mapArrow;
		}

		mapArrow = ImageUtil.loadImageResource(getClass(), "/util/hunter_rumour_arrow.png");
		return mapArrow;
	}

	public BufferedImage getHunterRumourImage()
	{
		return itemManager.getImage(getCurrentRumour().getItemId());
	}

	private void loadFromConfig()
	{
		// Load current hunter
		Hunter loadedCurrentHunter = configManager.getRSProfileConfiguration("hunterrumours", "current.hunter", Hunter.class);
		if (loadedCurrentHunter == null)
		{
			loadedCurrentHunter = Hunter.NONE;
		}

		this.currentHunter = loadedCurrentHunter;

		// Load all hunter rumours
		for (var hunter : Hunter.allValues())
		{
			Rumour rumour = configManager.getRSProfileConfiguration("hunterrumours", "hunter." + hunter.getNpcId(), Rumour.class);
			if (rumour == null)
			{
				rumour = Rumour.NONE;
			}

			hunterRumours.put(hunter, rumour);
		}

		// Load detached rumour
		Rumour loadedDetachedRumour = configManager.getRSProfileConfiguration(HunterRumoursConfig.GROUP, "current.detached.rumour", Rumour.class);
		if (loadedDetachedRumour == null)
		{
			loadedDetachedRumour = Rumour.NONE;
		}
		currentDetachedRumour = loadedDetachedRumour;

		// Reset overlay
		npcOverlayService.rebuild();
		handleInfoBox();
		handleWorldMap();
	}

	public boolean isInBurrows()
	{
		Player local = client.getLocalPlayer();
		if (local == null)
		{
			return false;
		}

		WorldPoint location = local.getWorldLocation();
		if (location.getPlane() != 0)
		{
			return false;
		}

		int x = location.getX();
		int y = location.getY();

		return x >= 1549 && x <= 1565 && y >= 9449 && y <= 9464;
	}

	public Rumour getCurrentRumour()
	{
		if (currentHunter == Hunter.NONE)
		{
			return currentDetachedRumour == null ? Rumour.NONE : currentDetachedRumour;
		}

		return hunterRumours.get(currentHunter);
	}

	private Hunter getSpeakingHunter(String message)
	{
		for (Hunter hunter : Hunter.allValues())
		{
			NPCComposition npc = client.getNpcDefinition(hunter.getNpcId());
			if (message.startsWith(npc.getName() + "|"))
			{
				return hunter;
			}
		}

		return Hunter.NONE;
	}

	private Hunter getReferencedHunter(String message)
	{
		for (var hunterName : Hunter.allCommonNames())
		{
			if (!message.contains(hunterName.toLowerCase()))
			{
				continue;
			}

			return Hunter.fromCommonName(hunterName);
		}

		return Hunter.NONE;
	}

	private Rumour getReferencedRumour(String message)
	{
		for (var rumour : Rumour.allValues())
		{
			if (!message.contains(rumour.getName().toLowerCase()))
			{
				continue;
			}

			return rumour;
		}

		return Rumour.NONE;
	}

	private void resetParams()
	{
		for (var hunter : hunterRumours.keySet())
		{
			hunterRumours.replace(hunter, Rumour.NONE);
		}

		currentHunter = Hunter.NONE;
	}
}
