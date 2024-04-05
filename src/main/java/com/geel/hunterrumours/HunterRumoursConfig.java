package com.geel.hunterrumours;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.*;

@ConfigGroup(HunterRumoursConfig.GROUP)
public interface HunterRumoursConfig extends Config
{
	String GROUP = "hunterrumours";

	@ConfigSection(
		name = "Hunter Tiers",
		description = "The tiers of hunters that are enabled",
		position = 3
	)
	String tiersSection = "tiersSection";
	@ConfigSection(
		name = "Highlights",
		description = "Highlights for Hunters and Hunter Targets",
		position = 4
	)
	String highlightSection = "highlightSection";

	@ConfigItem(
		position = 0,
		keyName = "showOverlay",
		name = "Show Hunter Guild Overlay",
		description = "Show an overlay that lists the current hunter guild rumours when in the burrows"
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		position = 1,
		keyName = "showRumourInfoBox",
		name = "Show Rumour Infobox",
		description = "Whether an infobox containing your current rumour target should be displayed"
	)
	default boolean showInfoBox()
	{
		return true;
	}

	@ConfigItem(
		position = 2,
		keyName = "showWorldMapLocations",
		name = "Show World Map Locations",
		description = "Whether the locations of your current rumour should show up on your world map"
	)
	default boolean showWorldMapLocations()
	{
		return true;
	}

	@ConfigItem(
		position = 0,
		keyName = "includeMasterHunters",
		name = "Include Master Hunters",
		description = "Include master hunters in highlighting and info reports",
		section = tiersSection
	)
	default boolean includeMasterHunters()
	{
		return true;
	}

	@ConfigItem(
		position = 1,
		keyName = "includeExpertHunters",
		name = "Include Expert Hunters",
		description = "Include Expert hunters in highlighting and info reports",
		section = tiersSection
	)
	default boolean includeExpertHunters()
	{
		return true;
	}

	@ConfigItem(
		position = 2,
		keyName = "includeAdeptHunters",
		name = "Include Adept Hunters",
		description = "Include adept hunters in highlighting and info reports",
		section = tiersSection
	)
	default boolean includeAdeptHunters()
	{
		return true;
	}

	@ConfigItem(
		position = 3,
		keyName = "includeNoviceHunters",
		name = "Include Novice Hunters",
		description = "Include novice hunters in highlighting and info reports",
		section = tiersSection
	)
	default boolean includeNoviceHunters()
	{
		return true;
	}

	@ConfigItem(
		position = 0,
		keyName = "highlightCurrentHunter",
		name = "Highlight current hunter",
		description = "Whether the hunter that assigned your current task should be highlighted",
		section = highlightSection
	)
	default boolean highlightCurrentHunter()
	{
		return true;
	}

	@ConfigItem(
		position = 1,
		keyName = "highlightKnownHunters",
		name = "Highlight known hunters",
		description = "Whether hunters whose rumour is known should be highlighted",
		section = highlightSection
	)
	default boolean highlightKnownHunters()
	{
		return true;
	}

	@ConfigItem(
		position = 2,
		keyName = "highlightUnknownHunters",
		name = "Highlight unknown hunters",
		description = "Whether hunters whose rumour is known should be highlighted",
		section = highlightSection
	)
	default boolean highlightUnknownHunters()
	{
		return true;
	}

	@ConfigItem(
		position = 3,
		keyName = "highlightHunterNPCs",
		name = "Highlight Hunter NPCs",
		description = "Whether your current rumour target should be highlighted",
		section = highlightSection
	)
	default boolean highlightHunterNPCs()
	{
		return true;
	}

	@ConfigItem(
		position = 4,
		keyName = "currentHunterHighlightColor",
		name = "Current hunter highlight color",
		description = "Highlight color for hunter who assigned your current rumour",
		section = highlightSection
	)
	default Color currentHunterHighlightColor()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		position = 5,
		keyName = "knownHunterHighlightColor",
		name = "Known hunter highlight color",
		description = "Highlight color for hunters whose rumours are known",
		section = highlightSection
	)
	default Color knownHunterHighlightColor()
	{
		return Color.YELLOW;
	}

	@ConfigItem(
		position = 6,
		keyName = "unknownHunterHighlightColor",
		name = "Unknown hunter highlight color",
		description = "Highlight color for hunters whose rumours are not known",
		section = highlightSection
	)
	default Color unknownHunterHighlightColor()
	{
		return Color.RED;
	}

	@ConfigItem(
		position = 7,
		keyName = "hunterNPCHighlightColor",
		name = "Hunter NPC Highlight Color",
		description = "Highlight color for Hunter targets",
		section = highlightSection
	)
	default Color hunterNPCHighlightColor()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		position = 7,
		keyName = "inventoryTag",
		name = "Rumour Item Inventory Tag",
		description = "Highlights the rumour items in your inventory"
	)
	default boolean inventoryTags()
	{
		return true;
	}

	@ConfigItem(
		position = 8,
		keyName = "lootbeams",
		name = "Rumour Item Lootbeams",
		description = "Shows a lootbeam whenever your current rumour item is dropped on the ground"
	)
	default boolean lootbeams()
	{
		return true;
	}
}
