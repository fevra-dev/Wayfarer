package com.wayfarer;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
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
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
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
		clientThread.invoke(groundItems::clear);
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
	}

	@Provides
	WayfarerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WayfarerConfig.class);
	}
}
