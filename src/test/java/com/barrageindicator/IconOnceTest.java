package com.barrageindicator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The icon (spell or chinchompa) is drawn once: on the tile to cast on when that tile is marked, over the best pile
 * only when it is not. Before, the chinchompa was drawn twice, on the tile and over the pile.
 */
public class IconOnceTest
{
	@Test
	public void withTheTileMarkedTheIconIsOnlyOnTheTile()
	{
		// the defaults: both on; the tile carries the icon, the pile only its number
		assertFalse(BarrageIndicatorOverlay.iconOverPile(true, true, true));
	}

	@Test
	public void withoutTheTileTheIconGoesOverTheBestPile()
	{
		assertTrue(BarrageIndicatorOverlay.iconOverPile(true, true, false));
	}

	@Test
	public void neverOverTheOtherPilesOrWhenTurnedOff()
	{
		assertFalse(BarrageIndicatorOverlay.iconOverPile(false, true, false));
		assertFalse(BarrageIndicatorOverlay.iconOverPile(true, false, false));
		assertFalse(BarrageIndicatorOverlay.iconOverPile(false, false, true));
	}
}
