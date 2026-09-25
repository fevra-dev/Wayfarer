package com.wayfarer;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Wayfarer Compass",
	description = "A Skyrim-style compass strip: a heading tape that slides as you turn, with nearby players and NPCs marked by direction",
	tags = {"compass", "heading", "direction", "skyrim", "navigation", "immersion", "hud"}
)
public class WayfarerPlugin extends Plugin
{
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WayfarerOverlay overlay;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
	}

	@Provides
	WayfarerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WayfarerConfig.class);
	}
}
