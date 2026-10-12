package com.geel.hunterrumours.blocklist;

import com.geel.hunterrumours.HunterRumoursConfig;
import com.geel.hunterrumours.enums.Hunter;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Persists the named blocklist library behind a small list/save/delete interface. */
public class CustomBlocklistStore
{
    private static final String INDEX_PREFIX = "blocklist.index.";
    private static final String VALUE_PREFIX = "blocklist.saved.";
    private final ConfigManager configManager;
    private final BlocklistCodec codec;

    @Inject
    public CustomBlocklistStore(ConfigManager configManager)
    {
        this(configManager, new BlocklistCodec());
    }

    CustomBlocklistStore(ConfigManager configManager, BlocklistCodec codec)
    {
        this.configManager = configManager;
        this.codec = codec;
    }

    public List<CustomBlocklist> list(Hunter activeHunter)
    {
        String index = configManager.getConfiguration(HunterRumoursConfig.GROUP, indexKey(activeHunter));
        if (index == null || index.trim().isEmpty())
        {
            return Collections.emptyList();
        }

        List<CustomBlocklist> result = new ArrayList<>();
        for (String id : index.split(","))
        {
            String json = configManager.getConfiguration(HunterRumoursConfig.GROUP, VALUE_PREFIX + id);
            if (json == null)
            {
                continue;
            }
            try
            {
                CustomBlocklist list = codec.deserialize(json);
                if (list.getActiveHunter() == activeHunter)
                {
                    result.add(list);
                }
            }
            catch (IllegalArgumentException ignored)
            {
                // Keep one corrupt entry from hiding the rest of the library.
            }
        }
        result.sort((left, right) -> left.getName().compareToIgnoreCase(right.getName()));
        return result;
    }

    public void save(CustomBlocklist list)
    {
        boolean duplicateName = list(list.getActiveHunter()).stream()
                .anyMatch(existing -> !existing.getId().equals(list.getId())
                        && existing.getName().equalsIgnoreCase(list.getName()));
        if (duplicateName)
        {
            throw new IllegalArgumentException("A " + list.getActiveHunter().getCommonName()
                    + " blocklist named '" + list.getName() + "' already exists");
        }

        configManager.setConfiguration(HunterRumoursConfig.GROUP, VALUE_PREFIX + list.getId(), codec.serialize(list));
        List<String> ids = ids(list.getActiveHunter());
        if (!ids.contains(list.getId()))
        {
            ids.add(list.getId());
            configManager.setConfiguration(HunterRumoursConfig.GROUP, indexKey(list.getActiveHunter()),
                    String.join(",", ids));
        }
    }

    public void delete(CustomBlocklist list)
    {
        configManager.unsetConfiguration(HunterRumoursConfig.GROUP, VALUE_PREFIX + list.getId());
        List<String> ids = ids(list.getActiveHunter());
        ids.remove(list.getId());
        if (ids.isEmpty())
        {
            configManager.unsetConfiguration(HunterRumoursConfig.GROUP, indexKey(list.getActiveHunter()));
        }
        else
        {
            configManager.setConfiguration(HunterRumoursConfig.GROUP, indexKey(list.getActiveHunter()),
                    String.join(",", ids));
        }
    }

    public String exportList(CustomBlocklist list)
    {
        return codec.exportList(list);
    }

    public CustomBlocklist importList(String json)
    {
        return codec.importList(json);
    }

    private List<String> ids(Hunter hunter)
    {
        String index = configManager.getConfiguration(HunterRumoursConfig.GROUP, indexKey(hunter));
        if (index == null || index.trim().isEmpty())
        {
            return new ArrayList<>();
        }
        return Arrays.stream(index.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static String indexKey(Hunter hunter)
    {
        return INDEX_PREFIX + hunter.name();
    }
}
