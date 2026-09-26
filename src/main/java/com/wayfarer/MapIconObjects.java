package com.wayfarer;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.SpritePixels;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.worldmap.MapElementConfig;

/**
 * Scenery in the top-level world that carries a minimap icon (banks,
 * altars, shops, rare trees...), kept from object spawn/despawn events so
 * the overlay never scans the scene per frame. The icon is the object's
 * own map function, the same one the minimap draws.
 *
 * Discovery: which icon ids mean "bank" or "altar" isn't named anywhere
 * in the API, so each new icon id is logged once at debug level with its
 * category and the object's name. The toggle groups are built from that.
 *
 * All calls arrive on the client thread (spawn events), so plain
 * collections are enough.
 */
@Slf4j
@Singleton
class MapIconObjects
{
	/** Object -> its minimap icon id. */
	private final Map<TileObject, Integer> icons = new HashMap<>();
	/** Icon ids already logged, so discovery prints each one once. */
	private final Set<Integer> logged = new HashSet<>();
	/** Per icon id, looked up once: its world-map category and its sprite. */
	private final Map<Integer, Integer> categories = new HashMap<>();
	private final Map<Integer, BufferedImage> sprites = new HashMap<>();

	private final Client client;

	@Inject
	MapIconObjects(Client client)
	{
		this.client = client;
	}

	void spawned(TileObject object)
	{
		WorldView view = object.getWorldView();
		if (view == null || !view.isTopLevel())
		{
			return; // boat scenery: not on the main-world minimap
		}
		ObjectComposition def = client.getObjectDefinition(object.getId());
		if (def == null)
		{
			return;
		}
		if (def.getImpostorIds() != null && def.getImpostor() != null)
		{
			def = def.getImpostor();
		}
		int iconId = def.getMapIconId();
		if (iconId < 0)
		{
			return;
		}
		icons.put(object, iconId);
		if (logged.add(iconId))
		{
			log.debug("Wayfarer map icon: icon={} category={} object={} '{}' at {}",
				iconId, category(iconId), object.getId(), def.getName(),
				object.getWorldLocation());
		}
	}

	void despawned(TileObject object)
	{
		icons.remove(object);
	}

	/**
	 * Spawn events already fired for scenery present before the plugin
	 * started, so read the scene once. A single pass at startup, not per
	 * frame (same as GroundItemTiles.seed).
	 */
	void seed(WorldView worldView)
	{
		icons.clear();
		for (Tile[][] plane : worldView.getScene().getTiles())
		{
			for (Tile[] row : plane)
			{
				for (Tile tile : row)
				{
					if (tile == null)
					{
						continue;
					}
					GameObject[] objects = tile.getGameObjects();
					if (objects != null)
					{
						for (GameObject object : objects)
						{
							spawnedIfPresent(object);
						}
					}
					spawnedIfPresent(tile.getWallObject());
					spawnedIfPresent(tile.getDecorativeObject());
					spawnedIfPresent(tile.getGroundObject());
				}
			}
		}
	}

	private void spawnedIfPresent(TileObject object)
	{
		if (object != null)
		{
			spawned(object);
		}
	}

	/** The scene reloaded; every object we hold belongs to the old one. */
	void unloaded(WorldView worldView)
	{
		if (worldView.isTopLevel())
		{
			icons.clear();
		}
	}

	void clear()
	{
		icons.clear();
	}

	/** World-map category of an icon id (see IconGroup), cached. */
	int category(int iconId)
	{
		return categories.computeIfAbsent(iconId, id ->
		{
			MapElementConfig config = client.getMapElementConfig(id);
			return config == null ? -1 : config.getCategory();
		});
	}

	/** The icon's own minimap sprite, cached; null if the game has none. */
	BufferedImage sprite(int iconId)
	{
		if (!sprites.containsKey(iconId))
		{
			MapElementConfig config = client.getMapElementConfig(iconId);
			SpritePixels pixels = config == null ? null : config.getMapIcon(false);
			sprites.put(iconId, pixels == null ? null : pixels.toBufferedImage());
		}
		return sprites.get(iconId);
	}

	Map<TileObject, Integer> icons()
	{
		return Collections.unmodifiableMap(icons);
	}
}
