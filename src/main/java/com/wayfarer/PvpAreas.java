package com.wayfarer;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import net.runelite.api.WorldType;

/**
 * Where other players are never marked. Showing which way nearby players are
 * works as a PK warning in PvP areas, which RuneLite does not accept, and
 * hiding them for everyone gives neither side the edge. The minimap still
 * shows them there, as it does for every player.
 */
final class PvpAreas
{
	/** Worlds built around fighting players, beyond what WorldType.isPvpWorld covers (PvP, Deadman). */
	private static final Set<WorldType> PVP_MINIGAME_WORLDS = EnumSet.of(WorldType.PVP_ARENA, WorldType.LAST_MAN_STANDING);

	private PvpAreas()
	{
	}

	/**
	 * @param inWilderness the game's own inside-Wilderness flag
	 * @param inPvpArea the game's PvP-area flag, set wherever players can attack each other
	 * @param worldTypes the current world's types
	 */
	static boolean hidesPlayers(boolean inWilderness, boolean inPvpArea, Collection<WorldType> worldTypes)
	{
		if (inWilderness || inPvpArea)
		{
			return true;
		}
		if (worldTypes == null || worldTypes.isEmpty())
		{
			return false;
		}
		if (WorldType.isPvpWorld(worldTypes))
		{
			return true;
		}
		for (WorldType type : worldTypes)
		{
			if (PVP_MINIGAME_WORLDS.contains(type))
			{
				return true;
			}
		}
		return false;
	}
}
