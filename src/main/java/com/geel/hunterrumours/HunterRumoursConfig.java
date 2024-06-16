package com.geel.hunterrumours;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup(HunterRumoursConfig.GROUP)
public interface HunterRumoursConfig extends Config {
    String GROUP = "hunterrumours";

    @ConfigSection(
            name = "Hunter Guild Display",
            description = "Configure the panel that displays information while underground in the Hunter Guild.",
            position = 0
    )
    String hunterGuildDisplaySection = "hunterGuildDisplaySection";

    @ConfigSection(
            name = "Current Rumour Infobox",
            description = "Configure the infobox that shows your current Rumour.",
            position = 1
    )
    String infoBoxSection = "infoBoxSection";

    @ConfigSection(
            name = "World Map",
            description = "Configure the hunter creature locations the plugin adds to the world map.",
            position = 2
    )
    String worldMapSection = "worldMapSection";

    @ConfigSection(
            name = "Chat Messages",
            description = "Configure various chat messages that the plugin creates.",
            position = 3
    )
    String messagesSection = "messagesSection";

    @ConfigSection(
            name = "Hunter Tiers",
            description = "The tiers of hunters that are enabled.",
            position = 4
    )
    String tiersSection = "tiersSection";

    @ConfigSection(
            name = "Highlights",
            description = "Highlights for Hunters and Hunter Targets.",
            position = 5
    )
    String highlightSection = "highlightSection";

    @ConfigItem(
            position = 0,
            keyName = "showWorldMapLocations",
            name = "Show World Map Locations",
            description = "Whether the locations of your current rumour should show up on your world map.",
            section = worldMapSection
    )
    default boolean showWorldMapLocations() {
        return true;
    }

    @ConfigItem(
            position = 1,
            keyName = "compactWorldMap",
            name = "Compact World Map Locations",
            description = "Only show 1 icon per location on the World Map.",
            section = worldMapSection
    )
    default boolean compactWorldMap() {
        return false;
    }

    @ConfigItem(
            position = 2,
            keyName = "worldMapLocationsDisableTimer",
            name = "Locations Disable Timer (minutes)",
            description = "Stops showing the locations on the World Map after a certain amount of time (in minutes) of no hunter rumour related activity.<br />Turn on 'Force Show Locations' if you want the info box to be visible at all times.",
            section = worldMapSection
    )
    @Range(min = 1)
    default int worldMapLocationsDisableTimer() {
        return 5;
    }

    @ConfigItem(
            position = 3,
            keyName = "forceShowWorldMapLocations",
            name = "Force Show Locations",
            description = "Forces the World Map Locations to be shown even after the time set above.<br />Only works if 'Show World Map Locations' is enabled.",
            section = worldMapSection
    )
    default boolean forceShowWorldMapLocations() {
        return false;
    }

    @ConfigItem(
            position = 0,
            keyName = "showOverlay",
            name = "Show Hunter Guild Info Panel",
            description = "Show an overlay that lists the current hunter guild rumours when in the burrows.",
            section = hunterGuildDisplaySection
    )
    default boolean showOverlay() {
        return true;
    }

    @ConfigItem(
            position = 1,
            keyName = "showAllHunters",
            name = "List All Hunter Rumours",
            description = "List all tracked hunters and their current rumours in the guild overlay.",
            section = hunterGuildDisplaySection
    )
    default boolean guildOverlayListHunters() {
        return false;
    }

    @ConfigItem(
            position = 2,
            keyName = "showBackToBackState",
            name = "Show Back-to-Back State",
            description = "Displays the back-to-back state (enabled/disabled) in the guild overlay.",
            section = hunterGuildDisplaySection
    )
    default boolean guildOverlayShowBackToBackState() {
        return true;
    }

    @ConfigItem(
            position = 0,
            keyName = "showRumourInfoBox",
            name = "Show Rumour Infobox",
            description = "Whether an infobox containing your current rumour target should be displayed.",
            section = infoBoxSection
    )
    default boolean showInfoBox() {
        return true;
    }

    @ConfigItem(
            position = 1,
            keyName = "showCatchesRemainingUntilPity",
            name = "Show Catches Remaining",
            description = "Show the catches remaining until the pity threshold is reached.",
            section = infoBoxSection
    )
    default boolean showCatchesRemainingUntilPity() {
        return true;
    }

    @ConfigItem(
            position = 2,
            keyName = "infoBoxDisableTimer",
            name = "Info Box Disable Timer (minutes)",
            description = "Disables the info box after a certain amount of time (in minutes) of no hunter rumour related activity.<br />Turn on 'Force Show Info Box' if you want the info box to be visible at all times.",
            section = infoBoxSection
    )
    @Range(min = 1)
    default int infoBoxDisableTimer() {
        return 5;
    }

    @ConfigItem(
            position = 3,
            keyName = "forceShowInfoBox",
            name = "Force Show Info Box",
            description = "Forces the infobox to be shown even after the time set above<br />Only works if 'Show Rumour Infobox' is enabled.",
            section = infoBoxSection
    )
    default boolean forceShowInfoBox() {
        return false;
    }

    @ConfigItem(
            position = 4,
            keyName = "infoBoxCompletedRumourColor",
            name = "Infobox Completed Rumour Color",
            description = "Text color of the infobox when you completed a rumour.",
            section = infoBoxSection
    )
    default Color completedRumourInfoBoxTextColor() {

        return Color.GREEN;
    }

    @ConfigItem(
            position = 5,
            keyName = "infoBoxLuckyRateColor",
            name = "Infobox Lucky Rate Color",
            description = "Text color of the infobox when you're still in the lucky bracket (<50%) for catching creatures based on pity rates.",
            section = infoBoxSection
    )
    default Color luckyRateInfoBoxTextColor() {

        return Color.RED;
    }

    @ConfigItem(
            position = 6,
            keyName = "infoBoxDefaultColor",
            name = "Infobox Default Color",
            description = "The default text color of the infobox.",
            section = infoBoxSection
    )
    default Color defaultInfoBoxTextColor() {

        return Color.WHITE;
    }

    @ConfigItem(
            position = 7,
            keyName = "infoBoxNormalRateColor",
            name = "Infobox Normal Rate Color",
            description = "Text color of the infobox when you're in the normal bracket (between 50% and 75%) for catching creatures based on pity rates.",
            section = infoBoxSection
    )
    default Color normalRateInfoBoxTextColor() {

        return Color.ORANGE.darker();
    }

    @ConfigItem(
            position = 8,
            keyName = "infoBoxUnluckyColor",
            name = "Infobox Unlucky Rate Color",
            description = "Text color of the infobox when you're in the unlucky bracket (75% or more) for catching creatures based on pity rates.",
            section = infoBoxSection
    )
    default Color unluckyRateInfoBoxTextColor() {
        return Color.ORANGE.brighter();
    }

    @ConfigItem(
            position = 0,
            keyName = "currentRumourMessage",
            name = "Current Rumour Message",
            description = "Place a message in chat whenever the current Rumour changes.",
            section = messagesSection
    )
    default boolean currentRumourMessage() {
        return true;
    }

    @ConfigItem(
            position = 1,
            keyName = "backToBackMessage",
            name = "Back-To-Back Message",
            description = "Place a message in chat whenever the current back-to-back status changes.",
            section = messagesSection
    )
    default boolean backToBackMessage() {
        return true;
    }

    @ConfigItem(
            position = 2,
            keyName = "endOfRumourMessage",
            name = "End-of-Rumour Message",
            description = "Place a message in chat whenever you complete a rumour, containing stats about the rumour.",
            section = messagesSection
    )
    default boolean endOfRumourMessage() {
        return false;
    }

    @ConfigItem(
            position = 3,
            keyName = "endOfRumourMessageUnluckyColor",
            name = "End-of-Rumour Unlucky Color",
            description = "Message color if you've caught >= 75% of the pity rate threshold.",
            section = messagesSection
    )
    default Color endOfRumourMessageUnluckyColor() {

        return Color.RED;
    }

    @ConfigItem(
            position = 4,
            keyName = "endOfRumourMessageNormalColor",
            name = "End-of-Rumour Normal Color",
            description = "Message color if you've caught between and including 50 and 74 percent of the pity rate threshold.",
            section = messagesSection
    )
    default Color endOfRumourMessageNormalColor() {

        return Color.ORANGE;
    }

    @ConfigItem(
            position = 5,
            keyName = "endOfRumourMessageLuckyColor",
            name = "End-of-Rumour Lucky Color",
            description = "Message color if you've caught less than 50% of the pity rate threshold.",
            section = messagesSection
    )
    default Color endOfRumourMessageLuckyColor() {

        return Color.GREEN;
    }

    @ConfigItem(
            position = 0,
            keyName = "includeMasterHunters",
            name = "Include Master Hunters",
            description = "Include master hunters in highlighting and info reports.",
            section = tiersSection
    )
    default boolean includeMasterHunters() {
        return true;
    }

    @ConfigItem(
            position = 1,
            keyName = "includeExpertHunters",
            name = "Include Expert Hunters",
            description = "Include Expert hunters in highlighting and info reports.",
            section = tiersSection
    )
    default boolean includeExpertHunters() {
        return true;
    }

    @ConfigItem(
            position = 2,
            keyName = "includeAdeptHunters",
            name = "Include Adept Hunters",
            description = "Include adept hunters in highlighting and info reports.",
            section = tiersSection
    )
    default boolean includeAdeptHunters() {
        return true;
    }

    @ConfigItem(
            position = 3,
            keyName = "includeNoviceHunters",
            name = "Include Novice Hunters",
            description = "Include novice hunters in highlighting and info reports.",
            section = tiersSection
    )
    default boolean includeNoviceHunters() {
        return true;
    }

    @ConfigItem(
            position = 0,
            keyName = "highlightCurrentHunter",
            name = "Highlight current hunter",
            description = "Whether the hunter that assigned your current task should be highlighted.",
            section = highlightSection
    )
    default boolean highlightCurrentHunter() {
        return true;
    }

    @ConfigItem(
            position = 1,
            keyName = "highlightKnownHunters",
            name = "Highlight known hunters",
            description = "Whether hunters whose rumour is known should be highlighted.",
            section = highlightSection
    )
    default boolean highlightKnownHunters() {
        return false;
    }

    @ConfigItem(
            position = 2,
            keyName = "highlightUnknownHunters",
            name = "Highlight unknown hunters",
            description = "Whether hunters whose rumour is unknown should be highlighted.",
            section = highlightSection
    )
    default boolean highlightUnknownHunters() {
        return false;
    }

    @ConfigItem(
            position = 3,
            keyName = "highlightHunterNPCs",
            name = "Highlight Hunter NPCs",
            description = "Whether your current rumour target should be highlighted.",
            section = highlightSection
    )
    default boolean highlightHunterNPCs() {
        return true;
    }

    @ConfigItem(
            position = 4,
            keyName = "currentHunterHighlightColor",
            name = "Current hunter highlight color",
            description = "Highlight color for hunter who assigned your current rumour.",
            section = highlightSection
    )
    default Color currentHunterHighlightColor() {

        return new Color(0, 0x96, 0);
    }

    @ConfigItem(
            position = 5,
            keyName = "knownHunterHighlightColor",
            name = "Known hunter highlight color",
            description = "Highlight color for hunters whose rumours are known.",
            section = highlightSection
    )
    default Color knownHunterHighlightColor() {
        return new Color(0xC8, 0xC8, 0);
    }

    @ConfigItem(
            position = 6,
            keyName = "unknownHunterHighlightColor",
            name = "Unknown hunter highlight color",
            description = "Highlight color for hunters whose rumours are not known.",
            section = highlightSection
    )
    default Color unknownHunterHighlightColor() {

        return new Color(0x96, 0, 0);
    }

    @ConfigItem(
            position = 7,
            keyName = "hunterNPCHighlightColor",
            name = "Hunter NPC Highlight Color",
            description = "Highlight color for Hunter targets.",
            section = highlightSection
    )
    default Color hunterNPCHighlightColor() {

        return new Color(0x2A, 0xBE, 0x2A);
    }
}
