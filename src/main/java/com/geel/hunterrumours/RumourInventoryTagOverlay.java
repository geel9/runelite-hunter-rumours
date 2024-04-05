package com.geel.hunterrumours;

import com.google.inject.Inject;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

public class RumourInventoryTagOverlay extends WidgetItemOverlay
{
	private final ItemManager itemManager;
	private final HunterRumoursPlugin plugin;
	private final HunterRumoursConfig config;

	@Inject
	private RumourInventoryTagOverlay(ItemManager itemManager, HunterRumoursPlugin plugin, HunterRumoursConfig config)
	{
		this.itemManager = itemManager;
		this.plugin = plugin;
		this.config = config;
		showOnInventory();
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		Rumour currentRumour = plugin.getCurrentRumour();
		if (currentRumour == null || currentRumour == Rumour.NONE)
		{
			return;
		}
		Rectangle bounds = widgetItem.getCanvasBounds();
		if (config.inventoryTags() && itemId == currentRumour.getItemId())
		{
			final BufferedImage outline = itemManager.getItemOutline(itemId, widgetItem.getQuantity(), Color.green);
			graphics.drawImage(outline, (int) bounds.getX(), (int) bounds.getY(), null);
		}
	}
}