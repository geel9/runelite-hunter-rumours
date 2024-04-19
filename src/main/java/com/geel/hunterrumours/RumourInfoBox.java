package com.geel.hunterrumours;

import java.util.List;
import java.util.Map;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.util.ColorUtil;

import javax.annotation.Nonnull;
import java.awt.*;

public class RumourInfoBox extends InfoBox
{
	private HunterRumoursPlugin plugin;

	public RumourInfoBox(Rumour rumour, @Nonnull HunterRumoursPlugin plugin, @Nonnull ItemManager itemManager)
	{
		super(itemManager.getImage(rumour.getItemId()), plugin);
		this.plugin = plugin;

		final Map<String, List<RumourLocation>> locations = RumourLocation.getGroupedLocationsForRumour(rumour);
		final StringBuilder sb = new StringBuilder();

		locations.keySet().forEach(locationName -> {
			var keyedLocations = locations.get(locationName);
			RumourLocation rumourLocation = keyedLocations.get(0);

			sb.append("</br>  • ").append(locationName).append(" (");
			if (!rumourLocation.getFairyRingCode().equals(""))
			{
				sb.append(rumourLocation.getFairyRingCode()).append(", ");
			}
			sb.append(keyedLocations.size()).append(" spawns)");
		});

		final Trap trap = rumour.getTrap();
		final int pityThreshold = plugin.hasFullHunterKit ? trap.getFullOutfitRate() : trap.getPityThreshold();
		String hasFinishedRumourText = plugin.getHunterRumourState() ? "Yes" : "No";

		this.setTooltip(
                ColorUtil.wrapWithColorTag("Rumour: ", Color.YELLOW) + rumour.getFullName() + "</br>" +
            ColorUtil.wrapWithColorTag("Finished: ", Color.YELLOW) + hasFinishedRumourText + "</br>" +
            ColorUtil.wrapWithColorTag("Item: ", Color.YELLOW) + itemManager.getItemComposition(rumour.getItemId()).getName() + "</br>" +
            ColorUtil.wrapWithColorTag("Caught: ", Color.YELLOW) + plugin.getCaughtRumourCreatures() + " / " + pityThreshold + "</br>" +
            ColorUtil.wrapWithColorTag("Locations:", Color.YELLOW) + sb
		);
	}

	@Override
	public String getText()
	{
		if (plugin.getHunterRumourState())
		{
			return "Done";
		}

		final Rumour currentRumour = plugin.getCurrentRumour();
		if (currentRumour != Rumour.NONE && showNumUntilPity())
		{
			return String.valueOf(currentRumour.getTrap().getPityThreshold() - plugin.getCaughtRumourCreatures());
		}

		return "";
	}

	@Override
	public Color getTextColor()
	{
		if (plugin.getHunterRumourState())
		{
			return Color.GREEN;
		}
		else if(!showNumUntilPity()) {
		    return Color.WHITE;
        } else {
			final Rumour currentRumour = plugin.getCurrentRumour();
			if (currentRumour == Rumour.NONE)
			{
				return Color.WHITE;
			}
			final int caughtCreatures = plugin.getCaughtRumourCreatures();
			final float pityThreshold = (float)(plugin.isHasFullHunterKit() ? plugin.getCurrentRumour().getTrap().getFullOutfitRate() : plugin.getCurrentRumour().getTrap().getPityThreshold());
			final float percentage = (float)caughtCreatures / pityThreshold * 100f;
			if (percentage >= 75)
			{
				return Color.ORANGE.brighter();
			}
			else if (percentage >= 50)
			{
				return Color.ORANGE.darker();
			}
			else
			{
				return Color.RED;
			}
		}
	}

	private boolean showNumUntilPity() {
	    if(this.plugin == null || this.plugin.getConfig() == null) {
	        return false;
        }

	    return this.plugin.getConfig().showCatchesRemainingUntilPity();
    }
}