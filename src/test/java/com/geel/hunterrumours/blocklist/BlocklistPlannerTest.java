package com.geel.hunterrumours.blocklist;

import com.geel.hunterrumours.enums.Hunter;
import com.geel.hunterrumours.enums.Rumour;
import org.junit.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BlocklistPlannerTest
{
    @Test
    public void preservesExistingPresetBlocksWhenResolving()
    {
        Map<Hunter, Rumour> observed = new EnumMap<>(Hunter.class);
        observed.put(Hunter.ADEPT_ORNUS, Rumour.PYRE_FOX);

        CustomBlocklist resolved = BlocklistPreset.ADEPT_CERVUS.resolve(observed);

        assertTrue(resolved.getBlocks().containsValue(Rumour.PYRE_FOX));
        assertTrue(resolved.getBlocks().containsValue(Rumour.SABRE_TOOTHED_KEBBIT));
        assertTrue(resolved.getBlocks().get(Hunter.ADEPT_ORNUS) == Rumour.PYRE_FOX);
    }

    @Test
    public void guidesUnknownBeforeCallingAListComplete()
    {
        CustomBlocklist list = BlocklistPreset.ADEPT_CERVUS.resolve(new EnumMap<>(Hunter.class));
        BlocklistPlan plan = new BlocklistPlanner().plan(list, new EnumMap<>(Hunter.class));

        assertFalse(plan.isComplete());
        assertTrue(plan.getNextAction().startsWith("Check"));
    }

    @Test
    public void reportsACompletedList()
    {
        CustomBlocklist list = BlocklistPreset.ADEPT_CERVUS.resolve(new EnumMap<>(Hunter.class));
        Map<Hunter, Rumour> observed = new EnumMap<>(Hunter.class);
        observed.putAll(list.getBlocks());

        BlocklistPlan plan = new BlocklistPlanner().plan(list, observed);

        assertTrue(plan.isComplete());
        assertTrue(plan.getNextAction().contains("Cervus"));
    }

    @Test
    public void everyPresetHasAValidAssignment()
    {
        for (BlocklistPreset preset : BlocklistPreset.values())
        {
            for (Hunter activeHunter : preset.getActiveHunters())
            {
                CustomBlocklist list = preset.resolve(activeHunter, new EnumMap<>(Hunter.class));
                assertFalse(list.getBlocks().containsKey(activeHunter));
                assertTrue(list.getBlocks().size() == preset.getTargets().size());
            }
        }
    }
}
