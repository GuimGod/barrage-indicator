# Barrage Indicator

Shows, on each pile of monsters of your slayer task, how many of them one burst or barrage cast there would hit, and puts the spell icon on the pile where it hits the most.

- A burst or barrage hits a 3x3 area centred on the south-west tile of its target, and every monster whose own south-west tile is inside that area, up to nine in all. A large monster counts by its south-west tile alone: two 2x2 monsters side by side are not hit together.
- A monster in that area is counted only when the target has line of sight to it: scenery between the two keeps the spell from reaching it. The game's own collision map is used.
- The count is redone every game tick from where the monsters stand.
- By default it only runs on Ancient Magicks, in multicombat, and for the monsters of your current slayer task (the task name is the one RuneLite's Slayer plugin saves). "Other monsters" adds names by hand.

## What it never does

- It does not click, cast, or change any menu. It only draws.
- It says nothing about what a monster will do, and with the default settings it stays off outside a slayer task.

## Settings

Smallest pile to mark (2 to 9), number on every pile or only the best, spell icon, the south-west tile of the monster to cast on filled in red with the spell icon on it (every one of the best, with a tie), tile outline, colours and number size.

The icon is the burst or barrage spell you have on autocast, or Ice Barrage when none is. Only monsters are counted and marked, never players.

In the Smoke Devil Dungeon three tiles are marked around the bones west of the cannon spot ("Group N", "Group W", "Group S"): with the cannon in the middle of the room, run from one to another to pile the smoke devils up beside the bones (OSRS Wiki, Smoke devil/Strategies, "Grouping"). Can be turned off.
