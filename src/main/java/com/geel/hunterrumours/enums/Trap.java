package com.geel.hunterrumours.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.ItemID;

@AllArgsConstructor
public enum Trap {
    SNARE("Bird snare", ItemID.BIRD_SNARE, 40),
    DEADFALL("Deadfall", ItemID.LOGS, 30),
    NET_TRAP("Net trap", ItemID.SMALL_FISHING_NET, 50),
    PIT("Pit Trap", ItemID.TEASING_STICK, 30),
    BOX_TRAP("Box Trap", ItemID.BOX_TRAP, 100),
    FALCONRY("Falconry", ItemID.FALCONERS_GLOVE, 20),
    BUTTERFLY("Butterfly Net", ItemID.BUTTERFLY_NET, 150),
    NOOSE("Tracking", ItemID.NOOSE_WAND, 30),
    NOOSE_HERBIBOAR("Tracking", ItemID.NOOSE_WAND, 14);

    @Getter
    private final String Name;

    @Getter
    private final int ItemId;

    @Getter
    private final int PityThreshold;

    public int getOutfitRate(final int items) {
        return (int) Math.floor(this.getPityThreshold() / (1 + items * 0.0125f));
    }
}
