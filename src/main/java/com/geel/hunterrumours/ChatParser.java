package com.geel.hunterrumours;

import com.geel.hunterrumours.enums.Hunter;
import com.geel.hunterrumours.enums.Rumour;
import net.runelite.api.Client;
import net.runelite.api.NPCComposition;

import javax.inject.Inject;
import java.util.Locale;

/**
 * Utility functions for parsing game/chat messages to extract rumour-related information,
 * such as which Hunter might be speaking, or which Hunter/Rumour is being referenced.
 */
public class ChatParser {
    private final Client client;

    @Inject
    public ChatParser(Client client) {
        this.client = client;
    }

    public Hunter getSpeakingHunter(String message) {
        for (Hunter hunter : Hunter.allValues()) {
            NPCComposition npc = client.getNpcDefinition(hunter.getNpcId());
            if (message.startsWith(npc.getName() + "|")) {
                return hunter;
            }
        }

        return Hunter.NONE;
    }

    public Hunter getReferencedHunter(String message) {
        message = message.toLowerCase();
        for (var hunterName : Hunter.allCommonNames()) {
            if (!message.contains(hunterName.toLowerCase())) {
                continue;
            }

            return Hunter.fromCommonName(hunterName);
        }

        return Hunter.NONE;
    }

    public Rumour getReferencedRumour(String message) {
        for (var rumour : Rumour.allValues()) {
            if (!message.contains(rumour.getName().toLowerCase())) {
                continue;
            }

            return rumour;
        }

        return Rumour.NONE;
    }
}
