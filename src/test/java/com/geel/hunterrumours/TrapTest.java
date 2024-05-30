package com.geel.hunterrumours;

import com.geel.hunterrumours.enums.Trap;
import org.junit.Test;

import static org.junit.Assert.fail;

public class TrapTest {
    @Test
    public void trapTests() {
        for (final Trap trap : Trap.values()) {
            // Check if trap has a name
            if (trap.getName().equals("")) {
                fail("Trap should have a name");
            }

            // Check if trap has an item id
            if (trap.getItemId() <= 0) {
                fail("Trap (" + trap.getName() + ") should have an item id set (>0)");
            }

            // Pity threshold should be > 0
            if (trap.getPityThreshold() <= 0) {
                fail("Trap (" + trap.getName() + ") should have a pity threshold > 0");
            }

            if (trap.getPityThresholdWithOutfit() <= 0) {
                fail("Trap (" + trap.getName() + ") should have a pity threshold (with outfit) > 0");
            }

            if (trap.getPityThresholdWithOutfit() >= trap.getPityThreshold()) {
                fail("Trap (" + trap.getName() + ") should have a pity threshold (with outfit) < pity threshold (without outfit)");
            }
        }
    }
}
