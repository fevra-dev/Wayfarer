package com.wayfarer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(WayfarerConfig.GROUP)
public interface WayfarerConfig extends Config
{
	String GROUP = "wayfarercompass";

	@ConfigSection(
		name = "Markers",
		description = "Dots along the bottom of the strip showing which way nearby players and NPCs are. Only what the minimap already shows is marked",
		position = 0
	)
	String markersSection = "markers";

	@ConfigItem(
		keyName = "showPlayers",
		name = "Players",
		description = "Mark other players (white)",
		position = 1,
		section = markersSection
	)
	default boolean showPlayers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMonsters",
		name = "Monsters",
		description = "Mark NPCs you can attack (red)",
		position = 2,
		section = markersSection
	)
	default boolean showMonsters()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showNpcs",
		name = "Other NPCs",
		description = "Mark NPCs you can't attack, such as bankers and shopkeepers (yellow)",
		position = 3,
		section = markersSection
	)
	default boolean showNpcs()
	{
		return true;
	}

	@Range(min = 1, max = 50)
	@ConfigItem(
		keyName = "nearbyRange",
		name = "Range",
		description = "How far away, in tiles, a player or NPC can be and still get a marker",
		position = 4,
		section = markersSection
	)
	default int nearbyRange()
	{
		return 25;
	}
}
