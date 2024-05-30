package com.geel.hunterrumours;

import com.geel.hunterrumours.enums.Creature;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

import org.junit.Test;

public class CreatureTest {
    @Test
    public void creatureTests() {
        for (Creature creature : Creature.values()) {
            if (creature == Creature.NONE) {
                continue;
            }

            if (creature.getHunterLevel() <= 0) {
                fail("Invalid hunter level of " + creature.getHunterLevel() + " for creature: " + creature.name());
            }

            if (creature.getItemId() <= 0) {
                fail("Invalid Item id of " + creature.getItemId() + " for creature: " + creature.name());
            }

            if (creature.getTrap() == null) {
                fail("You must select a trap for creature: " + creature.name());
            }

            if (creature.getPossibleXpDrops() == null || creature.getPossibleXpDrops().length == 0) {
                fail("You must have at least one possible xp drop for creature: " + creature.name());
            }

            List<Integer> possibleXpDrops = new ArrayList<>();
            for (int xpDrop : creature.getPossibleXpDrops()) {
                if (possibleXpDrops.contains(xpDrop)) {
                    fail("Duplicate possible xp drop of " + xpDrop + " for creature: " + creature.name());
                }
                if (xpDrop <= 0) {
                    fail("Invalid possible xp drop of " + xpDrop + " for creature: " + creature.name());
                }
                possibleXpDrops.add(xpDrop);
            }

            if (creature.getNpcId() <= 0 && creature != Creature.RAZOR_BACKED_KEBBIT) {
                fail("Invalid NPC id of " + creature.getNpcId() + " for creature: " + creature.name());
            }
        }
    }
}
