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
package com.l2journey.gameserver.model.olympiad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

/**
 * Pure timing helpers for Olympiad competition windows. A start task that runs after the period end must not pass a negative delay to ThreadPool.
 */
class OlympiadScheduleTest
{
	/** Competition start logged at server boot on 16 Oct 2026 (18:00:26 UTC). */
	private static final long COMP_START = Instant.parse("2026-10-16T18:00:26Z").toEpochMilli();
	/** Default OlympiadCPeriod (6 hours). */
	private static final long COMP_PERIOD = 21_600_000L;
	private static final long COMP_END = COMP_START + COMP_PERIOD;
	
	@Test
	void remainingPeriodIsPositiveWhileStillOpen()
	{
		assertEquals(60_000L, Olympiad.remainingUntilCompEnd(COMP_END, COMP_END - 60_000L));
	}
	
	@Test
	void expiredPeriodIsSkippedWhenStartRunsAtOrAfterEnd()
	{
		assertTrue(Olympiad.remainingUntilCompEnd(COMP_END, COMP_END) <= 0);
		assertTrue(Olympiad.remainingUntilCompEnd(COMP_END, COMP_END + 3_941_743L) <= 0);
	}
	
	@Test
	void lateCompetitionStartDoesNotScheduleNegativeDelay()
	{
		// Gameserver log 17 Oct 2026 01:06:08 UTC: start was scheduled for 16 Oct 18:00:26,
		// period end was 17 Oct 00:00:26, and ThreadPool logged delay -3941743.
		final long ranAt = Instant.parse("2026-10-17T01:06:08Z").toEpochMilli();
		final long naiveDelay = Olympiad.remainingUntilCompEnd(COMP_END, ranAt);
		
		assertEquals(Instant.parse("2026-10-17T00:00:26Z").toEpochMilli(), COMP_END);
		assertTrue(naiveDelay < 0, "logged scenario is a negative delay before clamping");
		assertEquals(-3_942_000L, naiveDelay);
		assertEquals(0L, Olympiad.clampScheduleDelay(naiveDelay));
		assertEquals(0L, Olympiad.clampScheduleDelay(-3_941_743L));
	}
	
	@Test
	void scheduleDelayIsNeverNegative()
	{
		assertEquals(0L, Olympiad.clampScheduleDelay(-3_941_743L));
		assertEquals(0L, Olympiad.clampScheduleDelay(0L));
		assertEquals(5_000L, Olympiad.clampScheduleDelay(5_000L));
	}
}
