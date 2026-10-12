package com.geel.hunterrumours.blocklist;

import com.geel.hunterrumours.enums.Hunter;
import com.geel.hunterrumours.enums.Rumour;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HunterAssignmentsTest
{
    @Test
    public void cervusCanUseGilmanAndOrnusForBlocks()
    {
        assertEquals(Arrays.asList(Hunter.NOVICE_GILMAN, Hunter.ADEPT_ORNUS),
                HunterAssignments.getBlockHolders(Hunter.ADEPT_CERVUS));
    }

    @Test
    public void differentHuntersAtTheSameTierHaveDifferentPools()
    {
        assertTrue(HunterAssignments.canAssign(Hunter.ADEPT_CERVUS, Rumour.SUNLIGHT_MOTH));
        assertFalse(HunterAssignments.canAssign(Hunter.ADEPT_ORNUS, Rumour.SUNLIGHT_MOTH));
        assertTrue(HunterAssignments.canAssign(Hunter.ADEPT_ORNUS, Rumour.RED_SALAMANDER));
        assertFalse(HunterAssignments.canAssign(Hunter.ADEPT_CERVUS, Rumour.RED_SALAMANDER));
    }

    @Test
    public void validBlocksAreAnIntersectionWithTheActiveHunter()
    {
        assertTrue(HunterAssignments.getValidBlocks(Hunter.ADEPT_CERVUS, Hunter.ADEPT_ORNUS)
                .contains(Rumour.PYRE_FOX));
        assertFalse(HunterAssignments.getValidBlocks(Hunter.ADEPT_CERVUS, Hunter.ADEPT_ORNUS)
                .contains(Rumour.RED_SALAMANDER));
    }
}
