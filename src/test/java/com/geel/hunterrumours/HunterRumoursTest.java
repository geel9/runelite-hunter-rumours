package com.geel.hunterrumours;

import com.geel.hunterrumours.enums.Rumour;
import org.junit.Test;

import static org.junit.Assert.fail;

public class HunterRumoursTest {
    @Test
    public void hunterRumoursTests() {
        for (final Rumour rumour : Rumour.values()) {
            if (rumour == Rumour.NONE) {
                continue;
            }

            // Should have a name
            if (rumour.getName().equals("")) {
                fail("Rumour has a missing name.");

            }

            // Should have an item id
            // TODO: Refactor into `Creature`-specific tests
            if (rumour.getTargetCreature().getItemId() <= 0) {
                fail("Rumour (" + rumour.getName() + ")" + " has invalid item id: " + rumour.getTargetCreature().getItemId());
            }

            // Should have a hunter level of at least 1
            if (rumour.getTargetCreature().getHunterLevel() <= 0) {
                fail("Rumour (" + rumour.getName() + ")" + " has invalid hunter level: " + rumour.getTargetCreature().getHunterLevel());
            }

            // Should have at least one possible xp drop
            if (rumour.getTargetCreature().getPossibleXpDrops().length == 0) {
                fail("Rumour (" + rumour.getName() + ")" + " has no possible xp drops");
            }

            // Should be at least one of these
            if (!rumour.isAdept() && !rumour.isExpert() && !rumour.isMaster() && !rumour.isNovice()) {
                fail("Rumour (" + rumour.getName() + ")" + " should be adept, novice, master or expert, cannot select none");
            }

            // Cannot have a null (unset) trap
            if (rumour.getTrap() == null) {
                fail("Rumour (" + rumour.getName() + ")" + " has no trap selected");
            }
        }
    }
}
