package com.geel.hunterrumours;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.awt.Color;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.MenuAction;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.ComponentID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;

@Singleton
@Slf4j
public class RumourItemHandler
{
	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private ItemManager itemManager;
	@Inject
	private ScheduledExecutorService executor;
	@Inject
	private HunterRumoursPlugin plugin;
	@Inject
	private HunterRumoursConfig config;

	private final Table<WorldPoint, Integer, GroundItem> collectedGroundItems = HashBasedTable.create();
	private final Map<WorldPoint, Lootbeam> lootbeams = new HashMap<>();
	private final Queue<Integer> droppedItemQueue = EvictingQueue.create(16); // recently dropped items
	private int lastUsedItem;

	protected void startUp()
	{
		executor.execute(this::reset);
		lastUsedItem = -1;
	}

	protected void shutDown()
	{
		collectedGroundItems.clear();
		clientThread.invokeLater(this::removeAllLootbeams);
	}

	public void onConfigChanged(ConfigChanged event)
	{
		executor.execute(this::reset);
	}

	public void onItemDespawned(ItemDespawned itemDespawned)
	{
		TileItem item = itemDespawned.getItem();
		Tile tile = itemDespawned.getTile();

		GroundItem groundItem = collectedGroundItems.get(tile.getWorldLocation(), item.getId());
		if (groundItem == null)
		{
			return;
		}

		if (groundItem.getQuantity() <= item.getQuantity())
		{
			collectedGroundItems.remove(tile.getWorldLocation(), item.getId());
		}
		else
		{
			groundItem.setQuantity(groundItem.getQuantity() - item.getQuantity());
			// When picking up an item when multiple stacks appear on the ground,
			// it is not known which item is picked up, so we invalidate the spawn
			// time
			groundItem.setSpawnTime(null);
			groundItem.reset();
		}

		handleLootbeam(tile.getWorldLocation());
	}

	public void onItemSpawned(ItemSpawned itemSpawned)
	{
		// Create beam / colour
		TileItem item = itemSpawned.getItem();
		Tile tile = itemSpawned.getTile();

		GroundItem groundItem = buildGroundItem(tile, item);
		GroundItem existing = collectedGroundItems.get(tile.getWorldLocation(), item.getId());
		if (existing != null)
		{
			existing.setQuantity(existing.getQuantity() + groundItem.getQuantity());
			// The spawn time remains set at the oldest spawn
			existing.reset();
		}
		else
		{
			collectedGroundItems.put(tile.getWorldLocation(), item.getId(), groundItem);
		}

		handleLootbeam(tile.getWorldLocation());
	}

	public void onMenuOptionClicked(MenuOptionClicked menuOptionClicked)
	{
		if (menuOptionClicked.isItemOp() && menuOptionClicked.getMenuOption().equals("Drop"))
		{
			int itemId = menuOptionClicked.getItemId();
			// Keep a queue of recently dropped items to better detect
			// item spawns that are drops
			droppedItemQueue.add(itemId);
		}
		else if (menuOptionClicked.getMenuAction() == MenuAction.WIDGET_TARGET_ON_GAME_OBJECT
			&& client.getSelectedWidget() != null
			&& client.getSelectedWidget().getId() == ComponentID.INVENTORY_CONTAINER)
		{
			lastUsedItem = client.getSelectedWidget().getItemId();
		}
	}

	public void onGameStateChanged(final GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			collectedGroundItems.clear();
			lootbeams.clear();
		}
	}

	private void handleLootbeam(WorldPoint worldPoint)
	{
		Rumour currentRumour = plugin.getCurrentRumour();
		Collection<GroundItem> groundItems = collectedGroundItems.row(worldPoint).values();
		for (GroundItem groundItem : groundItems)
		{
			if (currentRumour != null
				&& currentRumour != Rumour.NONE
				&& currentRumour.getItemId() == groundItem.getItemId()
				&& config.lootbeams())
			{
				addLootbeam(worldPoint, config.lootbeamColor());
				return;
			}
		}

		removeLootbeam(worldPoint);
	}

	private void handleLootbeams()
	{
		for (WorldPoint worldPoint : collectedGroundItems.rowKeySet())
		{
			handleLootbeam(worldPoint);
		}
	}

	private void removeAllLootbeams()
	{
		for (Lootbeam lootbeam : lootbeams.values())
		{
			lootbeam.remove();
		}

		lootbeams.clear();
	}

	private void addLootbeam(WorldPoint worldPoint, Color color)
	{
		Lootbeam lootbeam = lootbeams.get(worldPoint);
		if (lootbeam == null)
		{
			lootbeam = new Lootbeam(client, clientThread, worldPoint, color, Lootbeam.Style.MODERN);
			lootbeams.put(worldPoint, lootbeam);
		}
		else
		{
			lootbeam.setColor(color);
			lootbeam.setStyle(Lootbeam.Style.MODERN);
		}
	}

	private void removeLootbeam(WorldPoint worldPoint)
	{
		Lootbeam lootbeam = lootbeams.remove(worldPoint);
		if (lootbeam != null)
		{
			lootbeam.remove();
		}
	}

	private GroundItem buildGroundItem(final Tile tile, final TileItem item)
	{
		// Collect the data for the item
		final int itemId = item.getId();
		final ItemComposition itemComposition = itemManager.getItemComposition(itemId);
		final int realItemId = itemComposition.getNote() != -1 ? itemComposition.getLinkedNoteId() : itemId;
		final int alchPrice = itemComposition.getHaPrice();
		final boolean dropped = tile.getWorldLocation().equals(client.getLocalPlayer().getWorldLocation()) && droppedItemQueue.remove(itemId);
		final boolean table = itemId == lastUsedItem && tile.getItemLayer().getHeight() > 0;

		final GroundItem groundItem = GroundItem.builder()
			.id(itemId)
			.location(tile.getWorldLocation())
			.itemId(realItemId)
			.quantity(item.getQuantity())
			.name(itemComposition.getName())
			.haPrice(alchPrice)
			.height(tile.getItemLayer().getHeight())
			.tradeable(itemComposition.isTradeable())
			.lootType(dropped ? LootType.DROPPED : (table ? LootType.TABLE : LootType.UNKNOWN))
			.spawnTime(Instant.now())
			.stackable(itemComposition.isStackable())
			.build();


		return groundItem;
	}

	private void reset()
	{
		clientThread.invokeLater(() -> collectedGroundItems.values().forEach(GroundItem::reset));
		clientThread.invokeLater(this::handleLootbeams);
	}
}
