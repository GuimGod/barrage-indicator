/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.barrageindicator;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("barrageindicator")
public interface BarrageIndicatorConfig extends Config
{
	@ConfigItem(
		keyName = "onlyTask",
		name = "Only my slayer task",
		description = "Count only the monsters of your current slayer task (the name comes from RuneLite's Slayer plugin). Off: count every monster you can attack.",
		position = 1
	)
	default boolean onlyTask()
	{
		return true;
	}

	@ConfigItem(
		keyName = "extraNames",
		name = "Other monsters",
		description = "Names to count as well, separated by commas (part of the name is enough).",
		position = 2
	)
	default String extraNames()
	{
		return "";
	}

	@ConfigItem(
		keyName = "onlyAncients",
		name = "Only on Ancient Magicks",
		description = "Show nothing while another spellbook is active.",
		position = 3
	)
	default boolean onlyAncients()
	{
		return true;
	}

	@ConfigItem(
		keyName = "onlyMulti",
		name = "Only in multicombat",
		description = "Show nothing in single combat, where the spell hits one monster.",
		position = 4
	)
	default boolean onlyMulti()
	{
		return true;
	}

	@Range(min = 2, max = 9)
	@ConfigItem(
		keyName = "minimum",
		name = "Smallest pile",
		description = "A pile is marked from this many monsters hit by one cast.",
		position = 5
	)
	default int minimum()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "showCounts",
		name = "Number on every pile",
		description = "Write on each pile how many monsters a cast on it would hit. Off: only the best pile is marked.",
		position = 6
	)
	default boolean showCounts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showIcon",
		name = "Spell icon on the best pile",
		description = "Draw the barrage icon on the pile where one cast hits the most monsters.",
		position = 7
	)
	default boolean showIcon()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showTile",
		name = "Outline the best pile's tile",
		description = "Outline the tile of the monster to cast on.",
		position = 8
	)
	default boolean showTile()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showSouthWest",
		name = "Mark the tile to cast on",
		description = "Fill the south-west tile of the monster to cast on, the tile the spell's area is centred on, and draw the spell icon on it. With a tie, every one of the best is marked.",
		position = 8
	)
	default boolean showSouthWest()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "southWestColour",
		name = "Tile to cast on",
		description = "Colour of the south-west tile of the monster to cast on.",
		position = 8
	)
	default Color southWestColour()
	{
		return new Color(255, 0, 0, 150);
	}

	@ConfigItem(
		keyName = "showGroupingTiles",
		name = "Smoke devil grouping tiles",
		description = "In the Smoke Devil Dungeon, mark the three tiles around the bones west of the cannon spot: run from one to another to pile the smoke devils up beside the bones.",
		position = 8
	)
	default boolean showGroupingTiles()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "groupingColour",
		name = "Grouping tiles",
		description = "Colour of the smoke devil grouping tiles.",
		position = 8
	)
	default Color groupingColour()
	{
		return new Color(51, 153, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "bestColour",
		name = "Best pile",
		description = "Colour of the number and outline on the best pile.",
		position = 9
	)
	default Color bestColour()
	{
		return new Color(0, 255, 255);
	}

	@Alpha
	@ConfigItem(
		keyName = "otherColour",
		name = "Other piles",
		description = "Colour of the number on the other piles.",
		position = 10
	)
	default Color otherColour()
	{
		return new Color(255, 255, 255, 200);
	}

	@Range(min = 10, max = 40)
	@ConfigItem(
		keyName = "fontSize",
		name = "Number size",
		description = "Size of the numbers.",
		position = 11
	)
	default int fontSize()
	{
		return 18;
	}

	@ConfigItem(
		keyName = "reportBug",
		name = "Bug or suggestion",
		description = "Click to open a window where you write what is wrong or what you would like. It shows what goes with your words (the monster and the items you wear and carry) and sends it to the plugin's author only when you press Send",
		position = 100
	)
	default boolean reportBug()
	{
		return false;
	}
}
