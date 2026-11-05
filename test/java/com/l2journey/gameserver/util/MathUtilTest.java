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
package com.l2journey.gameserver.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MathUtil}.
 */
class MathUtilTest
{
	private static final double DELTA = 1e-9;
	
	@Test
	void addAndMulCoverPrimitiveOverloads()
	{
		assertEquals((byte) 3, MathUtil.add((byte) 1, (byte) 2));
		assertEquals((short) 3, MathUtil.add((short) 1, (short) 2));
		assertEquals(3, MathUtil.add(1, 2));
		assertEquals(3.5d, MathUtil.add(1.25d, 2.25d), DELTA);
		
		assertEquals((byte) (-2), MathUtil.add((byte) 127, (byte) 127));
		assertEquals((short) (-2), MathUtil.add((short) 32767, (short) 32767));
		
		assertEquals((byte) 6, MathUtil.mul((byte) 2, (byte) 3));
		assertEquals((short) 6, MathUtil.mul((short) 2, (short) 3));
		assertEquals(6, MathUtil.mul(2, 3));
		assertEquals(6.25d, MathUtil.mul(2.5d, 2.5d), DELTA);
		assertEquals((byte) (-128), MathUtil.mul((byte) 64, (byte) 2));
	}
	
	@Test
	void divCoversPrimitiveOverloadsAndZeroGuards()
	{
		assertEquals((byte) 2, MathUtil.div((byte) 6, (byte) 3));
		assertEquals((short) 2, MathUtil.div((short) 6, (short) 3));
		assertEquals(2, MathUtil.div(6, 3));
		assertEquals(2.5d, MathUtil.div(5.0d, 2.0d), DELTA);
		
		assertThrows(ArithmeticException.class, () -> MathUtil.div((byte) 1, (byte) 0));
		assertThrows(ArithmeticException.class, () -> MathUtil.div((short) 1, (short) 0));
		assertThrows(ArithmeticException.class, () -> MathUtil.div(1, 0));
		assertThrows(ArithmeticException.class, () -> MathUtil.div(1.0d, 0.0d));
	}
	
	@Test
	void clampReturnsMinWhenBelowRange()
	{
		assertEquals(0, MathUtil.clamp(-5, 0, 10));
		assertEquals(0L, MathUtil.clamp(-5L, 0L, 10L));
		assertEquals(0.0d, MathUtil.clamp(-5.0d, 0.0d, 10.0d), DELTA);
	}
	
	@Test
	void clampReturnsValueWhenInsideRange()
	{
		assertEquals(5, MathUtil.clamp(5, 0, 10));
		assertEquals(5L, MathUtil.clamp(5L, 0L, 10L));
		assertEquals(5.0d, MathUtil.clamp(5.0d, 0.0d, 10.0d), DELTA);
	}
	
	@Test
	void clampReturnsMaxWhenAboveRange()
	{
		assertEquals(10, MathUtil.clamp(15, 0, 10));
		assertEquals(10L, MathUtil.clamp(15L, 0L, 10L));
		assertEquals(10.0d, MathUtil.clamp(15.0d, 0.0d, 10.0d), DELTA);
	}
	
	@Test
	void scaleToRangeMapsSourceEndpoints()
	{
		assertEquals(0, MathUtil.scaleToRange(0, 0, 100, 0, 10));
		assertEquals(10, MathUtil.scaleToRange(100, 0, 100, 0, 10));
		assertEquals(0L, MathUtil.scaleToRange(0L, 0L, 100L, 0L, 10L));
		assertEquals(10L, MathUtil.scaleToRange(100L, 0L, 100L, 0L, 10L));
		assertEquals(0.0d, MathUtil.scaleToRange(0.0d, 0.0d, 100.0d, 0.0d, 10.0d), DELTA);
		assertEquals(10.0d, MathUtil.scaleToRange(100.0d, 0.0d, 100.0d, 0.0d, 10.0d), DELTA);
	}
	
	@Test
	void scaleToRangeMapsMidpointAndClampsOutsideSource()
	{
		assertEquals(5, MathUtil.scaleToRange(50, 0, 100, 0, 10));
		assertEquals(0, MathUtil.scaleToRange(-20, 0, 100, 0, 10));
		assertEquals(10, MathUtil.scaleToRange(200, 0, 100, 0, 10));
		assertEquals(5L, MathUtil.scaleToRange(50L, 0L, 100L, 0L, 10L));
		assertEquals(5.0d, MathUtil.scaleToRange(50.0d, 0.0d, 100.0d, 0.0d, 10.0d), DELTA);
	}
	
	@Test
	void minAndMaxFindExtremes()
	{
		assertEquals(-1, MathUtil.min(3, -1, 7, 2));
		assertEquals(7, MathUtil.max(3, -1, 7, 2));
		assertEquals(-1L, MathUtil.min(3L, -1L, 7L, 2L));
		assertEquals(7L, MathUtil.max(3L, -1L, 7L, 2L));
		assertEquals(-1.5d, MathUtil.min(3.0d, -1.5d, 7.0d), DELTA);
		assertEquals(7.0d, MathUtil.max(3.0d, -1.5d, 7.0d), DELTA);
	}
	
	@Test
	void indexOfMinAndMaxLocateExtremes()
	{
		final int[] values =
		{
			3,
			-1,
			7,
			2
		};
		assertEquals(1, MathUtil.getIndexOfMinValue(values));
		assertEquals(2, MathUtil.getIndexOfMaxValue(values));
	}
}
