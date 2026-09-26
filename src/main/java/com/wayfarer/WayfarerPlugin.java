package com.wayfarer;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Wayfarer Compass",
	description = "A Skyrim-style compass strip: a heading tape that slides as you turn, with nearby players, NPCs and drops marked by direction",
	tags = {"compass", "heading", "direction", "skyrim", "navigation", "immersion", "hud"}
)
public class WayfarerPlugin extends Plugin
{
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WayfarerOverlay overlay;

	@Inject
	private GroundItemTiles groundItems;

	@Inject
	private MapIconObjects mapIcons;

	@Inject
	private Client client;

	@Inject
	private WayfarerConfig config;

	@Inject
	private ClientThread clientThread;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		applyCentring();
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				groundItems.seed(client.getTopLevelWorldView());
			}
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		// The overlay reads the set on the client thread; clear it there too.
		clientThread.invoke(() ->
		{
			groundItems.clear();
			mapIcons.clear();
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (WayfarerConfig.GROUP.equals(event.getGroup()) && "centreOnGameView".equals(event.getKey()))
		{
			applyCentring();
		}
	}

	/**
	 * A saved drag location outranks even a DYNAMIC overlay's own placement
	 * (OverlayRenderer applies preferredLocation first), so centring clears
	 * it on the way in — the same reset as Alt+right-click.
	 */
	private void applyCentring()
	{
		boolean centred = config.centreOnGameView();
		if (centred)
		{
			overlayManager.resetOverlay(overlay);
		}
		overlay.setCentred(centred);
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned event)
	{
		groundItems.spawned(event.getTile());
	}

	@Subscribe
	public void onItemDespawned(ItemDespawned event)
	{
		groundItems.despawned(event.getTile());
	}

	@Subscribe
	public void onWorldViewUnloaded(WorldViewUnloaded event)
	{
		groundItems.unloaded(event.getWorldView());
		mapIcons.unloaded(event.getWorldView());
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		mapIcons.spawned(event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		mapIcons.despawned(event.getGameObject());
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event)
	{
		mapIcons.spawned(event.getWallObject());
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned event)
	{
		mapIcons.despawned(event.getWallObject());
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		mapIcons.spawned(event.getDecorativeObject());
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		mapIcons.despawned(event.getDecorativeObject());
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		mapIcons.spawned(event.getGroundObject());
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		mapIcons.despawned(event.getGroundObject());
	}

	@Provides
	WayfarerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WayfarerConfig.class);
	}
}
