package com.geel.hunterrumours;

import java.util.List;
import java.util.Map;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

import javax.annotation.Nonnull;
import java.awt.*;

public class RumourInfoBox extends InfoBox {
    public RumourInfoBox(Rumour rumour, @Nonnull Plugin plugin, @Nonnull ItemManager itemManager) {
        super(itemManager.getImage(rumour.getItemId()), plugin);

		Map<String, List<RumourLocation>> locations = RumourLocation.getGroupedLocationsForRumour(rumour);

		StringBuilder sb = new StringBuilder();

		locations.keySet().forEach(location ->  {
			sb.append("</br>" + location + " (" + locations.get(location).size() +" spawns)");
		});

		this.setTooltip(
                "Rumour: " + rumour.getFullName() +
                "</br>Item: " + itemManager.getItemComposition(rumour.getItemId()).getName() +
				"</br>Locations:" + sb

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
