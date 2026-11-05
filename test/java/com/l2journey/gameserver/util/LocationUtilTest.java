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

import com.l2journey.gameserver.model.Location;

/**
 * Characterization tests for the coordinate helpers in {@link LocationUtil}.
 */
class LocationUtilTest
{
	private static final double DELTA = 1e-9;
	
	@Test
	void calculateAngleFromCardinalDirections()
	{
		assertEquals(0.0, LocationUtil.calculateAngleFrom(0, 0, 1, 0), DELTA);
		assertEquals(90.0, LocationUtil.calculateAngleFrom(0, 0, 0, 1), DELTA);
		assertEquals(180.0, LocationUtil.calculateAngleFrom(0, 0, -1, 0), DELTA);
		assertEquals(270.0, LocationUtil.calculateAngleFrom(0, 0, 0, -1), DELTA);
		
		final Location origin = new Location(0, 0, 0);
		final Location target = new Location(0, 1, 0);
		assertEquals(90.0, LocationUtil.calculateAngleFrom(origin, target), DELTA);
	}
	
	@Test
	void calculateHeadingFromCardinalDirections()
	{
		assertEquals(0, LocationUtil.calculateHeadingFrom(0, 0, 1, 0));
		assertEquals(16383, LocationUtil.calculateHeadingFrom(0, 0, 0, 1));
		assertEquals(32767, LocationUtil.calculateHeadingFrom(0, 0, -1, 0));
		assertEquals(49151, LocationUtil.calculateHeadingFrom(0, 0, 0, -1));
		
		assertEquals(0, LocationUtil.calculateHeadingFrom(1.0, 0.0));
		assertEquals(16383, LocationUtil.calculateHeadingFrom(0.0, 1.0));
		assertEquals(32767, LocationUtil.calculateHeadingFrom(-1.0, 0.0));
		assertEquals(49151, LocationUtil.calculateHeadingFrom(0.0, -1.0));
		
		final Location origin = new Location(10, 20, 0);
		final Location target = new Location(10, 21, 0);
		assertEquals(16383, LocationUtil.calculateHeadingFrom(origin, target));
	}
	
	@Test
	void convertHeadingToDegreeUsesClientScale()
	{
		assertEquals(0.0, LocationUtil.convertHeadingToDegree(0), DELTA);
		assertEquals(90.0, LocationUtil.convertHeadingToDegree(16383), 0.01);
	}
	
	@Test
	void calculateDistanceUses345TriangleAndFlags()
	{
		assertEquals(25.0, LocationUtil.calculateDistance(0, 0, 0, 3, 4, 0, false, true), DELTA);
		assertEquals(5.0, LocationUtil.calculateDistance(0, 0, 0, 3, 4, 0, false, false), DELTA);
		assertEquals(169.0, LocationUtil.calculateDistance(0, 0, 0, 3, 4, 12, true, true), DELTA);
		assertEquals(13.0, LocationUtil.calculateDistance(0, 0, 0, 3, 4, 12, true, false), DELTA);
		
		final Location a = new Location(0, 0, 0);
		final Location b = new Location(3, 4, 12);
		assertEquals(5.0, LocationUtil.calculateDistance(a, b, false, false), DELTA);
		assertEquals(13.0, LocationUtil.calculateDistance(a, b, true, false), DELTA);
	}
}
