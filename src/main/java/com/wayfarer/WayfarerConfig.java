package com.wayfarer;

import java.awt.Color;
import net.runelite.client.config.Alpha;
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
		name = "Strip",
		description = "How the compass strip looks",
		position = 0
	)
	String stripSection = "strip";

	@ConfigItem(
		keyName = "shape",
		name = "Shape",
		description = "Rounded pill ends or square corners",
		position = 0,
		section = stripSection
	)
	default StripShape shape()
	{
		return StripShape.PILL;
	}

	@ConfigSection(
		name = "Markers",
		description = "Dots along the bottom of the strip showing which way nearby players and NPCs are. Only what the minimap already shows is marked",
		position = 1
	)
	String markersSection = "markers";

	@ConfigItem(
		keyName = "showPlayers",
		name = "Players",
		description = "Mark other players",
		position = 1,
		section = markersSection
	)
	default boolean showPlayers()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "playerColor",
		name = "Player colour",
		description = "Colour of player markers. Nearer players draw brighter",
		position = 2,
		section = markersSection
	)
	default Color playerColor()
	{
		return Palette.SNOW_WHITE;
	}

	@ConfigItem(
		keyName = "showMonsters",
		name = "Monsters",
		description = "Mark NPCs you can attack",
		position = 3,
		section = markersSection
	)
	default boolean showMonsters()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "monsterColor",
		name = "Monster colour",
		description = "Colour of markers for NPCs you can attack. Nearer ones draw brighter",
		position = 4,
		section = markersSection
	)
	default Color monsterColor()
	{
		return Palette.SIGNAL_RED;
	}

	@ConfigItem(
		keyName = "showNpcs",
		name = "Other NPCs",
		description = "Mark NPCs you can't attack, such as bankers and shopkeepers",
		position = 5,
		section = markersSection
	)
	default boolean showNpcs()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "npcColor",
		name = "Other NPC colour",
		description = "Colour of markers for NPCs you can't attack. Nearer ones draw brighter",
		position = 6,
		section = markersSection
	)
	default Color npcColor()
	{
		return Palette.RESULT_YELLOW;
	}

	@Range(min = 1, max = 50)
	@ConfigItem(
		keyName = "nearbyRange",
		name = "Range",
		description = "How far away, in tiles, a player or NPC can be and still get a marker",
		position = 7,
		section = markersSection
	)
	default int nearbyRange()
	{
		return 25;
	}

	@ConfigItem(
		keyName = "distanceAsHeight",
		name = "Distance as height",
		description = "Raise markers the further away they are: close things sit at the bottom of the strip, things at the edge of range near the top",
		position = 8,
		section = markersSection
	)
	default boolean distanceAsHeight()
	{
		return false;
	}
}
