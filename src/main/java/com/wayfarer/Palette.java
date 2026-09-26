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
 * - Ticks are aged paper, never pure white: pure #FFFFFF reads as UI
 *   chrome laid over the scene; paper reads as part of the world. Labels
 *   use SNOW_WHITE (still not pure white), which keeps 4.5:1 over bright
 *   scenery down to a 55% strip where PAPER needs 65% (contrast-sweep).
 * - Surfaces are warm black, never pure black or cool gray.
 * - Marker colors are classification, nothing else. They are the defaults;
 *   players can recolor markers in the config panel.
 */
final class Palette
{
	/** Aged paper — ticks and cardinal letters. */
	static final Color PAPER = new Color(0xD4, 0xCC, 0xBC);

	/** Warm black — the strip surface. */
	static final Color WARM_BLACK = new Color(0x0A, 0x09, 0x07);

	/** The single accent: dial-illumination amber. Pointing only. */
	static final Color AMBER = new Color(0xE8, 0xA0, 0x20);

	/**
	 * Marker for attackable NPCs (combat level &gt; 0). Signal orange: the
	 * warning colour, one step short of alarm red, and far enough round
	 * the hue wheel from AMBER (~22 vs ~38 degrees, with RESULT_YELLOW at
	 * ~52) that the pointing accent stays unique.
	 */
	static final Color SIGNAL_ORANGE = new Color(0xE8, 0x70, 0x2A);

	/**
	 * Marker for ground items. Red because the minimap already draws items
	 * as red dots: color as function means matching the meaning players
	 * already know. Deliberately dark (CIE L* ~42): at 4px, colours are told
	 * apart by lightness more than hue, so the warm markers step down in
	 * lightness — white 96, yellow 75, orange 61, red 42.
	 */
	static final Color SIGNAL_RED = new Color(0xB8, 0x30, 0x2A);

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
