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
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/** Draws the count on each pile and the spell icon on the best one. */
class BarrageIndicatorOverlay extends Overlay
{
	/** How far above the monster the mark is drawn. */
	private static final int ABOVE = 30;

	private final BarrageIndicatorPlugin plugin;
	private final BarrageIndicatorConfig config;
	private final SpriteManager spriteManager;
	private final Client client;

	@Inject
	BarrageIndicatorOverlay(BarrageIndicatorPlugin plugin, BarrageIndicatorConfig config, SpriteManager spriteManager,
		Client client)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.spriteManager = spriteManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		// over the tile marks of other plugins
		setPriority(Overlay.PRIORITY_HIGH);
	}

	/**
	 * Fills the monster's south-west tile, the one the spell's area is centred on, and draws the spell icon on it.
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
		BufferedImage icon = spriteManager.getSprite(plugin.spellSprite(), 0);
		Point where = icon == null ? null : Perspective.getCanvasImageLocation(client, at, icon, 0);
		if (where != null)
		{
			OverlayUtil.renderImageLocation(graphics, where, icon);
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
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

			BufferedImage icon = pile.best && config.showIcon()
				? spriteManager.getSprite(plugin.spellSprite(), 0) : null;
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
