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
package com.l2journey.gameserver.managers;

import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import com.l2journey.EventsConfig;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.zone.type.TownZone;
import com.l2journey.gameserver.util.Broadcast;

/**
 * @author KingHanker, Zoinha
 */
public class TownWarManager
{
	private static final Logger LOGGER = Logger.getLogger(TownWarManager.class.getName());
	private static final int[] TOWNS =
	{
		1,
		2,
		3,
		4,
		5,
		6,
		7,
		8,
		9,
		10,
		11,
		12,
		13,
		14,
		15,
		16,
		17,
		20
	};
	private TownWarStartTask _task;
	private boolean isInactive = true;
	private boolean isStarting = false;
	private boolean isStarted = false;
	
	private TownWarManager()
	{
		if (EventsConfig.TW_AUTO_EVENT)
		{
			scheduleEventStart();
			LOGGER.info("TownWar: Started.");
		}
		else
		{
			LOGGER.info("TownWar: Disabled.");
		}
	}
	
	public static TownWarManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public void scheduleEventStart()
	{
		try
		{
			Calendar currentTime = Calendar.getInstance();
			Calendar nextStartTime = null;
			Calendar testStartTime = null;
			for (String timeOfDay : EventsConfig.TW_INTERVAL)
			{
				testStartTime = Calendar.getInstance();
				testStartTime.setLenient(true);
				String[] splitTimeOfDay = timeOfDay.split(":");
				testStartTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(splitTimeOfDay[0]));
				testStartTime.set(Calendar.MINUTE, Integer.parseInt(splitTimeOfDay[1]));
				if (testStartTime.getTimeInMillis() < currentTime.getTimeInMillis())
				{
					testStartTime.add(Calendar.DAY_OF_MONTH, 1);
				}
				if ((nextStartTime == null) || (testStartTime.getTimeInMillis() < nextStartTime.getTimeInMillis()))
				{
					nextStartTime = testStartTime;
				}
			}
			if (nextStartTime == null)
			{
				LOGGER.warning("TownWar Engine: No valid start times configured in TownWarEventInterval.");
				return;
			}
			_task = new TownWarStartTask(nextStartTime.getTimeInMillis());
			ThreadPool.execute(_task);
		}
		catch (Exception _)
		{
			LOGGER.warning("TownWar Engine: Error figuring out a start time. Check TownWarEventInterval in config file.");
		}
	}
	
	public void starting()
	{
		isInactive = false;
		isStarting = true;
		_task.setStartTime(System.currentTimeMillis() + (60000L * EventsConfig.TW_TIME_BEFORE_START));
		ThreadPool.execute(_task);
	}
	
	public void startEvent()
	{
		isStarting = false;
		isStarted = true;
		updateTownZones(true);
		Broadcast.toAllOnlinePlayers("Town War: " + (EventsConfig.TW_ALL_TOWNS ? "All towns are war zone." : EventsConfig.TW_TOWN_NAME + " is a war zone."));
		
		_task.setStartTime(System.currentTimeMillis() + (60000L * EventsConfig.TW_RUNNING_TIME));
		ThreadPool.execute(_task);
	}
	
	public void endEvent()
	{
		isStarted = false;
		isInactive = true;
		updateTownZones(false);
		Broadcast.toAllOnlinePlayers("Town War: " + (EventsConfig.TW_ALL_TOWNS ? "All towns are returned normal." : EventsConfig.TW_TOWN_NAME + " is returned normal."));
		
		scheduleEventStart();
	}
	
	private void updateTownZones(boolean isWarZone)
	{
		if (!EventsConfig.TW_ALL_TOWNS)
		{
			TownZone defaultTown = TownManager.getTown(EventsConfig.TW_TOWN_ID);
			if (defaultTown == null)
			{
				return;
			}
			
			defaultTown.setIsTWZone(isWarZone);
			defaultTown.updateForCharactersInside();
			return;
		}
		
		for (int i = 1; i <= TOWNS.length; i++)
		{
			TownZone town = TownManager.getTown(i);
			if (town == null)
			{
				continue;
			}
			town.setIsTWZone(isWarZone);
			town.updateForCharactersInside();
		}
	}
	
	class TownWarStartTask implements Runnable
	{
		private long _startTime;
		public ScheduledFuture<?> nextRun;
		
		public TownWarStartTask(long startTime)
		{
			_startTime = startTime;
		}
		
		public void setStartTime(long startTime)
		{
			_startTime = startTime;
		}
		
		@Override
		public void run()
		{
			int delay = (int) Math.round((_startTime - System.currentTimeMillis()) / 1000.0);
			
			if (delay > 0)
			{
				announce(delay);
			}
			
			int nextMsg = calculateNextMessageTime(delay);
			
			if (nextMsg > 0)
			{
				nextRun = ThreadPool.schedule(this, nextMsg * 1000);
			}
			else
			{
				if (isInactive)
				{
					starting();
				}
				else if (isStarting)
				{
					startEvent();
				}
				else
				{
					endEvent();
				}
			}
		}
		
		private int calculateNextMessageTime(int delay)
		{
			int[] intervals =
			{
				3600,
				1800,
				900,
				600,
				300,
				60,
				5
			};
			for (int interval : intervals)
			{
				if (delay > interval)
				{
					return delay - interval;
				}
			}
			return delay;
		}
		
		private void announce(long time)
		{
			String message = formatAnnouncementMessage(time);
			if (isStarting)
			{
				Broadcast.toAllOnlinePlayers(message);
			}
			else if (isStarted)
			{
				for (Player onlinePlayer : World.getInstance().getPlayers())
				{
					if ((onlinePlayer != null) && onlinePlayer.isOnline())
					{
						onlinePlayer.sendMessage(message);
					}
				}
			}
		}
		
		private String formatAnnouncementMessage(long time)
		{
			if (time >= 3600)
			{
				return "Town War Event: " + (time / 3600) + " hour(s) " + (isStarting ? "until event starts!" : "until event is finished!");
			}
			else if (time >= 60)
			{
				return "Town War Event: " + (time / 60) + " minute(s) " + (isStarting ? "until event starts!" : "until the event is finished!");
			}
			else
			{
				return "Town War Event: " + time + " second(s) " + (isStarting ? "until event starts!" : "until the event is finished!");
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final TownWarManager _instance = new TownWarManager();
	}
}
