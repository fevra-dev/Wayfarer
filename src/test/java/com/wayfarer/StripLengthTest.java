package com.wayfarer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class StripLengthTest
{
	@Test
	public void longswordKeepsTheOriginalWidthRule()
	{
		// 40% of the view, clamped to 240..480 -- what the strip always was.
		assertEquals(400, StripLength.LONGSWORD.width(1000));
		assertEquals(240, StripLength.LONGSWORD.width(300));
		assertEquals(480, StripLength.LONGSWORD.width(3000));
	}

	@Test
	public void presetsRunShortestToLongest()
	{
		for (int view : new int[]{500, 1000, 1600, 2560})
		{
			StripLength[] all = StripLength.values();
			for (int i = 1; i < all.length; i++)
			{
				assertTrue(all[i] + " should be at least as long as " + all[i - 1] + " at " + view + "px",
					all[i].width(view) >= all[i - 1].width(view));
			}
		}
	}
}
