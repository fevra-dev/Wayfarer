package com.wayfarer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Singleton;
import net.runelite.api.ItemLayer;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.WorldView;

/**
 * Tiles holding ground items in the top-level world, kept from item
 * spawn/despawn events so the overlay never scans the scene per frame.
 * One marker per tile, like the minimap's red dot, however many items
 * are stacked there.
 *
 * All calls arrive on the client thread (events, the startup seed, and
 * overlay rendering), so a plain HashMap is enough.
 */
@Singleton
class GroundItemTiles
{
	/** Tile -> number of item stacks on it. */
	private final Map<Tile, Integer> counts = new HashMap<>();

	void spawned(Tile tile)
	{
		if (isTopLevel(tile))
		{
			counts.merge(tile, 1, Integer::sum);
		}
	}

	void despawned(Tile tile)
	{
		// Returning null from the remapping function removes the entry.
		counts.computeIfPresent(tile, (t, n) -> n > 1 ? n - 1 : null);
	}

	/** The scene reloaded; every tile we hold belongs to the old one. */
	void unloaded(WorldView worldView)
	{
		if (worldView.isTopLevel())
		{
			counts.clear();
		}
	}

	/**
	 * Item spawn events already fired for items present before the
	 * plugin started, so read them once from the scene. A single pass at
	 * startup, not per frame.
	 */
	void seed(WorldView worldView)
	{
		counts.clear();
		Scene scene = worldView.getScene();
		for (Tile[][] plane : scene.getTiles())
		{
			for (Tile[] row : plane)
			{
				for (Tile tile : row)
				{
					if (tile == null)
					{
						continue;
					}
					List<TileItem> items = tile.getGroundItems();
					if (items != null && !items.isEmpty())
					{
						counts.put(tile, items.size());
					}
				}
			}
		}
	}

	void clear()
	{
		counts.clear();
	}

	Set<Tile> tiles()
	{
		return Collections.unmodifiableSet(counts.keySet());
	}

	/**
	 * Items on boats (Sailing) live in the boat's own world view, whose
	 * local coordinates don't line up with the main world's; they are
	 * left out, the same as actors aboard other boats.
	 */
	private static boolean isTopLevel(Tile tile)
	{
		ItemLayer layer = tile.getItemLayer();
		WorldView view = layer == null ? null : layer.getWorldView();
		return view != null && view.isTopLevel();
	}
}
