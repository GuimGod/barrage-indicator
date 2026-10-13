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
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.LinkBrowser;

/**
 * Counts, for each pile of monsters, how many a burst or barrage spell cast there would hit,
 * and marks the pile where it hits the most. It only draws: no click, no menu, no spell.
 */
@PluginDescriptor(
	name = "Barrage Indicator",
	description = "Shows how many monsters a burst or barrage would hit on each pile, and marks the best pile",
	tags = {"barrage", "burst", "slayer", "stack", "pile", "aoe", "ancient", "magic"}
)
public class BarrageIndicatorPlugin extends Plugin
{
	private static final int ANCIENT_SPELLBOOK = 1;

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

	private List<Pile> work()
	{
		if (config.onlyAncients() && client.getVarbitValue(VarbitID.SPELLBOOK) != ANCIENT_SPELLBOOK)
		{
			return Collections.emptyList();
		}
		if (config.onlyMulti() && client.getVarbitValue(VarbitID.MULTIWAY_INDICATOR) != 1)
		{
			return Collections.emptyList();
		}
		String task = null;
		if (config.onlyTask())
		{
			if (client.getVarpValue(VarPlayerID.SLAYER_COUNT) > 0)
			{
				task = configManager.getRSProfileConfiguration("slayer", "taskName");
			}
			if ((task == null || task.isEmpty()) && config.extraNames().trim().isEmpty())
			{
				return Collections.emptyList();
			}
		}

		List<NPC> npcs = new ArrayList<>();
		List<PileCalc.Spot> spots = new ArrayList<>();
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
			if (config.onlyTask() && !PileCalc.matches(npc.getName(), task, config.extraNames()))
			{
				continue;
			}
			WorldPoint at = npc.getWorldLocation();
			npcs.add(npc);
			spots.add(new PileCalc.Spot(at.getX(), at.getY(), composition.getSize()));
		}
		if (npcs.isEmpty())
		{
			return Collections.emptyList();
		}

		// scenery between the target and another monster of its 3x3 area keeps the spell from reaching that one
		net.runelite.api.WorldView view = client.getTopLevelWorldView();
		int[] hits = PileCalc.hits(spots, (target, other) ->
		{
			net.runelite.api.coords.WorldArea from = npcs.get(target).getWorldArea();
			net.runelite.api.coords.WorldArea to = npcs.get(other).getWorldArea();
			return from == null || to == null || from.hasLineOfSightTo(view, to);
		});
		int most = 0;
		for (int h : hits)
		{
			most = Math.max(most, h);
		}
		if (most < config.minimum())
		{
			return Collections.emptyList();
		}
		// one mark per tile: monsters on the same tile give the same count
		Map<Long, Pile> byTile = new LinkedHashMap<>();
		for (int i = 0; i < hits.length; i++)
		{
			if (hits[i] < config.minimum())
			{
				continue;
			}
			PileCalc.Spot s = spots.get(i);
			long key = ((long) s.x << 32) | (s.y & 0xffffffffL);
			byTile.putIfAbsent(key, new Pile(npcs.get(i), hits[i], hits[i] == most));
		}
		return new ArrayList<>(byTile.values());
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

	/** The author's form for a bug or a suggestion, with the plugin's name filled in. The player writes and sends it in the browser. */
	static final String REPORT_FORM = "https://docs.google.com/forms/d/e/1FAIpQLSclvrdcVzomW3-4lj7-gnvbzmerY6qwHepApCuI7e4Q8raDyw/viewform?usp=pp_url&entry.2100394972=Barrage%20Indicator";

	/** "Bug or suggestion" in the settings works as a button: each click opens the form, whichever way the box ends up. */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("barrageindicator".equals(event.getGroup()) && "reportBug".equals(event.getKey()) && event.getNewValue() != null)
		{
			LinkBrowser.browse(REPORT_FORM);
		}
	}
}
