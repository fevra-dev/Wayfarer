package com.wayfarer;

import static com.wayfarer.PvpAreas.hidesPlayers;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.EnumSet;
import net.runelite.api.WorldType;
import org.junit.Test;

public class PvpAreasTest
{
	private static final EnumSet<WorldType> MEMBERS = EnumSet.of(WorldType.MEMBERS);

	@Test
	public void playersShowOnOrdinaryWorldsOutsidePvpAreas()
	{
		assertFalse(hidesPlayers(false, false, MEMBERS));
		assertFalse(hidesPlayers(false, false, EnumSet.noneOf(WorldType.class)));
		assertFalse(hidesPlayers(false, false, null));
	}

	@Test
	public void wildernessOrAnyPvpAreaHidesPlayers()
	{
		assertTrue(hidesPlayers(true, false, MEMBERS));
		assertTrue(hidesPlayers(false, true, MEMBERS));
	}

	@Test
	public void pvpWorldsHidePlayersEverywhere()
	{
		for (WorldType type : EnumSet.of(WorldType.PVP, WorldType.DEADMAN, WorldType.PVP_ARENA, WorldType.LAST_MAN_STANDING))
		{
			assertTrue(type.name(), hidesPlayers(false, false, EnumSet.of(WorldType.MEMBERS, type)));
		}
	}

	/** High-risk worlds are only dangerous in the Wilderness, which the flag already covers. */
	@Test
	public void otherWorldTypesDoNotHidePlayers()
	{
		for (WorldType type : EnumSet.of(WorldType.HIGH_RISK, WorldType.SKILL_TOTAL, WorldType.SEASONAL, WorldType.BETA_WORLD))
		{
			assertFalse(type.name(), hidesPlayers(false, false, EnumSet.of(type)));
		}
	}
}
