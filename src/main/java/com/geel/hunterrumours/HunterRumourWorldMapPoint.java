package com.geel.hunterrumours;


import java.awt.Graphics;
import java.awt.image.BufferedImage;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.util.ImageUtil;

class HunterRumourWorldMapPoint extends WorldMapPoint
{
	private static BufferedImage mapArrow;

	private final BufferedImage hunterRumourWorldImage;
	private final Point hunterRumourWorldImagePoint;

	HunterRumourWorldMapPoint(final WorldPoint worldPoint, ItemManager manager, RumourLocation location)
	{
		super(worldPoint, null);

		// Prepare the image we're going to draw on the map
		hunterRumourWorldImage = new BufferedImage(getMapArrow().getWidth(), getMapArrow().getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics graphics = hunterRumourWorldImage.getGraphics();
		graphics.drawImage(getMapArrow(), 0, 0, null);
		graphics.drawImage(manager.getImage(location.getRumour().getItemId()), 0, 0, null);

		// Center image horizontally over world point
		hunterRumourWorldImagePoint = new Point(
			hunterRumourWorldImage.getWidth() / 2,
			hunterRumourWorldImage.getHeight());

		this.setSnapToEdge(true);
		this.setJumpOnClick(true);
		this.setName(location.getRumour().getName() + " (" + location.getLocationName() + ")");
		this.setImage(hunterRumourWorldImage);
		this.setImagePoint(hunterRumourWorldImagePoint);
	}

	@Override
	public void onEdgeSnap()
	{
		this.setImage(hunterRumourWorldImage);
		this.setImagePoint(null);
	}

	@Override
	public void onEdgeUnsnap()
	{
		this.setImage(hunterRumourWorldImage);
		this.setImagePoint(hunterRumourWorldImagePoint);
	}

	private static BufferedImage getMapArrow() {
		if(mapArrow == null) {
			mapArrow = ImageUtil.loadImageResource(HunterRumoursPlugin.class, "/util/hunter_rumour_arrow.png");
		}

		return mapArrow;
	}
}
