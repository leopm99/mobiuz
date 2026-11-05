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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Characterization tests for {@link LinePointIterator}.
 */
class LinePointIteratorTest
{
	@Test
	void samePointYieldsOnlyOrigin()
	{
		assertArrayEquals(new int[][]
		{
			{
				0,
				0
			}
		}, collect(0, 0, 0, 0));
	}
	
	@Test
	void dominantXStepsAcrossAxis()
	{
		assertArrayEquals(new int[][]
		{
			{
				0,
				0
			},
			{
				1,
				0
			},
			{
				2,
				1
			},
			{
				3,
				1
			}
		}, collect(0, 0, 3, 1));
	}
	
	@Test
	void equalDeltaUsesDominantXBranch()
	{
		assertArrayEquals(new int[][]
		{
			{
				0,
				0
			},
			{
				1,
				1
			},
			{
				2,
				2
			}
		}, collect(0, 0, 2, 2));
	}
	
	@Test
	void dominantYStepsAcrossAxis()
	{
		assertArrayEquals(new int[][]
		{
			{
				0,
				0
			},
			{
				0,
				1
			},
			{
				1,
				2
			},
			{
				1,
				3
			}
		}, collect(0, 0, 1, 3));
	}
	
	@Test
	void negativeDirectionStepsBackward()
	{
		assertArrayEquals(new int[][]
		{
			{
				3,
				1
			},
			{
				2,
				1
			},
			{
				1,
				0
			},
			{
				0,
				0
			}
		}, collect(3, 1, 0, 0));
	}
	
	private static int[][] collect(int srcX, int srcY, int dstX, int dstY)
	{
		final LinePointIterator iterator = new LinePointIterator(srcX, srcY, dstX, dstY);
		final List<int[]> points = new ArrayList<>();
		while (iterator.next())
		{
			points.add(new int[]
			{
				iterator.x(),
				iterator.y()
			});
		}
		assertFalse(iterator.next());
		return points.toArray(new int[0][]);
	}
}
