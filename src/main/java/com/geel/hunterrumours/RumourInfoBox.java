package com.geel.hunterrumours;

import java.util.List;
import java.util.Map;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;

import javax.annotation.Nonnull;
import java.awt.*;

public class RumourInfoBox extends InfoBox
{
	private HunterRumoursPlugin plugin;

	public RumourInfoBox(Rumour rumour, @Nonnull HunterRumoursPlugin plugin, @Nonnull ItemManager itemManager)
	{
		super(itemManager.getImage(rumour.getItemId()), plugin);

		this.plugin = plugin;

		Map<String, List<RumourLocation>> locations = RumourLocation.getGroupedLocationsForRumour(rumour);

		StringBuilder sb = new StringBuilder();

		locations.keySet().forEach(location -> {
			sb.append("</br>" + location + " (" + locations.get(location).size() + " spawns)");
		});

		String hasFinishedRumourText = plugin.getHunterRumourState() ? "Yes" : "No";

		this.setTooltip(
			"Rumour: " + rumour.getFullName() +
				"</br>Finished: " + hasFinishedRumourText +
				"</br>Item: " + itemManager.getItemComposition(rumour.getItemId()).getName() +
				"</br>Locations:" + sb

		);
	}

	@Override
	public String getText()
	{
		return "Rumour";
	}

	@Override
	public Color getTextColor()
	{
		if (plugin.getHunterRumourState())
		{
			return Color.GREEN;
		}
		else
		{
			return Color.WHITE;
		}
	}

}
