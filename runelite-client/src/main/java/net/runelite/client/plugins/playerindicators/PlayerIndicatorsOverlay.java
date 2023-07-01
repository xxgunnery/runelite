/*
 * Copyright (c) 2018, Tomas Slusny <slusnucky@gmail.com>
 * Copyright (c) 2019, Jordan Atwood <nightfirecat@protonmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.playerindicators;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatRank;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.Text;

@Singleton
public class PlayerIndicatorsOverlay extends Overlay
{
	private static final int ACTOR_OVERHEAD_TEXT_MARGIN = 40;
	private static final int ACTOR_HORIZONTAL_TEXT_MARGIN = 10;

	private static final FontManager fontManager = new FontManager();

	private static final Color RED = new Color(221, 44, 0);
	private static final Color GREEN = new Color(0, 200, 83);
	private static final Color ORANGE = new Color(255, 109, 0);
	private static final Color YELLOW = new Color(255, 214, 0);
	private static final Color CYAN = new Color(0, 184, 212);
	private static final Color BLUE = new Color(41, 98, 255);
	private static final Color DEEP_PURPLE = new Color(98, 0, 234);
	private static final Color PURPLE = new Color(170, 0, 255);
	private static final Color GRAY = new Color(158, 158, 158);

	private final PlayerIndicatorsService playerIndicatorsService;
	private final PlayerIndicatorsConfig config;
	private final ChatIconManager chatIconManager;

	private final Client client;

	@Inject
	private PlayerIndicatorsOverlay(Client client, PlayerIndicatorsConfig config, PlayerIndicatorsService playerIndicatorsService,
									ChatIconManager chatIconManager)
	{
		this.config = config;
		this.client = client;
		this.playerIndicatorsService = playerIndicatorsService;
		this.chatIconManager = chatIconManager;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.MED);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		playerIndicatorsService.forEachPlayer((player, decorations) -> renderPlayerOverlay(graphics, player, decorations));

		renderPlayers(graphics);

		return null;
	}

	private void renderPlayers(Graphics2D graphics)
	{

		List<Player> players = client.getPlayers();
		Player mainPlayer = client.getLocalPlayer();

		Widget wildernessLevel = client.getWidget(WidgetInfo.PVP_WILDERNESS_LEVEL);
		String widgetText = wildernessLevel.getText();

		if (!widgetText.equals("--") && !widgetText.equals("Level: --") && !widgetText.equals(""))
		{
			String[] lines = new String[2];
			if (widgetText.contains("\n"))
			{
				lines = widgetText.split("\n");
			}
			else if (widgetText.contains("<br>"))
			{
				lines = widgetText.split("<br>");
			}

			String[] rangeValues = lines[1].split("-");
			int minValue = Integer.parseInt(rangeValues[0].trim());
			int maxValue = Integer.parseInt(rangeValues[1].trim());

			for (Player player : players)
			{
				boolean shouldStopRendering = false;

				Color color = Color.BLACK;

				int mainPlayerLevel = mainPlayer.getCombatLevel();
				int playerLevel = player.getCombatLevel();

				int combatDiff = playerLevel - mainPlayerLevel;

				if (combatDiff > 10)
				{
					color = RED;
				}
				else if (combatDiff < -10)
				{
					color = GREEN;
				}
				else if (combatDiff < 3 && combatDiff > -3)
				{
					color = YELLOW;
				}
				else if (combatDiff < 10 && combatDiff > -10)
				{
					color = ORANGE;
				}

				String text = player.getName() + " (level: " + playerLevel + ")";

				if (playerLevel >= minValue && playerLevel <= maxValue)
				{
					graphics.setFont(fontManager.getRunescapeBoldFont());
				}
				else
				{
					if (combatDiff > 34 || combatDiff < -34)
					{
						shouldStopRendering = true;
					}
					graphics.setFont(fontManager.getRunescapeSmallFont());
				}

				if ((!player.getName().equals(mainPlayer.getName())) && !shouldStopRendering)
				{
					OverlayUtil.renderActorOverlay(graphics, player, text, color);
				}

			}
		}
	}

	private static int extractLevel(String line)
	{
		String[] parts = line.split(":");
		return Integer.parseInt(parts[1].trim());
	}


	private void renderPlayerOverlay(Graphics2D graphics, Player actor, PlayerIndicatorsService.Decorations decorations)
	{
		final PlayerNameLocation drawPlayerNamesConfig = config.playerNamePosition();
		if (drawPlayerNamesConfig == PlayerNameLocation.DISABLED)
		{
			return;
		}

		final int zOffset;
		switch (drawPlayerNamesConfig)
		{
			case MODEL_CENTER:
			case MODEL_RIGHT:
				zOffset = actor.getLogicalHeight() / 2;
				break;
			default:
				zOffset = actor.getLogicalHeight() + ACTOR_OVERHEAD_TEXT_MARGIN;
		}

		final String name = Text.sanitize(actor.getName());
		Point textLocation = actor.getCanvasTextLocation(graphics, name, zOffset);

		if (drawPlayerNamesConfig == PlayerNameLocation.MODEL_RIGHT)
		{
			textLocation = actor.getCanvasTextLocation(graphics, "", zOffset);

			if (textLocation == null)
			{
				return;
			}

			textLocation = new Point(textLocation.getX() + ACTOR_HORIZONTAL_TEXT_MARGIN, textLocation.getY());
		}

		if (textLocation == null)
		{
			return;
		}

		BufferedImage rankImage = null;
		if (decorations.getFriendsChatRank() != null && config.showFriendsChatRanks())
		{
			if (decorations.getFriendsChatRank() != FriendsChatRank.UNRANKED)
			{
				rankImage = chatIconManager.getRankImage(decorations.getFriendsChatRank());
			}
		}
		else if (decorations.getClanTitle() != null && config.showClanChatRanks())
		{
			rankImage = chatIconManager.getRankImage(decorations.getClanTitle());
		}

		if (rankImage != null)
		{
			final int imageWidth = rankImage.getWidth();
			final int imageTextMargin;
			final int imageNegativeMargin;

			if (drawPlayerNamesConfig == PlayerNameLocation.MODEL_RIGHT)
			{
				imageTextMargin = imageWidth;
				imageNegativeMargin = 0;
			}
			else
			{
				imageTextMargin = imageWidth / 2;
				imageNegativeMargin = imageWidth / 2;
			}

			final int textHeight = graphics.getFontMetrics().getHeight() - graphics.getFontMetrics().getMaxDescent();
			final Point imageLocation = new Point(textLocation.getX() - imageNegativeMargin - 1, textLocation.getY() - textHeight / 2 - rankImage.getHeight() / 2);
			OverlayUtil.renderImageLocation(graphics, imageLocation, rankImage);

			// move text
			textLocation = new Point(textLocation.getX() + imageTextMargin, textLocation.getY());
		}

		OverlayUtil.renderTextLocation(graphics, textLocation, name, decorations.getColor());
	}
}