package com.barrageindicator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import net.runelite.api.CollisionDataFlag;
import org.junit.Test;

/**
 * The best pile is the best one the player can cast on without walking: within the spell's range and in line of
 * sight from the player. OSRS Wiki, Attack range (read 2026-10-15): combat spells reach 10 squares, measured as
 * Chebyshev distance; Line of sight: for a monster larger than one tile, the tile closest to the player counts.
 * With none in reach, the largest pile of all.
 */
public class ReachTest
{
	@Test
	public void aPileOutOfRangeIsNotTheBest() throws Exception
	{
		// another player's pile 15 tiles away (Greater Nechryael): the best is the one in reach
		TestWorld w = new TestWorld();
		w.player(50, 50);
		w.addMany("Greater Nechryael", 55, 50, 5);
		w.addMany("Greater Nechryael", 65, 50, 8);
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		String s = TestWorld.show(piles);
		assertTrue(s, TestWorld.at(piles, 55, 50).best);
		assertEquals(s, 8, TestWorld.at(piles, 65, 50).hits);
		assertFalse(s, TestWorld.at(piles, 65, 50).best);
	}

	@Test
	public void tenTilesIsInRangeElevenIsNot() throws Exception
	{
		TestWorld w = new TestWorld();
		w.player(50, 50);
		w.addMany("Smoke devil", 60, 57, 3);
		w.addMany("Smoke devil", 61, 50, 6);
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		String s = TestWorld.show(piles);
		assertTrue(s, TestWorld.at(piles, 60, 57).best);
		assertFalse(s, TestWorld.at(piles, 61, 50).best);
	}

	@Test
	public void aLargeMonsterIsMeasuredToItsClosestTile() throws Exception
	{
		// three 2x2 piled up west of the player; the middle one's south-west tile (39,50) is 11 away,
		// but its body reaches (40,50), 10 away: in range
		TestWorld w = new TestWorld();
		w.player(50, 50);
		w.add("Araxyte", 38, 50, 2);
		w.add("Araxyte", 39, 50, 2);
		w.add("Araxyte", 40, 50, 2);
		w.addMany("Araxyte", 50, 55, 2);
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		String s = TestWorld.show(piles);
		assertEquals(s, 3, TestWorld.at(piles, 39, 50).hits);
		assertTrue(s, TestWorld.at(piles, 39, 50).best);
		assertFalse(s, TestWorld.at(piles, 50, 55).best);

		TestWorld far = new TestWorld();
		far.player(50, 50);
		far.add("Araxyte", 60, 39, 2);
		far.add("Araxyte", 61, 39, 2);
		far.add("Araxyte", 62, 39, 2);
		far.addMany("Araxyte", 50, 45, 2);
		piles = far.run();
		s = TestWorld.show(piles);
		// (61,39): its closest tile is (61,40), 11 away in x: out of range, though it is the largest
		assertEquals(s, 3, TestWorld.at(piles, 61, 39).hits);
		assertFalse(s, TestWorld.at(piles, 61, 39).best);
		assertTrue(s, TestWorld.at(piles, 50, 45).best);
	}

	@Test
	public void aPileBehindAWallIsNotTheBest() throws Exception
	{
		TestWorld w = new TestWorld();
		w.player(50, 50);
		// a wall on the tile east of the player, from the ground up: no sight to the east pile
		w.flags[51][50] |= CollisionDataFlag.BLOCK_LINE_OF_SIGHT_FULL | CollisionDataFlag.BLOCK_MOVEMENT_FULL;
		w.addMany("Smoke devil", 54, 50, 8);
		w.addMany("Smoke devil", 50, 54, 4);
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		String s = TestWorld.show(piles);
		assertEquals(s, 8, TestWorld.at(piles, 54, 50).hits);
		assertFalse(s, TestWorld.at(piles, 54, 50).best);
		assertTrue(s, TestWorld.at(piles, 50, 54).best);
	}

	@Test
	public void withNoPileInReachTheLargestIsMarked() throws Exception
	{
		TestWorld w = new TestWorld();
		w.player(10, 10);
		w.addMany("Smoke devil", 50, 50, 3);
		w.addMany("Smoke devil", 70, 70, 6);
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		String s = TestWorld.show(piles);
		assertTrue(s, TestWorld.at(piles, 70, 70).best);
		assertFalse(s, TestWorld.at(piles, 50, 50).best);
	}

	@Test
	public void aPileTooSmallToDrawDoesNotCountAsInReach() throws Exception
	{
		// the only monster in reach is alone (below the smallest pile): the largest pile is marked
		TestWorld w = new TestWorld();
		w.player(50, 50);
		w.add("Smoke devil", 52, 50, 1);
		w.addMany("Smoke devil", 70, 70, 4);
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		assertTrue(TestWorld.show(piles), TestWorld.at(piles, 70, 70).best);
	}
}
