package com.barrageindicator;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class PileCalcTest
{
	private static PileCalc.Spot at(int x, int y)
	{
		return new PileCalc.Spot(x, y, 1);
	}

	@Test
	public void aLoneMonsterIsHitAlone()
	{
		assertArrayEquals(new int[]{1}, PileCalc.hits(Arrays.asList(at(10, 10))));
	}

	@Test
	public void sameTileAndNeighboursCount()
	{
		// three on one tile, one diagonal to it, one two tiles away
		List<PileCalc.Spot> spots = Arrays.asList(at(10, 10), at(10, 10), at(10, 10), at(11, 11), at(12, 10));
		assertArrayEquals(new int[]{4, 4, 4, 5, 2}, PileCalc.hits(spots));
	}

	@Test
	public void theMiddleOfALineHitsMore()
	{
		List<PileCalc.Spot> spots = Arrays.asList(at(10, 10), at(11, 10), at(12, 10));
		assertArrayEquals(new int[]{2, 3, 2}, PileCalc.hits(spots));
	}

	@Test
	public void neverMoreThanNine()
	{
		List<PileCalc.Spot> spots = new ArrayList<>();
		for (int i = 0; i < 14; i++)
		{
			spots.add(at(10, 10));
		}
		assertEquals(9, PileCalc.hits(spots)[0]);
	}

	@Test
	public void aLargeMonsterCountsByItsSouthWestTileAlone()
	{
		PileCalc.Spot small = at(10, 10);
		// a 3x3 monster whose south-west tile is next to (10,10) is hit; one whose body only reaches the area is not
		assertTrue(PileCalc.inArea(small, new PileCalc.Spot(11, 11, 3)));
		assertFalse(PileCalc.inArea(small, new PileCalc.Spot(12, 12, 3)));
		assertFalse(PileCalc.inArea(small, new PileCalc.Spot(8, 8, 3)));
		// two 2x2 monsters side by side: their south-west tiles are two apart, so neither cast hits the other
		PileCalc.Spot left = new PileCalc.Spot(20, 20, 2);
		PileCalc.Spot right = new PileCalc.Spot(22, 20, 2);
		assertFalse(PileCalc.inArea(left, right));
		assertFalse(PileCalc.inArea(right, left));
		// overlapping by a tile, they are
		assertTrue(PileCalc.inArea(left, new PileCalc.Spot(21, 21, 2)));
	}

	@Test
	public void taskNamesMatchTheirMonsters()
	{
		assertTrue(PileCalc.matches("Smoke devil", "Smoke devils", ""));
		assertTrue(PileCalc.matches("Greater Nechryael", "Nechryael", ""));
		assertTrue(PileCalc.matches("Abyssal demon", "Abyssal demons", ""));
		assertTrue(PileCalc.matches("Dust devil", "Dust devils", ""));
		assertTrue(PileCalc.matches("Ankou", "Ankou", ""));
		assertFalse(PileCalc.matches("Ankou", "Dust devils", ""));
		assertFalse(PileCalc.matches("Smoke devil", null, ""));
		assertTrue(PileCalc.matches("Deviant spectre", "Dust devils", "spectre, kurask"));
		assertEquals("abyss", PileCalc.singular("Abyss"));
	}

	@Test
	public void theIconIsTheSpellOnAutocast()
	{
		assertEquals(net.runelite.api.SpriteID.SPELL_BLOOD_BARRAGE, BarrageIndicatorPlugin.spellSprite(45));
		assertEquals(net.runelite.api.SpriteID.SPELL_ICE_BURST, BarrageIndicatorPlugin.spellSprite(38));
		assertEquals(net.runelite.api.SpriteID.SPELL_ICE_BARRAGE, BarrageIndicatorPlugin.spellSprite(46));
		assertEquals(net.runelite.api.SpriteID.SPELL_ICE_BARRAGE, BarrageIndicatorPlugin.spellSprite(0));
	}

	@Test
	public void aMonsterTheTargetCannotSeeIsNotCounted()
	{
		List<PileCalc.Spot> spots = new ArrayList<>();
		spots.add(at(10, 10));
		spots.add(at(11, 10));
		spots.add(at(9, 10));
		// the middle one sees both; the two at the ends have something between them
		int[] hits = PileCalc.hits(spots, (target, other) -> !(target == 1 && other == 2) && !(target == 2 && other == 1));
		assertEquals(3, hits[0]);
		assertEquals(2, hits[1]);
		assertEquals(2, hits[2]);
	}
}
