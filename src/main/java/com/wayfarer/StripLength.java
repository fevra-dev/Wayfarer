package com.wayfarer;

import lombok.RequiredArgsConstructor;

/**
 * Strip length presets, shortest to longest, named for OSRS blades. Each
 * takes a share of the game view's width, clamped so the strip stays
 * readable in a small window and doesn't sprawl in a large one.
 */
@RequiredArgsConstructor
public enum StripLength
{
	DAGGER("Dagger", 0.25, 200, 300),
	SCIMITAR("Scimitar", 0.32, 220, 400),
	LONGSWORD("Longsword", 0.40, 240, 480),
	GODSWORD("Godsword", 0.55, 320, 720);

	private final String label;
	private final double viewportShare;
	private final int minWidth;
	private final int maxWidth;

	int width(int viewportWidth)
	{
		return Math.max(minWidth, Math.min(maxWidth, (int) (viewportWidth * viewportShare)));
	}

	@Override
	public String toString()
	{
		return label;
	}
}
