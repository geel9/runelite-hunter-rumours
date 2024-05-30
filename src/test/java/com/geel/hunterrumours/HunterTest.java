package com.geel.hunterrumours;

import com.geel.hunterrumours.enums.Hunter;

import com.geel.hunterrumours.enums.HunterTier;

import static org.junit.Assert.fail;

import org.junit.Test;

public class HunterTest {
    @Test
    public void hunterTests() {
        for (Hunter hunter : Hunter.values()) {
            if (hunter == Hunter.NONE) {
                continue;
            }

            if (hunter.getNpcId() <= 0) {
                fail("Invalid NPC id of " + hunter.getNpcId() + " for hunter: " + hunter.name());
            }

            if (hunter.getTier() == null || hunter.getTier() == HunterTier.NONE) {
                fail("Invalid hunter tier of " + hunter.getTier() + " for hunter: " + hunter.name());
            }

            if (hunter.getCommonName() == null || hunter.getCommonName().isBlank()) {
                fail("You must set a name for hunter: " + hunter.name());
            }
        }
    }
}
