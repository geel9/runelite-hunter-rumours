package com.geel.hunterrumours.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.Item;
import net.runelite.api.ItemID;

@AllArgsConstructor
public enum Trap {
    SNARE("Bird snare", ItemID.BIRD_SNARE, 40, 38),
    DEADFALL("Deadfall", ItemID.LOGS, 30, 28),
    NET_TRAP("Net trap", ItemID.SMALL_FISHING_NET, 50, 46),
    PIT("Pit Trap", ItemID.TEASING_STICK, 30, 28),
    BOX_TRAP("Box Trap", ItemID.BOX_TRAP, 100, 94),
    FALCONRY("Falconry", ItemID.FALCONERS_GLOVE, 20, 18),
    BUTTERFLY("Butterfly Net", ItemID.BUTTERFLY_NET, 150, 142),
    NOOSE("Tracking", ItemID.NOOSE_WAND, 30, 28),
    NOOSE_HERBIBOAR("Tracking", ItemID.NOOSE_WAND, 14, 12);

    @Getter
    private final String Name;

    @Getter
    private final int ItemId;

    @Getter
    private final int pityThreshold;

    @Getter
    private final int pityThresholdWithOutfit;

    public int calculatePityRateForItems(final int items) {
        int difference = pityThreshold - pityThresholdWithOutfit;
        double differenceForItems = difference / 4d * items;
        return (int) Math.floor(pityThreshold - differenceForItems);
    }
}
