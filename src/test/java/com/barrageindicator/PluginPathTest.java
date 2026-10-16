package com.barrageindicator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

/** The plugin's whole path, one game tick at a time, with RuneLite's real line of sight. */
public class PluginPathTest
{
	@Test
	public void realSightOnOpenGround()
	{
		// what the plugin relies on: the jar's line of sight sees a neighbour on open ground, not through a wall,
		// and says "no" between areas that overlap
		TestWorld w = new TestWorld();
		WorldArea a = new WorldArea(new WorldPoint(TestWorld.BASE_X + 50, TestWorld.BASE_Y + 50, 0), 1, 1);
		WorldArea same = new WorldArea(new WorldPoint(TestWorld.BASE_X + 50, TestWorld.BASE_Y + 50, 0), 1, 1);
		WorldArea east = new WorldArea(new WorldPoint(TestWorld.BASE_X + 51, TestWorld.BASE_Y + 50, 0), 1, 1);
		WorldArea diagonal = new WorldArea(new WorldPoint(TestWorld.BASE_X + 49, TestWorld.BASE_Y + 49, 0), 1, 1);
		assertTrue(!a.hasLineOfSightTo(w.view, same));
		assertTrue(a.hasLineOfSightTo(w.view, east));
		assertTrue(a.hasLineOfSightTo(w.view, diagonal));
		w.flags[51][50] |= CollisionDataFlag.BLOCK_LINE_OF_SIGHT_WEST;
		assertTrue(!a.hasLineOfSightTo(w.view, east));
	}

	@Test
	public void aWallBetweenTwoMonstersKeepsThemApart() throws Exception
	{
		TestWorld open = new TestWorld();
		open.add("Smoke devil", 50, 50, 1);
		open.add("Smoke devil", 51, 50, 1);
		List<BarrageIndicatorPlugin.Pile> piles = open.run();
		assertEquals(TestWorld.show(piles), 2, piles.size());
		assertEquals(2, piles.get(0).hits);

		TestWorld walled = new TestWorld();
		walled.flags[51][50] |= CollisionDataFlag.BLOCK_LINE_OF_SIGHT_WEST;
		walled.flags[50][50] |= CollisionDataFlag.BLOCK_LINE_OF_SIGHT_EAST;
		walled.add("Smoke devil", 50, 50, 1);
		walled.add("Smoke devil", 51, 50, 1);
		assertTrue("each one alone, below the smallest pile", walled.run().isEmpty());
	}

	@Test
	public void deadMonstersAreLeftOut() throws Exception
	{
		TestWorld w = new TestWorld();
		w.addMany("Smoke devil", 50, 50, 3);
		for (int i = 0; i < 3; i++)
		{
			w.add("Smoke devil", 50, 50, 1, true);
		}
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		assertEquals(TestWorld.show(piles), 1, piles.size());
		assertEquals(3, piles.get(0).hits);
	}

	@Test
	public void neverMoreThanNine() throws Exception
	{
		TestWorld w = new TestWorld();
		w.addMany("Smoke devil", 50, 50, 14);
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		assertEquals(TestWorld.show(piles), 1, piles.size());
		assertEquals(9, piles.get(0).hits);
	}

	@Test
	public void onlyTheTaskMonstersCount() throws Exception
	{
		TestWorld w = new TestWorld();
		w.onlyTask = true;
		w.task = "Smoke devils";
		w.taskLeft = 40;
		w.addMany("Smoke devil", 50, 50, 3);
		w.addMany("Skeleton", 50, 50, 3);
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		assertEquals(TestWorld.show(piles), 1, piles.size());
		assertEquals(3, piles.get(0).hits);
		assertEquals("Smoke devil", piles.get(0).npc.getName());
	}

	@Test
	public void withoutATaskNothingIsDrawn() throws Exception
	{
		TestWorld w = new TestWorld();
		w.onlyTask = true;
		w.task = "Smoke devils";
		w.taskLeft = 0;
		w.addMany("Smoke devil", 50, 50, 3);
		assertTrue(w.run().isEmpty());

		// the extra names still work without a task
		w.extraNames = "smoke";
		assertEquals(3, w.run().get(0).hits);
	}

	@Test
	public void onlyOnAncientsAndInMulti() throws Exception
	{
		TestWorld w = new TestWorld();
		w.addMany("Smoke devil", 50, 50, 3);
		w.onlyAncients = true;
		w.spellbook = 0;
		assertTrue(w.run().isEmpty());
		w.spellbook = 1;
		assertEquals(1, w.run().size());
		w.onlyMulti = true;
		w.multi = 0;
		assertTrue(w.run().isEmpty());
		w.multi = 1;
		assertEquals(1, w.run().size());
	}

	@Test
	public void smallerPilesThanTheSmallestAreNotDrawn() throws Exception
	{
		TestWorld w = new TestWorld();
		w.minimum = 4;
		w.addMany("Smoke devil", 50, 50, 3);
		w.addMany("Smoke devil", 60, 60, 5);
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		assertNull(TestWorld.show(piles), TestWorld.at(piles, 50, 50));
		assertNotNull(TestWorld.at(piles, 60, 60));
		assertEquals(5, TestWorld.at(piles, 60, 60).hits);
	}
}
