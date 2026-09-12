package com.geel.hunterrumours.blocklist;

import com.geel.hunterrumours.enums.Hunter;
import com.geel.hunterrumours.enums.HunterTier;
import com.geel.hunterrumours.enums.Rumour;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Authoritative assignment pools and block-holder rules. */
public final class HunterAssignments
{
    private static final Map<Hunter, Set<Rumour>> POOLS = new EnumMap<>(Hunter.class);

    static
    {
        POOLS.put(Hunter.NOVICE_GILMAN, rumours(
                Rumour.TROPICAL_WAGTAIL, Rumour.WILD_KEBBIT, Rumour.SAPPHIRE_GLACIALIS,
                Rumour.SWAMP_LIZARD, Rumour.SPINED_LARUPIA, Rumour.BARB_TAILED_KEBBIT,
                Rumour.SNOWY_KNIGHT, Rumour.PRICKLY_KEBBIT, Rumour.EMBERTAILED_JERBOA,
                Rumour.HORNED_GRAAHK, Rumour.SPOTTED_KEBBIT, Rumour.BLACK_WARLOCK,
                Rumour.ORANGE_SALAMANDER, Rumour.RAZOR_BACKED_KEBBIT, Rumour.SABRE_TOOTHED_KEBBIT,
                Rumour.GREY_CHINCHOMPA, Rumour.SABRE_TOOTHED_KYATT, Rumour.DARK_KEBBIT,
                Rumour.PYRE_FOX, Rumour.RED_SALAMANDER, Rumour.RED_CHINCHOMPA,
                Rumour.DASHING_KEBBIT, Rumour.SUNLIGHT_ANTELOPE, Rumour.SUNLIGHT_MOTH,
                Rumour.TECU_SALAMANDER, Rumour.HERBIBOAR, Rumour.MOONLIGHT_MOTH,
                Rumour.MOONLIGHT_ANTELOPE));
        POOLS.put(Hunter.ADEPT_CERVUS, rumours(
                Rumour.SWAMP_LIZARD, Rumour.HORNED_GRAAHK, Rumour.SPOTTED_KEBBIT,
                Rumour.BLACK_WARLOCK, Rumour.ORANGE_SALAMANDER, Rumour.RAZOR_BACKED_KEBBIT,
                Rumour.SABRE_TOOTHED_KEBBIT, Rumour.GREY_CHINCHOMPA, Rumour.DARK_KEBBIT,
                Rumour.PYRE_FOX, Rumour.RED_CHINCHOMPA, Rumour.SUNLIGHT_MOTH));
        POOLS.put(Hunter.ADEPT_ORNUS, rumours(
                Rumour.SPINED_LARUPIA, Rumour.SNOWY_KNIGHT, Rumour.EMBERTAILED_JERBOA,
                Rumour.SPOTTED_KEBBIT, Rumour.ORANGE_SALAMANDER, Rumour.SABRE_TOOTHED_KEBBIT,
                Rumour.SABRE_TOOTHED_KYATT, Rumour.PYRE_FOX, Rumour.RED_SALAMANDER,
                Rumour.RED_CHINCHOMPA));
        POOLS.put(Hunter.EXPERT_ACO, rumours(
                Rumour.ORANGE_SALAMANDER, Rumour.SABRE_TOOTHED_KEBBIT, Rumour.GREY_CHINCHOMPA,
                Rumour.SABRE_TOOTHED_KYATT, Rumour.DARK_KEBBIT, Rumour.RED_SALAMANDER,
                Rumour.RED_CHINCHOMPA, Rumour.DASHING_KEBBIT, Rumour.SUNLIGHT_ANTELOPE,
                Rumour.TECU_SALAMANDER, Rumour.MOONLIGHT_MOTH));
        POOLS.put(Hunter.EXPERT_TECO, rumours(
                Rumour.SABRE_TOOTHED_KEBBIT, Rumour.GREY_CHINCHOMPA, Rumour.SABRE_TOOTHED_KYATT,
                Rumour.DARK_KEBBIT, Rumour.RED_SALAMANDER, Rumour.RED_CHINCHOMPA,
                Rumour.DASHING_KEBBIT, Rumour.SUNLIGHT_ANTELOPE, Rumour.SUNLIGHT_MOTH,
                Rumour.HERBIBOAR));
        POOLS.put(Hunter.MASTER_WOLF, rumours(
                Rumour.RED_SALAMANDER, Rumour.RED_CHINCHOMPA, Rumour.DASHING_KEBBIT,
                Rumour.SUNLIGHT_ANTELOPE, Rumour.TECU_SALAMANDER, Rumour.HERBIBOAR,
                Rumour.MOONLIGHT_MOTH, Rumour.MOONLIGHT_ANTELOPE));
    }

    private HunterAssignments()
    {
    }

    public static Set<Rumour> getPool(Hunter hunter)
    {
        Set<Rumour> pool = POOLS.get(hunter);
        return pool == null ? Collections.emptySet() : Collections.unmodifiableSet(pool);
    }

    public static boolean canAssign(Hunter hunter, Rumour rumour)
    {
        return getPool(hunter).contains(canonical(rumour));
    }

    public static List<Hunter> getBlockHolders(Hunter activeHunter)
    {
        if (activeHunter == null || activeHunter == Hunter.NONE)
        {
            return Collections.emptyList();
        }

        int activeRank = tierRank(activeHunter.getTier());
        return Arrays.stream(Hunter.allValues())
                .filter(hunter -> hunter != activeHunter)
                .filter(hunter -> tierRank(hunter.getTier()) <= activeRank)
                .sorted((left, right) -> {
                    int rankComparison = Integer.compare(tierRank(left.getTier()), tierRank(right.getTier()));
                    return rankComparison != 0 ? rankComparison : left.getCommonName().compareTo(right.getCommonName());
                })
                .collect(Collectors.toList());
    }

    public static List<Hunter> getAllBlockHolders(Hunter activeHunter)
    {
        if (activeHunter == null || activeHunter == Hunter.NONE)
        {
            return Collections.emptyList();
        }
        return Arrays.stream(Hunter.allValues())
                .filter(hunter -> hunter != activeHunter)
                .collect(Collectors.toList());
    }

    public static List<Rumour> getValidBlocks(Hunter activeHunter, Hunter blockHolder)
    {
        return getPool(blockHolder).stream()
                .filter(getPool(activeHunter)::contains)
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .collect(Collectors.toList());
    }

    public static Rumour canonical(Rumour rumour)
    {
        return rumour == Rumour.RED_CHINCHOMPA_2 ? Rumour.RED_CHINCHOMPA : rumour;
    }

    private static Set<Rumour> rumours(Rumour... rumours)
    {
        return EnumSet.copyOf(Arrays.asList(rumours));
    }

    private static int tierRank(HunterTier tier)
    {
        switch (tier)
        {
            case NOVICE: return 0;
            case ADEPT: return 1;
            case EXPERT: return 2;
            case MASTER: return 3;
            default: return -1;
        }
    }
}
