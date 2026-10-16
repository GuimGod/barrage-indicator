/*
 * Copyright (c) 2026, GuimGod
 * All rights reserved. Licensed under the BSD 2-Clause License (see LICENSE).
 */
package com.barrageindicator;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/** Draws the count on each pile and the spell's or chinchompa's icon on the best one. */
class BarrageIndicatorOverlay extends Overlay
{
	/** How far above the monster the mark is drawn. */
	private static final int ABOVE = 30;

	private final BarrageIndicatorPlugin plugin;
	private final BarrageIndicatorConfig config;
	private final SpriteManager spriteManager;
	private final ItemManager itemManager;
	private final Client client;

	@Inject
	BarrageIndicatorOverlay(BarrageIndicatorPlugin plugin, BarrageIndicatorConfig config, SpriteManager spriteManager,
		ItemManager itemManager, Client client)
	{
		this.client = client;
		this.itemManager = itemManager;
		this.plugin = plugin;
		this.config = config;
		this.spriteManager = spriteManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		// over the tile marks of other plugins
		setPriority(Overlay.PRIORITY_HIGH);
	}

	/** The icon for the best pile and the tile to cast on: the chinchompa held, or the spell on autocast. */
	private BufferedImage icon()
	{
		return icon(plugin.chinchompa(), itemManager::getImage, () -> spriteManager.getSprite(plugin.spellSprite(), 0));
	}

	/**
	 * Whether the icon goes over the pile: only on the best one, and only when the tile to cast on is not marked,
	 * since that tile already carries the icon. The icon is drawn once.
	 */
	static boolean iconOverPile(boolean best, boolean showIcon, boolean showSouthWest)
	{
		return best && showIcon && !showSouthWest;
	}

	/** With a chinchompa held (its item id), that chinchompa's picture; otherwise the spell's. */
	static BufferedImage icon(int chinchompa, java.util.function.IntFunction<BufferedImage> itemImage,
		java.util.function.Supplier<BufferedImage> spellImage)
	{
		return chinchompa > 0 ? itemImage.apply(chinchompa) : spellImage.get();
	}

	/** The Smoke Devil Dungeon, and the tiles to group the smoke devils from: north, west and south of the bones. */
	private static final int SMOKE_DEVIL_DUNGEON = 9619;
	private static final int[][] GROUPING_TILES = {{2393, 9445}, {2392, 9443}, {2393, 9441}};
	private static final String[] GROUPING_LABELS = {"Group N", "Group W", "Group S"};

	/** Outlines the grouping tiles when the player is in the Smoke Devil Dungeon. */
	private void markGroupingTiles(Graphics2D graphics)
	{
		net.runelite.api.Player me = client.getLocalPlayer();
		if (me == null)
		{
			return;
		}
		net.runelite.api.WorldView view = me.getWorldView();
		net.runelite.api.coords.WorldPoint here = net.runelite.api.coords.WorldPoint.fromLocalInstance(client, me.getLocalLocation());
		if (here == null || here.getRegionID() != SMOKE_DEVIL_DUNGEON)
		{
			return;
		}
		Color colour = config.groupingColour();
		for (int i = 0; i < GROUPING_TILES.length; i++)
		{
			net.runelite.api.coords.WorldPoint tile = new net.runelite.api.coords.WorldPoint(
				GROUPING_TILES[i][0], GROUPING_TILES[i][1], here.getPlane());
			// the dungeon can be shown as an instance: the same tile may sit somewhere else in the scene
			for (net.runelite.api.coords.WorldPoint shown : net.runelite.api.coords.WorldPoint.toLocalInstance(view, tile))
			{
				LocalPoint at = LocalPoint.fromWorld(view, shown);
				if (at == null)
				{
					continue;
				}
				Polygon poly = Perspective.getCanvasTilePoly(client, at);
				if (poly != null)
				{
					OverlayUtil.renderPolygon(graphics, poly, colour, new BasicStroke(2));
				}
				Point text = Perspective.getCanvasTextLocation(client, graphics, at, GROUPING_LABELS[i], 0);
				if (text != null)
				{
					OverlayUtil.renderTextLocation(graphics, text, GROUPING_LABELS[i], colour);
				}
			}
		}
	}

	/**
	 * Fills the monster's south-west tile, the one the spell's area is centred on, and draws the icon on it.
	 */
	private void markSouthWest(Graphics2D graphics, NPC npc)
	{
		LocalPoint at = LocalPoint.fromWorld(npc.getWorldView(), npc.getWorldLocation());
		if (at == null)
		{
			return;
		}
		Polygon tile = Perspective.getCanvasTilePoly(client, at);
		if (tile == null)
		{
			return;
		}
		Color colour = config.southWestColour();
		graphics.setColor(colour);
		graphics.fillPolygon(tile);
		graphics.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), 255));
		graphics.setStroke(new BasicStroke(2));
		graphics.drawPolygon(tile);
		BufferedImage icon = icon();
		Point where = icon == null ? null : Perspective.getCanvasImageLocation(client, at, icon, 0);
		if (where != null)
		{
			OverlayUtil.renderImageLocation(graphics, where, icon);
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (config.showGroupingTiles())
		{
			graphics.setFont(FontManager.getRunescapeSmallFont());
			markGroupingTiles(graphics);
		}
		Font font = FontManager.getRunescapeBoldFont().deriveFont((float) config.fontSize());
		graphics.setFont(font);
		for (BarrageIndicatorPlugin.Pile pile : plugin.piles())
		{
			if (!pile.best && !config.showCounts())
			{
				continue;
			}
			NPC npc = pile.npc;
			Color colour = pile.best ? config.bestColour() : config.otherColour();
			String text = Integer.toString(pile.hits);
			int height = npc.getLogicalHeight() + ABOVE;

			if (pile.best && config.showSouthWest())
			{
				markSouthWest(graphics, npc);
			}
			if (pile.best && config.showTile())
			{
				Polygon tile = npc.getCanvasTilePoly();
				if (tile != null)
				{
					OverlayUtil.renderPolygon(graphics, tile, colour, new BasicStroke(2));
				}
			}

			BufferedImage icon = iconOverPile(pile.best, config.showIcon(), config.showSouthWest()) ? icon() : null;
			if (icon != null)
			{
				Point at = npc.getCanvasImageLocation(icon, height);
				if (at != null)
				{
					// icon on the left, number on the right, the pair centred over the monster
					int width = graphics.getFontMetrics().stringWidth(text);
					int x = at.getX() - width / 2 - 1;
					OverlayUtil.renderImageLocation(graphics, new Point(x, at.getY()), icon);
					int baseline = at.getY() + icon.getHeight() / 2 + graphics.getFontMetrics().getAscent() / 2 - 1;
					OverlayUtil.renderTextLocation(graphics, new Point(x + icon.getWidth() + 2, baseline), text, colour);
				}
				continue;
			}
			Point at = npc.getCanvasTextLocation(graphics, text, height);
			if (at != null)
			{
				OverlayUtil.renderTextLocation(graphics, at, text, colour);
			}
		}
		return null;
	}
}
