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

import java.util.Calendar;
import java.util.List;

/**
 * Pure Olympiad timing helpers. Input calendars are cloned; callers are never mutated.
 * Seconds and milliseconds on competition windows are left as on the cloned input (matches {@code Olympiad#init}).
 */
final class OlympiadSchedule
{
	/** Returned by {@link #millisToCompBegin} when the current window has ended and a next window must be computed. */
	static final long NEED_NEXT_WINDOW = -1L;
	
	private OlympiadSchedule()
	{
	}
	
	record CompetitionWindow(long startMillis, long endMillis)
	{
	}
	
	record SeasonSchedule(long olympiadEndMillis, long nextWeeklyChangeMillis, boolean weeklyChangeDefined)
	{
	}
	
	static long remainingUntilCompEnd(long compEnd, long now)
	{
		return compEnd - now;
	}
	
	static long clampScheduleDelay(long delay)
	{
		return Math.max(0L, delay);
	}
	
	/**
	 * @return {@code 10} when inside the open window, millis until start when start is in the future,
	 *         or {@link #NEED_NEXT_WINDOW} when the window has already ended
	 */
	static long millisToCompBegin(long compStart, long compEnd, long now)
	{
		if ((compStart < now) && (compEnd > now))
		{
			return 10L;
		}
		if (compStart > now)
		{
			return compStart - now;
		}
		return NEED_NEXT_WINDOW;
	}
	
	static int daysUntilCompetitionDay(int fromDayOfWeek, List<Integer> competitionDays)
	{
		boolean dayFound = false;
		int dayCounter = 0;
		for (int i = fromDayOfWeek; i < 8; i++)
		{
			if (competitionDays.contains(i))
			{
				dayFound = true;
				break;
			}
			dayCounter++;
		}
		if (!dayFound)
		{
			for (int i = 1; i < 8; i++)
			{
				if (competitionDays.contains(i))
				{
					break;
				}
				dayCounter++;
			}
		}
		return dayCounter;
	}
	
	/**
	 * Competition window as computed by {@code Olympiad#init} (optionally advance to a competition day, then set hour/minute).
	 */
	static CompetitionWindow computeCompetitionWindow(Calendar now, int startHour, int startMin, long cPeriodMs, boolean useCustomDays, List<Integer> competitionDays)
	{
		final Calendar compStart = (Calendar) now.clone();
		if (useCustomDays)
		{
			final int dayCounter = daysUntilCompetitionDay(compStart.get(Calendar.DAY_OF_WEEK), competitionDays);
			if (dayCounter > 0)
			{
				compStart.add(Calendar.DAY_OF_MONTH, dayCounter);
			}
		}
		compStart.set(Calendar.HOUR_OF_DAY, startHour);
		compStart.set(Calendar.MINUTE, startMin);
		final long startMillis = compStart.getTimeInMillis();
		return new CompetitionWindow(startMillis, startMillis + cPeriodMs);
	}
	
	/**
	 * Next competition window as computed by {@code Olympiad#setNewCompBegin} (including the {@code +24h} advance).
	 */
	static CompetitionWindow computeNextCompetitionWindow(Calendar now, int startHour, int startMin, long cPeriodMs, boolean useCustomDays, List<Integer> competitionDays)
	{
		final Calendar compStart = (Calendar) now.clone();
		
		int currentDay = compStart.get(Calendar.DAY_OF_WEEK);
		compStart.set(Calendar.HOUR_OF_DAY, startHour);
		compStart.set(Calendar.MINUTE, startMin);
		
		// Today's competitions ended, start checking from next day.
		if (currentDay == compStart.get(Calendar.DAY_OF_WEEK))
		{
			if (currentDay == Calendar.SATURDAY)
			{
				currentDay = Calendar.SUNDAY;
			}
			else
			{
				currentDay++;
			}
		}
		
		if (useCustomDays)
		{
			final int dayCounter = daysUntilCompetitionDay(currentDay, competitionDays);
			if (dayCounter > 0)
			{
				compStart.add(Calendar.DAY_OF_MONTH, dayCounter);
			}
		}
		compStart.add(Calendar.HOUR_OF_DAY, 24);
		final long startMillis = compStart.getTimeInMillis();
		return new CompetitionWindow(startMillis, startMillis + cPeriodMs);
	}
	
	/**
	 * Season end and next weekly-change instant as computed by {@code Olympiad#setNewOlympiadEnd}.
	 * For custom {@code DAY} with multiplier {@code < 7}, {@link SeasonSchedule#weeklyChangeDefined()} is false
	 * (production leaves the previous weekly-change value unchanged).
	 */
	static SeasonSchedule computeSeasonSchedule(Calendar now, boolean useCustom, String period, int multiplier, long weeklyPeriodMs)
	{
		final Calendar currentTime = (Calendar) now.clone();
		final long nextChangeBase = now.getTimeInMillis();
		
		if (!useCustom)
		{
			currentTime.add(Calendar.MONTH, 1);
			currentTime.set(Calendar.DAY_OF_MONTH, 1);
			currentTime.set(Calendar.AM_PM, Calendar.AM);
			currentTime.set(Calendar.HOUR_OF_DAY, 12);
			currentTime.set(Calendar.MINUTE, 0);
			currentTime.set(Calendar.SECOND, 0);
			return new SeasonSchedule(currentTime.getTimeInMillis(), nextChangeBase + weeklyPeriodMs, true);
		}
		
		currentTime.set(Calendar.AM_PM, Calendar.AM);
		currentTime.set(Calendar.HOUR_OF_DAY, 12);
		currentTime.set(Calendar.MINUTE, 0);
		currentTime.set(Calendar.SECOND, 0);
		
		long nextWeeklyChange = nextChangeBase;
		boolean weeklyDefined = true;
		
		switch (period)
		{
			case "DAY":
			{
				currentTime.add(Calendar.DAY_OF_MONTH, multiplier);
				currentTime.add(Calendar.DAY_OF_MONTH, -1); // last day is for validation
				
				if (multiplier >= 14)
				{
					nextWeeklyChange = nextChangeBase + weeklyPeriodMs;
				}
				else if (multiplier >= 7)
				{
					nextWeeklyChange = nextChangeBase + (weeklyPeriodMs / 2);
				}
				else
				{
					weeklyDefined = false;
				}
				break;
			}
			case "WEEK":
			{
				currentTime.add(Calendar.WEEK_OF_MONTH, multiplier);
				currentTime.add(Calendar.DAY_OF_MONTH, -1); // last day is for validation
				
				if (multiplier > 1)
				{
					nextWeeklyChange = nextChangeBase + weeklyPeriodMs;
				}
				else
				{
					nextWeeklyChange = nextChangeBase + (weeklyPeriodMs / 2);
				}
				break;
			}
			case "MONTH":
			{
				currentTime.add(Calendar.MONTH, multiplier);
				currentTime.add(Calendar.DAY_OF_MONTH, -1); // last day is for validation
				
				nextWeeklyChange = nextChangeBase + weeklyPeriodMs;
				break;
			}
			default:
			{
				weeklyDefined = false;
				break;
			}
		}
		
		return new SeasonSchedule(currentTime.getTimeInMillis(), nextWeeklyChange, weeklyDefined);
	}
}
