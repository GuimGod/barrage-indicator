package com.barrageindicator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/**
 * "Count monsters not on task": the spell hits every monster it can attack, up to nine (Death spawn among the
 * Greater Nechryael take up some of the nine). Off by default: only the task's monsters are counted, as before.
 * On: the others count too, and the marks stay on monsters of the task.
 */
public class OtherMonstersTest
{
	private static TestWorld nechryael(boolean countOthers)
	{
		TestWorld w = new TestWorld();
		w.onlyTask = true;
		w.countOthers = countOthers;
		w.task = "Nechryael";
		w.taskLeft = 150;
		for (int i = 0; i < 5; i++)
		{
			w.add("Death spawn", 50, 50, 1);
		}
		w.addMany("Greater Nechryael", 50, 50, 4);
		w.addMany("Greater Nechryael", 60, 60, 6);
		w.addMany("Death spawn", 70, 70, 9);
		return w;
	}

	@Test
	public void offByDefault()
	{
		assertFalse(new BarrageIndicatorConfig()
		{
		}.countOthers());
	}

	@Test
	public void offOnlyTheTaskCounts() throws Exception
	{
		List<BarrageIndicatorPlugin.Pile> piles = nechryael(false).run();
		String s = TestWorld.show(piles);
		assertEquals(s, 4, TestWorld.at(piles, 50, 50).hits);
		assertTrue(s, TestWorld.at(piles, 60, 60).best);
		assertNull(s, TestWorld.at(piles, 70, 70));
	}

	@Test
	public void onTheOthersCountAndTheMarksStayOnTheTask() throws Exception
	{
		List<BarrageIndicatorPlugin.Pile> piles = nechryael(true).run();
		String s = TestWorld.show(piles);
		BarrageIndicatorPlugin.Pile mixed = TestWorld.at(piles, 50, 50);
		assertEquals(s, 9, mixed.hits);
		assertTrue(s, mixed.best);
		assertEquals("Greater Nechryael", mixed.npc.getName());
		assertEquals(s, 6, TestWorld.at(piles, 60, 60).hits);
		assertFalse(s, TestWorld.at(piles, 60, 60).best);
		assertNull("a pile of Death spawn alone is never marked " + s, TestWorld.at(piles, 70, 70));
	}
}
