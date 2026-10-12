/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.barrageindicator;

import java.util.List;

/**
 * How many monsters a burst or barrage spell hits when cast on each one of them.
 *
 * The spell hits a 3x3 area centred on the south-west tile of its target: every other monster whose own
 * south-west tile is inside that area is hit, up to nine monsters in all. A large monster counts by its
 * south-west tile alone, not by the rest of its body: two 2x2 monsters side by side are not hit together.
 */
final class PileCalc
{
	/** The most monsters one cast can hit, the target included. */
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

	/** For each monster, how many a cast on it hits (itself included), in the order given. */
	static int[] hits(List<Spot> spots)
	{
		int[] hits = new int[spots.size()];
		for (int i = 0; i < hits.length; i++)
		{
			Spot target = spots.get(i);
			int count = 0;
			for (Spot other : spots)
			{
				if (inArea(target, other))
				{
					count++;
				}
			}
			hits[i] = Math.min(MAX_TARGETS, count);
		}
		return hits;
	}

	/** Whether the south-west tile of {@code other} is inside the 3x3 area around the target's south-west tile. */
	static boolean inArea(Spot target, Spot other)
	{
		return Math.abs(other.x - target.x) <= 1 && Math.abs(other.y - target.y) <= 1;
	}

	/** The singular of a slayer task name, in lower case, to look for inside a monster's name. */
	static String singular(String task)
	{
		if (task == null)
		{
			return "";
		}
		String name = task.trim().toLowerCase();
		if (name.endsWith("ves"))
		{
			// "Elves", "Wolves"
			return name.substring(0, name.length() - 3);
		}
		if (name.endsWith("s") && !name.endsWith("ss"))
		{
			return name.substring(0, name.length() - 1);
		}
		return name;
	}

	/** Whether a monster's name belongs to the task, or to one of the extra names (comma separated). */
	static boolean matches(String npcName, String task, String extraNames)
	{
		if (npcName == null)
		{
			return false;
		}
		String name = npcName.toLowerCase();
		String wanted = singular(task);
		if (!wanted.isEmpty() && name.contains(wanted))
		{
			return true;
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
