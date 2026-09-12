package com.geel.hunterrumours;

import net.runelite.api.Menu;
import net.runelite.api.MenuEntry;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

import java.awt.Color;

final class FairyRingFavouriteMenuHighlighter {
    private static final String FAVOURITES = "Favourites";
    private static final Color RUMOUR_COLOR = Color.GREEN;

    private FairyRingFavouriteMenuHighlighter() {
    }

    static boolean highlight(MenuEntry[] entries, String fairyRingCode) {
        if (entries == null || fairyRingCode == null || fairyRingCode.length() != 3) {
            return false;
        }

        for (MenuEntry entry : entries) {
            Menu submenu = entry.getSubMenu();
            if (submenu == null || !FAVOURITES.equals(Text.removeTags(entry.getOption()))) {
                continue;
            }

            for (MenuEntry favourite : submenu.getMenuEntries()) {
                String option = Text.removeTags(favourite.getOption());
                if (fairyRingCode.equalsIgnoreCase(option)) {
                    favourite.setOption(ColorUtil.prependColorTag(option, RUMOUR_COLOR));
                    return true;
                }
            }
        }
        return false;
    }
}
