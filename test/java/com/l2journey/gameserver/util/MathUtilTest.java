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
	@Test
	void divIntByZeroThrows()
	{
		assertThrows(ArithmeticException.class, () -> MathUtil.div(1, 0));
	}
	
	@Test
	void divDoubleByZeroThrows()
	{
		assertThrows(ArithmeticException.class, () -> MathUtil.div(1.0d, 0.0d));
	}
	
	@Test
	void clampReturnsMinWhenBelowRange()
	{
		assertEquals(0, MathUtil.clamp(-5, 0, 10));
	}
	
	@Test
	void clampReturnsValueWhenInsideRange()
	{
		assertEquals(5, MathUtil.clamp(5, 0, 10));
	}
	
	@Test
	void clampReturnsMaxWhenAboveRange()
	{
		assertEquals(10, MathUtil.clamp(15, 0, 10));
	}
	
	@Test
	void scaleToRangeMapsSourceEndpoints()
	{
		assertEquals(0, MathUtil.scaleToRange(0, 0, 100, 0, 10));
		assertEquals(10, MathUtil.scaleToRange(100, 0, 100, 0, 10));
	}
	
	@Test
	void scaleToRangeMapsMidpointWithIntegerDivision()
	{
		assertEquals(5, MathUtil.scaleToRange(50, 0, 100, 0, 10));
	}
	
	@Test
	void minAndMaxFindExtremes()
	{
		final int[] values = { 3, -1, 7, 2 };
		assertEquals(-1, MathUtil.min(values));
		assertEquals(7, MathUtil.max(values));
	}
	
	@Test
	void indexOfMinAndMaxLocateExtremes()
	{
		final int[] values = { 3, -1, 7, 2 };
		assertEquals(1, MathUtil.getIndexOfMinValue(values));
		assertEquals(2, MathUtil.getIndexOfMaxValue(values));
	}
}
