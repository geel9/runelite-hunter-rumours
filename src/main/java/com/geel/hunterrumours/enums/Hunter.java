package com.geel.hunterrumours.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.ItemID;
import net.runelite.api.ObjectID;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@AllArgsConstructor
public enum Hunter {
    NONE(0, "Unknown", HunterTier.NONE),

    MASTER_WOLF(13126, "Wolf", HunterTier.MASTER),
    EXPERT_TECO(13125, "Teco", HunterTier.EXPERT),
    EXPERT_ACO(13124, "Aco", HunterTier.EXPERT),
    ADEPT_CERVUS(13123, "Cervus", HunterTier.ADEPT),
    ADEPT_ORNUS(13122, "Ornus", HunterTier.ADEPT),
    NOVICE_GILMAN(13121, "Gilman", HunterTier.NOVICE);

    @Getter
    private final int NpcId;

    @Getter
    private final String CommonName;

    @Getter
    private final HunterTier Tier;

    public static Hunter[] allValues() {
        return Arrays.stream(Hunter.values()).filter(hunter -> hunter.NpcId != 0).toArray(Hunter[]::new);
    }

    private final static Map<Integer, Hunter> npcIdToHunter = new HashMap<Integer, Hunter>() {
        {
            for(var hunter : Hunter.allValues()) {
                put(hunter.NpcId, hunter);
            }
        }
    };

    private final static Map<String, Hunter> npcCommonNameToHunter = new HashMap<String, Hunter>() {
        {
            for(var hunter : Hunter.allValues()) {
                put(hunter.CommonName.toLowerCase(), hunter);
            }
        }
    };

    public static Hunter fromNpcId(int npcId) {
        return npcIdToHunter.getOrDefault(npcId, Hunter.NONE);
    }

    public static Hunter fromCommonName(String commonName) {
        return npcCommonNameToHunter.getOrDefault(commonName.toLowerCase(), Hunter.NONE);
    }

    public static String[] allCommonNames() {
        return npcCommonNameToHunter.keySet().toArray(new String[0]);
    }
}
