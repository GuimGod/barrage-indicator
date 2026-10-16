package com.barrageindicator;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.IndexedObjectSet;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;

/**
 * A fake game for the plugin's whole path (onGameTick -> work() -> PileCalc), with RuneLite's real
 * WorldArea.hasLineOfSightTo: a 104x104 scene on plane 0 with real tiles and editable collision flags
 * (open ground = 0), the client, the local player and the monsters, all as plain proxies.
 * Monsters behave like the injected client: getWorldArea() = new WorldArea(getWorldLocation(), size, size).
 * Coordinates given to it are scene coordinates; the world ones are BASE_X/BASE_Y further.
 */
final class TestWorld
{
	static final int BASE_X = 3200;
	static final int BASE_Y = 3200;

	final int[][] flags = new int[104][104];
	final List<NPC> npcs = new ArrayList<>();
	final WorldView view;

	/** Plugin settings, the plugin's defaults except the three context filters. */
	boolean onlyTask = false;
	boolean countOthers = false;
	boolean onlyAncients = false;
	boolean onlyMulti = false;
	String extraNames = "";
	int minimum = 2;

	/** Game state read by the plugin. */
	int spellbook = 1;
	int multi = 1;
	int taskLeft = 0;
	String task = null;
	/** The item in the weapon slot (-1: none) and the attack style setting (3: long fuse). */
	int weapon = -1;
	int attackStyle = 0;
	private Player player;
	/** The plugin of the last run(), to read what it keeps for the overlay. */
	BarrageIndicatorPlugin lastPlugin;

	TestWorld()
	{
		Tile[][][] tiles = new Tile[4][104][104];
		for (int z = 0; z < 4; z++)
		{
			for (int x = 0; x < 104; x++)
			{
				for (int y = 0; y < 104; y++)
				{
					final int fz = z, fx = x, fy = y;
					Map<String, Function<Object[], Object>> t = new HashMap<>();
					t.put("getPlane", a -> fz);
					t.put("getSceneLocation", a -> new net.runelite.api.Point(fx, fy));
					t.put("getWorldLocation", a -> new WorldPoint(BASE_X + fx, BASE_Y + fy, fz));
					tiles[z][x][y] = proxy(Tile.class, t);
				}
			}
		}
		Map<String, Function<Object[], Object>> c = new HashMap<>();
		c.put("getFlags", a -> flags);
		CollisionData cd = proxy(CollisionData.class, c);
		CollisionData[] maps = {cd, cd, cd, cd};
		Map<String, Function<Object[], Object>> s = new HashMap<>();
		s.put("getTiles", a -> tiles);
		Scene scene = proxy(Scene.class, s);
		Map<String, Function<Object[], Object>> set = new HashMap<>();
		set.put("iterator", a -> npcs.iterator());
		@SuppressWarnings("unchecked")
		IndexedObjectSet<NPC> npcSet = proxy(IndexedObjectSet.class, set);
		Map<String, Function<Object[], Object>> v = new HashMap<>();
		v.put("getScene", a -> scene);
		v.put("getCollisionMaps", a -> maps);
		v.put("getPlane", a -> 0);
		v.put("getBaseX", a -> BASE_X);
		v.put("getBaseY", a -> BASE_Y);
		v.put("getSizeX", a -> 104);
		v.put("getSizeY", a -> 104);
		v.put("getId", a -> -1);
		v.put("isTopLevel", a -> true);
		v.put("npcs", a -> npcSet);
		view = proxy(WorldView.class, v);
	}

	/** A monster whose south-west tile is (x,y) of the scene; it goes at the end of the client's list. */
	NPC add(String name, int x, int y, int size)
	{
		return add(name, x, y, size, false);
	}

	NPC add(String name, int x, int y, int size, boolean dead)
	{
		Map<String, Function<Object[], Object>> comp = new HashMap<>();
		comp.put("getActions", a -> new String[]{null, "Attack", null, null, null});
		comp.put("getSize", a -> size);
		comp.put("getName", a -> name);
		NPCComposition composition = proxy(NPCComposition.class, comp);
		WorldPoint sw = new WorldPoint(BASE_X + x, BASE_Y + y, 0);
		Map<String, Function<Object[], Object>> n = new HashMap<>();
		n.put("getName", a -> name);
		n.put("isDead", a -> dead);
		n.put("getComposition", a -> composition);
		n.put("getTransformedComposition", a -> composition);
		n.put("getWorldLocation", a -> sw);
		n.put("getWorldArea", a -> new WorldArea(sw, size, size));
		n.put("getWorldView", a -> view);
		n.put("toString", a -> name + "@" + x + "," + y + "/" + size);
		NPC npc = proxy(NPC.class, n);
		npcs.add(npc);
		return npc;
	}

	void addMany(String name, int x, int y, int count)
	{
		for (int i = 0; i < count; i++)
		{
			add(name, x, y, 1);
		}
	}

	/** Puts the local player on (x,y) of the scene. Without it there is no local player. */
	void player(int x, int y)
	{
		WorldPoint at = new WorldPoint(BASE_X + x, BASE_Y + y, 0);
		Map<String, Function<Object[], Object>> p = new HashMap<>();
		p.put("getWorldLocation", a -> at);
		p.put("getWorldArea", a -> new WorldArea(at, 1, 1));
		p.put("getWorldView", a -> view);
		p.put("getName", a -> "player");
		player = proxy(Player.class, p);
	}

	BarrageIndicatorConfig config()
	{
		return new BarrageIndicatorConfig()
		{
			@Override
			public boolean onlyTask()
			{
				return onlyTask;
			}

			@Override
			public boolean countOthers()
			{
				return countOthers;
			}

			@Override
			public boolean onlyAncientsOrChins()
			{
				return onlyAncients;
			}

			@Override
			public boolean onlyMulti()
			{
				return onlyMulti;
			}

			@Override
			public String extraNames()
			{
				return extraNames;
			}

			@Override
			public int minimum()
			{
				return minimum;
			}
		};
	}

	Client client()
	{
		Map<String, Function<Object[], Object>> c = new HashMap<>();
		c.put("getTopLevelWorldView", a -> view);
		c.put("getLocalPlayer", a -> player);
		c.put("getVarbitValue", a ->
		{
			int id = (Integer) a[0];
			if (id == VarbitID.SPELLBOOK)
			{
				return spellbook;
			}
			if (id == VarbitID.MULTIWAY_INDICATOR)
			{
				return multi;
			}
			return 0;
		});
		c.put("getVarpValue", a ->
		{
			int id = (Integer) a[0];
			if (id == VarPlayerID.SLAYER_COUNT)
			{
				return taskLeft;
			}
			return id == VarPlayerID.COM_MODE ? attackStyle : 0;
		});
		c.put("getItemContainer", a ->
		{
			if ((Integer) a[0] != InventoryID.WORN || weapon < 0)
			{
				return null;
			}
			Item item = new Item(weapon, 1);
			Map<String, Function<Object[], Object>> worn = new HashMap<>();
			worn.put("getItem", b -> (Integer) b[0] == EquipmentInventorySlot.WEAPON.getSlotIdx() ? item : null);
			return proxy(ItemContainer.class, worn);
		});
		return proxy(Client.class, c);
	}

	/** One game tick of the plugin in this world: what it would draw. */
	List<BarrageIndicatorPlugin.Pile> run() throws Exception
	{
		// the task name is the one RuneLite's Slayer plugin saves in the profile
		BarrageIndicatorPlugin plugin = new BarrageIndicatorPlugin()
		{
			@Override
			String slayerTask()
			{
				return task;
			}
		};
		set(plugin, "client", client());
		set(plugin, "config", config());
		plugin.onGameTick(new GameTick());
		lastPlugin = plugin;
		return plugin.piles();
	}

	/** The pile drawn on the tile (x,y) of the scene, or null. */
	static BarrageIndicatorPlugin.Pile at(List<BarrageIndicatorPlugin.Pile> piles, int x, int y)
	{
		for (BarrageIndicatorPlugin.Pile p : piles)
		{
			WorldPoint w = p.npc.getWorldLocation();
			if (w.getX() == BASE_X + x && w.getY() == BASE_Y + y)
			{
				return p;
			}
		}
		return null;
	}

	/** The piles as text, "(x,y)=hits", a star on the best. */
	static String show(List<BarrageIndicatorPlugin.Pile> piles)
	{
		StringBuilder sb = new StringBuilder("[");
		for (BarrageIndicatorPlugin.Pile p : piles)
		{
			WorldPoint w = p.npc.getWorldLocation();
			sb.append(" (").append(w.getX() - BASE_X).append(',').append(w.getY() - BASE_Y).append(")=")
				.append(p.hits).append(p.best ? "*" : "");
		}
		return sb.append(" ]").toString();
	}

	static void set(Object target, String field, Object value) throws Exception
	{
		Field f = BarrageIndicatorPlugin.class.getDeclaredField(field);
		f.setAccessible(true);
		f.set(target, value);
	}

	private static Object zero(Class<?> t)
	{
		if (!t.isPrimitive() || t == void.class)
		{
			return null;
		}
		if (t == boolean.class)
		{
			return false;
		}
		if (t == long.class)
		{
			return 0L;
		}
		if (t == double.class)
		{
			return 0d;
		}
		if (t == float.class)
		{
			return 0f;
		}
		return 0;
	}

	@SuppressWarnings("unchecked")
	static <T> T proxy(Class<T> type, Map<String, Function<Object[], Object>> impl)
	{
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, m, a) ->
		{
			Function<Object[], Object> f = impl.get(m.getName());
			if (f != null)
			{
				return f.apply(a);
			}
			switch (m.getName())
			{
				case "hashCode":
					return System.identityHashCode(p);
				case "equals":
					return p == a[0];
				case "toString":
					return type.getSimpleName();
				default:
					return zero(m.getReturnType());
			}
		});
	}
}
