package com.wayfarer;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

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
		description = "Rounded pill ends, square corners, or pointed tips",
		position = 0,
		section = stripSection
	)
	default StripShape shape()
	{
		return StripShape.PILL;
	}

	@ConfigItem(
		keyName = "length",
		name = "Length",
		description = "How long the strip is, shortest to longest: Dagger, Scimitar, Longsword, Godsword",
		position = 1,
		section = stripSection
	)
	default StripLength length()
	{
		return StripLength.LONGSWORD;
	}

	@ConfigItem(
		keyName = "centreOnGameView",
		name = "Centre on game view",
		description = "Pin the strip to the exact top centre of the game view. RuneLite's own top-centre snap centres on the area left of the minimap and inventory, which sits left of true centre. While on, the strip can't be dragged",
		position = 2,
		section = stripSection
	)
	default boolean centreOnGameView()
	{
		return true;
	}

	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "backgroundOpacity",
		name = "Background opacity",
		description = "How solid the strip behind the letters is. Below 55% the letters lose contrast over bright scenery such as fog, sand and pale stone",
		position = 3,
		section = stripSection
	)
	default int backgroundOpacity()
	{
		return 55;
	}

	@ConfigSection(
		name = "Markers",
		description = "Dots on the strip showing which way nearby players, NPCs and ground items are. Only what the minimap already shows is marked",
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
		description = "Colour of player markers",
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
		description = "Colour of markers for NPCs you can attack",
		position = 4,
		section = markersSection
	)
	default Color monsterColor()
	{
		return Palette.SIGNAL_ORANGE;
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
		description = "Colour of markers for NPCs you can't attack",
		position = 6,
		section = markersSection
	)
	default Color npcColor()
	{
		return Palette.RESULT_YELLOW;
	}

	@ConfigItem(
		keyName = "showItems",
		name = "Ground items",
		description = "Mark tiles with items on the ground, such as drops. One marker per tile, like the minimap's red dot",
		position = 7,
		section = markersSection
	)
	default boolean showItems()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "itemColor",
		name = "Ground item colour",
		description = "Colour of ground item markers",
		position = 8,
		section = markersSection
	)
	default Color itemColor()
	{
		return Palette.SIGNAL_RED;
	}

	@Range(min = 4, max = 50)
	@ConfigItem(
		keyName = "nearbyRange",
		name = "Range",
		description = "How far away, in tiles, something can be and still get a marker",
		position = 9,
		section = markersSection
	)
	default int nearbyRange()
	{
		return 20;
	}

	@ConfigItem(
		keyName = "distanceAsHeight",
		name = "Distance as height",
		description = "Raise markers the further away they are: close things sit at the bottom of the strip, things at the edge of range near the top",
		position = 10,
		section = markersSection
	)
	default boolean distanceAsHeight()
	{
		return true;
	}

	@ConfigItem(
		keyName = "distanceAsSize",
		name = "Distance as size",
		description = "Shrink markers the further away they are: close things draw large, things at the edge of range small. Works together with distance as height",
		position = 11,
		section = markersSection
	)
	default boolean distanceAsSize()
	{
		return false;
	}

	@ConfigItem(
		keyName = "rangeFollowsZoom",
		name = "Range follows zoom",
		description = "Zoomed all the way out, markers reach your full Range; zooming in narrows them to the nearer third or so, the way the minimap shows less as it zooms in",
		position = 12,
		section = markersSection
	)
	default boolean rangeFollowsZoom()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shrinkWhenZoomedOut",
		name = "Shrink when zoomed out",
		description = "Like looking down from higher up: the further out your camera is zoomed, the smaller every marker draws",
		position = 13,
		section = markersSection
	)
	default boolean shrinkWhenZoomedOut()
	{
		return false;
	}
}
