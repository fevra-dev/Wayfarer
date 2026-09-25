package com.wayfarer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Horizontal compass strip, the Skyrim/FPS "heading tape": a fixed center
 * caret marks where the camera points, and the tape of ticks and cardinal
 * letters slides under it as the camera turns. Reads Client#getCameraYaw()
 * and draws with plain Graphics2D. No game state is touched, display only.
 *
 * All angle math lives in CompassMath, which is pure and unit tested —
 * including the yaw-direction convention (camera yaw counterclockwise,
 * compass bearing clockwise). Turning the camera right (clockwise, e.g.
 * north to east) increases bearing, and the tape slides left.
 *
 * Positioned top-center by default; Alt-drag moves it like any overlay.
 */
class WayfarerOverlay extends Overlay
{
	/** Degrees visible from center to each edge of the strip. */
	private static final double HALF_SPAN_DEG = 60.0;
	/** Outer fraction of each strip half over which elements fade out. */
	private static final double FADE_ZONE = 0.22;
	private static final int MINOR_TICK_STEP_DEG = 15;

	// Spacing on the 8dp grid (see Palette for the color rules).
	private static final int STRIP_HEIGHT = 24;
	private static final int CARET_GAP = 2;
	private static final int CARET_LENGTH = 6;
	private static final int MIN_WIDTH = 240;
	private static final int MAX_WIDTH = 480;
	/** RuneLite's snap-corner inset (SnapCorners.BORDER). */
	private static final int SNAP_BORDER = 5;

	private static final Color BACKGROUND = Palette.withAlpha(Palette.WARM_BLACK, 80);
	private static final Color CARET = Palette.withAlpha(Palette.AMBER, 230);
	private static final String[] CARDINAL_LETTERS = {"N", "E", "S", "W"};

	private static final int LOCAL_TILE_SIZE = 128;
	private static final int MARKER_DOT_SIZE = 4;
	/** Markers closer than this many tiles fade toward NEAR_FADE_FLOOR. */
	private static final double NEAR_FADE_TILES = 4.0;
	private static final double NEAR_FADE_FLOOR = 0.2;

	private final Client client;
	private final WayfarerConfig config;

	/** Reused per frame so attackable NPCs can be drawn last without a second NPC pass. */
	private final List<LocalPoint> threats = new ArrayList<>();

	@Inject
	private WayfarerOverlay(Client client, WayfarerConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.TOP_CENTER);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	/** Movable sits on the TOP_CENTER snap; centred-above-chat positions itself and cannot be dragged. */
	void applyPlacement(StripPlacement placement)
	{
		boolean selfPlaced = placement == StripPlacement.ABOVE_CHAT;
		setPosition(selfPlaced ? OverlayPosition.DYNAMIC : OverlayPosition.TOP_CENTER);
		setMovable(!selfPlaced);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		int stripWidth = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, (int) (client.getViewportWidth() * 0.40)));
		int height = STRIP_HEIGHT + CARET_GAP + CARET_LENGTH + 1;

		if (getPosition() != OverlayPosition.DYNAMIC)
		{
			drawStrip(graphics, stripWidth);
			return new Dimension(stripWidth, height);
		}

		// Centred above chat: self-positioned, so translate to our own origin.
		Point origin = aboveChatOrigin(stripWidth, height);
		graphics.translate(origin.x, origin.y);
		drawStrip(graphics, stripWidth);
		graphics.translate(-origin.x, -origin.y);
		return null;
	}

	/**
	 * Mirrors RuneLite's own above-chat snap line (SnapCorners: CHATBOX_TOP
	 * less BORDER, with the collapsed-chat adjustment from OverlayManager),
	 * centred on the same HUD container TOP_CENTER uses, so the two
	 * placements line up vertically.
	 */
	private Point aboveChatOrigin(int width, int height)
	{
		Widget hud = topLevel(InterfaceID.ToplevelOsrsStretch.HUD_CONTAINER_FRONT,
			InterfaceID.ToplevelPreEoc.HUD_CONTAINER_FRONT, InterfaceID.Toplevel.OVERLAY_HUD);
		Widget chat = topLevel(InterfaceID.ToplevelOsrsStretch.CHAT_CONTAINER,
			InterfaceID.ToplevelPreEoc.CHAT_CONTAINER, InterfaceID.Toplevel.CHAT_CONTAINER);

		int centerX = hud != null
			? hud.getBounds().x + hud.getBounds().width / 2
			: client.getViewportXOffset() + client.getViewportWidth() / 2;

		int chatTop;
		if (chat != null)
		{
			chatTop = chat.getBounds().y;
			Widget chatArea = client.getWidget(InterfaceID.Chatbox.CHATAREA);
			if (chatArea != null && chatArea.isSelfHidden())
			{
				chatTop += chatArea.getBounds().height;
			}
		}
		else
		{
			chatTop = client.getViewportYOffset() + client.getViewportHeight();
		}
		return new Point(centerX - width / 2, chatTop - SNAP_BORDER - height);
	}

	/** Same layout switch RuneLite uses: stretched, pre-EoC resizable, or fixed. */
	private Widget topLevel(int stretch, int preEoc, int fixed)
	{
		if (!client.isResized())
		{
			return client.getWidget(fixed);
		}
		return client.getWidget(client.getTopLevelInterfaceId() == InterfaceID.TOPLEVEL_PRE_EOC ? preEoc : stretch);
	}

	private void drawStrip(Graphics2D graphics, int stripWidth)
	{
		int halfWidth = stripWidth / 2;
		int centerX = halfWidth;
		int midY = STRIP_HEIGHT / 2;

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		int arc = config.shape() == StripShape.PILL ? STRIP_HEIGHT : 0;
		graphics.setColor(BACKGROUND);
		graphics.fillRoundRect(0, 0, stripWidth, STRIP_HEIGHT, arc, arc);

		double heading = CompassMath.bearingDegrees(client.getCameraYaw());

		graphics.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics fm = graphics.getFontMetrics();

		for (int bearing = 0; bearing < 360; bearing += MINOR_TICK_STEP_DEG)
		{
			double fraction = CompassMath.screenOffsetFraction(
				CompassMath.signedDeltaDegrees(bearing, heading), HALF_SPAN_DEG);
			if (Math.abs(fraction) > 1.0)
			{
				continue;
			}
			double alpha = CompassMath.edgeAlpha(fraction, FADE_ZONE);
			if (alpha <= 0)
			{
				continue;
			}
			int x = centerX + (int) Math.round(fraction * halfWidth);

			if (bearing % 90 == 0)
			{
				String letter = CARDINAL_LETTERS[bearing / 90];
				graphics.setColor(Palette.withAlpha(Palette.PAPER, (int) (220 * alpha)));
				graphics.drawString(letter, x - fm.stringWidth(letter) / 2, midY + fm.getAscent() / 2 - 1);
			}
			else if (bearing % 45 == 0)
			{
				graphics.setColor(Palette.withAlpha(Palette.PAPER, (int) (140 * alpha)));
				graphics.drawLine(x, midY - 5, x, midY + 5);
			}
			else
			{
				graphics.setColor(Palette.withAlpha(Palette.PAPER, (int) (80 * alpha)));
				graphics.drawLine(x, midY - 3, x, midY + 3);
			}
		}

		if (config.showPlayers() || config.showMonsters() || config.showNpcs())
		{
			renderMarkers(graphics, heading, centerX, halfWidth);
		}

		// Fixed center caret just below the strip, pointing up at the
		// current heading. Amber — the single accent, reserved for "where
		// you are pointing" (see Palette).
		graphics.setColor(CARET);
		graphics.drawLine(centerX, STRIP_HEIGHT + CARET_GAP, centerX, STRIP_HEIGHT + CARET_GAP + CARET_LENGTH);
	}

	/**
	 * Draw order is deliberate: non-attackable NPCs (yellow) first, other
	 * players (white) over them, attackable NPCs (red) last — a threat
	 * marker is never buried under a crowd.
	 *
	 * Only NPCs the minimap itself would draw are marked. Some content
	 * uses invisible NPCs to drive mechanics; the minimap hides those and
	 * so must this, or the strip becomes a mechanic indicator.
	 */
	private void renderMarkers(Graphics2D graphics, double heading, int centerX, int halfWidth)
	{
		Player local = client.getLocalPlayer();
		if (local == null)
		{
			return;
		}
		LocalPoint me = mainWorldLocation(local);
		if (me == null)
		{
			return;
		}
		int rangeLocal = config.nearbyRange() * LOCAL_TILE_SIZE;
		int railY = STRIP_HEIGHT - 5;

		// ponytail: top-level world view only, so players and NPCs aboard
		// other boats are not marked; iterate client.getWorldViews() and
		// transform each through its WorldEntity if that gets asked for.
		WorldView world = client.getTopLevelWorldView();
		boolean showMonsters = config.showMonsters();
		boolean showNpcs = config.showNpcs();
		threats.clear();
		for (NPC npc : world.npcs())
		{
			if (npc == null || npc.isDead())
			{
				continue;
			}
			NPCComposition composition = npc.getTransformedComposition();
			if (composition == null || !composition.isMinimapVisible())
			{
				continue;
			}
			if (npc.getCombatLevel() > 0)
			{
				if (showMonsters)
				{
					threats.add(npc.getLocalLocation());
				}
			}
			else if (showNpcs)
			{
				drawMarker(graphics, heading, me, npc.getLocalLocation(), rangeLocal, centerX, halfWidth, railY, config.npcColor());
			}
		}
		if (config.showPlayers())
		{
			for (Player player : world.players())
			{
				if (player != null && player != local)
				{
					drawMarker(graphics, heading, me, player.getLocalLocation(), rangeLocal, centerX, halfWidth, railY, config.playerColor());
				}
			}
		}
		Color monsterColor = config.monsterColor();
		for (LocalPoint threat : threats)
		{
			drawMarker(graphics, heading, me, threat, rangeLocal, centerX, halfWidth, railY, monsterColor);
		}
		threats.clear();
	}

	/**
	 * The local player's position in top-level world coordinates. Aboard a
	 * boat (Sailing), the player lives in the boat's own world view, whose
	 * local coordinates mean nothing against the main world's NPCs, so the
	 * point is carried through the boat's WorldEntity first.
	 */
	private LocalPoint mainWorldLocation(Player local)
	{
		LocalPoint lp = local.getLocalLocation();
		WorldView view = local.getWorldView();
		if (lp == null || view == null || view.isTopLevel())
		{
			return lp;
		}
		WorldEntity boat = client.getTopLevelWorldView().worldEntities().byIndex(view.getId());
		return boat == null ? null : boat.transformToMainWorld(lp);
	}

	private void drawMarker(Graphics2D graphics, double heading, LocalPoint me, LocalPoint them, int rangeLocal,
		int centerX, int halfWidth, int railY, Color color)
	{
		if (them == null)
		{
			return;
		}
		int dxEast = them.getX() - me.getX();
		int dyNorth = them.getY() - me.getY();
		if (dxEast == 0 && dyNorth == 0)
		{
			return;
		}
		long distSq = (long) dxEast * dxEast + (long) dyNorth * dyNorth;
		if (distSq > (long) rangeLocal * rangeLocal)
		{
			return;
		}

		double fraction = CompassMath.screenOffsetFraction(
			CompassMath.signedDeltaDegrees(CompassMath.bearingToTarget(dxEast, dyNorth), heading), HALF_SPAN_DEG);
		if (Math.abs(fraction) > 1.0)
		{
			return;
		}
		double edge = CompassMath.edgeAlpha(fraction, FADE_ZONE);
		if (edge <= 0)
		{
			return;
		}

		// Nearer reads stronger (230 alpha falling to 120 at the range cap),
		// except the last few tiles, which go quiet (see nearFade). Times the
		// strip's own edge fade and the user's chosen transparency.
		double dist = Math.sqrt((double) distSq);
		double near = CompassMath.nearFade(dist / LOCAL_TILE_SIZE, NEAR_FADE_TILES, NEAR_FADE_FLOOR);
		int alpha = (int) (edge * near * (230 - 110 * dist / rangeLocal) * color.getAlpha() / 255.0);

		int x = centerX + (int) Math.round(fraction * halfWidth);
		graphics.setColor(Palette.withAlpha(color, alpha));
		graphics.fillOval(x - MARKER_DOT_SIZE / 2, railY - MARKER_DOT_SIZE / 2, MARKER_DOT_SIZE, MARKER_DOT_SIZE);
	}
}
