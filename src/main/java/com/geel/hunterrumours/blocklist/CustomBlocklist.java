package com.geel.hunterrumours.blocklist;

import com.geel.hunterrumours.enums.Hunter;
import com.geel.hunterrumours.enums.Rumour;
import lombok.Getter;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** A named desired blocklist. This never represents observed in-game state. */
@Getter
public final class CustomBlocklist
{
    public static final int CURRENT_VERSION = 1;
    public static final int MAX_NAME_LENGTH = 60;

    private final int version;
    private final String id;
    private final String name;
    private final Hunter activeHunter;
    private final Map<Hunter, Rumour> blocks;

    public CustomBlocklist(String id, String name, Hunter activeHunter, Map<Hunter, Rumour> blocks)
    {
        this(CURRENT_VERSION, id, name, activeHunter, blocks);
    }

    public CustomBlocklist(int version, String id, String name, Hunter activeHunter, Map<Hunter, Rumour> blocks)
    {
        this.version = version;
        this.id = normalizeId(id);
        this.name = normalizeName(name);
        this.activeHunter = Objects.requireNonNull(activeHunter, "Active hunter is required");
        this.blocks = validateBlocks(activeHunter, blocks);
        if (version != CURRENT_VERSION)
        {
            throw new IllegalArgumentException("Unsupported blocklist version: " + version);
        }
        if (activeHunter == Hunter.NONE)
        {
            throw new IllegalArgumentException("Active hunter cannot be unknown");
        }
    }

    public static CustomBlocklist create(String name, Hunter activeHunter, Map<Hunter, Rumour> blocks)
    {
        return new CustomBlocklist(UUID.randomUUID().toString(), name, activeHunter, blocks);
    }

    public CustomBlocklist edited(String newName, Map<Hunter, Rumour> newBlocks)
    {
        return new CustomBlocklist(version, id, newName, activeHunter, newBlocks);
    }

    public CustomBlocklist copy(String newName)
    {
        return create(newName, activeHunter, blocks);
    }

    public Map<Hunter, Rumour> getBlocks()
    {
        return Collections.unmodifiableMap(blocks);
    }

    @Override
    public String toString()
    {
        return name;
    }

    private static String normalizeId(String id)
    {
        if (id == null || id.trim().isEmpty())
        {
            throw new IllegalArgumentException("Blocklist id is required");
        }
        return id.trim();
    }

    private static String normalizeName(String name)
    {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty())
        {
            throw new IllegalArgumentException("Blocklist name is required");
        }
        if (normalized.length() > MAX_NAME_LENGTH)
        {
            throw new IllegalArgumentException("Blocklist names cannot exceed " + MAX_NAME_LENGTH + " characters");
        }
        return normalized;
    }

    private static Map<Hunter, Rumour> validateBlocks(Hunter activeHunter, Map<Hunter, Rumour> requested)
    {
        Map<Hunter, Rumour> result = new EnumMap<>(Hunter.class);
        Set<Rumour> used = new HashSet<>();
        if (requested == null)
        {
            return result;
        }

        for (Map.Entry<Hunter, Rumour> entry : requested.entrySet())
        {
            Hunter holder = entry.getKey();
            Rumour rumour = HunterAssignments.canonical(entry.getValue());
            if (holder == null || rumour == null || rumour == Rumour.NONE)
            {
                continue;
            }
            if (!HunterAssignments.getAllBlockHolders(activeHunter).contains(holder))
            {
                throw new IllegalArgumentException(holder.getCommonName() + " cannot hold a block for " + activeHunter.getCommonName());
            }
            if (!HunterAssignments.canAssign(activeHunter, rumour) || !HunterAssignments.canAssign(holder, rumour))
            {
                throw new IllegalArgumentException(rumour.getName() + " is not shared by "
                        + activeHunter.getCommonName() + " and " + holder.getCommonName());
            }
            if (!used.add(rumour))
            {
                throw new IllegalArgumentException(rumour.getName() + " is selected more than once");
            }
            result.put(holder, rumour);
        }
        return result;
    }
}
