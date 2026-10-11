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
package com.l2journey.gameserver.managers.games;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pure timing helpers used when a delayed UC start task must not schedule a negative ThreadPool delay.
 */
class UndergroundColiseumScheduleTest
{
	private static final long PERIOD_END = 1_000_000L;
	
	@Test
	void remainingPeriodIsPositiveWhileStillOpen()
	{
		assertEquals(60_000L, UndergroundColiseumManager.remainingUntilPeriodEnd(PERIOD_END, PERIOD_END - 60_000L));
	}
	
	@Test
	void expiredPeriodIsSkippedWhenStartRunsAtOrAfterEnd()
	{
		assertTrue(UndergroundColiseumManager.remainingUntilPeriodEnd(PERIOD_END, PERIOD_END) <= 0);
		assertTrue(UndergroundColiseumManager.remainingUntilPeriodEnd(PERIOD_END, PERIOD_END + 1_071_422L) <= 0);
	}
	
	@Test
	void scheduleDelayIsNeverNegative()
	{
		assertEquals(0L, UndergroundColiseumManager.clampScheduleDelay(-1_071_422L));
		assertEquals(0L, UndergroundColiseumManager.clampScheduleDelay(0L));
		assertEquals(5_000L, UndergroundColiseumManager.clampScheduleDelay(5_000L));
	}
}
