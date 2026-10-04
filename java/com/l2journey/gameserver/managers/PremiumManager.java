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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.events.Containers;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.ListenersContainer;
import com.l2journey.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import com.l2journey.gameserver.model.events.holders.actor.player.OnPlayerLogout;
import com.l2journey.gameserver.model.events.listeners.ConsumerEventListener;
import com.l2journey.gameserver.network.serverpackets.ExBrPremiumState;
import com.l2journey.gameserver.network.serverpackets.PremiumState;

/**
 * @author Mobius
 */
public class PremiumManager
{
	private static final Logger LOGGER = Logger.getLogger(PremiumManager.class.getName());
	
	private static final String LOAD_SQL = "SELECT account_name,enddate FROM account_premium WHERE account_name = ?";
	private static final String UPDATE_SQL = "REPLACE INTO account_premium (account_name,enddate) VALUE (?,?)";
	private static final String DELETE_SQL = "DELETE FROM account_premium WHERE account_name = ?";
	
	class PremiumExpireTask implements Runnable
	{
		final Player _player;
		
		PremiumExpireTask(Player player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			_player.setPremiumStatus(false);
			_player.sendPacket(new PremiumState(_player.getObjectId(), 0));
			_player.sendPacket(new ExBrPremiumState(_player.getObjectId(), 0));
		}
	}
	
	// Data Cache
	private final Map<String, Long> _premiumData = new ConcurrentHashMap<>();
	
	// expireTasks
	private final Map<String, ScheduledFuture<?>> _expiretasks = new ConcurrentHashMap<>();
	
	// Listeners
	private final ListenersContainer _listenerContainer = Containers.Players();
	
	private final Consumer<OnPlayerLogin> _playerLoginEvent = event ->
	{
		final Player player = event.getPlayer();
		final String accountName = player.getAccountName();
		loadPremiumData(accountName);
		final long now = System.currentTimeMillis();
		final long premiumExpiration = getPremiumExpiration(accountName);
		player.setPremiumStatus(premiumExpiration > now);
		if (player.hasPremiumStatus())
		{
			startExpireTask(player, premiumExpiration - now);
			player.sendPacket(new PremiumState(player.getObjectId(), 1));
			player.sendPacket(new ExBrPremiumState(player.getObjectId(), 1));
		}
		else if (premiumExpiration > 0)
		{
			removePremiumStatus(accountName, false);
		}
	};
	
	private final Consumer<OnPlayerLogout> _playerLogoutEvent = event ->
	{
		stopExpireTask(event.getPlayer());
	};
	
	protected PremiumManager()
	{
		_listenerContainer.addListener(new ConsumerEventListener(_listenerContainer, EventType.ON_PLAYER_LOGIN, _playerLoginEvent, this));
		_listenerContainer.addListener(new ConsumerEventListener(_listenerContainer, EventType.ON_PLAYER_LOGOUT, _playerLogoutEvent, this));
	}
	
	/**
	 * @param player
	 * @param delay
	 */
	private void startExpireTask(Player player, long delay)
	{
		_expiretasks.put(player.getAccountName().toLowerCase(), ThreadPool.schedule(new PremiumExpireTask(player), delay));
	}
	
	/**
	 * @param player
	 */
	private void stopExpireTask(Player player)
	{
		ScheduledFuture<?> task = _expiretasks.remove(player.getAccountName().toLowerCase());
		if (task != null)
		{
			task.cancel(false);
		}
	}
	
	private void loadPremiumData(String accountName)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement stmt = con.prepareStatement(LOAD_SQL))
		{
			stmt.setString(1, accountName.toLowerCase());
			try (ResultSet rset = stmt.executeQuery())
			{
				while (rset.next())
				{
					_premiumData.put(rset.getString(1).toLowerCase(), rset.getLong(2));
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("Problem with PremiumManager: " + e.getMessage());
		}
	}
	
	public long getPremiumExpiration(String accountName)
	{
		return _premiumData.getOrDefault(accountName.toLowerCase(), 0L);
	}
	
	public void addPremiumTime(String accountName, int timeValue, TimeUnit timeUnit)
	{
		final long addTime = timeUnit.toMillis(timeValue);
		final long now = System.currentTimeMillis();
		// new premium task at least from now
		final long oldPremiumExpiration = Math.max(now, getPremiumExpiration(accountName));
		final long newPremiumExpiration = oldPremiumExpiration + addTime;
		
		// UPDATE DATABASE
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement stmt = con.prepareStatement(UPDATE_SQL))
		{
			stmt.setString(1, accountName.toLowerCase());
			stmt.setLong(2, newPremiumExpiration);
			stmt.execute();
		}
		catch (SQLException e)
		{
			LOGGER.warning("Problem with PremiumManager: " + e.getMessage());
		}
		
		// UPDATE CACHE
		_premiumData.put(accountName.toLowerCase(), newPremiumExpiration);
		
		// UPDATE PlAYER PREMIUMSTATUS
		for (Player player : World.getInstance().getPlayers())
		{
			if (accountName.equalsIgnoreCase(player.getAccountName()))
			{
				stopExpireTask(player);
				startExpireTask(player, newPremiumExpiration - now);
				if (!player.hasPremiumStatus())
				{
					player.setPremiumStatus(true);
					player.sendPacket(new PremiumState(player.getObjectId(), 1));
					player.sendPacket(new ExBrPremiumState(player.getObjectId(), 1));
				}
				
				break;
			}
		}
	}
	
	public void removePremiumStatus(String accountName, boolean checkOnline)
	{
		if (checkOnline)
		{
			for (Player player : World.getInstance().getPlayers())
			{
				if (accountName.equalsIgnoreCase(player.getAccountName()) && player.hasPremiumStatus())
				{
					player.setPremiumStatus(false);
					player.sendPacket(new PremiumState(player.getObjectId(), 0));
					player.sendPacket(new ExBrPremiumState(player.getObjectId(), 0));
					stopExpireTask(player);
					
					break;
				}
			}
		}
		
		// UPDATE CACHE
		_premiumData.remove(accountName.toLowerCase());
		
		// UPDATE DATABASE
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement stmt = con.prepareStatement(DELETE_SQL))
		{
			stmt.setString(1, accountName.toLowerCase());
			stmt.execute();
		}
		catch (SQLException e)
		{
			LOGGER.warning("Problem with PremiumManager: " + e.getMessage());
		}
	}
	
	public static PremiumManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PremiumManager INSTANCE = new PremiumManager();
	}
}