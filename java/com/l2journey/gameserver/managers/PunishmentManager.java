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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.LoginServerThread;
import com.l2journey.gameserver.data.holders.PunishmentHolder;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.IllegalActionPunishmentType;
import com.l2journey.gameserver.model.actor.tasks.player.IllegalPlayerActionTask;
import com.l2journey.gameserver.model.punishment.PunishmentAffect;
import com.l2journey.gameserver.model.punishment.PunishmentTask;
import com.l2journey.gameserver.model.punishment.PunishmentType;

/**
 * @author UnAfraid, Skache
 */
public class PunishmentManager
{
	private static final Logger LOGGER = Logger.getLogger(PunishmentManager.class.getName());
	
	private final Map<PunishmentAffect, PunishmentHolder> _tasks = new ConcurrentHashMap<>();
	
	protected PunishmentManager()
	{
		load();
	}
	
	private void load()
	{
		// Initiate task holders.
		for (PunishmentAffect affect : PunishmentAffect.values())
		{
			_tasks.put(affect, new PunishmentHolder());
		}
		
		int initiated = 0;
		int expired = 0;
		
		// Load punishments.
		try (Connection con = DatabaseFactory.getConnection();
			Statement st = con.createStatement();
			ResultSet rset = st.executeQuery("SELECT id, `key`, affect, type, expiration, reason, punishedBy FROM punishments"))
		{
			while (rset.next())
			{
				final int id = rset.getInt("id");
				final String key = rset.getString("key");
				final PunishmentAffect affect = PunishmentAffect.getByName(rset.getString("affect"));
				final PunishmentType type = PunishmentType.getByName(rset.getString("type"));
				final long expirationTime = rset.getLong("expiration");
				if ((type != null) && (affect != null))
				{
					if ((expirationTime > 0) && (System.currentTimeMillis() > expirationTime)) // expired task.
					{
						expired++;
					}
					else
					{
						initiated++;
						_tasks.get(affect).addPunishment(new PunishmentTask(id, key, affect, type, expirationTime, rset.getString("reason"), rset.getString("punishedBy"), true));
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Error while loading punishments: " + e.getMessage());
		}
		
		final int loggedExpired = expired;
		final int loggedInitiated = initiated;
		LOGGER.info(() -> getClass().getSimpleName() + ": Loaded " + loggedInitiated + " active and " + loggedExpired + " loggedExpired punishments.");
	}
	
	public void startPunishment(PunishmentTask task)
	{
		final PunishmentAffect affect = task.getAffect();
		if (task.getType() == PunishmentType.BAN)
		{
			switch (affect)
			{
				case ACCOUNT:
				{
					final String accountName = (String) task.getKey();
					LoginServerThread.getInstance().sendAccessLevel(accountName, -1);
					break;
				}
				case CHARACTER:
				{
					final String charId = String.valueOf(task.getKey());
					changeCharAccessLevel(charId, -1);
					break;
				}
			}
		}
		
		_tasks.get(affect).addPunishment(task);
	}
	
	public void stopPunishment(Object key, PunishmentAffect affect, PunishmentType type)
	{
		final PunishmentTask task = getPunishment(key, affect, type);
		if (task == null)
		{
			return;
		}
		
		if (type == PunishmentType.BAN)
		{
			switch (affect)
			{
				case ACCOUNT:
				{
					final String accountName = (String) task.getKey();
					LoginServerThread.getInstance().sendAccessLevel(accountName, 0);
					break;
				}
				case CHARACTER:
				{
					final String charId = String.valueOf(task.getKey());
					changeCharAccessLevel(charId, 0);
					break;
				}
			}
		}
		
		_tasks.get(affect).stopPunishment(task);
	}
	
	private void changeCharAccessLevel(String charId, int lvl)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE characters SET accessLevel=? WHERE charId=?"))
		{
			ps.setInt(1, lvl);
			ps.setString(2, charId);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Error updating access level for character: " + charId + ". " + e.getMessage());
		}
	}
	
	public boolean hasPunishment(Object key, PunishmentAffect affect, PunishmentType type)
	{
		final PunishmentHolder holder = _tasks.get(affect);
		return holder.hasPunishment(String.valueOf(key), type);
	}
	
	public long getPunishmentExpiration(Object key, PunishmentAffect affect, PunishmentType type)
	{
		final PunishmentTask p = getPunishment(key, affect, type);
		return p != null ? p.getExpirationTime() : 0;
	}
	
	private PunishmentTask getPunishment(Object key, PunishmentAffect affect, PunishmentType type)
	{
		return _tasks.get(affect).getPunishment(String.valueOf(key), type);
	}
	
	public static void handleIllegalPlayerAction(Player actor, String message, IllegalActionPunishmentType punishment)
	{
		ThreadPool.schedule(new IllegalPlayerActionTask(actor, message, punishment), 5000);
	}
	
	public static PunishmentManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PunishmentManager INSTANCE = new PunishmentManager();
	}
}
