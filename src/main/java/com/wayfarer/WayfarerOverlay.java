package com.wayfarer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
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

	private static final Color BACKGROUND = Palette.withAlpha(Palette.WARM_BLACK, 80);
	private static final Color CARET = Palette.withAlpha(Palette.AMBER, 230);
	private static final String[] CARDINAL_LETTERS = {"N", "E", "S", "W"};

	private static final int LOCAL_TILE_SIZE = 128;
	private static final int MARKER_DOT_SIZE = 4;
	/** Distance as size: dot diameter beside you, and at the range cap. */
	private static final int MARKER_NEAR_SIZE = 6;
	private static final int MARKER_FAR_SIZE = 2;
	/** The marker rail: bottom of the strip, and where distance-as-height puts near things. */
	private static final int MARKER_RAIL_Y = STRIP_HEIGHT - 5;
	/** Top of the marker band when distance is shown as height. */
	private static final int MARKER_FAR_Y = 5;
	/** Markers closer than this many tiles fade toward NEAR_FADE_FLOOR. */
	private static final double NEAR_FADE_TILES = 4.0;
	private static final double NEAR_FADE_FLOOR = 0.2;

	private final Client client;
	private final WayfarerConfig config;
	private final GroundItemTiles groundItems;

	/** Reused per frame so attackable NPCs can be drawn last without a second NPC pass. */
	private final List<LocalPoint> threats = new ArrayList<>();

	@Inject
	private WayfarerOverlay(Client client, WayfarerConfig config, GroundItemTiles groundItems)
	{
		this.client = client;
		this.config = config;
		this.groundItems = groundItems;
		setPosition(OverlayPosition.TOP_CENTER);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		int stripWidth = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, (int) (client.getViewportWidth() * 0.40)));
		drawStrip(graphics, stripWidth);
		return new Dimension(stripWidth, STRIP_HEIGHT + CARET_GAP + CARET_LENGTH + 1);
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

		if (config.showPlayers() || config.showMonsters() || config.showNpcs() || config.showItems())
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
	 * Everything a marker needs that is the same for every marker in a
	 * frame. With distance-as-height or -as-size off, near and far are the
	 * same value, so the distance mapping is a no-op rather than a branch.
	 */
	private static final class Frame
	{
		final double heading;
		final LocalPoint me;
		final int rangeLocal;
		final int centerX;
		final int halfWidth;
		final int nearY;
		final int farY;
		final int nearSize;
		final int farSize;

		Frame(double heading, LocalPoint me, int rangeLocal, int centerX, int halfWidth,
			int nearY, int farY, int nearSize, int farSize)
		{
			this.heading = heading;
			this.me = me;
			this.rangeLocal = rangeLocal;
			this.centerX = centerX;
			this.halfWidth = halfWidth;
			this.nearY = nearY;
			this.farY = farY;
			this.nearSize = nearSize;
			this.farSize = farSize;
		}
	}

	/**
	 * Draw order is deliberate, least urgent first: ground items (red),
	 * non-attackable NPCs (yellow), other players (white), attackable NPCs
	 * (orange) last — a threat marker is never buried under a crowd.
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
		boolean bySize = config.distanceAsSize();
		Frame frame = new Frame(heading, me, config.nearbyRange() * LOCAL_TILE_SIZE, centerX, halfWidth,
			MARKER_RAIL_Y, config.distanceAsHeight() ? MARKER_FAR_Y : MARKER_RAIL_Y,
			bySize ? MARKER_NEAR_SIZE : MARKER_DOT_SIZE, bySize ? MARKER_FAR_SIZE : MARKER_DOT_SIZE);

		// ponytail: top-level world view only, so players, NPCs and items
		// aboard other boats are not marked; iterate client.getWorldViews()
		// and transform each through its WorldEntity if that gets asked for.
		WorldView world = client.getTopLevelWorldView();

		if (config.showItems())
		{
			Color itemColor = config.itemColor();
			int plane = world.getPlane();
			for (Tile tile : groundItems.tiles())
			{
				// The minimap only shows items on your own floor.
				if (tile.getPlane() == plane)
				{
					drawMarker(graphics, frame, tile.getLocalLocation(), itemColor);
				}
			}
		}

		boolean showMonsters = config.showMonsters();
		boolean showNpcs = config.showNpcs();
		Color npcColor = config.npcColor();
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
				drawMarker(graphics, frame, npc.getLocalLocation(), npcColor);
			}
		}
		if (config.showPlayers())
		{
			Color playerColor = config.playerColor();
			for (Player player : world.players())
			{
				if (player != null && player != local)
				{
					drawMarker(graphics, frame, player.getLocalLocation(), playerColor);
				}
			}
		}
		Color monsterColor = config.monsterColor();
		for (LocalPoint threat : threats)
		{
			drawMarker(graphics, frame, threat, monsterColor);
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

	private void drawMarker(Graphics2D graphics, Frame frame, LocalPoint them, Color color)
	{
		if (them == null)
		{
			return;
		}
		int dxEast = them.getX() - frame.me.getX();
		int dyNorth = them.getY() - frame.me.getY();
		if (dxEast == 0 && dyNorth == 0)
		{
			return;
		}
		long distSq = (long) dxEast * dxEast + (long) dyNorth * dyNorth;
		if (distSq > (long) frame.rangeLocal * frame.rangeLocal)
		{
			return;
		}

		double fraction = CompassMath.screenOffsetFraction(
			CompassMath.signedDeltaDegrees(CompassMath.bearingToTarget(dxEast, dyNorth), frame.heading), HALF_SPAN_DEG);
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
		double distFraction = dist / frame.rangeLocal;
		double near = CompassMath.nearFade(dist / LOCAL_TILE_SIZE, NEAR_FADE_TILES, NEAR_FADE_FLOOR);
		int alpha = (int) (edge * near * (230 - 110 * distFraction) * color.getAlpha() / 255.0);

		int x = frame.centerX + (int) Math.round(fraction * frame.halfWidth);
		int y = CompassMath.byDistance(distFraction, frame.nearY, frame.farY);
		int size = CompassMath.byDistance(distFraction, frame.nearSize, frame.farSize);
		graphics.setColor(Palette.withAlpha(color, alpha));
		graphics.fillOval(x - size / 2, y - size / 2, size, size);
	}
}
