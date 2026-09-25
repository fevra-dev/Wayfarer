package com.wayfarer;

import java.awt.Color;

/**
 * The plugin's entire color vocabulary, in one place. Braun-derived warm
 * neutrals (Rams: color as function, one accent per view) chosen to sit
 * inside RuneScape's parchment-and-gold aesthetic rather than fight it.
 *
 * Rules this file enforces by construction:
 * - One accent: AMBER, and it means exactly one thing — "where you are
 *   pointing" (the compass caret). Nothing else may be amber.
 * - Marks and text are aged paper, never pure white: pure #FFFFFF reads
 *   as UI chrome laid over the scene; paper reads as part of the world.
 * - Surfaces are warm black, never pure black or cool gray.
 * - Marker colors are classification, nothing else.
 */
final class Palette
{
	/** Aged paper — ticks and cardinal letters. */
	static final Color PAPER = new Color(0xD4, 0xCC, 0xBC);

	/** Warm black — the strip surface. */
	static final Color WARM_BLACK = new Color(0x0A, 0x09, 0x07);

	/** The single accent: dial-illumination amber. Pointing only. */
	static final Color AMBER = new Color(0xE8, 0xA0, 0x20);

	/** Marker for attackable NPCs (combat level &gt; 0) — red = threat. */
	static final Color SIGNAL_RED = new Color(0xD6, 0x45, 0x45);

	/**
	 * Marker for other players. Braun snow white — deliberately brighter
	 * than PAPER so player dots read above the tape's own marks.
	 */
	static final Color SNOW_WHITE = new Color(0xF5, 0xF2, 0xED);

	/**
	 * Marker for non-attackable NPCs. Braun result yellow, kept clearly
	 * yellower than AMBER so the pointing accent stays unique.
	 */
	static final Color RESULT_YELLOW = new Color(0xD4, 0xB8, 0x00);

	private Palette()
	{
	}

	static Color withAlpha(Color base, int alpha)
	{
		return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
	}
}
