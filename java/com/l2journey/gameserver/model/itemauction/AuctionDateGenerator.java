/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.l2journey.gameserver.model.itemauction;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import com.l2journey.gameserver.model.StatSet;

/**
 * @author Forsaiken
 */
public class AuctionDateGenerator
{
	public static final String FIELD_INTERVAL = "interval";
	public static final String FIELD_DAY_OF_WEEK = "day_of_week";
	public static final String FIELD_HOUR_OF_DAY = "hour_of_day";
	public static final String FIELD_MINUTE_OF_HOUR = "minute_of_hour";
	
	private static final long MILLIS_IN_WEEK = TimeUnit.MILLISECONDS.convert(7, TimeUnit.DAYS);
	
	private final Calendar _calendar;
	
	private final int _interval;
	private int _dayOfWeek;
	private int _hourOfDay;
	private int _minuteOfHour;
	
	public AuctionDateGenerator(StatSet config)
	{
		_calendar = Calendar.getInstance();
		_interval = config.getInt(FIELD_INTERVAL, -1);
		// NC week start in Monday.
		final int fixedDayWeek = config.getInt(FIELD_DAY_OF_WEEK, -1) + 1;
		_dayOfWeek = (fixedDayWeek > 7) ? 1 : fixedDayWeek;
		_hourOfDay = config.getInt(FIELD_HOUR_OF_DAY, -1);
		_minuteOfHour = config.getInt(FIELD_MINUTE_OF_HOUR, -1);
		checkDayOfWeek(-1);
		checkHourOfDay(-1);
		checkMinuteOfHour(0);
	}
	
	public synchronized long nextDate(long date)
	{
		_calendar.setTimeInMillis(date);
		_calendar.set(Calendar.MILLISECOND, 0);
		_calendar.set(Calendar.SECOND, 0);
		_calendar.set(Calendar.MINUTE, _minuteOfHour);
		_calendar.set(Calendar.HOUR_OF_DAY, _hourOfDay);
		if (_dayOfWeek > 0)
		{
			_calendar.set(Calendar.DAY_OF_WEEK, _dayOfWeek);
			return calcDestTime(_calendar.getTimeInMillis(), date, MILLIS_IN_WEEK);
		}
		return calcDestTime(_calendar.getTimeInMillis(), date, TimeUnit.MILLISECONDS.convert(_interval, TimeUnit.DAYS));
	}
	
	private long calcDestTime(long timeValue, long date, long add)
	{
		long time = timeValue;
		if (time < date)
		{
			time += ((date - time) / add) * add;
			if (time < date)
			{
				time += add;
			}
		}
		return time;
	}
	
	private void checkDayOfWeek(int defaultValue)
	{
		if ((_dayOfWeek < 1) || (_dayOfWeek > 7))
		{
			if ((defaultValue == -1) && (_interval < 1))
			{
				throw new IllegalArgumentException("Illegal params for '" + FIELD_DAY_OF_WEEK + "': " + (_dayOfWeek == -1 ? "not found" : _dayOfWeek));
			}
			_dayOfWeek = defaultValue;
		}
		else if (_interval > 1)
		{
			throw new IllegalArgumentException("Illegal params for '" + FIELD_INTERVAL + "' and '" + FIELD_DAY_OF_WEEK + "': you can use only one, not both");
		}
	}
	
	private void checkHourOfDay(int defaultValue)
	{
		if ((_hourOfDay < 0) || (_hourOfDay > 23))
		{
			if (defaultValue == -1)
			{
				throw new IllegalArgumentException("Illegal params for '" + FIELD_HOUR_OF_DAY + "': " + (_hourOfDay == -1 ? "not found" : _hourOfDay));
			}
			_hourOfDay = defaultValue;
		}
	}
	
	private void checkMinuteOfHour(int defaultValue)
	{
		if ((_minuteOfHour < 0) || (_minuteOfHour > 59))
		{
			if (defaultValue == -1)
			{
				throw new IllegalArgumentException("Illegal params for '" + FIELD_MINUTE_OF_HOUR + "': " + (_minuteOfHour == -1 ? "not found" : _minuteOfHour));
			}
			_minuteOfHour = defaultValue;
		}
	}
}