package com.geel.hunterrumours;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.ItemID;

@AllArgsConstructor
public enum Trap {
    SNARE("Bird snare", ItemID.BIRD_SNARE),
    DEADFALL("Deadfall", ItemID.LOGS),
    NET_TRAP("Net trap", ItemID.SMALL_FISHING_NET),
    PIT("Pit Trap", ItemID.TEASING_STICK),
    BOX_TRAP("Box Trap", ItemID.BOX_TRAP),
    FALCONRY("Falconry", ItemID.FALCONERS_GLOVE),
    BUTTERFLY("Butterfly Net", ItemID.BUTTERFLY_NET),
    NOOSE("Tracking", ItemID.NOOSE_WAND);

    @Getter
    private final String Name;

    @Getter
    private final int ItemId;
}
