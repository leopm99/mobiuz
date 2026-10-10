/*
 * Copyright (c) 2025 L2Journey Project
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.l2journey.gameserver.model.actor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Ferry routes are private arrays in the vehicle scripts, so these tests read that source.
 * <p>
 * The client steers along its heading and turns toward each {@code VehicleDeparture} at {@code rotationSpeed}. The turn radius is {@code speed * 65536 / (rotationSpeed * 2π)}. A heading change whose arc is longer than the leg makes the ship orbit the point. Heading 0 is east.
 */
class BoatRouteGeometryTest
{
	private static final double HEADING_UNITS = 65536.0;
	private static final int TALKING_DOCK_Y = 261660;
	private static final int GLUDIN_DOCK_X = -95686;
	private static final int GLUDIN_CHANNEL_Y = 147718;
	private static final int GLUDIN_DOCK_Y = 150514;
	
	private static final Pattern ARRAY = Pattern.compile("VehiclePathPoint\\[\\]\\s+([A-Z0-9_]+)\\s*=\\s*\\{([^}]*)\\}", Pattern.DOTALL);
	private static final Pattern POINT = Pattern.compile("new\\s+VehiclePathPoint\\s*\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*,\\s*-?\\d+\\s*,\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)");
	
	@Test
	void everyFerryLegCanFinishItsTurn() throws IOException
	{
		final Map<String, List<Waypoint>> arrays = loadRoutes();
		for (Route route : Route.values())
		{
			final List<Waypoint> path = route.join(arrays);
			assertTrue(path.size() >= 2, route.name());
			for (int i = 2; i < path.size(); i++)
			{
				final Waypoint from = path.get(i - 1);
				final Waypoint to = path.get(i);
				final double distance = distance(from, to);
				final double turn = Math.abs(wrap(heading(from, to) - heading(path.get(i - 2), from)));
				assertTrue(distance > 1.0, route.name() + " has an empty hop into " + to);
				assertTrue(turnFits(distance, turn, to.moveSpeed, to.rotationSpeed), route.name() + " cannot finish a " + Math.toDegrees(turn) + "° turn over " + (int) distance + " units into " + to + " at speed " + to.moveSpeed + " rotation " + to.rotationSpeed);
			}
		}
	}
	
	@Test
	void talkingIslandBerthStaysOnOneLine() throws IOException
	{
		final Map<String, List<Waypoint>> arrays = loadRoutes();
		assertDockSharesAxis(Route.GLUDIN_TO_TALKING.join(arrays), false, TALKING_DOCK_Y);
		assertDockSharesAxis(Route.GIRAN_TO_TALKING.join(arrays), false, TALKING_DOCK_Y);
	}
	
	@Test
	void gludinBerthIsOneStraightRun() throws IOException
	{
		final List<Waypoint> path = Route.TALKING_TO_GLUDIN.join(loadRoutes());
		final Waypoint channel = path.get(path.size() - 2);
		final Waypoint dock = path.get(path.size() - 1);
		assertEquals(GLUDIN_DOCK_X, channel.x);
		assertEquals(GLUDIN_CHANNEL_Y, channel.y);
		assertEquals(GLUDIN_DOCK_X, dock.x);
		assertEquals(GLUDIN_DOCK_Y, dock.y);
		assertEquals(150, dock.moveSpeed);
		assertEquals(800, dock.rotationSpeed);
		assertEquals(1800, channel.rotationSpeed);
		assertFalse(path.stream().anyMatch(point -> (point.y == 148718) || (point.y == 149718)), "short in-harbor points make the client orbit the berth");
	}
	
	@Test
	void oldDockHopsCannotAbsorbAResetHeading()
	{
		// A finished move used to broadcast a zero-length MoveToLocation, which set the client heading to east.
		// The Talking Island dock is due west. The old hop (~2380) at speed 150 / rotation 1800 cannot turn 180°.
		assertFalse(turnFits(2380, Math.PI, 150, 1800));
		// The old Gludin harbor was 1000-unit steps. Facing north from an east heading does not fit in one of them.
		assertFalse(turnFits(1000, Math.PI / 2.0, 180, 800));
		assertFalse(turnFits(796, Math.PI / 2.0, 150, 800));
	}
	
	@Test
	void oneUnitBerthOffsetIsNotTheDockLine()
	{
		final Waypoint kink = new Waypoint(-94242, 261659, 150, 1800);
		final Waypoint dock = new Waypoint(-96622, TALKING_DOCK_Y, 150, 1800);
		assertFalse(sharesAxis(kink, dock));
	}
	
	@Test
	void finishedRouteTellsTheClientToStop() throws IOException
	{
		final String source = Files.readString(repoRoot().resolve("java/com/l2journey/gameserver/ai/BoatAI.java"));
		final int start = source.indexOf("void clientStoppedMoving()");
		assertTrue(start >= 0, "clientStoppedMoving");
		final int end = source.indexOf("public Boat getActor()", start);
		assertTrue(end > start, "getActor");
		final String body = source.substring(start, end);
		// Path end clears the move first, so clientStopMoving(null) sends nothing and the client keeps steering.
		assertTrue(body.contains("stopMove(null)"), body);
	}
	
	private static void assertDockSharesAxis(List<Waypoint> path, boolean sameX, int coordinate)
	{
		final Waypoint approach = path.get(path.size() - 2);
		final Waypoint dock = path.get(path.size() - 1);
		assertTrue(sharesAxis(approach, dock), approach + " to " + dock);
		assertEquals(coordinate, sameX ? dock.x : dock.y);
		assertEquals(coordinate, sameX ? approach.x : approach.y);
	}
	
	/**
	 * @param distance leg length in world units
	 * @param turn absolute heading change in radians
	 * @param moveSpeed units traveled per second
	 * @param rotationSpeed client heading units per second
	 * @return {@code true} when the arc of that turn is no longer than the leg
	 */
	static boolean turnFits(double distance, double turn, int moveSpeed, int rotationSpeed)
	{
		if ((moveSpeed <= 0) || (rotationSpeed <= 0))
		{
			return distance > 1.0;
		}
		final double radius = (moveSpeed * HEADING_UNITS) / (rotationSpeed * (2.0 * Math.PI));
		return (Math.abs(turn) * radius) <= (distance + 1.0);
	}
	
	private static boolean sharesAxis(Waypoint from, Waypoint to)
	{
		return (from.x == to.x) || (from.y == to.y);
	}
	
	private static double heading(Waypoint from, Waypoint to)
	{
		return Math.atan2(to.y - from.y, to.x - from.x);
	}
	
	private static double wrap(double radians)
	{
		double wrapped = radians % (2.0 * Math.PI);
		if (wrapped > Math.PI)
		{
			wrapped -= 2.0 * Math.PI;
		}
		else if (wrapped < -Math.PI)
		{
			wrapped += 2.0 * Math.PI;
		}
		return wrapped;
	}
	
	private static double distance(Waypoint from, Waypoint to)
	{
		final double dx = to.x - from.x;
		final double dy = to.y - from.y;
		return Math.hypot(dx, dy);
	}
	
	private static Map<String, List<Waypoint>> loadRoutes() throws IOException
	{
		final Map<String, List<Waypoint>> routes = new java.util.HashMap<>();
		final Path dir = repoRoot().resolve("dist/game/data/scripts/vehicles");
		try (var files = Files.list(dir))
		{
			for (Path file : files.filter(path -> path.getFileName().toString().startsWith("Boat") && path.getFileName().toString().endsWith(".java")).toList())
			{
				final String source = Files.readString(file);
				final Matcher arrays = ARRAY.matcher(source);
				while (arrays.find())
				{
					final List<Waypoint> points = new ArrayList<>();
					final Matcher pointsMatcher = POINT.matcher(arrays.group(2));
					while (pointsMatcher.find())
					{
						points.add(new Waypoint(Integer.parseInt(pointsMatcher.group(1)), Integer.parseInt(pointsMatcher.group(2)), Integer.parseInt(pointsMatcher.group(3)), Integer.parseInt(pointsMatcher.group(4))));
					}
					assertFalse(points.isEmpty(), file.getFileName() + " " + arrays.group(1));
					final List<Waypoint> previous = routes.put(file.getFileName() + ":" + arrays.group(1), points);
					assertNull(previous, arrays.group(1));
				}
			}
		}
		return routes;
	}
	
	private static Path repoRoot()
	{
		Path dir = Path.of("").toAbsolutePath();
		for (int i = 0; i < 4; i++)
		{
			if (Files.isDirectory(dir.resolve("dist/game/data/scripts/vehicles")))
			{
				return dir;
			}
			final Path parent = dir.getParent();
			if (parent == null)
			{
				break;
			}
			dir = parent;
		}
		throw new IllegalStateException("Vehicle scripts not found from " + Path.of("").toAbsolutePath());
	}
	
	private enum Route
	{
		TALKING_TO_GLUDIN("BoatTalkingGludin.java:TALKING_TO_GLUDIN", "BoatTalkingGludin.java:GLUDIN_DOCK"),
		GLUDIN_TO_TALKING("BoatTalkingGludin.java:GLUDIN_TO_TALKING", "BoatTalkingGludin.java:TALKING_DOCK"),
		GIRAN_TO_TALKING("BoatGiranTalking.java:GIRAN_TO_TALKING", "BoatGiranTalking.java:TALKING_DOCK"),
		TALKING_TO_GIRAN("BoatGiranTalking.java:TALKING_TO_GIRAN"),
		GLUDIN_TO_RUNE("BoatGludinRune.java:GLUDIN_TO_RUNE", "BoatGludinRune.java:RUNE_DOCK"),
		RUNE_TO_GLUDIN("BoatGludinRune.java:RUNE_TO_GLUDIN", "BoatGludinRune.java:GLUDIN_DOCK"),
		RUNE_TO_PRIMEVAL("BoatRunePrimeval.java:RUNE_TO_PRIMEVAL"),
		PRIMEVAL_TO_RUNE("BoatRunePrimeval.java:PRIMEVAL_TO_RUNE", "BoatRunePrimeval.java:RUNE_DOCK"),
		INNADRIL_TOUR("BoatInnadrilTour.java:TOUR");
		
		private final String[] keys;
		
		Route(String... keys)
		{
			this.keys = keys;
		}
		
		private List<Waypoint> join(Map<String, List<Waypoint>> arrays)
		{
			final List<Waypoint> path = new ArrayList<>();
			for (String key : keys)
			{
				final List<Waypoint> part = arrays.get(key);
				assertNotNull(part, key);
				path.addAll(part);
			}
			return path;
		}
	}
	
	private record Waypoint(int x, int y, int moveSpeed, int rotationSpeed)
	{
		@Override
		public String toString()
		{
			return "(" + x + ", " + y + ")";
		}
	}
}
