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
package com.l2journey.gameserver.model.groups.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.gameserver.managers.MapRegionManager;
import com.l2journey.gameserver.model.MapRegion;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Player;

/**
 * Covers PartyMatchRoom.getLocation when the leader has or lacks a map region.
 */
class PartyMatchRoomLocationTest
{
	@Test
	void checkLocationReturnsBbsWhenLeaderHasRegion()
	{
		final Player leader = mock(Player.class);
		final MapRegion region = new MapRegion("heiness_town", "Heine Town", 926, 6, 12);
		final MapRegionManager mapRegionManager = mock(MapRegionManager.class);
		when(mapRegionManager.getMapRegion(any(WorldObject.class))).thenReturn(region);
		
		try (MockedStatic<MapRegionManager> mapRegions = mockStatic(MapRegionManager.class))
		{
			mapRegions.when(MapRegionManager::getInstance).thenReturn(mapRegionManager);
			final PartyMatchRoom room = new PartyMatchRoom(1, "test", 0, 1, 80, 9, leader);
			assertEquals(12, room.getLocation());
		}
	}
	
	@Test
	void checkLocationReturnsZeroWhenLeaderHasNoRegion()
	{
		final Player leader = mock(Player.class);
		final MapRegionManager mapRegionManager = mock(MapRegionManager.class);
		when(mapRegionManager.getMapRegion(any(WorldObject.class))).thenReturn(null);
		
		try (MockedStatic<MapRegionManager> mapRegions = mockStatic(MapRegionManager.class))
		{
			mapRegions.when(MapRegionManager::getInstance).thenReturn(mapRegionManager);
			final PartyMatchRoom room = new PartyMatchRoom(1, "test", 0, 1, 80, 9, leader);
			assertEquals(0, room.getLocation());
		}
	}
}
