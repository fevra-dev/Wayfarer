package com.wayfarer;

/**
 * Pure math for the compass strip: camera yaw (JAU14) to compass bearing,
 * and bearing deltas to horizontal strip positions. No RuneLite types, so
 * every function here is covered directly by CompassMathTest.
 *
 * Convention, derived from live source and worth restating because it is
 * easy to get backwards:
 *
 * - Camera yaw 0 faces NORTH, and increasing yaw turns the camera
 *   COUNTERCLOCKWISE on the compass (N -> W -> S -> E). Derived from
 *   Perspective#localToCanvasCpu: forward at yaw theta is
 *   (-sin theta, cos theta) in world axes, with dy=+1 = north per
 *   WorldArea#canTravelInDirection.
 * - Compass bearing runs the opposite way, CLOCKWISE from north
 *   (N=0, E=90, S=180, W=270), because that's what every compass in
 *   every game HUD means.
 *
 * Hence the sign flip in bearingDegrees(): bearing = -yaw, wrapped.
 */
final class CompassMath
{
	static final int JAU14_FULL_CIRCLE = 16384;

	private CompassMath()
	{
	}

	/**
	 * Camera yaw (JAU14, 0-16383, counterclockwise from north) to compass
	 * bearing in degrees (0-360, clockwise from north: N=0, E=90).
	 */
	static double bearingDegrees(int yawJau14)
	{
		int wrapped = Math.floorMod(JAU14_FULL_CIRCLE - yawJau14, JAU14_FULL_CIRCLE);
		return wrapped * 360.0 / JAU14_FULL_CIRCLE;
	}

	/**
	 * Signed shortest angular distance from heading to feature, in degrees,
	 * in [-180, 180). Positive means the feature sits clockwise of the
	 * heading, i.e. renders right of the strip's center.
	 */
	static double signedDeltaDegrees(double featureBearing, double headingBearing)
	{
		double delta = (featureBearing - headingBearing) % 360.0;
		if (delta < -180.0)
		{
			delta += 360.0;
		}
		else if (delta >= 180.0)
		{
			delta -= 360.0;
		}
		return delta;
	}

	/**
	 * Horizontal position of a feature on the strip as a fraction of the
	 * strip's half-width: 0 = center, -1/+1 = left/right edge. Callers
	 * cull anything with |fraction| > 1 rather than clamping, so features
	 * scroll off the ends instead of piling up there.
	 */
	static double screenOffsetFraction(double signedDeltaDegrees, double halfSpanDegrees)
	{
		return signedDeltaDegrees / halfSpanDegrees;
	}

	/**
	 * Compass bearing (degrees, clockwise from north) from me toward a
	 * target, given the world-axis delta: dxEast = target x - my x
	 * (positive east), dyNorth = target y - my y (positive north), both
	 * per WorldArea#canTravelInDirection's sign convention.
	 * atan2(east, north) is bearing by definition: due north (0, +1) is
	 * 0, due east (+1, 0) is 90.
	 */
	static double bearingToTarget(int dxEast, int dyNorth)
	{
		double bearing = Math.toDegrees(Math.atan2(dxEast, dyNorth));
		return bearing < 0 ? bearing + 360.0 : bearing;
	}

	/**
	 * Edge fade for strip elements: full alpha through the middle, linear
	 * fade to zero over the outer fadeZone fraction of each side. Input
	 * fraction is the screenOffsetFraction value, output is 0..1.
	 */
	static double edgeAlpha(double offsetFraction, double fadeZone)
	{
		double edgeDistance = 1.0 - Math.abs(offsetFraction);
		if (edgeDistance <= 0)
		{
			return 0;
		}
		if (edgeDistance >= fadeZone)
		{
			return 1.0;
		}
		return edgeDistance / fadeZone;
	}
}
