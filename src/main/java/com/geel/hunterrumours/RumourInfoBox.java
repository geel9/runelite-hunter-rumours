package com.geel.hunterrumours;

import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

import javax.annotation.Nonnull;
import java.awt.*;

public class RumourInfoBox extends InfoBox {
    public RumourInfoBox(Rumour rumour, @Nonnull Plugin plugin, @Nonnull ItemManager itemManager) {
        super(itemManager.getImage(rumour.getItemId()), plugin);

        this.setTooltip(
                "Rumour: " + rumour.getFullName() +
                " | Item: " + itemManager.getItemComposition(rumour.getItemId()).getName()
        );
    }

    @Override
    public String getText() {
        return "Rumour";
    }

    @Override
    public Color getTextColor() {
        return Color.WHITE;
    }
}
