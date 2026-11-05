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
package com.l2journey.gameserver.managers;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.MapRegion;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.zone.type.RespawnZone;

/**
 * Null-safe map region lookups in {@link MapRegionManager}.
 */
class MapRegionManagerNullGuardTest
{
	private static final String DEFAULT_RESPAWN = "talking_island_town";
	
	@Test
	void getMapRegionReturnsNullWhenObjectMissing()
	{
		final MapRegionManager manager = mock(MapRegionManager.class, CALLS_REAL_METHODS);
		assertNull(manager.getMapRegion((WorldObject) null));
	}
	
	@Test
	void getMapRegionDelegatesCoordinatesWhenObjectPresent()
	{
		final MapRegionManager manager = mock(MapRegionManager.class, CALLS_REAL_METHODS);
		final MapRegion region = mock(MapRegion.class);
		doReturn(region).when(manager).getMapRegion(anyInt(), anyInt());
		
		final WorldObject object = mock(WorldObject.class);
		when(object.getX()).thenReturn(100);
		when(object.getY()).thenReturn(200);
		
		assertSame(region, manager.getMapRegion(object));
		verify(object).getX();
		verify(object).getY();
		verify(manager).getMapRegion(100, 200);
	}
	
	@Test
	void getNearestTownRespawnFallsBackWhenRegionMissing() throws Exception
	{
		final MapRegionManager manager = mock(MapRegionManager.class, CALLS_REAL_METHODS);
		final Creature creature = mock(Creature.class);
		doReturn(null).when(manager).getMapRegion(creature);
		
		final Location fallback = mock(Location.class);
		final MapRegion defaultRegion = mock(MapRegion.class);
		when(defaultRegion.getSpawnLoc()).thenReturn(fallback);
		final Map<String, MapRegion> regions = regions();
		regions.put(DEFAULT_RESPAWN, defaultRegion);
		try (MockedStatic<ZoneManager> zones = mockStatic(ZoneManager.class))
		{
			final ZoneManager zoneManager = mock(ZoneManager.class);
			zones.when(ZoneManager::getInstance).thenReturn(zoneManager);
			when(zoneManager.getZone(eq(creature), eq(RespawnZone.class))).thenReturn(null);
			assertSame(fallback, manager.getNearestTownRespawn(creature));
		}
		finally
		{
			regions.remove(DEFAULT_RESPAWN);
		}
	}
	
	@Test
	void getNearestTownRespawnUsesRegionWhenPresent() throws Exception
	{
		final MapRegionManager manager = mock(MapRegionManager.class, CALLS_REAL_METHODS);
		final Creature creature = mock(Creature.class);
		final MapRegion region = mock(MapRegion.class);
		final Location spawn = mock(Location.class);
		when(region.getBannedRace()).thenReturn(Collections.emptyMap());
		when(region.getSpawnLoc()).thenReturn(spawn);
		doReturn(region).when(manager).getMapRegion(creature);
		
		try (MockedStatic<ZoneManager> zones = mockStatic(ZoneManager.class))
		{
			final ZoneManager zoneManager = mock(ZoneManager.class);
			zones.when(ZoneManager::getInstance).thenReturn(zoneManager);
			when(zoneManager.getZone(eq(creature), eq(RespawnZone.class))).thenReturn(null);
			assertSame(spawn, manager.getNearestTownRespawn(creature));
		}
	}
	
	@Test
	void getNearestKarmaRespawnFallsBackWhenRegionMissing() throws Exception
	{
		final MapRegionManager manager = mock(MapRegionManager.class, CALLS_REAL_METHODS);
		final Player player = mock(Player.class);
		when(player.isFlyingMounted()).thenReturn(false);
		doReturn(null).when(manager).getMapRegion(player);
		
		final Location fallback = mock(Location.class);
		final MapRegion defaultRegion = mock(MapRegion.class);
		when(defaultRegion.getChaoticSpawnLoc()).thenReturn(fallback);
		final Map<String, MapRegion> regions = regions();
		regions.put(DEFAULT_RESPAWN, defaultRegion);
		try (MockedStatic<ZoneManager> zones = mockStatic(ZoneManager.class))
		{
			final ZoneManager zoneManager = mock(ZoneManager.class);
			zones.when(ZoneManager::getInstance).thenReturn(zoneManager);
			when(zoneManager.getZone(eq(player), eq(RespawnZone.class))).thenReturn(null);
			assertSame(fallback, manager.getNearestKarmaRespawn(player));
		}
		finally
		{
			regions.remove(DEFAULT_RESPAWN);
		}
	}
	
	@Test
	void getNearestKarmaRespawnFallsBackWhenPlayerMissing() throws Exception
	{
		final MapRegionManager manager = mock(MapRegionManager.class, CALLS_REAL_METHODS);
		final Location fallback = mock(Location.class);
		final MapRegion defaultRegion = mock(MapRegion.class);
		when(defaultRegion.getChaoticSpawnLoc()).thenReturn(fallback);
		final Map<String, MapRegion> regions = regions();
		regions.put(DEFAULT_RESPAWN, defaultRegion);
		try
		{
			assertSame(fallback, manager.getNearestKarmaRespawn(null));
		}
		finally
		{
			regions.remove(DEFAULT_RESPAWN);
		}
	}
	
	@Test
	void getNearestKarmaRespawnUsesFlyingFallbackWhenRegionMissing() throws Exception
	{
		final MapRegionManager manager = mock(MapRegionManager.class, CALLS_REAL_METHODS);
		final Player player = mock(Player.class);
		when(player.isFlyingMounted()).thenReturn(true);
		doReturn(null).when(manager).getMapRegion(player);
		
		final Location flying = mock(Location.class);
		final MapRegion flyingRegion = mock(MapRegion.class);
		when(flyingRegion.getChaoticSpawnLoc()).thenReturn(flying);
		final Map<String, MapRegion> regions = regions();
		regions.put("union_base_of_kserth", flyingRegion);
		try (MockedStatic<ZoneManager> zones = mockStatic(ZoneManager.class))
		{
			final ZoneManager zoneManager = mock(ZoneManager.class);
			zones.when(ZoneManager::getInstance).thenReturn(zoneManager);
			when(zoneManager.getZone(eq(player), eq(RespawnZone.class))).thenReturn(null);
			assertSame(flying, manager.getNearestKarmaRespawn(player));
		}
		finally
		{
			regions.remove("union_base_of_kserth");
		}
	}
	
	@Test
	void getNearestKarmaRespawnUsesRegionWhenPresent() throws Exception
	{
		final MapRegionManager manager = mock(MapRegionManager.class, CALLS_REAL_METHODS);
		final Player player = mock(Player.class);
		final MapRegion region = mock(MapRegion.class);
		final Location chaotic = mock(Location.class);
		when(region.getBannedRace()).thenReturn(Collections.emptyMap());
		when(region.getChaoticSpawnLoc()).thenReturn(chaotic);
		doReturn(region).when(manager).getMapRegion(player);
		
		try (MockedStatic<ZoneManager> zones = mockStatic(ZoneManager.class))
		{
			final ZoneManager zoneManager = mock(ZoneManager.class);
			zones.when(ZoneManager::getInstance).thenReturn(zoneManager);
			when(zoneManager.getZone(eq(player), eq(RespawnZone.class))).thenReturn(null);
			assertSame(chaotic, manager.getNearestKarmaRespawn(player));
		}
	}
	
	@SuppressWarnings("unchecked")
	private static Map<String, MapRegion> regions() throws Exception
	{
		final Field field = MapRegionManager.class.getDeclaredField("REGIONS");
		field.setAccessible(true);
		return (Map<String, MapRegion>) field.get(null);
	}
}
