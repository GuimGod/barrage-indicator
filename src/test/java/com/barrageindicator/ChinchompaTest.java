package com.barrageindicator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;

/**
 * Chinchompas count like a burst or barrage: the same 3x3 area, up to 11 monsters (12 for black ones), and a reach
 * of 9 squares, 10 on long fuse (OSRS Wiki, Chinchompa (weapon), Chinchompa, Black chinchompa, read 2026-10-16).
 */
public class ChinchompaTest
{
	private static final int[] CHINCHOMPAS = {ItemID.CHINCHOMPA_CAPTURED, ItemID.CHINCHOMPA_BIG_CAPTURED, ItemID.CHINCHOMPA_BLACK};

	@Test
	public void theThreeChinchompas()
	{
		assertEquals(11, BarrageIndicatorPlugin.chinchompaTargets(ItemID.CHINCHOMPA_CAPTURED));
		assertEquals(11, BarrageIndicatorPlugin.chinchompaTargets(ItemID.CHINCHOMPA_BIG_CAPTURED));
		assertEquals(12, BarrageIndicatorPlugin.chinchompaTargets(ItemID.CHINCHOMPA_BLACK));
		// the Hunter guide's captured chinchompas are not a weapon
		assertEquals(0, BarrageIndicatorPlugin.chinchompaTargets(ItemID.HUNTGUIDE_CHINCHOMPA_CAPTURED));
		assertEquals(0, BarrageIndicatorPlugin.chinchompaTargets(-1));
	}

	@Test
	public void chinchompasInHandShowOffAncients() throws Exception
	{
		for (int chin : CHINCHOMPAS)
		{
			TestWorld w = new TestWorld();
			w.addMany("Skeleton", 50, 50, 3);
			w.onlyAncients = true;
			w.spellbook = 0;
			w.weapon = chin;
			List<BarrageIndicatorPlugin.Pile> piles = w.run();
			assertEquals(TestWorld.show(piles), 1, piles.size());
			assertEquals(3, piles.get(0).hits);
		}
	}

	@Test
	public void neitherAncientsNorChinchompasShowsNothing() throws Exception
	{
		TestWorld w = new TestWorld();
		w.addMany("Skeleton", 50, 50, 3);
		w.onlyAncients = true;
		w.spellbook = 0;
		assertTrue(w.run().isEmpty());
		w.weapon = ItemID.TWISTED_BOW;
		assertTrue(w.run().isEmpty());
	}

	@Test
	public void ancientsWithoutChinchompasShowAsBefore() throws Exception
	{
		TestWorld w = new TestWorld();
		w.addMany("Skeleton", 50, 50, 14);
		w.onlyAncients = true;
		w.spellbook = 1;
		w.weapon = ItemID.KODAI_WAND;
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		assertEquals(TestWorld.show(piles), 1, piles.size());
		assertEquals(9, piles.get(0).hits);
	}

	@Test
	public void chinchompasHitElevenOrTwelve() throws Exception
	{
		TestWorld w = new TestWorld();
		w.addMany("Skeleton", 50, 50, 14);
		w.weapon = ItemID.CHINCHOMPA_BIG_CAPTURED;
		assertEquals(11, w.run().get(0).hits);
		w.weapon = ItemID.CHINCHOMPA_BLACK;
		assertEquals(12, w.run().get(0).hits);
		// on Ancients with chinchompas in hand, the chinchompas are what is thrown
		w.spellbook = 1;
		assertEquals(12, w.run().get(0).hits);
	}

	@Test
	public void theTileToCastOnShowsTheChinchompaHeld() throws Exception
	{
		// the overlay draws the same icon on the tile to cast on and over the best pile
		java.awt.image.BufferedImage spell = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
		java.util.Map<Integer, java.awt.image.BufferedImage> pictures = new java.util.HashMap<>();
		for (int chin : CHINCHOMPAS)
		{
			pictures.put(chin, new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB));
		}

		for (int chin : CHINCHOMPAS)
		{
			TestWorld w = new TestWorld();
			w.addMany("Skeleton", 50, 50, 3);
			w.spellbook = 1;
			w.weapon = chin;
			w.run();
			int held = w.lastPlugin.chinchompa();
			assertEquals(chin, held);
			assertTrue("the picture of chinchompa " + chin,
				BarrageIndicatorOverlay.icon(held, pictures::get, () -> spell) == pictures.get(chin));
		}

		// no chinchompa in hand, or put away: the spell again
		TestWorld w = new TestWorld();
		w.addMany("Skeleton", 50, 50, 3);
		w.weapon = ItemID.KODAI_WAND;
		w.run();
		assertEquals(-1, w.lastPlugin.chinchompa());
		assertTrue(BarrageIndicatorOverlay.icon(w.lastPlugin.chinchompa(), pictures::get, () -> spell) == spell);
		w.weapon = -1;
		w.run();
		assertEquals(-1, w.lastPlugin.chinchompa());
	}

	@Test
	public void chinchompasStillNeedMulticombat() throws Exception
	{
		TestWorld w = new TestWorld();
		w.addMany("Skeleton", 50, 50, 3);
		w.weapon = ItemID.CHINCHOMPA_BLACK;
		w.onlyMulti = true;
		w.multi = 0;
		assertTrue(w.run().isEmpty());
	}

	@Test
	public void chinchompasReachNineSquaresTenOnLongFuse() throws Exception
	{
		// a pile of 2 ten squares away and a pile of 3 further: the 2 is the best one only while it is in reach
		TestWorld w = new TestWorld();
		w.player(40, 50);
		w.addMany("Skeleton", 50, 50, 2);
		w.addMany("Skeleton", 70, 50, 3);

		// spells reach 10
		assertTrue(TestWorld.show(w.run()), TestWorld.at(w.run(), 50, 50).best);

		// chinchompas on short or medium fuse reach 9: none in reach, so the largest is the best
		w.weapon = ItemID.CHINCHOMPA_BLACK;
		w.attackStyle = 1;
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		assertTrue(TestWorld.show(piles), !TestWorld.at(piles, 50, 50).best);
		assertTrue(TestWorld.show(piles), TestWorld.at(piles, 70, 50).best);

		// long fuse reaches 10
		w.attackStyle = 3;
		piles = w.run();
		assertTrue(TestWorld.show(piles), TestWorld.at(piles, 50, 50).best);
		assertTrue(TestWorld.show(piles), !TestWorld.at(piles, 70, 50).best);
	}
}
