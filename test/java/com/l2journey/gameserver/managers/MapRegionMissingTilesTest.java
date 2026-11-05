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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.l2journey.Config;
import com.l2journey.gameserver.model.MapRegion;

/**
 * Registers the occupied squares fixed for issue #12 and pins their neighbors.
 */
class MapRegionMissingTilesTest
{
	private static File previousDatapackRoot;
	
	@BeforeAll
	static void loadMapRegions()
	{
		previousDatapackRoot = Config.DATAPACK_ROOT;
		Config.DATAPACK_ROOT = new File("dist/game");
		MapRegionManager.getInstance().load();
	}
	
	@AfterAll
	static void restoreConfig()
	{
		Config.DATAPACK_ROOT = previousDatapackRoot;
		if (previousDatapackRoot != null)
		{
			MapRegionManager.getInstance().load();
		}
	}
	
	@Test
	void checkCrate01OwnsSquare1717Alone()
	{
		assertSoleOwner(-87123, -16464, "crate01");
	}
	
	@Test
	void checkBorderOfNightmareOwnsSquare1613Alone()
	{
		assertSoleOwner(-114633, -154243, "border_of_nightmare");
	}
	
	@Test
	void checkHeineOwnsSquare2422Alone()
	{
		assertSoleOwner(146592, 151888, "heiness_town");
	}
	
	@Test
	void checkThemeparkOwnsNeighbor1716()
	{
		assertRegionName(-80000, -40000, "themepark");
	}
	
	@Test
	void checkRiftOwnsNeighbor1612()
	{
		assertRegionName(-114444, -178322, "rift_bitween_worlds");
	}
	
	@Test
	void checkHunterOwnsNeighbor2421()
	{
		assertRegionName(140000, 100000, "hunter_town");
	}
	
	@Test
	void checkHeineOwnsNeighbor2423()
	{
		assertRegionName(134928, 172432, "heiness_town");
	}
	
	private static void assertSoleOwner(int locX, int locY, String expectedName)
	{
		assertRegionName(locX, locY, expectedName);
		assertEquals(1, countOwners(locX, locY), "square must belong to exactly one region");
	}
	
	private static void assertRegionName(int locX, int locY, String expectedName)
	{
		final MapRegion region = MapRegionManager.getInstance().getMapRegion(locX, locY);
		assertNotNull(region, "expected a map region at " + locX + "," + locY);
		assertEquals(expectedName, region.getName());
	}
	
	@SuppressWarnings("unchecked")
	private static int countOwners(int locX, int locY)
	{
		try
		{
			final MapRegionManager manager = MapRegionManager.getInstance();
			final int mapX = manager.getMapRegionX(locX);
			final int mapY = manager.getMapRegionY(locY);
			final Field regionsField = MapRegionManager.class.getDeclaredField("REGIONS");
			regionsField.setAccessible(true);
			final Map<String, MapRegion> regions = (Map<String, MapRegion>) regionsField.get(null);
			int owners = 0;
			for (MapRegion region : regions.values())
			{
				if (region.isZoneInRegion(mapX, mapY))
				{
					owners++;
				}
			}
			return owners;
		}
		catch (ReflectiveOperationException e)
		{
			throw new IllegalStateException("Unable to inspect map regions", e);
		}
	}
}
