package com.wayfarer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.VarClientID;
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
	// Two lanes, so text and markers never overlap: a label band on top
	// (ticks and N/NE/E...) and a marker lane beneath it.
	private static final int STRIP_HEIGHT = 30;
	private static final int LABEL_BAND_MID = 9;
	private static final int CARET_GAP = 2;
	private static final int CARET_LENGTH = 6;
	private static final int MIN_WIDTH = 240;
	private static final int MAX_WIDTH = 480;
	/** Gap above the centred strip; RuneLite's snap corners inset by the same 5px. */
	private static final int CENTRED_TOP_MARGIN = 5;
	/** Range follows zoom: fraction of the full range left when zoomed all the way in. */
	private static final double ZOOM_MIN_RANGE_FRACTION = 0.35;

	// 65% opaque: the lowest opacity at which PAPER labels clear 4.5:1
	// over bright stone, sand, fog and dark ground alike (contrast-sweep,
	// 2026-09-25: 4.57:1 worst case at 65%; the old 31% fell to ~1.6:1).
	private static final Color BACKGROUND = Palette.withAlpha(Palette.WARM_BLACK, 166);
	private static final Color CARET = Palette.withAlpha(Palette.AMBER, 230);
	private static final String[] DIRECTION_LABELS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

	private static final int LOCAL_TILE_SIZE = 128;
	private static final int MARKER_DOT_SIZE = 4;
	/** Distance as size: dot diameter beside you, and at the range cap. */
	private static final int MARKER_NEAR_SIZE = 5;
	private static final int MARKER_FAR_SIZE = 2;
	/** The marker rail: bottom of the strip, and where distance-as-height puts near things. */
	private static final int MARKER_RAIL_Y = STRIP_HEIGHT - 5;
	/** Top of the marker band when distance is shown as height. */
	private static final int MARKER_FAR_Y = STRIP_HEIGHT - 11;
	/** Markers closer than this many tiles fade toward NEAR_FADE_FLOOR. */
	private static final double NEAR_FADE_TILES = 4.0;
	private static final double NEAR_FADE_FLOOR = 0.2;
	/** Marker easing (see CompassMath.smoothBearing): time constant, and top speed of half a strip width a second. */
	private static final double MARKER_EASE_SECONDS = 0.35;
	private static final double MARKER_MAX_DEG_PER_SEC = 60.0;
	/** After a stall (alt-tab, loading), ease as if one short frame passed rather than jumping. */
	private static final double MAX_FRAME_SECONDS = 0.1;
	/**
	 * A true bearing that jumps further than this in one frame (someone
	 * running straight through you flips ~180 degrees) is not eased the
	 * long way round the strip: the marker fades back in at its new place.
	 */
	private static final double MARKER_SNAP_DEG = 90.0;
	/** New and snapped markers fade in over this long instead of popping. */
	private static final double MARKER_FADE_IN_SECONDS = 0.3;

	private final Client client;
	private final WayfarerConfig config;
	private final GroundItemTiles groundItems;

	/** Reused per frame so attackable NPCs can be drawn last without a second NPC pass. */
	private final List<NPC> threats = new ArrayList<>();

	/**
	 * Each marker's shown bearing, keyed by what it marks (the NPC, player
	 * or item tile). Entries not drawn this frame are dropped, so a marker
	 * that leaves and returns starts at its true bearing rather than
	 * swooping in from where it was last seen.
	 */
	private final Map<Object, ShownBearing> shownBearings = new HashMap<>();
	private long frameNumber;
	private long lastFrameNanos;
	private double frameSeconds;
	private double clockSeconds;

	private static final class ShownBearing
	{
		double bearing;
		long frame;
		double born;
	}

	@Inject
	private WayfarerOverlay(Client client, WayfarerConfig config, GroundItemTiles groundItems)
	{
		this.client = client;
		this.config = config;
		this.groundItems = groundItems;
		setPosition(OverlayPosition.TOP_CENTER);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	/** Snap/drag as a normal overlay, or pinned to the true top centre of the game view. */
	void setCentred(boolean centred)
	{
		setPosition(centred ? OverlayPosition.DYNAMIC : OverlayPosition.TOP_CENTER);
		setMovable(!centred);
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

		// Centred: for a DYNAMIC overlay RuneLite translates the graphics to
		// wherever the overlay was drawn LAST frame (OverlayRenderer: location
		// = bounds.x/y, then safeRender translates by it). Drawing at an
		// absolute point on top of that counts the offset twice, which is
		// what put an earlier self-positioned version far right of centre.
		// So correct by the difference, then record the true origin so the
		// next frame starts from it.
		int x = client.getViewportXOffset() + client.getViewportWidth() / 2 - stripWidth / 2;
		int y = client.getViewportYOffset() + CENTRED_TOP_MARGIN;
		Rectangle bounds = getBounds();
		int dx = x - bounds.x;
		int dy = y - bounds.y;
		graphics.translate(dx, dy);
		drawStrip(graphics, stripWidth);
		graphics.translate(-dx, -dy);
		bounds.setLocation(x, y);
		return new Dimension(stripWidth, height);
	}

	/** Edge-faded alpha (0..1) for a strip feature at this bearing; 0 when off the strip. */
	private static double stripAlpha(double bearing, double heading)
	{
		double fraction = CompassMath.screenOffsetFraction(CompassMath.signedDeltaDegrees(bearing, heading), HALF_SPAN_DEG);
		return Math.abs(fraction) > 1.0 ? 0 : CompassMath.edgeAlpha(fraction, FADE_ZONE);
	}

	private static int stripX(double bearing, double heading, int centerX, int halfWidth)
	{
		double fraction = CompassMath.screenOffsetFraction(CompassMath.signedDeltaDegrees(bearing, heading), HALF_SPAN_DEG);
		return centerX + (int) Math.round(fraction * halfWidth);
	}

	private void drawStrip(Graphics2D graphics, int stripWidth)
	{
		int halfWidth = stripWidth / 2;
		int centerX = halfWidth;

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		int arc = config.shape() == StripShape.PILL ? STRIP_HEIGHT : 0;
		graphics.setColor(BACKGROUND);
		graphics.fillRoundRect(0, 0, stripWidth, STRIP_HEIGHT, arc, arc);

		double heading = CompassMath.bearingDegrees(client.getCameraYaw());

		Font cardinalFont = FontManager.getRunescapeFont();
		Font intercardinalFont = FontManager.getRunescapeSmallFont();

		// Draw order, bottom to top: minor ticks, markers, then the eight
		// direction labels. Labels go last so a marker passing through the
		// letter band slides behind the heading instead of covering it.
		for (int bearing = 0; bearing < 360; bearing += MINOR_TICK_STEP_DEG)
		{
			if (bearing % 45 == 0)
			{
				continue; // labelled bearings carry a letter instead of a tick
			}
			double alpha = stripAlpha(bearing, heading);
			if (alpha <= 0)
			{
				continue;
			}
			int x = stripX(bearing, heading, centerX, halfWidth);
			graphics.setColor(Palette.withAlpha(Palette.PAPER, (int) (80 * alpha)));
			graphics.drawLine(x, LABEL_BAND_MID - 3, x, LABEL_BAND_MID + 3);
		}

		if (config.showPlayers() || config.showMonsters() || config.showNpcs() || config.showItems())
		{
			renderMarkers(graphics, heading, centerX, halfWidth);
		}

		for (int i = 0; i < DIRECTION_LABELS.length; i++)
		{
			int bearing = i * 45;
			double alpha = stripAlpha(bearing, heading);
			if (alpha <= 0)
			{
				continue;
			}
			String label = DIRECTION_LABELS[i];
			// Cardinals in the regular font, intercardinals in the small one.
			// Both at full strength: dimming the intercardinals would drop
			// them back under 4.5:1, so size carries the hierarchy instead.
			graphics.setFont(i % 2 == 0 ? cardinalFont : intercardinalFont);
			FontMetrics fm = graphics.getFontMetrics();
			int strength = 255;
			int x = stripX(bearing, heading, centerX, halfWidth) - fm.stringWidth(label) / 2;
			int y = LABEL_BAND_MID + fm.getAscent() / 2 - 1;
			graphics.setColor(Palette.withAlpha(Palette.WARM_BLACK, (int) (strength * alpha)));
			graphics.drawString(label, x + 1, y + 1);
			graphics.setColor(Palette.withAlpha(Palette.PAPER, (int) (strength * alpha)));
			graphics.drawString(label, x, y);
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
		double rangeTiles = config.rangeFollowsZoom()
			? CompassMath.zoomedRange(config.nearbyRange(), zoomIn(), ZOOM_MIN_RANGE_FRACTION)
			: config.nearbyRange();
		Frame frame = new Frame(heading, me, (int) Math.round(rangeTiles * LOCAL_TILE_SIZE), centerX, halfWidth,
			MARKER_RAIL_Y, config.distanceAsHeight() ? MARKER_FAR_Y : MARKER_RAIL_Y,
			bySize ? MARKER_NEAR_SIZE : MARKER_DOT_SIZE, bySize ? MARKER_FAR_SIZE : MARKER_DOT_SIZE);

		long now = System.nanoTime();
		frameSeconds = lastFrameNanos == 0 ? 0 : Math.min(MAX_FRAME_SECONDS, (now - lastFrameNanos) / 1e9);
		lastFrameNanos = now;
		frameNumber++;
		clockSeconds += frameSeconds;

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
					drawMarker(graphics, frame, tile, tile.getLocalLocation(), itemColor);
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
					threats.add(npc);
				}
			}
			else if (showNpcs)
			{
				drawMarker(graphics, frame, npc, npc.getLocalLocation(), npcColor);
			}
		}
		if (config.showPlayers())
		{
			Color playerColor = config.playerColor();
			for (Player player : world.players())
			{
				if (player != null && player != local)
				{
					drawMarker(graphics, frame, player, player.getLocalLocation(), playerColor);
				}
			}
		}
		Color monsterColor = config.monsterColor();
		for (NPC threat : threats)
		{
			drawMarker(graphics, frame, threat, threat.getLocalLocation(), monsterColor);
		}
		threats.clear();
		shownBearings.values().removeIf(s -> s.frame != frameNumber);
	}

	/**
	 * How far the camera is zoomed in: 0 all the way out, 1 all the way in.
	 * Reads the same client values RuneLite's Camera plugin adjusts, and
	 * the live limits rather than fixed numbers, so an extended zoom range
	 * still maps to 0..1. A higher zoom value is further in (the Camera
	 * plugin widens the outer limit by lowering MIN).
	 * ponytail: BIG is taken to be the resizable-mode value and SMALL the
	 * fixed-mode one from their names; if zoom does nothing in one mode,
	 * swap them.
	 */
	private double zoomIn()
	{
		boolean resized = client.isResized();
		int zoom = client.getVarcIntValue(resized ? VarClientID.CAMERA_ZOOM_BIG : VarClientID.CAMERA_ZOOM_SMALL);
		int min = client.getVarcIntValue(resized ? VarClientID.CAMERA_ZOOM_BIG_MIN : VarClientID.CAMERA_ZOOM_SMALL_MIN);
		int max = client.getVarcIntValue(resized ? VarClientID.CAMERA_ZOOM_BIG_MAX : VarClientID.CAMERA_ZOOM_SMALL_MAX);
		if (max <= min)
		{
			return 0;
		}
		return (zoom - min) / (double) (max - min);
	}

	/**
	 * This marker's shown bearing: eased toward the true bearing, or
	 * snapped (and faded back in) when the true bearing flips. A marker new
	 * this frame starts at its true bearing and fades in.
	 */
	private ShownBearing track(Object key, double trueBearing)
	{
		ShownBearing s = shownBearings.get(key);
		if (s == null)
		{
			s = new ShownBearing();
			s.bearing = trueBearing;
			s.born = clockSeconds;
			shownBearings.put(key, s);
		}
		else if (CompassMath.isFlip(s.bearing, trueBearing, MARKER_SNAP_DEG))
		{
			s.bearing = trueBearing;
			s.born = clockSeconds;
		}
		else
		{
			s.bearing = CompassMath.smoothBearing(s.bearing, trueBearing, frameSeconds, MARKER_EASE_SECONDS, MARKER_MAX_DEG_PER_SEC);
		}
		s.frame = frameNumber;
		return s;
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

	private void drawMarker(Graphics2D graphics, Frame frame, Object key, LocalPoint them, Color color)
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

		ShownBearing shown = track(key, CompassMath.bearingToTarget(dxEast, dyNorth));
		double fraction = CompassMath.screenOffsetFraction(
			CompassMath.signedDeltaDegrees(shown.bearing, frame.heading), HALF_SPAN_DEG);
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
		double appear = CompassMath.appearFade(clockSeconds - shown.born, MARKER_FADE_IN_SECONDS);
		int alpha = (int) (edge * near * appear * (230 - 110 * distFraction) * color.getAlpha() / 255.0);

		int x = frame.centerX + (int) Math.round(fraction * frame.halfWidth);
		int y = CompassMath.byDistance(distFraction, frame.nearY, frame.farY);
		int size = CompassMath.byDistance(distFraction, frame.nearSize, frame.farSize);
		graphics.setColor(Palette.withAlpha(color, alpha));
		graphics.fillOval(x - size / 2, y - size / 2, size, size);
	}
}
