/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.barrageindicator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How many monsters a burst or barrage spell, or a chinchompa, hits when cast on each one of them.
 *
 * The spell hits a 3x3 area centred on the south-west tile of its target: every other monster whose own
 * south-west tile is inside that area is hit, up to nine monsters in all. A large monster counts by its
 * south-west tile alone, not by the rest of its body: two 2x2 monsters side by side are not hit together.
 * Chinchompas hit the same area, up to 11 monsters (12 for black ones) (OSRS Wiki, Chinchompa and
 * Multicombat area, read 2026-10-16).
 */
final class PileCalc
{
	/** The most monsters one spell can hit, the target included. */
	static final int MAX_TARGETS = 9;

	/** A monster: its south-west tile and how many tiles wide it is. */
	static final class Spot
	{
		final int x;
		final int y;
		final int size;

		Spot(int x, int y, int size)
		{
			this.x = x;
			this.y = y;
			this.size = Math.max(1, size);
		}
	}

	private PileCalc()
	{
	}

	/** Whether the monster a spell is cast on has line of sight to another one, by their positions in the list. */
	interface Sight
	{
		boolean sees(int target, int other);
	}

	/** For each monster, how many a cast on it hits (itself included), in the order given. */
	static int[] hits(List<Spot> spots)
	{
		return hits(spots, (target, other) -> true);
	}

	/**
	 * The same, leaving out the monsters the target has no line of sight to: the spell spreads from the monster
	 * it is cast on, and scenery between that monster and another one in the area keeps the other from being hit.
	 */
	static int[] hits(List<Spot> spots, Sight sight)
	{
		return hits(spots, MAX_TARGETS, sight);
	}

	/** The same, with at most {@code most} monsters hit by one cast (9 for spells, 11 or 12 for chinchompas). */
	static int[] hits(List<Spot> spots, int most, Sight sight)
	{
		int[] hits = new int[spots.size()];
		for (int i = 0; i < hits.length; i++)
		{
			Spot target = spots.get(i);
			int count = 0;
			for (int j = 0; j < hits.length; j++)
			{
				if (inArea(target, spots.get(j)) && (i == j || sight.sees(i, j)))
				{
					count++;
				}
			}
			hits[i] = Math.min(most, count);
		}
		return hits;
	}

	/** Whether the south-west tile of {@code other} is inside the 3x3 area around the target's south-west tile. */
	static boolean inArea(Spot target, Spot other)
	{
		return Math.abs(other.x - target.x) <= 1 && Math.abs(other.y - target.y) <= 1;
	}

	/**
	 * Monsters of a task whose names the task's own name does not give (lower case: task, then its monsters).
	 * From the OSRS Wiki pages "Slayer task/..." (Jellies, Lizardmen, Elves, Spiritual creatures, Aberrant spectres,
	 * Fossil Island wyverns, Wolves, Dwarves, Warped creatures, Dust devils, Nechryael; read 2026-10-15); the others
	 * from the target names of RuneLite's Slayer plugin (Task.java).
	 */
	private static final Map<String, String[]> TASK_MONSTERS = new HashMap<>();

	static
	{
		monsters("jellies", "jelly");
		monsters("lizardmen", "lizardman");
		monsters("elves", "elf", "iorwerth warrior", "iorwerth archer");
		monsters("spiritual creatures", "spiritual ranger", "spiritual mage", "spiritual warrior");
		monsters("aberrant spectres", "spectre");
		monsters("fossil island wyverns", "ancient wyvern", "long-tailed wyvern", "spitting wyvern", "taloned wyvern");
		monsters("wolves", "wolf");
		monsters("werewolves", "werewolf");
		monsters("dwarves", "dwarf", "black guard");
		monsters("warped creatures", "warped terrorbird", "warped tortoise", "mutated terrorbird", "mutated tortoise");
		monsters("dust devils", "choke devil");
		monsters("nechryael", "nechryarch");
		monsters("fleshcrawlers", "flesh crawler");
		monsters("mutated zygomites", "zygomite");
		monsters("metal dragons", "bronze dragon", "iron dragon", "steel dragon", "mithril dragon", "adamant dragon", "rune dragon");
		monsters("lesser nagua", "sulphur nagua", "frost nagua");
		monsters("dark beasts", "night beast");
		monsters("cave horrors", "cave abomination");
		monsters("cave crawlers", "chasm crawler");
		monsters("cockatrice", "cockathrice");
		monsters("crawling hands", "crushing hand");
		monsters("infernal mages", "malevolent mage");
		monsters("pyrefiends", "flaming pyrelord");
		monsters("wyrms", "wyrmling");
		monsters("scabarites", "scarab swarm", "locust rider", "scarab mage");
	}

	private static void monsters(String task, String... names)
	{
		TASK_MONSTERS.put(task, names);
	}

	/** The singular of a slayer task name, in lower case, to look for inside a monster's name. */
	static String singular(String task)
	{
		if (task == null)
		{
			return "";
		}
		String name = task.trim().toLowerCase();
		if (name.endsWith("s") && !name.endsWith("ss"))
		{
			return name.substring(0, name.length() - 1);
		}
		return name;
	}

	/** Whether {@code word} is in {@code name} as whole words: "elf" is in "Elf Archer", not in "Shelf". */
	static boolean hasWord(String name, String word)
	{
		for (int at = name.indexOf(word); at >= 0; at = name.indexOf(word, at + 1))
		{
			int end = at + word.length();
			if ((at == 0 || !Character.isLetter(name.charAt(at - 1)))
				&& (end == name.length() || !Character.isLetter(name.charAt(end))))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether a monster's name belongs to the task, or to one of the extra names (comma separated, part of a name
	 * is enough). The task's singular and its other monsters count as whole words.
	 */
	static boolean matches(String npcName, String task, String extraNames)
	{
		if (npcName == null)
		{
			return false;
		}
		String name = npcName.replace('\u00a0', ' ').toLowerCase();
		String wanted = singular(task);
		if (!wanted.isEmpty())
		{
			if (hasWord(name, wanted))
			{
				return true;
			}
			for (String other : TASK_MONSTERS.getOrDefault(task.trim().toLowerCase(), new String[0]))
			{
				if (hasWord(name, other))
				{
					return true;
				}
			}
		}
		if (extraNames != null)
		{
			for (String extra : extraNames.split(","))
			{
				String e = extra.trim().toLowerCase();
				if (!e.isEmpty() && name.contains(e))
				{
					return true;
				}
			}
		}
		return false;
	}
}
