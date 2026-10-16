/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.barrageindicator;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * Counts, for each pile of monsters, how many a burst or barrage spell, or a chinchompa, cast there would hit,
 * and marks the pile where it hits the most. It only draws: no click, no menu, no spell.
 */
@PluginDescriptor(
	name = "Barrage Indicator",
	description = "Shows how many monsters a burst, barrage or chinchompa would hit on each pile, and marks the best pile",
	tags = {"barrage", "burst", "slayer", "stack", "pile", "aoe", "ancient", "magic", "chinchompa", "chins"}
)
public class BarrageIndicatorPlugin extends Plugin
{
	private static final int ANCIENT_SPELLBOOK = 1;

	/** How far combat spells reach, in squares (OSRS Wiki, Attack range). */
	static final int MAGIC_RANGE = 10;

	/** How far chinchompas reach, in squares, and on long fuse (OSRS Wiki, Chinchompa (weapon), read 2026-10-16). */
	static final int CHINCHOMPA_RANGE = 9;
	static final int CHINCHOMPA_RANGE_LONG = 10;

	/** The attack style setting on long fuse, the fourth style like on every ranged weapon (RuneLite, WeaponType). */
	private static final int LONG_FUSE = 3;

	/** One pile to draw: a monster standing on it and what a cast on it hits. */
	static final class Pile
	{
		final NPC npc;
		final int hits;
		final boolean best;

		Pile(NPC npc, int hits, boolean best)
		{
			this.npc = npc;
			this.hits = hits;
			this.best = best;
		}
	}

	@Inject
	private Client client;

	@Inject
	private BarrageIndicatorConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BarrageIndicatorOverlay overlay;

	private volatile List<Pile> piles = Collections.emptyList();

	/** The chinchompa held at the last count, or -1: its icon goes on the best pile instead of the spell's. */
	private volatile int chinchompa = -1;

	@Provides
	BarrageIndicatorConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BarrageIndicatorConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		piles = Collections.emptyList();
		chinchompa = -1;
	}

	List<Pile> piles()
	{
		return piles;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			piles = Collections.emptyList();
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		piles = work();
	}

	/**
	 * The sprite of the burst or barrage spell on autocast, by its number in the game's autocast setting
	 * (OSRS Wiki, varbit 276); Ice Barrage when none of them is.
	 */
	static int spellSprite(int autocast)
	{
		switch (autocast)
		{
			case 35:
				return net.runelite.api.SpriteID.SPELL_SMOKE_BURST;
			case 36:
				return net.runelite.api.SpriteID.SPELL_SHADOW_BURST;
			case 37:
				return net.runelite.api.SpriteID.SPELL_BLOOD_BURST;
			case 38:
				return net.runelite.api.SpriteID.SPELL_ICE_BURST;
			case 43:
				return net.runelite.api.SpriteID.SPELL_SMOKE_BARRAGE;
			case 44:
				return net.runelite.api.SpriteID.SPELL_SHADOW_BARRAGE;
			case 45:
				return net.runelite.api.SpriteID.SPELL_BLOOD_BARRAGE;
			default:
				return net.runelite.api.SpriteID.SPELL_ICE_BARRAGE;
		}
	}

	/** The sprite to draw on the best pile: the spell on autocast, or Ice Barrage. */
	int spellSprite()
	{
		return spellSprite(client.getVarbitValue(VarbitID.AUTOCAST_SPELL));
	}

	/**
	 * The most monsters one chinchompa hits, by the item held: grey and red ones 11, black ones 12; 0 for any other
	 * item (OSRS Wiki, Chinchompa, Red chinchompa and Black chinchompa, read 2026-10-16).
	 */
	static int chinchompaTargets(int itemId)
	{
		switch (itemId)
		{
			case ItemID.CHINCHOMPA_CAPTURED:
			case ItemID.CHINCHOMPA_BIG_CAPTURED:
				return 11;
			case ItemID.CHINCHOMPA_BLACK:
				return 12;
			default:
				return 0;
		}
	}

	/** The chinchompa held at the last count, or -1. */
	int chinchompa()
	{
		return chinchompa;
	}

	/** The item in the weapon slot, or -1. */
	private int weapon()
	{
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		Item item = worn == null ? null : worn.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		return item == null ? -1 : item.getId();
	}

	/** The name of the current slayer task, as RuneLite's Slayer plugin saves it. */
	String slayerTask()
	{
		return configManager.getRSProfileConfiguration("slayer", "taskName");
	}

	private List<Pile> work()
	{
		// chinchompas in hand: their area is the spell's, with more targets and their own reach
		int weapon = weapon();
		int chinTargets = chinchompaTargets(weapon);
		chinchompa = chinTargets > 0 ? weapon : -1;
		if (config.onlyAncientsOrChins() && chinTargets == 0
			&& client.getVarbitValue(VarbitID.SPELLBOOK) != ANCIENT_SPELLBOOK)
		{
			return Collections.emptyList();
		}
		int mostTargets = chinTargets > 0 ? chinTargets : PileCalc.MAX_TARGETS;
		int range = chinTargets == 0 ? MAGIC_RANGE
			: client.getVarpValue(VarPlayerID.COM_MODE) == LONG_FUSE ? CHINCHOMPA_RANGE_LONG : CHINCHOMPA_RANGE;
		if (config.onlyMulti() && client.getVarbitValue(VarbitID.MULTIWAY_INDICATOR) != 1)
		{
			return Collections.emptyList();
		}
		String task = null;
		if (config.onlyTask())
		{
			if (client.getVarpValue(VarPlayerID.SLAYER_COUNT) > 0)
			{
				task = slayerTask();
			}
			if ((task == null || task.isEmpty()) && config.extraNames().trim().isEmpty())
			{
				return Collections.emptyList();
			}
		}

		// with "Count monsters not on task", the other monsters take up some of the nine targets but are never marked
		List<NPC> npcs = new ArrayList<>();
		List<PileCalc.Spot> spots = new ArrayList<>();
		List<Boolean> onTask = new ArrayList<>();
		boolean anyOnTask = false;
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (npc == null || npc.isDead())
			{
				continue;
			}
			NPCComposition composition = npc.getTransformedComposition();
			if (composition == null || !attackable(composition))
			{
				continue;
			}
			boolean ours = !config.onlyTask() || PileCalc.matches(npc.getName(), task, config.extraNames());
			if (!ours && !config.countOthers())
			{
				continue;
			}
			WorldPoint at = npc.getWorldLocation();
			npcs.add(npc);
			spots.add(new PileCalc.Spot(at.getX(), at.getY(), composition.getSize()));
			onTask.add(ours);
			anyOnTask |= ours;
		}
		if (!anyOnTask)
		{
			return Collections.emptyList();
		}

		// scenery between the target and another monster of its 3x3 area keeps the spell from reaching that one;
		// RuneLite's line of sight says "no" between areas that overlap, but monsters on top of each other are hit together
		net.runelite.api.WorldView view = client.getTopLevelWorldView();
		int[] hits = PileCalc.hits(spots, mostTargets, (target, other) ->
		{
			net.runelite.api.coords.WorldArea from = npcs.get(target).getWorldArea();
			net.runelite.api.coords.WorldArea to = npcs.get(other).getWorldArea();
			return from == null || to == null || from.intersectsWith(to) || from.hasLineOfSightTo(view, to);
		});
		int most = 0;
		for (int i = 0; i < hits.length; i++)
		{
			if (onTask.get(i))
			{
				most = Math.max(most, hits[i]);
			}
		}
		if (most < config.minimum())
		{
			return Collections.emptyList();
		}
		// the best pile is the best one the player can cast on without walking (walking undoes the pile);
		// with none in reach, the largest of all
		net.runelite.api.Player me = client.getLocalPlayer();
		net.runelite.api.coords.WorldArea here = me == null ? null : me.getWorldArea();
		boolean[] inReach = new boolean[hits.length];
		int mostInReach = 0;
		for (int i = 0; i < hits.length; i++)
		{
			if (onTask.get(i) && hits[i] >= config.minimum() && inReach(view, here, npcs.get(i).getWorldArea(), range))
			{
				inReach[i] = true;
				mostInReach = Math.max(mostInReach, hits[i]);
			}
		}
		boolean anyInReach = mostInReach > 0;
		if (anyInReach)
		{
			most = mostInReach;
		}
		// one mark per tile; monsters of different sizes on one tile can give different counts:
		// the tile keeps the largest, and is the best if any of them is
		Map<Long, Pile> byTile = new LinkedHashMap<>();
		for (int i = 0; i < hits.length; i++)
		{
			if (!onTask.get(i) || hits[i] < config.minimum())
			{
				continue;
			}
			PileCalc.Spot s = spots.get(i);
			long key = ((long) s.x << 32) | (s.y & 0xffffffffL);
			Pile pile = new Pile(npcs.get(i), hits[i], hits[i] == most && (inReach[i] || !anyInReach));
			Pile before = byTile.get(key);
			if (before != null)
			{
				Pile larger = before.hits >= pile.hits ? before : pile;
				pile = new Pile(larger.npc, larger.hits, before.best || pile.best);
			}
			byTile.put(key, pile);
		}
		return new ArrayList<>(byTile.values());
	}

	/**
	 * Whether the player can cast on a monster from where they stand: within 10 squares, Chebyshev distance, to the
	 * monster's tile closest to them, and in line of sight from them (OSRS Wiki, Attack range and Line of sight).
	 */
	static boolean inReach(net.runelite.api.WorldView view, net.runelite.api.coords.WorldArea player,
		net.runelite.api.coords.WorldArea monster)
	{
		return inReach(view, player, monster, MAGIC_RANGE);
	}

	/** The same, within {@code range} squares (9 for chinchompas, 10 on long fuse). */
	static boolean inReach(net.runelite.api.WorldView view, net.runelite.api.coords.WorldArea player,
		net.runelite.api.coords.WorldArea monster, int range)
	{
		if (player == null || monster == null || player.distanceTo(monster) > range)
		{
			return false;
		}
		return player.intersectsWith(monster) || player.hasLineOfSightTo(view, monster);
	}

	private static boolean attackable(NPCComposition composition)
	{
		String[] actions = composition.getActions();
		if (actions == null)
		{
			return false;
		}
		for (String action : actions)
		{
			if ("Attack".equals(action))
			{
				return true;
			}
		}
		return false;
	}

	@Inject
	private net.runelite.client.callback.ClientThread reportThread;

	@Inject
	private okhttp3.OkHttpClient reportHttp;

	/** "Bug or suggestion" in the settings works as a button: each click opens the report window, whichever way the box ends up. */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("barrageindicator".equals(event.getGroup()) && "reportBug".equals(event.getKey()) && event.getNewValue() != null)
		{
			reportThread.invokeLater(() ->
			{
				final NPC monster = null;
				final String name = monster == null ? null : monster.getName();
				final String context = ReportWindow.context(client, monster);
				javax.swing.SwingUtilities.invokeLater(() -> ReportWindow.open(client.getCanvas(), name, context, reportHttp));
			});
		}
	}
}
