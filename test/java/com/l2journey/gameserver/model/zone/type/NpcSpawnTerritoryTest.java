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
 * 
 * ---
 * 
 * Portions of this software are derived from the L2JMobius Project, 
 * shared under the MIT License. The original license terms are preserved where 
 * applicable..
 * 
 */
package com.l2journey.gameserver.model.zone.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.zone.ZoneForm;

/**
 * Unit tests for {@link NpcSpawnTerritory} geo out-of-range handling.
 */
class NpcSpawnTerritoryTest
{
	/**
	 * Stub form that stays outside world/geo bounds and never touches {@code GeoData}.
	 * Real {@code ZoneCuboid}/{@code ZoneNPoly} call GeoData in {@code getRandomPoint()}, which would
	 * pull in the geo singleton and is unsuitable for this unit test.
	 */
	private static final class OutOfBoundsZoneForm extends ZoneForm
	{
		private final Location _point;
		
		OutOfBoundsZoneForm(Location point)
		{
			_point = point;
		}
		
		@Override
		public boolean isInsideZone(int x, int y, int z)
		{
			return false;
		}
		
		@Override
		public boolean intersectsRectangle(int x1, int x2, int y1, int y2)
		{
			return false;
		}
		
		@Override
		public double getDistanceToZone(int x, int y)
		{
			return Double.MAX_VALUE;
		}
		
		@Override
		public int getLowZ()
		{
			return _point.getZ() - 100;
		}
		
		@Override
		public int getHighZ()
		{
			return _point.getZ() + 100;
		}
		
		@Override
		public void visualizeZone(int z)
		{
			// no-op for tests
		}
		
		@Override
		public Location getRandomPoint()
		{
			return _point;
		}
		
		@Override
		public Location getCenterPoint()
		{
			return _point;
		}
	}
	
	@Test
	void getRandomPointOutOfWorldBoundsDoesNotHangOrThrow()
	{
		final Location outOfBounds = new Location(World.WORLD_X_MAX + 50_000, World.WORLD_Y_MAX + 50_000, 0);
		final NpcSpawnTerritory territory = new NpcSpawnTerritory("oob-test", new OutOfBoundsZoneForm(outOfBounds));
		
		final Location loc = assertTimeoutPreemptively(Duration.ofSeconds(2), territory::getRandomPoint);
		
		assertNotNull(loc);
		assertEquals(outOfBounds.getX(), loc.getX());
		assertEquals(outOfBounds.getY(), loc.getY());
		assertEquals(outOfBounds.getZ(), loc.getZ());
	}
}
