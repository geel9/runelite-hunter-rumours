package com.geel.hunterrumours;

import com.geel.hunterrumours.enums.Creature;
import com.geel.hunterrumours.enums.Rumour;
import com.geel.hunterrumours.enums.RumourLocation;
import net.runelite.client.config.ConfigManager;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PreferredLocationPreferencesTest
{
    private ConfigManager configManager;
    private PreferredLocationPreferences preferences;

    @Before
    public void setUp()
    {
        configManager = mock(ConfigManager.class);
        preferences = new PreferredLocationPreferences(configManager);
    }

    @Test
    public void initializesWithFirstFairyRingLocationRatherThanFirstLocation()
    {
        preferences.initializeDefaults();

        verify(configManager).setConfiguration(
                HunterRumoursConfig.GROUP,
                PreferredLocationPreferences.CREATURE_KEY_PREFIX + Creature.SNOWY_KNIGHT.name(),
                RumourLocation.SNOWY_KNIGHT_RELLEKKA_HUNTER_AREA_UPPER_1.name());
        verify(configManager).setConfiguration(
                HunterRumoursConfig.GROUP,
                PreferredLocationPreferences.INITIALIZED_KEY,
                true);
    }

    @Test
    public void fallsBackToFirstLocationWhenCreatureHasNoFairyRingLocation()
    {
        Creature creature = preferences.getCreatures().stream()
                .filter(candidate -> preferences.getLocations(candidate).stream()
                        .noneMatch(location -> location.getFairyRingCode().length() == 3))
                .findFirst()
                .orElseThrow(AssertionError::new);
        List<RumourLocation> locations = preferences.getLocations(creature);

        assertEquals(locations.get(0), preferences.getPreferredLocation(creature));
    }

    @Test
    public void includesLocationsWithoutFairyRingCodesAndDeduplicatesSpawns()
    {
        List<RumourLocation> locations = preferences.getLocations(Creature.SWAMP_LIZARD);

        assertTrue(locations.stream().anyMatch(location -> location.getFairyRingCode().isEmpty()));
        assertEquals(2, locations.size());
    }

    @Test
    public void returnsStoredValidPreferenceAndRejectsStaleValues()
    {
        String key = PreferredLocationPreferences.CREATURE_KEY_PREFIX + Creature.TROPICAL_WAGTAIL.name();
        RumourLocation conch = RumourLocation.TROPICAL_WAGTAIL_THE_GREAT_CONCH_1;
        when(configManager.getConfiguration(HunterRumoursConfig.GROUP, key)).thenReturn(conch.name());
        assertEquals(conch, preferences.getPreferredLocation(Rumour.TROPICAL_WAGTAIL));

        when(configManager.getConfiguration(HunterRumoursConfig.GROUP, key)).thenReturn("REMOVED_LOCATION");
        preferences.refreshPreference(key);
        assertEquals(RumourLocation.TROPICAL_WAGTAIL_FELDIP_HUNTER_AREA_1,
                preferences.getPreferredLocation(Rumour.TROPICAL_WAGTAIL));
    }

    @Test
    public void migratesLegacyFairyRingPreferenceToMatchingLocation()
    {
        when(configManager.getConfiguration(
                HunterRumoursConfig.GROUP,
                "fairyRingPreferences.creature." + Creature.TROPICAL_WAGTAIL.name()))
                .thenReturn("CJQ");

        preferences.initializeDefaults();

        verify(configManager).setConfiguration(
                HunterRumoursConfig.GROUP,
                PreferredLocationPreferences.CREATURE_KEY_PREFIX + Creature.TROPICAL_WAGTAIL.name(),
                RumourLocation.TROPICAL_WAGTAIL_THE_GREAT_CONCH_1.name());
    }

    @Test
    public void migratesLegacyGlobalFairyRingPreferenceToMatchingLocations()
    {
        when(configManager.getConfiguration(HunterRumoursConfig.GROUP, "preferredFairyRingCode"))
                .thenReturn("CJQ");

        preferences.initializeDefaults();

        verify(configManager).setConfiguration(
                HunterRumoursConfig.GROUP,
                PreferredLocationPreferences.CREATURE_KEY_PREFIX + Creature.TROPICAL_WAGTAIL.name(),
                RumourLocation.TROPICAL_WAGTAIL_THE_GREAT_CONCH_1.name());
    }

    @Test
    public void everyRumourCreatureHasAtLeastOneLocation()
    {
        for (Creature creature : preferences.getCreatures())
        {
            assertFalse(preferences.getLocations(creature).isEmpty());
        }
    }

    @Test
    public void creatureNamesAreAlphabeticalAndNeverUnknown()
    {
        List<String> names = preferences.getCreatures().stream()
                .map(preferences::getDisplayName)
                .collect(Collectors.toList());
        List<String> sorted = names.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        assertEquals(sorted, names);
        assertFalse(names.contains("Unknown"));
        assertTrue(names.contains("Barb-tailed Kebbit"));
    }
}
