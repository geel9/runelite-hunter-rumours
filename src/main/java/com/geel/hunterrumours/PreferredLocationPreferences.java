package com.geel.hunterrumours;

import com.geel.hunterrumours.enums.Creature;
import com.geel.hunterrumours.enums.Rumour;
import com.geel.hunterrumours.enums.RumourLocation;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Stores the preferred hunting location for each rumour creature. */
@Singleton
public class PreferredLocationPreferences
{
    static final String INITIALIZED_KEY = "locationPreferences.initialized";
    static final String CREATURE_KEY_PREFIX = "locationPreferences.creature.";
    private static final String LEGACY_CREATURE_KEY_PREFIX = "fairyRingPreferences.creature.";
    private static final String LEGACY_GLOBAL_FAIRY_RING_KEY = "preferredFairyRingCode";

    private final ConfigManager configManager;
    private final Map<Creature, List<RumourLocation>> locationsByCreature;
    private final Map<Creature, String> displayNames;
    private final List<Creature> creatures;
    private final Map<Creature, RumourLocation> selectedLocations = new ConcurrentHashMap<>();

    @Inject
    PreferredLocationPreferences(ConfigManager configManager)
    {
        this.configManager = configManager;

        Map<Creature, Map<String, RumourLocation>> groupedLocations = new LinkedHashMap<>();
        for (RumourLocation location : RumourLocation.values())
        {
            if (location.getRumour() == Rumour.NONE)
            {
                continue;
            }
            groupedLocations
                    .computeIfAbsent(location.getRumour().getTargetCreature(), ignored -> new LinkedHashMap<>())
                    .putIfAbsent(normalize(location.getLocationName()), location);
        }

        Map<Creature, List<RumourLocation>> cachedLocations = new LinkedHashMap<>();
        groupedLocations.forEach((creature, locations) -> cachedLocations.put(
                creature, Collections.unmodifiableList(new ArrayList<>(locations.values()))));
        locationsByCreature = Collections.unmodifiableMap(cachedLocations);

        Map<Creature, String> cachedNames = new LinkedHashMap<>();
        for (Rumour rumour : Rumour.allValues())
        {
            if (rumour != Rumour.NONE)
            {
                cachedNames.putIfAbsent(rumour.getTargetCreature(), rumour.getName());
            }
        }
        displayNames = Collections.unmodifiableMap(cachedNames);

        List<Creature> sortedCreatures = new ArrayList<>(locationsByCreature.keySet());
        sortedCreatures.sort(Comparator.comparing(this::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        creatures = Collections.unmodifiableList(sortedCreatures);
    }

    public void initializeDefaults()
    {
        for (Creature creature : getCreatures())
        {
            String key = keyFor(creature);
            String configured = configManager.getConfiguration(HunterRumoursConfig.GROUP, key);
            RumourLocation location = findByStoredValue(creature, configured);
            if (location != null)
            {
                selectedLocations.put(creature, location);
                continue;
            }

            location = migrateLegacyFairyRingPreference(creature);
            if (location == null)
            {
                location = getDefaultLocation(creature);
            }
            selectedLocations.put(creature, location);
            configManager.setConfiguration(HunterRumoursConfig.GROUP, key, location.name());
        }

        if (configManager.getConfiguration(HunterRumoursConfig.GROUP, INITIALIZED_KEY) == null)
        {
            configManager.setConfiguration(HunterRumoursConfig.GROUP, INITIALIZED_KEY, true);
        }
    }

    public RumourLocation getPreferredLocation(Rumour rumour)
    {
        return getPreferredLocation(rumour.getTargetCreature());
    }

    public RumourLocation getPreferredLocation(Creature creature)
    {
        RumourLocation cached = selectedLocations.get(creature);
        if (cached != null)
        {
            return cached;
        }

        String configured = configManager.getConfiguration(HunterRumoursConfig.GROUP, keyFor(creature));
        RumourLocation location = findByStoredValue(creature, configured);
        RumourLocation selected = location == null ? getDefaultLocation(creature) : location;
        selectedLocations.put(creature, selected);
        return selected;
    }

    public void setPreferredLocation(Creature creature, RumourLocation location)
    {
        if (!getLocations(creature).contains(location))
        {
            throw new IllegalArgumentException(location + " is not a location for " + creature);
        }
        selectedLocations.put(creature, location);
        configManager.setConfiguration(HunterRumoursConfig.GROUP, keyFor(creature), location.name());
    }

    public void refreshPreference(String key)
    {
        if (!key.startsWith(CREATURE_KEY_PREFIX))
        {
            return;
        }

        try
        {
            Creature creature = Creature.valueOf(key.substring(CREATURE_KEY_PREFIX.length()));
            selectedLocations.remove(creature);
            getPreferredLocation(creature);
        }
        catch (IllegalArgumentException ignored)
        {
            // Ignore stale configuration keys for creatures which no longer exist.
        }
    }

    public List<Creature> getCreatures()
    {
        return creatures;
    }

    /** Returns one representative spawn for each named location, in declaration order. */
    public List<RumourLocation> getLocations(Creature creature)
    {
        return locationsByCreature.getOrDefault(creature, Collections.emptyList());
    }

    public String getDisplayName(Creature creature)
    {
        return displayNames.getOrDefault(creature, creature.name());
    }

    private RumourLocation migrateLegacyFairyRingPreference(Creature creature)
    {
        String code = configManager.getConfiguration(
                HunterRumoursConfig.GROUP, LEGACY_CREATURE_KEY_PREFIX + creature.name());
        if (code == null)
        {
            code = configManager.getConfiguration(HunterRumoursConfig.GROUP, LEGACY_GLOBAL_FAIRY_RING_KEY);
        }
        final String preferredCode = code;
        return getLocations(creature).stream()
                .filter(location -> preferredCode != null && preferredCode.equals(location.getFairyRingCode()))
                .findFirst()
                .orElse(null);
    }

    private RumourLocation findByStoredValue(Creature creature, String value)
    {
        if (value == null)
        {
            return null;
        }
        return getLocations(creature).stream()
                .filter(location -> location.name().equals(value))
                .findFirst()
                .orElse(null);
    }

    private RumourLocation getDefaultLocation(Creature creature)
    {
        List<RumourLocation> locations = getLocations(creature);
        if (locations.isEmpty())
        {
            throw new IllegalArgumentException("No rumour locations exist for " + creature);
        }
        return locations.stream()
                .filter(location -> location.getFairyRingCode().length() == 3)
                .findFirst()
                .orElse(locations.get(0));
    }

    private static String normalize(String locationName)
    {
        return locationName.toLowerCase(Locale.ENGLISH);
    }

    private static String keyFor(Creature creature)
    {
        return CREATURE_KEY_PREFIX + creature.name();
    }
}
