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
	 * Eases a marker's shown bearing toward its true bearing so nothing
	 * on the strip moves in a jarring snap: an exponential approach with
	 * time constant tauSeconds, and never faster than maxDegreesPerSecond.
	 * Takes the short way round across north. Only the target's world
	 * bearing is eased — the camera heading is not — so turning the camera
	 * still moves the tape and every marker instantly.
	 */
	static double smoothBearing(double shown, double target, double dtSeconds, double tauSeconds, double maxDegreesPerSecond)
	{
		if (dtSeconds <= 0)
		{
			return shown;
		}
		double delta = signedDeltaDegrees(target, shown);
		double step = delta * (1.0 - Math.exp(-dtSeconds / tauSeconds));
		double cap = maxDegreesPerSecond * dtSeconds;
		step = Math.max(-cap, Math.min(cap, step));
		double result = (shown + step) % 360.0;
		return result < 0 ? result + 360.0 : result;
	}

	/**
	 * Marker range under "range follows zoom": the full range when zoomed
	 * all the way out (zoomIn 0), shrinking linearly to minFraction of it
	 * when zoomed all the way in (zoomIn 1). Never below floorTiles (or the
	 * full range, if that is smaller).
	 */
	static double zoomedRange(double rangeTiles, double zoomIn, double minFraction, double floorTiles)
	{
		double z = Math.max(0.0, Math.min(1.0, zoomIn));
		return Math.max(Math.min(floorTiles, rangeTiles), rangeTiles * (1.0 - (1.0 - minFraction) * z));
	}

	/**
	 * True when a marker's true bearing has jumped more than snapDegrees from
	 * where it is shown — e.g. someone running straight through you, whose
	 * bearing flips ~180 degrees. Easing that would slide the marker the
	 * long way across the strip; the caller snaps and fades it instead.
	 */
	static boolean isFlip(double shown, double target, double snapDegrees)
	{
		return Math.abs(signedDeltaDegrees(target, shown)) > snapDegrees;
	}

	/** Fade-in for a marker that appeared or snapped ageSeconds ago: 0 -> 1 over fadeSeconds. */
	static double appearFade(double ageSeconds, double fadeSeconds)
	{
		return Math.max(0.0, Math.min(1.0, ageSeconds / fadeSeconds));
	}

	/**
	 * Near-field fade for markers. Bearing to a moving actor changes at
	 * speed / distance, so something passing within a tile or two swings
	 * across the whole strip in a fraction of a second — the most
	 * eye-catching motion on the HUD, for the actor you can already see
	 * in the scene. Returns floor at distance 0, rising linearly to 1.0 at
	 * fadeTiles and beyond. Quiets the flick without delaying or moving
	 * the marker, so it still points truthfully.
	 */
	static double nearFade(double distanceTiles, double fadeTiles, double floor)
	{
		if (distanceTiles >= fadeTiles)
		{
			return 1.0;
		}
		return floor + (1.0 - floor) * Math.max(0.0, distanceTiles) / fadeTiles;
	}

	/**
	 * Linear mapping from distance (0 = beside you, 1 = range cap) to a
	 * pixel value, clamped to [near, far]. Used for marker height (near at
	 * the bottom rail, far rising toward the top, like distant objects
	 * sitting higher toward the horizon) and marker size (near large, far
	 * small).
	 */
	static int byDistance(double distanceFraction, int near, int far)
	{
		double f = Math.max(0.0, Math.min(1.0, distanceFraction));
		return (int) Math.round(near + (far - near) * f);
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
