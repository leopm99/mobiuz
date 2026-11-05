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
package com.l2journey.gameserver.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.World;

/**
 * Unit tests for {@link GeoUtils#formatGeoMap(int, int)}.
 */
class GeoUtilsTest
{
	private static final String MIN_TILE_MESSAGE = "GeoMap: 11_10 (-294912,-262144 to -262145,-229377)";
	
	@Test
	void formatGeoMapAtWorldMinimumReturnsMinTile()
	{
		assertEquals(MIN_TILE_MESSAGE, GeoUtils.formatGeoMap(World.WORLD_X_MIN, World.WORLD_Y_MIN));
	}
	
	@Test
	void formatGeoMapInsideTileKeepsSameRegionAndBounds()
	{
		assertEquals(MIN_TILE_MESSAGE, GeoUtils.formatGeoMap(World.WORLD_X_MIN + 100, World.WORLD_Y_MIN + 100));
	}
	
	@Test
	void formatGeoMapOneTileEastUsesInclusiveUpperBound()
	{
		final int minX = ((12 - World.TILE_ZERO_COORD_X) * World.TILE_SIZE);
		final int minY = ((10 - World.TILE_ZERO_COORD_Y) * World.TILE_SIZE);
		final int maxX = (minX + World.TILE_SIZE) - 1;
		final int maxY = (minY + World.TILE_SIZE) - 1;
		final String expected = "GeoMap: 12_10 (" + minX + "," + minY + " to " + maxX + "," + maxY + ")";
		assertEquals(expected, GeoUtils.formatGeoMap(World.WORLD_X_MIN + World.TILE_SIZE, World.WORLD_Y_MIN));
	}
}
