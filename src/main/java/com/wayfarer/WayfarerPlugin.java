package com.wayfarer;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
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

	@Inject
	private WayfarerConfig config;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		applyPlacement();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (WayfarerConfig.GROUP.equals(event.getGroup()) && "placement".equals(event.getKey()))
		{
			applyPlacement();
		}
	}

	/**
	 * RuneLite's snap points are a fixed set with no bottom-centre, so
	 * "Centred above chat" is self-positioned (DYNAMIC) rather than a
	 * snap. A saved drag location outranks even a DYNAMIC overlay's own
	 * placement (OverlayRenderer applies preferredLocation first), so it
	 * is cleared on the way in — the same reset as Alt+right-click.
	 */
	private void applyPlacement()
	{
		StripPlacement placement = config.placement();
		if (placement == StripPlacement.ABOVE_CHAT)
		{
			overlayManager.resetOverlay(overlay);
		}
		overlay.applyPlacement(placement);
	}

	@Provides
	WayfarerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WayfarerConfig.class);
	}
}
