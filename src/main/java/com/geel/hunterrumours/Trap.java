package com.geel.hunterrumours;

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
    NOOSE("Tracking", ItemID.NOOSE_WAND, 30);

    @Getter
    private final String Name;

    @Getter
    private final int ItemId;

    @Getter
    private final int PityThreshold;

    // TODO: Investigate if partial outfit provides any benefits.
    public int getFullOutfitRate() {
        return (int)Math.floor((double)this.getPityThreshold() * 0.95d);
    }
}
