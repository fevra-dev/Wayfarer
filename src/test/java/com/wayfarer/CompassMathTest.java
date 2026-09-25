package com.wayfarer;

import static com.wayfarer.CompassMath.bearingDegrees;
import static com.wayfarer.CompassMath.bearingToTarget;
import static com.wayfarer.CompassMath.edgeAlpha;
import static com.wayfarer.CompassMath.nearFade;
import static com.wayfarer.CompassMath.screenOffsetFraction;
import static com.wayfarer.CompassMath.signedDeltaDegrees;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class CompassMathTest
{
	private static final double EPS = 1e-9;

	/**
	 * Yaw increases counterclockwise (N -> W -> S -> E); bearing increases
	 * clockwise (N -> E -> S -> W).
	 */
	@Test
	public void cardinalYawsMapToCompassBearings()
	{
		assertEquals(0.0, bearingDegrees(0), EPS);
		assertEquals(270.0, bearingDegrees(4096), EPS);
		assertEquals(180.0, bearingDegrees(8192), EPS);
		assertEquals(90.0, bearingDegrees(12288), EPS);
	}

	@Test
	public void bearingWrapsIntoZeroTo360()
	{
		double nearFull = bearingDegrees(1);
		assertTrue("got " + nearFull, nearFull > 359.9 && nearFull < 360.0);
		assertEquals(270.0, bearingDegrees(16384 + 4096), EPS);
		assertEquals(90.0, bearingDegrees(-4096), EPS);
	}

	@Test
	public void signedDeltaTakesTheShortWayAcrossNorth()
	{
		assertEquals(20.0, signedDeltaDegrees(10, 350), EPS);
		assertEquals(-20.0, signedDeltaDegrees(350, 10), EPS);
		assertEquals(45.0, signedDeltaDegrees(90, 45), EPS);
		assertEquals(0.0, signedDeltaDegrees(0, 0), EPS);
		// Directly behind maps to one edge (-180), never both.
		assertEquals(-180.0, signedDeltaDegrees(180, 0), EPS);
	}

	@Test
	public void screenFractionIsLinearAndUncapped()
	{
		assertEquals(0.0, screenOffsetFraction(0, 60), EPS);
		assertEquals(1.0, screenOffsetFraction(60, 60), EPS);
		assertEquals(-0.5, screenOffsetFraction(-30, 60), EPS);
		// Beyond the span exceeds 1 so the caller culls it rather than piling it at the edge.
		assertTrue(screenOffsetFraction(90, 60) > 1.0);
	}

	/**
	 * The convention that is easy to get backwards: turning the camera
	 * right (toward east) must slide the tape LEFT, and turning right is
	 * DECREASING yaw.
	 */
	@Test
	public void turningRightSlidesTheTapeLeft()
	{
		double before = screenOffsetFraction(signedDeltaDegrees(0, 0), 60);
		double after = screenOffsetFraction(signedDeltaDegrees(0, 10), 60);
		assertEquals(0.0, before, EPS);
		assertTrue("N should move left of the caret, got " + after, after < 0.0);

		assertEquals(10.0, bearingDegrees(16384 - 455), 0.01);
	}

	@Test
	public void edgeFadeIsSolidInTheMiddleAndZeroAtTheEdges()
	{
		assertEquals(1.0, edgeAlpha(0, 0.22), EPS);
		assertEquals(0.0, edgeAlpha(1.0, 0.22), EPS);
		assertEquals(0.0, edgeAlpha(-1.0, 0.22), EPS);
		assertEquals(0.5, edgeAlpha(0.89, 0.22), 0.001);
	}

	@Test
	public void nearFadeQuietsOnlyTheLastFewTiles()
	{
		assertEquals(0.2, nearFade(0, 4, 0.2), EPS);
		assertEquals(0.6, nearFade(2, 4, 0.2), EPS);
		assertEquals(1.0, nearFade(4, 4, 0.2), EPS);
		assertEquals(1.0, nearFade(25, 4, 0.2), EPS);
		// Never brighter than full, never below the floor.
		assertEquals(0.2, nearFade(-1, 4, 0.2), EPS);
	}

	@Test
	public void bearingToTargetUsesEastAndNorthAxes()
	{
		assertEquals(0.0, bearingToTarget(0, 1), EPS);
		assertEquals(90.0, bearingToTarget(1, 0), EPS);
		assertEquals(180.0, bearingToTarget(0, -1), EPS);
		assertEquals(270.0, bearingToTarget(-1, 0), EPS);
		assertEquals(225.0, bearingToTarget(-1, -1), 0.001);
	}

	@Test
	public void facingEastADueNorthMarkerSitsLeftOfTheCaret()
	{
		double fraction = screenOffsetFraction(signedDeltaDegrees(bearingToTarget(0, 1), 90.0), 60);
		assertTrue("got " + fraction, fraction < 0);
	}
}
