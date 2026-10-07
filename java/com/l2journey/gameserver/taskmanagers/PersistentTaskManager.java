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
package com.l2journey.gameserver.taskmanagers;

import static com.l2journey.gameserver.taskmanagers.tasks.PersistentTaskType.TYPE_NONE;
import static com.l2journey.gameserver.taskmanagers.tasks.PersistentTaskType.TYPE_SHEDULED;
import static com.l2journey.gameserver.taskmanagers.tasks.PersistentTaskType.TYPE_TIME;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.taskmanagers.tasks.PersistentTask;
import com.l2journey.gameserver.taskmanagers.tasks.PersistentTaskType;
import com.l2journey.gameserver.taskmanagers.tasks.TaskBirthday;
import com.l2journey.gameserver.taskmanagers.tasks.TaskClanLeaderApply;
import com.l2journey.gameserver.taskmanagers.tasks.TaskCleanUp;
import com.l2journey.gameserver.taskmanagers.tasks.TaskDailySkillReuseClean;
import com.l2journey.gameserver.taskmanagers.tasks.TaskNevit;
import com.l2journey.gameserver.taskmanagers.tasks.TaskRaidPointsReset;
import com.l2journey.gameserver.taskmanagers.tasks.TaskRecom;
import com.l2journey.gameserver.taskmanagers.tasks.TaskRestart;
import com.l2journey.gameserver.taskmanagers.tasks.TaskSevenSignsUpdate;
import com.l2journey.gameserver.taskmanagers.tasks.TaskShutdown;

/**
 * @author Layane
 */
public class PersistentTaskManager
{
	static final Logger LOGGER = Logger.getLogger(PersistentTaskManager.class.getName());
	
	private final Map<Integer, PersistentTask> _tasks = new ConcurrentHashMap<>();
	final Collection<ExecutedTask> _currentTasks = ConcurrentHashMap.newKeySet();
	
	static final String[] SQL_STATEMENTS =
	{
		"SELECT id,task,type,last_activation,param1,param2,param3 FROM global_tasks",
		"UPDATE global_tasks SET last_activation=? WHERE id=?",
		"SELECT id FROM global_tasks WHERE task=?",
		"INSERT INTO global_tasks (task,type,last_activation,param1,param2,param3) VALUES(?,?,?,?,?,?)"
	};
	
	protected PersistentTaskManager()
	{
		initializate();
		startAllTasks();
		LOGGER.info(() -> getClass().getSimpleName() + ": Loaded " + _tasks.size() + " Tasks.");
	}
	
	public class ExecutedTask implements Runnable
	{
		int id;
		private long lastActivation;
		private final PersistentTask task;
		private final PersistentTaskType type;
		private final String[] params;
		ScheduledFuture<?> scheduled;
		
		ExecutedTask(PersistentTask ptask, PersistentTaskType ptype, ResultSet rset) throws SQLException
		{
			task = ptask;
			type = ptype;
			id = rset.getInt("id");
			lastActivation = rset.getLong("last_activation");
			params = new String[]
			{
				rset.getString("param1"),
				rset.getString("param2"),
				rset.getString("param3")
			};
		}
		
		@Override
		public void run()
		{
			task.onTimeElapsed(this);
			lastActivation = System.currentTimeMillis();
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement(SQL_STATEMENTS[1]))
			{
				statement.setLong(1, lastActivation);
				statement.setInt(2, id);
				statement.executeUpdate();
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Cannot updated the Global Task " + id + ": " + e.getMessage());
			}
			
			if ((type == TYPE_SHEDULED) || (type == TYPE_TIME))
			{
				stopTask();
			}
		}
		
		@Override
		public boolean equals(Object object)
		{
			return (this == object) || ((object instanceof ExecutedTask) && (id == ((ExecutedTask) object).id));
		}
		
		@Override
		public int hashCode()
		{
			return id;
		}
		
		public PersistentTask getTask()
		{
			return task;
		}
		
		public PersistentTaskType getType()
		{
			return type;
		}
		
		public int getId()
		{
			return id;
		}
		
		public String[] getParams()
		{
			return params;
		}
		
		public long getLastActivation()
		{
			return lastActivation;
		}
		
		private void stopTask()
		{
			task.onDestroy();
			
			if (scheduled != null)
			{
				scheduled.cancel(true);
			}
			
			_currentTasks.remove(this);
		}
	}
	
	private void initializate()
	{
		registerTask(new TaskBirthday());
		registerTask(new TaskClanLeaderApply());
		registerTask(new TaskCleanUp());
		registerTask(new TaskDailySkillReuseClean());
		registerTask(new TaskNevit());
		registerTask(new TaskRaidPointsReset());
		registerTask(new TaskRecom());
		registerTask(new TaskRestart());
		registerTask(new TaskSevenSignsUpdate());
		registerTask(new TaskShutdown());
	}
	
	private void registerTask(PersistentTask task)
	{
		_tasks.computeIfAbsent(task.getName().hashCode(), _ ->
		{
			task.initializate();
			return task;
		});
	}
	
	private void startAllTasks()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SQL_STATEMENTS[0]);
			ResultSet rset = statement.executeQuery())
		{
			while (rset.next())
			{
				final PersistentTask task = _tasks.get(rset.getString("task").trim().toLowerCase().hashCode());
				if (task == null)
				{
					continue;
				}
				
				final PersistentTaskType type = PersistentTaskType.valueOf(rset.getString("type"));
				if (type != TYPE_NONE)
				{
					final ExecutedTask current = new ExecutedTask(task, type, rset);
					if (launchTask(current))
					{
						_currentTasks.add(current);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e, () -> getClass().getSimpleName() + ": Error while loading Global Task table: " + e.getMessage());
		}
	}
	
	private boolean launchTask(ExecutedTask task)
	{
		final PersistentTaskType type = task.getType();
		long delay;
		long interval;
		switch (type)
		{
			case TYPE_STARTUP:
			{
				task.run();
				return false;
			}
			case TYPE_SHEDULED:
			{
				delay = Long.parseLong(task.getParams()[0]);
				task.scheduled = ThreadPool.schedule(task, delay);
				return true;
			}
			case TYPE_FIXED_SHEDULED:
			{
				delay = Long.parseLong(task.getParams()[0]);
				interval = Long.parseLong(task.getParams()[1]);
				task.scheduled = ThreadPool.scheduleAtFixedRate(task, delay, interval);
				return true;
			}
			case TYPE_TIME:
			{
				try
				{
					final Date desired = DateFormat.getInstance().parse(task.getParams()[0]);
					final long diff = desired.getTime() - System.currentTimeMillis();
					if (diff >= 0)
					{
						task.scheduled = ThreadPool.schedule(task, diff);
						return true;
					}
					LOGGER.info(getClass().getSimpleName() + ": Task " + task.getId() + " is obsoleted.");
				}
				catch (Exception _)
				{
					// Ignore.
				}
				break;
			}
			case TYPE_SPECIAL:
			{
				final ScheduledFuture<?> result = task.getTask().launchSpecial(task);
				if (result != null)
				{
					task.scheduled = result;
					return true;
				}
				break;
			}
			case TYPE_GLOBAL_TASK:
			{
				interval = Long.parseLong(task.getParams()[0]) * 86400000;
				final String[] hour = task.getParams()[1].split(":");
				
				if (hour.length != 3)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Task " + task.getId() + " has incorrect parameters");
					return false;
				}
				
				final Calendar check = Calendar.getInstance();
				check.setTimeInMillis(task.getLastActivation() + interval);
				
				final Calendar min = Calendar.getInstance();
				try
				{
					min.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour[0]));
					min.set(Calendar.MINUTE, Integer.parseInt(hour[1]));
					min.set(Calendar.SECOND, Integer.parseInt(hour[2]));
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Bad parameter on task " + task.getId() + ": " + e.getMessage());
					return false;
				}
				
				delay = min.getTimeInMillis() - System.currentTimeMillis();
				
				if (check.after(min) || (delay < 0))
				{
					delay += interval;
				}
				task.scheduled = ThreadPool.scheduleAtFixedRate(task, delay, interval);
				return true;
			}
			default:
			{
				return false;
			}
		}
		return false;
	}
	
	public static boolean addUniqueTask(String task, PersistentTaskType type, String param1, String param2, String param3)
	{
		return addUniqueTask(task, type, param1, param2, param3, 0);
	}
	
	private static boolean addUniqueTask(String task, PersistentTaskType type, String param1, String param2, String param3, long lastActivation)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps1 = con.prepareStatement(SQL_STATEMENTS[2]))
		{
			ps1.setString(1, task);
			try (ResultSet rs = ps1.executeQuery())
			{
				if (!rs.next())
				{
					try (PreparedStatement ps2 = con.prepareStatement(SQL_STATEMENTS[3]))
					{
						ps2.setString(1, task);
						ps2.setString(2, type.toString());
						ps2.setLong(3, lastActivation);
						ps2.setString(4, param1);
						ps2.setString(5, param2);
						ps2.setString(6, param3);
						ps2.execute();
					}
				}
			}
			return true;
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, e, () -> PersistentTaskManager.class.getSimpleName() + ": Cannot add the unique task: " + e.getMessage());
		}
		return false;
	}
	
	public static boolean addTask(String task, PersistentTaskType type, String param1, String param2, String param3)
	{
		return addTask(task, type, param1, param2, param3, 0);
	}
	
	private static boolean addTask(String task, PersistentTaskType type, String param1, String param2, String param3, long lastActivation)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SQL_STATEMENTS[3]))
		{
			statement.setString(1, task);
			statement.setString(2, type.toString());
			statement.setLong(3, lastActivation);
			statement.setString(4, param1);
			statement.setString(5, param2);
			statement.setString(6, param3);
			statement.execute();
			return true;
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, e, () -> PersistentTaskManager.class.getSimpleName() + ": Cannot add the task: " + e.getMessage());
		}
		return false;
	}
	
	public static PersistentTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PersistentTaskManager INSTANCE = new PersistentTaskManager();
	}
}