package com.barrageindicator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import net.runelite.api.CollisionDataFlag;
import org.junit.Test;

/**
 * One mark per tile: with monsters of different sizes on the same south-west tile, the counts on it can differ
 * (the large one sees from more tiles). The tile keeps the largest count, and is the best if any of them is.
 */
public class OneMarkPerTileTest
{
	@Test
	public void theBestMarkNeverGoesAway() throws Exception
	{
		// a 1x1 and a 2x2 on (50,50), the 1x1 first in the client's list; scenery on (50,49);
		// a 1x1 on (49,49) and another on (51,49). Found by searching every such arrangement.
		TestWorld w = new TestWorld();
		w.flags[50][49] |= CollisionDataFlag.BLOCK_LINE_OF_SIGHT_FULL;
		w.add("Kalphite Worker", 50, 50, 1);
		w.add("Kalphite Soldier", 50, 50, 2);
		w.add("Kalphite Worker", 49, 49, 1);
		w.add("Kalphite Worker", 51, 49, 1);
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		String s = TestWorld.show(piles);
		BarrageIndicatorPlugin.Pile middle = TestWorld.at(piles, 50, 50);
		assertEquals(s, 4, middle.hits);
		assertTrue(s, middle.best);
		assertEquals(s, 3, TestWorld.at(piles, 49, 49).hits);
		assertFalse(s, TestWorld.at(piles, 49, 49).best);
		assertEquals(s, 3, piles.size());
	}

	@Test
	public void sameTileSameSizeIsOneMark() throws Exception
	{
		TestWorld w = new TestWorld();
		w.addMany("Smoke devil", 50, 50, 5);
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		assertEquals(TestWorld.show(piles), 1, piles.size());
	}
}
