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
package com.l2journey.gameserver.model.sevensigns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

/**
 * Pure timing helpers for Seven Signs period changes. A competition-end task that runs after the 15-minute results window must not pass a negative delay to ThreadPool.
 */
class SevenSignsScheduleTest
{
	/** Competition period end logged at startup on 12 Oct 2026. */
	private static final long COMPETITION_END = Instant.parse("2026-10-12T18:00:05Z").toEpochMilli();
	
	@Test
	void resultsWindowStaysFifteenMinutesWhenChangeRunsOnTime()
	{
		final long boundary = SevenSigns.nextMinorPeriodBoundary(COMPETITION_END);
		
		assertEquals(COMPETITION_END + SevenSigns.PERIOD_MINOR_LENGTH, boundary);
		assertEquals(SevenSigns.PERIOD_MINOR_LENGTH, SevenSigns.clampScheduleDelay(boundary - COMPETITION_END));
	}
	
	@Test
	void openResultsWindowKeepsItsOriginalEndWhenChangeIsOnlySlightlyLate()
	{
		final long ranAt = COMPETITION_END + (5 * 60 * 1000L);
		final long boundary = SevenSigns.nextMinorPeriodBoundary(COMPETITION_END);
		
		assertEquals(COMPETITION_END + SevenSigns.PERIOD_MINOR_LENGTH, boundary);
		assertEquals(10 * 60 * 1000L, SevenSigns.clampScheduleDelay(boundary - ranAt));
	}
	
	@Test
	void lateResultsPeriodDoesNotScheduleNegativeDelay()
	{
		// Gameserver log 12 Oct 2026 19:07:30 UTC: competition end was 18:00:05, results were set to 18:15:05, and ThreadPool logged delay -3144707.
		final long ranAt = Instant.parse("2026-10-12T19:07:30Z").toEpochMilli();
		final long boundary = SevenSigns.nextMinorPeriodBoundary(COMPETITION_END);
		final long naiveDelay = boundary - ranAt;
		
		assertEquals(Instant.parse("2026-10-12T18:15:05Z").toEpochMilli(), boundary);
		assertTrue(naiveDelay < 0, "logged scenario is a negative delay before clamping");
		assertEquals(-3_145_000L, naiveDelay);
		assertEquals(0L, SevenSigns.clampScheduleDelay(naiveDelay));
		assertEquals(0L, SevenSigns.clampScheduleDelay(-3_144_707L));
	}
}
