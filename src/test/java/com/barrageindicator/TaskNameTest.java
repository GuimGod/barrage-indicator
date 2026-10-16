package com.barrageindicator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/**
 * Task names that are not a plain plural of their monsters find the right monsters, and only them.
 * Monster names from the OSRS Wiki pages Slayer task/<task> (read 2026-10-15).
 */
public class TaskNameTest
{
	private static void finds(String task, String... monsters)
	{
		for (String m : monsters)
		{
			assertTrue(m + " is on the task " + task, PileCalc.matches(m, task, ""));
		}
	}

	private static void skips(String task, String... monsters)
	{
		for (String m : monsters)
		{
			assertFalse(m + " is not on the task " + task, PileCalc.matches(m, task, ""));
		}
	}

	@Test
	public void irregularTaskNames()
	{
		finds("Jellies", "Jelly", "Warped jelly", "Chilled jelly", "Vitreous Jelly", "Vitreous Warped Jelly", "Vitreous Chilled Jelly");
		finds("Lizardmen", "Lizardman", "Lizardman shaman", "Lizardman brute");
		finds("Elves", "Elf Warrior", "Elf Archer", "Iorwerth Warrior", "Iorwerth Archer", "Reanimated elf");
		finds("Spiritual creatures", "Spiritual ranger", "Spiritual mage", "Spiritual warrior");
		finds("Aberrant spectres", "Aberrant spectre", "Abhorrent spectre", "Deviant spectre", "Repugnant spectre");
		finds("Fossil island wyverns", "Ancient Wyvern", "Long-tailed Wyvern", "Spitting Wyvern", "Taloned Wyvern");
		finds("Wolves", "Big Wolf", "Desert Wolf", "Dire Wolf", "Ice wolf", "Jungle wolf");
		finds("Dwarves", "Dwarf", "Chaos dwarf", "Black Guard", "Black Guard Berserker", "Dwarf gang member");
		finds("Warped Creatures", "Warped Terrorbird", "Warped Tortoise", "Mutated Terrorbird", "Mutated Tortoise");
		// the superior ones
		finds("Dust devils", "Dust devil", "Choke devil");
		finds("Nechryael", "Nechryael", "Greater Nechryael", "Nechryarch");
	}

	@Test
	public void onlyThoseMonsters()
	{
		skips("Elves", "Skeleton", "Shelob");
		skips("Wolves", "Werewolf");
		skips("Fossil island wyverns", "Skeletal Wyvern");
		skips("Nechryael", "Death spawn");
		skips("Jellies", "Smoke devil");
		skips("Smoke devils", "Jelly");
	}

	@Test
	public void plainPluralsStillWork()
	{
		finds("Smoke devils", "Smoke devil", "Nuclear smoke devil");
		finds("Abyssal demons", "Abyssal demon", "Greater abyssal demon");
		finds("Kalphites", "Kalphite Worker", "Kalphite Soldier");
		finds("Tzhaar", "TzHaar-Ket", "TzHaar-Xil");
		finds("Ankou", "Ankou");
		finds("Hellhounds", "Hellhound");
		skips("Dust devils", "Ankou");
	}

	@Test
	public void aJelliesTaskOnTheWholePath() throws Exception
	{
		TestWorld w = new TestWorld();
		w.onlyTask = true;
		w.task = "Jellies";
		w.taskLeft = 120;
		w.addMany("Warped jelly", 50, 50, 4);
		w.addMany("Skeleton", 50, 50, 3);
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		assertEquals(TestWorld.show(piles), 1, piles.size());
		assertEquals(4, piles.get(0).hits);
	}

	@Test
	public void anElvesTaskLeavesSkeletonsOut() throws Exception
	{
		TestWorld w = new TestWorld();
		w.onlyTask = true;
		w.task = "Elves";
		w.taskLeft = 120;
		w.addMany("Skeleton", 50, 50, 5);
		assertTrue(TestWorld.show(w.run()), w.run().isEmpty());
		w.addMany("Iorwerth Warrior", 60, 60, 3);
		List<BarrageIndicatorPlugin.Pile> piles = w.run();
		assertEquals(TestWorld.show(piles), 1, piles.size());
		assertEquals("Iorwerth Warrior", piles.get(0).npc.getName());
	}
}
