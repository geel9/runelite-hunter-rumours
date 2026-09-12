package com.geel.hunterrumours.blocklist;

import com.geel.hunterrumours.enums.Hunter;
import com.geel.hunterrumours.enums.Rumour;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Getter
public enum BlocklistPreset
{
    ADEPT_CERVUS("Adept 57-72", new Hunter[]{Hunter.ADEPT_CERVUS},
            Rumour.PYRE_FOX, Rumour.SABRE_TOOTHED_KEBBIT),
    EXPERT_72("Expert 72-90", new Hunter[]{Hunter.EXPERT_ACO, Hunter.EXPERT_TECO},
            Rumour.SABRE_TOOTHED_KEBBIT, Rumour.GREY_CHINCHOMPA,
            Rumour.SABRE_TOOTHED_KYATT, Rumour.SUNLIGHT_ANTELOPE),
    EXPERT_ACO_91("Expert Aco 91+", new Hunter[]{Hunter.EXPERT_ACO},
            Rumour.SABRE_TOOTHED_KEBBIT, Rumour.GREY_CHINCHOMPA,
            Rumour.SABRE_TOOTHED_KYATT, Rumour.RED_CHINCHOMPA, Rumour.SUNLIGHT_ANTELOPE),
    EXPERT_TECO_91("Expert Teco 91+", new Hunter[]{Hunter.EXPERT_TECO},
            Rumour.SABRE_TOOTHED_KEBBIT, Rumour.GREY_CHINCHOMPA,
            Rumour.SABRE_TOOTHED_KYATT, Rumour.RED_CHINCHOMPA, Rumour.SUNLIGHT_ANTELOPE);

    private final String displayName;
    private final List<Hunter> activeHunters;
    private final List<Rumour> targets;

    BlocklistPreset(String displayName, Hunter[] activeHunters, Rumour... targets)
    {
        this.displayName = displayName;
        this.activeHunters = Collections.unmodifiableList(Arrays.asList(activeHunters));
        this.targets = Collections.unmodifiableList(Arrays.asList(targets));
    }

    public Hunter getActiveHunter()
    {
        return activeHunters.get(0);
    }

    public CustomBlocklist resolve(java.util.Map<Hunter, Rumour> observed)
    {
        return resolve(getActiveHunter(), observed);
    }

    public CustomBlocklist resolve(Hunter activeHunter, java.util.Map<Hunter, Rumour> observed)
    {
        if (!activeHunters.contains(activeHunter))
        {
            throw new IllegalArgumentException(activeHunter.getCommonName() + " is not valid for " + displayName);
        }
        java.util.Map<Hunter, Rumour> result = new java.util.EnumMap<>(Hunter.class);
        if (!match(activeHunter, 0, EnumSet.noneOf(Hunter.class), result, observed))
        {
            throw new IllegalStateException("Preset has no valid hunter assignment: " + name());
        }
        return new CustomBlocklist("preset-" + name(), displayName, activeHunter, result);
    }

    private boolean match(Hunter activeHunter, int targetIndex, Set<Hunter> used, java.util.Map<Hunter, Rumour> result,
                          java.util.Map<Hunter, Rumour> observed)
    {
        if (targetIndex == targets.size())
        {
            return true;
        }

        Rumour target = targets.get(targetIndex);
        for (Hunter holder : orderedHolders(activeHunter, target, observed))
        {
            if (used.contains(holder))
            {
                continue;
            }
            used.add(holder);
            result.put(holder, target);
            if (match(activeHunter, targetIndex + 1, used, result, observed))
            {
                return true;
            }
            result.remove(holder);
            used.remove(holder);
        }
        return false;
    }

    private List<Hunter> orderedHolders(Hunter activeHunter, Rumour target, java.util.Map<Hunter, Rumour> observed)
    {
        List<Hunter> holders = new java.util.ArrayList<>();
        for (Hunter holder : getAllowedHolders(activeHunter))
        {
            if (HunterAssignments.canAssign(holder, target))
            {
                holders.add(holder);
            }
        }
        holders.sort((left, right) -> Boolean.compare(
                !matches(observed.get(left), target), !matches(observed.get(right), target)));
        return holders;
    }

    private List<Hunter> getAllowedHolders(Hunter activeHunter)
    {
        List<Hunter> holders = new java.util.ArrayList<>(HunterAssignments.getBlockHolders(activeHunter));
        if (this == EXPERT_ACO_91 || this == EXPERT_TECO_91)
        {
            holders.add(Hunter.MASTER_WOLF);
        }
        return holders;
    }

    private static boolean matches(Rumour actual, Rumour expected)
    {
        return HunterAssignments.canonical(actual) == HunterAssignments.canonical(expected);
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
