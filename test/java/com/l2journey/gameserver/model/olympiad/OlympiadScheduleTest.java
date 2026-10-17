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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

/**
 * Pure timing helpers for Olympiad competition windows and season ends.
 */
class OlympiadScheduleTest
{
	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
	private static final long WEEKLY_PERIOD = 604_800_000L;
	private static final long SIX_HOURS = 21_600_000L;
	private static final long ONE_DAY = 86_400_000L;
	
	/** Competition start logged at server boot on 16 Oct 2026 (18:00:26 UTC). */
	private static final long LOGGED_COMP_START = Instant.parse("2026-10-16T18:00:26Z").toEpochMilli();
	private static final long LOGGED_COMP_END = LOGGED_COMP_START + SIX_HOURS;
	
	@Test
	void remainingPeriodIsPositiveWhileStillOpen()
	{
		assertEquals(60_000L, OlympiadSchedule.remainingUntilCompEnd(LOGGED_COMP_END, LOGGED_COMP_END - 60_000L));
	}
	
	@Test
	void expiredPeriodIsSkippedWhenStartRunsAtOrAfterEnd()
	{
		assertTrue(OlympiadSchedule.remainingUntilCompEnd(LOGGED_COMP_END, LOGGED_COMP_END) <= 0);
		assertTrue(OlympiadSchedule.remainingUntilCompEnd(LOGGED_COMP_END, LOGGED_COMP_END + 3_941_743L) <= 0);
	}
	
	@Test
	void lateCompetitionStartDoesNotScheduleNegativeDelay()
	{
		// Gameserver log 17 Oct 2026 01:06:08 UTC: start was scheduled for 16 Oct 18:00:26,
		// period end was 17 Oct 00:00:26, and ThreadPool logged delay -3941743.
		final long ranAt = Instant.parse("2026-10-17T01:06:08Z").toEpochMilli();
		final long naiveDelay = OlympiadSchedule.remainingUntilCompEnd(LOGGED_COMP_END, ranAt);
		
		assertEquals(Instant.parse("2026-10-17T00:00:26Z").toEpochMilli(), LOGGED_COMP_END);
		assertTrue(naiveDelay < 0, "logged scenario is a negative delay before clamping");
		assertEquals(-3_942_000L, naiveDelay);
		assertEquals(0L, OlympiadSchedule.clampScheduleDelay(naiveDelay));
		assertEquals(0L, OlympiadSchedule.clampScheduleDelay(-3_941_743L));
	}
	
	@Test
	void scheduleDelayIsNeverNegative()
	{
		assertEquals(0L, OlympiadSchedule.clampScheduleDelay(-3_941_743L));
		assertEquals(0L, OlympiadSchedule.clampScheduleDelay(0L));
		assertEquals(5_000L, OlympiadSchedule.clampScheduleDelay(5_000L));
	}
	
	@Test
	void defaultSeasonEndsFirstOfNextMonthAtNoon()
	{
		// Wednesday 15 Oct 2026 10:30:45.123 UTC
		final Calendar now = utc(2026, Calendar.OCTOBER, 15, 10, 30, 45, 123);
		final OlympiadSchedule.SeasonSchedule season = OlympiadSchedule.computeSeasonSchedule(now, false, "MONTH", 1, WEEKLY_PERIOD);
		
		final Calendar expectedEnd = utc(2026, Calendar.NOVEMBER, 1, 12, 0, 0, 123);
		assertEquals(expectedEnd.getTimeInMillis(), season.olympiadEndMillis());
		assertTrue(season.weeklyChangeDefined());
		assertEquals(now.getTimeInMillis() + WEEKLY_PERIOD, season.nextWeeklyChangeMillis());
		assertEquals(now.getTimeInMillis(), utc(2026, Calendar.OCTOBER, 15, 10, 30, 45, 123).getTimeInMillis(), "input calendar must not be mutated");
	}
	
	@Test
	void customDayTimesSevenEndsAfterSixDaysAtNoon()
	{
		final Calendar now = utc(2026, Calendar.OCTOBER, 15, 10, 0, 0, 0);
		final OlympiadSchedule.SeasonSchedule season = OlympiadSchedule.computeSeasonSchedule(now, true, "DAY", 7, WEEKLY_PERIOD);
		
		final Calendar expectedEnd = utc(2026, Calendar.OCTOBER, 21, 12, 0, 0, 0);
		assertEquals(expectedEnd.getTimeInMillis(), season.olympiadEndMillis());
		assertTrue(season.weeklyChangeDefined());
		assertEquals(now.getTimeInMillis() + (WEEKLY_PERIOD / 2), season.nextWeeklyChangeMillis());
	}
	
	@Test
	void customWeekTimesOneEndsAfterSixDaysAtNoon()
	{
		final Calendar now = utc(2026, Calendar.OCTOBER, 15, 10, 0, 0, 0);
		final OlympiadSchedule.SeasonSchedule season = OlympiadSchedule.computeSeasonSchedule(now, true, "WEEK", 1, WEEKLY_PERIOD);
		
		// WEEK_OF_MONTH +1 then -1 day from noon on the 15th → noon on the 21st
		final Calendar expectedEnd = utc(2026, Calendar.OCTOBER, 21, 12, 0, 0, 0);
		assertEquals(expectedEnd.getTimeInMillis(), season.olympiadEndMillis());
		assertTrue(season.weeklyChangeDefined());
		assertEquals(now.getTimeInMillis() + (WEEKLY_PERIOD / 2), season.nextWeeklyChangeMillis());
	}
	
	@Test
	void customMonthTimesOneEndsLastDayBeforeNextMonthNoon()
	{
		final Calendar now = utc(2026, Calendar.OCTOBER, 15, 10, 0, 0, 0);
		final OlympiadSchedule.SeasonSchedule season = OlympiadSchedule.computeSeasonSchedule(now, true, "MONTH", 1, WEEKLY_PERIOD);
		
		final Calendar expectedEnd = utc(2026, Calendar.NOVEMBER, 14, 12, 0, 0, 0);
		assertEquals(expectedEnd.getTimeInMillis(), season.olympiadEndMillis());
		assertTrue(season.weeklyChangeDefined());
		assertEquals(now.getTimeInMillis() + WEEKLY_PERIOD, season.nextWeeklyChangeMillis());
	}
	
	@Test
	void retailCompetitionWindowIsSixHoursFromEighteen()
	{
		final Calendar now = utc(2026, Calendar.OCTOBER, 15, 10, 0, 30, 0); // Wednesday
		final OlympiadSchedule.CompetitionWindow window = OlympiadSchedule.computeCompetitionWindow(now, 18, 0, SIX_HOURS, false, List.of());
		
		assertEquals(utc(2026, Calendar.OCTOBER, 15, 18, 0, 30, 0).getTimeInMillis(), window.startMillis());
		assertEquals(window.startMillis() + SIX_HOURS, window.endMillis());
	}
	
	@Test
	void allDayWindowFromMidnightSpansTwentyFourHours()
	{
		final Calendar now = utc(2026, Calendar.OCTOBER, 15, 12, 0, 0, 0);
		final OlympiadSchedule.CompetitionWindow window = OlympiadSchedule.computeCompetitionWindow(now, 0, 0, ONE_DAY, false, List.of());
		
		assertEquals(utc(2026, Calendar.OCTOBER, 15, 0, 0, 0, 0).getTimeInMillis(), window.startMillis());
		assertEquals(utc(2026, Calendar.OCTOBER, 16, 0, 0, 0, 0).getTimeInMillis(), window.endMillis());
	}
	
	@Test
	void allDayWindowFromEveningSpansTwentyFourHours()
	{
		final Calendar now = utc(2026, Calendar.OCTOBER, 15, 12, 0, 0, 0);
		final OlympiadSchedule.CompetitionWindow window = OlympiadSchedule.computeCompetitionWindow(now, 18, 0, ONE_DAY, false, List.of());
		
		assertEquals(utc(2026, Calendar.OCTOBER, 15, 18, 0, 0, 0).getTimeInMillis(), window.startMillis());
		assertEquals(window.startMillis() + ONE_DAY, window.endMillis());
	}
	
	@Test
	void inWindowBootUsesImmediateStartDelay()
	{
		final Calendar noon = utc(2026, Calendar.OCTOBER, 15, 12, 0, 0, 0);
		final OlympiadSchedule.CompetitionWindow window = OlympiadSchedule.computeCompetitionWindow(noon, 0, 0, ONE_DAY, false, List.of());
		
		assertEquals(10L, OlympiadSchedule.millisToCompBegin(window.startMillis(), window.endMillis(), noon.getTimeInMillis()));
	}
	
	@Test
	void futureStartReturnsDelayUntilCompetitionBegin()
	{
		final long start = utc(2026, Calendar.OCTOBER, 15, 18, 0, 0, 0).getTimeInMillis();
		final long end = start + SIX_HOURS;
		final long now = utc(2026, Calendar.OCTOBER, 15, 10, 0, 0, 0).getTimeInMillis();
		
		assertEquals(start - now, OlympiadSchedule.millisToCompBegin(start, end, now));
	}
	
	@Test
	void endedWindowSignalsNeedNextWindow()
	{
		final long start = utc(2026, Calendar.OCTOBER, 15, 18, 0, 0, 0).getTimeInMillis();
		final long end = start + SIX_HOURS;
		final long now = end + 1_000L;
		
		assertEquals(OlympiadSchedule.NEED_NEXT_WINDOW, OlympiadSchedule.millisToCompBegin(start, end, now));
	}
	
	@Test
	void competitionDaysAdvanceInitWindowToNextAllowedDay()
	{
		// Wednesday 14 Oct 2026; only Sunday(1) and Saturday(7) → advance 3 days to Saturday 17 Oct
		final Calendar wednesday = utc(2026, Calendar.OCTOBER, 14, 10, 0, 0, 0);
		assertEquals(Calendar.WEDNESDAY, wednesday.get(Calendar.DAY_OF_WEEK));
		
		final OlympiadSchedule.CompetitionWindow window = OlympiadSchedule.computeCompetitionWindow(wednesday, 18, 0, SIX_HOURS, true, List.of(Calendar.SUNDAY, Calendar.SATURDAY));
		
		final Calendar start = Calendar.getInstance(UTC);
		start.setTimeInMillis(window.startMillis());
		assertEquals(Calendar.SATURDAY, start.get(Calendar.DAY_OF_WEEK));
		assertEquals(18, start.get(Calendar.HOUR_OF_DAY));
		assertEquals(utc(2026, Calendar.OCTOBER, 17, 18, 0, 0, 0).getTimeInMillis(), window.startMillis());
	}
	
	@Test
	void nextCompetitionWindowKeepsInvariantsAfterExpiry()
	{
		// After a retail window ended (Thu 01:00), next schedule must be in the future with end = start + cPeriod.
		final Calendar now = utc(2026, Calendar.OCTOBER, 16, 1, 0, 0, 0); // Thursday
		final OlympiadSchedule.CompetitionWindow next = OlympiadSchedule.computeNextCompetitionWindow(now, 18, 0, SIX_HOURS, true, List.of(1, 2, 3, 4, 5, 6, 7));
		
		assertEquals(next.startMillis() + SIX_HOURS, next.endMillis());
		assertTrue(next.startMillis() >= now.getTimeInMillis());
		assertTrue(OlympiadSchedule.clampScheduleDelay(next.startMillis() - now.getTimeInMillis()) >= 0);
		
		final Calendar start = Calendar.getInstance(UTC);
		start.setTimeInMillis(next.startMillis());
		assertTrue(List.of(1, 2, 3, 4, 5, 6, 7).contains(start.get(Calendar.DAY_OF_WEEK)));
	}
	
	@Test
	void nextCompetitionWindowWithWeekendOnlyLandsOnAllowedDay()
	{
		// Step-through of setNewCompBegin from Wednesday 14 Oct 2026 01:00 UTC, startHour=18:
		// - set hour 18 on Wednesday; currentDay advances to Thursday for day search
		// - days until Saturday from Thursday = 2 → add 2 days → Friday 18:00
		// - add HOUR 24 → Saturday 18:00
		final Calendar now = utc(2026, Calendar.OCTOBER, 14, 1, 0, 0, 0);
		final OlympiadSchedule.CompetitionWindow next = OlympiadSchedule.computeNextCompetitionWindow(now, 18, 0, SIX_HOURS, true, List.of(Calendar.SUNDAY, Calendar.SATURDAY));
		
		assertEquals(next.startMillis() + SIX_HOURS, next.endMillis());
		assertTrue(next.startMillis() > now.getTimeInMillis());
		
		final Calendar start = Calendar.getInstance(UTC);
		start.setTimeInMillis(next.startMillis());
		assertEquals(Calendar.SATURDAY, start.get(Calendar.DAY_OF_WEEK));
		assertTrue(List.of(Calendar.SUNDAY, Calendar.SATURDAY).contains(start.get(Calendar.DAY_OF_WEEK)));
	}
	
	@Test
	void daysUntilCompetitionDayWrapsAcrossWeekBoundary()
	{
		// Saturday with only Sunday allowed: first loop finds nothing, second loop wraps to Sunday.
		assertEquals(1, OlympiadSchedule.daysUntilCompetitionDay(Calendar.SATURDAY, List.of(Calendar.SUNDAY)));
	}
	
	@Test
	void nextCompetitionWindowFromSaturdayAdvancesCurrentDayToSunday()
	{
		// Saturday 17 Oct 2026 01:00 UTC: currentDay wraps SATURDAY → SUNDAY before day search.
		final Calendar saturday = utc(2026, Calendar.OCTOBER, 17, 1, 0, 0, 0);
		assertEquals(Calendar.SATURDAY, saturday.get(Calendar.DAY_OF_WEEK));
		
		final OlympiadSchedule.CompetitionWindow next = OlympiadSchedule.computeNextCompetitionWindow(saturday, 18, 0, SIX_HOURS, true, List.of(Calendar.SUNDAY));
		
		assertEquals(next.startMillis() + SIX_HOURS, next.endMillis());
		final Calendar start = Calendar.getInstance(UTC);
		start.setTimeInMillis(next.startMillis());
		assertEquals(Calendar.SUNDAY, start.get(Calendar.DAY_OF_WEEK));
	}
	
	@Test
	void customDayTimesFourteenUsesFullWeeklyPeriod()
	{
		final Calendar now = utc(2026, Calendar.OCTOBER, 15, 10, 0, 0, 0);
		final OlympiadSchedule.SeasonSchedule season = OlympiadSchedule.computeSeasonSchedule(now, true, "DAY", 14, WEEKLY_PERIOD);
		
		assertTrue(season.weeklyChangeDefined());
		assertEquals(now.getTimeInMillis() + WEEKLY_PERIOD, season.nextWeeklyChangeMillis());
		assertEquals(utc(2026, Calendar.OCTOBER, 28, 12, 0, 0, 0).getTimeInMillis(), season.olympiadEndMillis());
	}
	
	@Test
	void customDayBelowSevenLeavesWeeklyChangeUndefined()
	{
		final Calendar now = utc(2026, Calendar.OCTOBER, 15, 10, 0, 0, 0);
		final OlympiadSchedule.SeasonSchedule season = OlympiadSchedule.computeSeasonSchedule(now, true, "DAY", 6, WEEKLY_PERIOD);
		
		assertFalse(season.weeklyChangeDefined());
		assertEquals(utc(2026, Calendar.OCTOBER, 20, 12, 0, 0, 0).getTimeInMillis(), season.olympiadEndMillis());
	}
	
	@Test
	void customWeekTimesTwoUsesFullWeeklyPeriod()
	{
		final Calendar now = utc(2026, Calendar.OCTOBER, 15, 10, 0, 0, 0);
		final OlympiadSchedule.SeasonSchedule season = OlympiadSchedule.computeSeasonSchedule(now, true, "WEEK", 2, WEEKLY_PERIOD);
		
		assertTrue(season.weeklyChangeDefined());
		assertEquals(now.getTimeInMillis() + WEEKLY_PERIOD, season.nextWeeklyChangeMillis());
	}
	
	@Test
	void unknownPeriodLeavesWeeklyChangeUndefined()
	{
		final Calendar now = utc(2026, Calendar.OCTOBER, 15, 10, 0, 0, 0);
		final OlympiadSchedule.SeasonSchedule season = OlympiadSchedule.computeSeasonSchedule(now, true, "YEAR", 1, WEEKLY_PERIOD);
		
		assertFalse(season.weeklyChangeDefined());
	}
	
	@Test
	void privateConstructorIsCoveredForUtilityClass() throws Exception
	{
		final var ctor = OlympiadSchedule.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		assertTrue(ctor.newInstance() instanceof OlympiadSchedule);
	}
	
	@Test
	void daysUntilWrapsPastSundayWhenOnlyEarlierWeekdayRemains()
	{
		// Thursday (5): no match in 5..7, then Sun/Mon/Tue miss before Wednesday (4).
		assertEquals(6, OlympiadSchedule.daysUntilCompetitionDay(5, List.of(4)));
	}
	
	private static Calendar utc(int year, int month, int day, int hour, int minute, int second, int millis)
	{
		final Calendar cal = Calendar.getInstance(UTC);
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DAY_OF_MONTH, day);
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, minute);
		cal.set(Calendar.SECOND, second);
		cal.set(Calendar.MILLISECOND, millis);
		return cal;
	}
}
