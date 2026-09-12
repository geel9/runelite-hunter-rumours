package com.geel.hunterrumours.blocklist;

import com.geel.hunterrumours.enums.Hunter;
import com.geel.hunterrumours.enums.Rumour;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BlocklistPlanner
{
    public BlocklistPlan plan(CustomBlocklist list, Map<Hunter, Rumour> observed)
    {
        List<BlocklistPlan.Row> rows = new ArrayList<>();
        Rumour activeObserved = value(observed, list.getActiveHunter());
        rows.add(new BlocklistPlan.Row(list.getActiveHunter(), Rumour.NONE, activeObserved,
                BlocklistPlan.Row.Status.ACTIVE));

        BlocklistPlan.Row next = null;
        for (Map.Entry<Hunter, Rumour> entry : list.getBlocks().entrySet())
        {
            Hunter holder = entry.getKey();
            Rumour desired = entry.getValue();
            Rumour actual = value(observed, holder);
            BlocklistPlan.Row.Status status;
            if (actual == Rumour.NONE)
            {
                status = BlocklistPlan.Row.Status.UNKNOWN;
            }
            else if (HunterAssignments.canonical(actual) == HunterAssignments.canonical(desired))
            {
                status = BlocklistPlan.Row.Status.COMPLETE;
            }
            else
            {
                status = BlocklistPlan.Row.Status.NEEDS_REROLL;
            }
            BlocklistPlan.Row row = new BlocklistPlan.Row(holder, desired, actual, status);
            rows.add(row);
            if (next == null && status != BlocklistPlan.Row.Status.COMPLETE)
            {
                next = row;
            }
        }

        if (next == null)
        {
            return new BlocklistPlan(rows, true,
                    "Blocklist ready. Use " + list.getActiveHunter().getCommonName() + " for active rumours.");
        }
        if (next.getStatus() == BlocklistPlan.Row.Status.UNKNOWN)
        {
            return new BlocklistPlan(rows, false, "Check " + next.getHunter().getCommonName()
                    + "'s current rumour. Target: " + next.getDesired().getName() + ".");
        }
        return new BlocklistPlan(rows, false, "Use " + next.getHunter().getCommonName()
                + " until they assign " + next.getDesired().getName() + ".");
    }

    private static Rumour value(Map<Hunter, Rumour> observed, Hunter hunter)
    {
        Rumour value = observed == null ? null : observed.get(hunter);
        return value == null ? Rumour.NONE : value;
    }
}
