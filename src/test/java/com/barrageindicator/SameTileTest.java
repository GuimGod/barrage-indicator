package com.barrageindicator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import net.runelite.api.CollisionDataFlag;
import org.junit.Test;

/**
 * Monsters on the same tile (areas on top of each other) are hit together. RuneLite's line of sight says
 * "no sight" between areas that overlap, so the plugin has to count those itself. Whole path of the plugin,
 * real line of sight.
 */
public class SameTileTest
{
	@Test
	public void nineOnOneTileCountNine() throws Exception
	{
		TestWorld w = new TestWorld();
		w.addMany("Smoke devil", 50, 50, 9);
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		assertEquals(TestWorld.show(piles), 1, piles.size());
		assertEquals(9, piles.get(0).hits);
		assertTrue(piles.get(0).best);
	}

	@Test
	public void aPileOfSixBeatsFourSpreadOut() throws Exception
	{
		TestWorld w = new TestWorld();
		w.addMany("Smoke devil", 50, 50, 6);
		w.add("Smoke devil", 60, 60, 1);
		w.add("Smoke devil", 61, 60, 1);
		w.add("Smoke devil", 60, 61, 1);
		w.add("Smoke devil", 61, 61, 1);
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		BarrageIndicatorPlugin.Pile six = TestWorld.at(piles, 50, 50);
		assertNotNull(TestWorld.show(piles), six);
		assertEquals(6, six.hits);
		assertTrue(six.best);
		for (BarrageIndicatorPlugin.Pile p : piles)
		{
			if (p != six)
			{
				assertEquals(4, p.hits);
				assertFalse(p.best);
			}
		}
	}

	@Test
	public void pilesOfFiveTwoAndTwoAroundThePlayer() throws Exception
	{
		// north x5, west x2, east x2: a cast on the north pile hits all 9, one on a side pile 7
		TestWorld w = new TestWorld();
		w.addMany("Greater Nechryael", 50, 51, 5);
		w.addMany("Greater Nechryael", 49, 50, 2);
		w.addMany("Greater Nechryael", 51, 50, 2);
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		String s = TestWorld.show(piles);
		assertEquals(s, 9, TestWorld.at(piles, 50, 51).hits);
		assertTrue(s, TestWorld.at(piles, 50, 51).best);
		assertEquals(s, 7, TestWorld.at(piles, 49, 50).hits);
		assertFalse(s, TestWorld.at(piles, 49, 50).best);
		assertEquals(s, 7, TestWorld.at(piles, 51, 50).hits);
		assertFalse(s, TestWorld.at(piles, 51, 50).best);
	}

	@Test
	public void threeLargeMonstersOnTopOfEachOther() throws Exception
	{
		// three 2x2 with south-west tiles one apart (araxytes, hellhounds): each cast hits the three
		TestWorld w = new TestWorld();
		w.add("Araxyte", 50, 50, 2);
		w.add("Araxyte", 51, 50, 2);
		w.add("Araxyte", 50, 51, 2);
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		assertEquals(TestWorld.show(piles), 3, piles.size());
		for (BarrageIndicatorPlugin.Pile p : piles)
		{
			assertEquals(3, p.hits);
			assertTrue(p.best);
		}
	}

	/** The bones of the Smoke Devil Dungeon on (50,50), blocking sight, and smoke devils on each side. */
	private static TestWorld bones(int north, int east, int south, int west)
	{
		TestWorld w = new TestWorld();
		w.flags[50][50] |= CollisionDataFlag.BLOCK_LINE_OF_SIGHT_FULL | CollisionDataFlag.BLOCK_MOVEMENT_FULL;
		w.addMany("Smoke devil", 50, 51, north);
		w.addMany("Smoke devil", 51, 50, east);
		w.addMany("Smoke devil", 50, 49, south);
		w.addMany("Smoke devil", 49, 50, west);
		return w;
	}

	@Test
	public void sightRoundTheBonesStaysOneWay() throws Exception
	{
		// seen in game (2026-10-12): cast on the east one, the north one is hit; cast on the north one, the east one is not
		TestWorld w = bones(1, 1, 0, 0);
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		String s = TestWorld.show(piles);
		assertEquals(s, 2, TestWorld.at(piles, 51, 50).hits);
		assertNull("a cast on the north one hits it alone: below the smallest pile, not drawn " + s, TestWorld.at(piles, 50, 51));
	}

	@Test
	public void pilesRoundTheBones() throws Exception
	{
		// north 3, east 2, south 3, west 2: east and west see north and south, not the other way round
		TestWorld w = bones(3, 2, 3, 2);
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		String s = TestWorld.show(piles);
		assertEquals(s, 8, TestWorld.at(piles, 51, 50).hits);
		assertEquals(s, 8, TestWorld.at(piles, 49, 50).hits);
		assertEquals(s, 3, TestWorld.at(piles, 50, 51).hits);
		assertEquals(s, 3, TestWorld.at(piles, 50, 49).hits);
		assertTrue(TestWorld.at(piles, 51, 50).best);
		assertTrue(TestWorld.at(piles, 49, 50).best);
		assertFalse(TestWorld.at(piles, 50, 51).best);
	}
}
