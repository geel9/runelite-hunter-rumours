package com.geel.hunterrumours;


import java.awt.Graphics;
import java.awt.image.BufferedImage;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

class HunterRumourWorldMapPoint extends WorldMapPoint
{
	private final HunterRumoursPlugin plugin;
	private final BufferedImage hunterRumourWorldImage;
	private final Point hunterRumourWorldImagePoint;

	HunterRumourWorldMapPoint(final WorldPoint worldPoint, HunterRumoursPlugin plugin, RumourLocation location)
	{
		super(worldPoint, null);

		hunterRumourWorldImage = new BufferedImage(plugin.getMapArrow().getWidth(), plugin.getMapArrow().getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics graphics = hunterRumourWorldImage.getGraphics();
		graphics.drawImage(plugin.getMapArrow(), 0, 0, null);
		graphics.drawImage(plugin.getHunterRumourImage(), 0, 0, null);
		hunterRumourWorldImagePoint = new Point(
			hunterRumourWorldImage.getWidth() / 2,
			hunterRumourWorldImage.getHeight());

		this.plugin = plugin;
		this.setSnapToEdge(true);
		this.setJumpOnClick(true);
		this.setName(location.getRumour().getName() + " (" + location.getLocationName() + ")");
		this.setImage(hunterRumourWorldImage);
		this.setImagePoint(hunterRumourWorldImagePoint);
	}

	@Override
	public void onEdgeSnap()
	{
		this.setImage(plugin.getHunterRumourImage());
		this.setImagePoint(null);
	}

	@Override
	public void onEdgeUnsnap()
	{
		this.setImage(hunterRumourWorldImage);
		this.setImagePoint(hunterRumourWorldImagePoint);
	}
}
