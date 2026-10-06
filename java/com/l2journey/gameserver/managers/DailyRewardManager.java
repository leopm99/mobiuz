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
package com.l2journey.gameserver.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.util.IXmlReader;
import com.l2journey.gameserver.data.holders.DailyRewardHolder;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;

/**
 * Manager for the Daily Reward system. Players can claim rewards once per day by logging in. The rewards cycle through a configurable number of days.
 * @author L2Journey
 */
public class DailyRewardManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(DailyRewardManager.class.getName());
	
	// SQL Queries
	private static final String SELECT_ALL = "SELECT account_name, last_reward_time, current_day FROM daily_reward_history";
	private static final String SELECT_PLAYER = "SELECT account_name FROM daily_reward_history WHERE account_name = ?";
	private static final String INSERT_PLAYER = "INSERT INTO daily_reward_history (account_name, last_reward_time, current_day) VALUES (?, ?, ?)";
	private static final String UPDATE_PLAYER = "UPDATE daily_reward_history SET last_reward_time = ?, current_day = ? WHERE account_name = ?";
	private static final String RESET_ALL_DAYS = "UPDATE daily_reward_history SET current_day = 1";
	
	// Reward data
	private final List<DailyRewardHolder> _rewards = new ArrayList<>();
	
	// Player reward tracking (accountName -> lastRewardTime)
	private final Map<String, Long> _rewardedPlayers = new ConcurrentHashMap<>();
	
	// Player current day tracking (accountName -> currentDay)
	private final Map<String, Integer> _playerDays = new ConcurrentHashMap<>();
	
	// Reset task
	private ScheduledFuture<?> _resetTask;
	
	// Reward type
	private RewardCheckType _checkType = RewardCheckType.ACCOUNT;
	
	// Reset hour (0-23)
	private int _resetHour = 6;
	private int _resetMinute = 30;
	
	/**
	 * Type of check for preventing abuse
	 */
	public enum RewardCheckType
	{
		ACCOUNT,
		CHARACTER,
		IP
	}
	
	protected DailyRewardManager()
	{
		if (Config.DAILY_REWARD_ENABLED)
		{
			load();
			loadPlayerData();
			scheduleResetTask();
		}
	}
	
	@Override
	public void load()
	{
		_rewards.clear();
		parseDatapackFile("data/DailyRewards.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _rewards.size() + " daily rewards.");
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		final Element root = document.getDocumentElement();
		
		// Parse settings
		if (root.hasAttribute("checkType"))
		{
			_checkType = RewardCheckType.valueOf(root.getAttribute("checkType").toUpperCase());
		}
		if (root.hasAttribute("resetHour"))
		{
			_resetHour = Integer.parseInt(root.getAttribute("resetHour"));
		}
		if (root.hasAttribute("resetMinute"))
		{
			_resetMinute = Integer.parseInt(root.getAttribute("resetMinute"));
		}
		
		// Parse rewards
		final NodeList dayNodes = root.getElementsByTagName("day");
		for (int i = 0; i < dayNodes.getLength(); i++)
		{
			final Node dayNode = dayNodes.item(i);
			if (dayNode.getNodeType() == Node.ELEMENT_NODE)
			{
				final Element dayElement = (Element) dayNode;
				final int dayNumber = Integer.parseInt(dayElement.getAttribute("number"));
				final String icon = dayElement.hasAttribute("icon") ? dayElement.getAttribute("icon") : "";
				
				final Map<Integer, Long> rewards = new HashMap<>();
				final NodeList rewardNodes = dayElement.getElementsByTagName("reward");
				for (int j = 0; j < rewardNodes.getLength(); j++)
				{
					final Element rewardElement = (Element) rewardNodes.item(j);
					final int itemId = Integer.parseInt(rewardElement.getAttribute("itemId"));
					final long count = Long.parseLong(rewardElement.getAttribute("count"));
					rewards.put(itemId, count);
				}
				
				final DailyRewardHolder holder = new DailyRewardHolder(dayNumber, rewards);
				if (!icon.isEmpty())
				{
					holder.setIcon(icon);
				}
				_rewards.add(holder);
			}
		}
		
		// Sort by day number
		_rewards.sort((a, b) -> Integer.compare(a.getDay(), b.getDay()));
	}
	
	/**
	 * Load player reward data from database.
	 */
	private void loadPlayerData()
	{
		_rewardedPlayers.clear();
		_playerDays.clear();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_ALL);
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				final String accountName = rs.getString("account_name");
				final long lastRewardTime = rs.getLong("last_reward_time");
				final int currentDay = rs.getInt("current_day");
				
				_rewardedPlayers.put(accountName, lastRewardTime);
				_playerDays.put(accountName, currentDay);
			}
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _rewardedPlayers.size() + " player reward records.");
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error loading player data.", e);
		}
	}
	
	/**
	 * Schedule the daily reset task.
	 */
	private void scheduleResetTask()
	{
		final long nextReset = getNextResetTime();
		final long delay = nextReset - System.currentTimeMillis();
		
		if (_resetTask != null)
		{
			_resetTask.cancel(false);
		}
		
		_resetTask = ThreadPool.schedule(this::performDailyReset, delay);
		
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(nextReset);
		LOGGER.info(getClass().getSimpleName() + ": Next daily reset scheduled for " + String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));
	}
	
	/**
	 * Calculate the next reset time.
	 * @return timestamp of next reset
	 */
	private long getNextResetTime()
	{
		final Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, _resetHour);
		cal.set(Calendar.MINUTE, _resetMinute);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		if (cal.getTimeInMillis() <= System.currentTimeMillis())
		{
			cal.add(Calendar.DAY_OF_MONTH, 1);
		}
		
		return cal.getTimeInMillis();
	}
	
	/**
	 * Perform the daily reset.
	 */
	private void performDailyReset()
	{
		LOGGER.info(getClass().getSimpleName() + ": Performing daily reset...");
		
		// Check if it's the first day of the month - reset all players to day 1
		final Calendar now = Calendar.getInstance();
		if (now.get(Calendar.DAY_OF_MONTH) == 1)
		{
			resetAllPlayersToDay1();
		}
		
		// Schedule next reset
		scheduleResetTask();
	}
	
	/**
	 * Reset all players' progress to day 1. Called on the first day of each month.
	 */
	private void resetAllPlayersToDay1()
	{
		LOGGER.info(getClass().getSimpleName() + ": First day of the month - Resetting all players to Day 1...");
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(RESET_ALL_DAYS))
		{
			final int updated = ps.executeUpdate();
			LOGGER.info(getClass().getSimpleName() + ": Reset " + updated + " player records to Day 1.");
			
			// Update in-memory data
			for (String accountName : _playerDays.keySet())
			{
				_playerDays.put(accountName, 1);
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error resetting player days.", e);
		}
	}
	
	/**
	 * Get the player identifier based on check type.
	 * @param player the player
	 * @return the identifier string
	 */
	private String getPlayerIdentifier(Player player)
	{
		return switch (_checkType)
		{
			case CHARACTER -> String.valueOf(player.getObjectId());
			case IP -> player.getClient() != null ? player.getClient().getIp() : "";
			default -> player.getAccountName();
		};
	}
	
	/**
	 * Check if player can claim today's reward.
	 * @param player the player
	 * @return true if can claim
	 */
	public boolean canClaimReward(Player player)
	{
		final String identifier = getPlayerIdentifier(player);
		if (identifier.isEmpty())
		{
			return false;
		}
		
		final Long lastReward = _rewardedPlayers.get(identifier);
		if (lastReward == null)
		{
			return true;
		}
		
		// Check if last reward was before today's reset
		final Calendar todayReset = Calendar.getInstance();
		todayReset.set(Calendar.HOUR_OF_DAY, _resetHour);
		todayReset.set(Calendar.MINUTE, _resetMinute);
		todayReset.set(Calendar.SECOND, 0);
		todayReset.set(Calendar.MILLISECOND, 0);
		
		// If current time is before reset hour, use yesterday's reset
		if (System.currentTimeMillis() < todayReset.getTimeInMillis())
		{
			todayReset.add(Calendar.DAY_OF_MONTH, -1);
		}
		
		return lastReward < todayReset.getTimeInMillis();
	}
	
	/**
	 * Get current reward day for player.
	 * @param player the player
	 * @return current day (1-based)
	 */
	public int getCurrentDay(Player player)
	{
		final String identifier = getPlayerIdentifier(player);
		return _playerDays.getOrDefault(identifier, 1);
	}
	
	/**
	 * Get reward holder for a specific day.
	 * @param day the day number
	 * @return the reward holder, or null if not found
	 */
	public DailyRewardHolder getReward(int day)
	{
		for (DailyRewardHolder holder : _rewards)
		{
			if (holder.getDay() == day)
			{
				return holder;
			}
		}
		return null;
	}
	
	/**
	 * Get all daily rewards.
	 * @return list of reward holders
	 */
	public List<DailyRewardHolder> getRewards()
	{
		return _rewards;
	}
	
	/**
	 * Get total number of reward days in the cycle.
	 * @return number of days
	 */
	public int getTotalDays()
	{
		return _rewards.size();
	}
	
	/**
	 * Claim reward for player.
	 * @param player the player
	 * @return true if reward was claimed successfully
	 */
	public boolean claimReward(Player player)
	{
		if (!canClaimReward(player))
		{
			return false;
		}
		
		final String identifier = getPlayerIdentifier(player);
		final int currentDay = getCurrentDay(player);
		final DailyRewardHolder reward = getReward(currentDay);
		
		if (reward == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": No reward found for day " + currentDay);
			return false;
		}
		
		// Check inventory space
		if ((player.getInventory().getSize() + reward.getRewards().size()) > player.getInventoryLimit())
		{
			player.sendMessage("Not enough inventory space to claim reward.");
			return false;
		}
		
		// Give rewards
		for (Map.Entry<Integer, Long> entry : reward.getRewards().entrySet())
		{
			final int itemId = entry.getKey();
			final long count = entry.getValue();
			player.addItem(ItemProcessType.REWARD, itemId, count, player, true);
		}
		
		// Calculate next day (cycle back to 1 after max)
		final int nextDay = currentDay >= getTotalDays() ? 1 : currentDay + 1;
		
		// Update tracking
		final long now = System.currentTimeMillis();
		_rewardedPlayers.put(identifier, now);
		_playerDays.put(identifier, nextDay);
		
		// Save to database
		savePlayerData(identifier, now, nextDay);
		
		player.sendMessage("Daily reward claimed! Come back tomorrow for day " + nextDay + " reward.");
		
		return true;
	}
	
	/**
	 * Save player reward data to database.
	 * @param identifier player identifier
	 * @param lastRewardTime last reward timestamp
	 * @param currentDay current day in cycle
	 */
	private void savePlayerData(String identifier, long lastRewardTime, int currentDay)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			// Check if record exists
			try (PreparedStatement ps = con.prepareStatement(SELECT_PLAYER))
			{
				ps.setString(1, identifier);
				try (ResultSet rs = ps.executeQuery())
				{
					if (rs.next())
					{
						// Update existing record
						try (PreparedStatement updatePs = con.prepareStatement(UPDATE_PLAYER))
						{
							updatePs.setLong(1, lastRewardTime);
							updatePs.setInt(2, currentDay);
							updatePs.setString(3, identifier);
							updatePs.executeUpdate();
						}
					}
					else
					{
						// Insert new record
						try (PreparedStatement insertPs = con.prepareStatement(INSERT_PLAYER))
						{
							insertPs.setString(1, identifier);
							insertPs.setLong(2, lastRewardTime);
							insertPs.setInt(3, currentDay);
							insertPs.executeUpdate();
						}
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error saving player data.", e);
		}
	}
	
	/**
	 * Get the time remaining until player can claim next reward.
	 * @param player the player
	 * @return remaining time in milliseconds, or 0 if can claim now
	 */
	public long getTimeUntilNextReward(Player player)
	{
		if (canClaimReward(player))
		{
			return 0;
		}
		
		return getNextResetTime() - System.currentTimeMillis();
	}
	
	/**
	 * Format time remaining as string.
	 * @param millis time in milliseconds
	 * @return formatted string (HH:MM:SS)
	 */
	public static String formatTime(long millis)
	{
		final long seconds = millis / 1000;
		final long hours = seconds / 3600;
		final long minutes = (seconds % 3600) / 60;
		final long secs = seconds % 60;
		return String.format("%02d:%02d:%02d", hours, minutes, secs);
	}
	
	/**
	 * Get item name by ID.
	 * @param itemId the item ID
	 * @return item name
	 */
	public String getItemName(int itemId)
	{
		final var template = ItemData.getInstance().getTemplate(itemId);
		return template != null ? template.getName() : "Unknown Item";
	}
	
	/**
	 * Get item icon by ID.
	 * @param itemId the item ID
	 * @return icon path
	 */
	public String getItemIcon(int itemId)
	{
		final var template = ItemData.getInstance().getTemplate(itemId);
		return template != null ? template.getIcon() : "icon.NOIMAGE";
	}
	
	/**
	 * Called when server shuts down.
	 */
	public void shutdown()
	{
		if (_resetTask != null)
		{
			_resetTask.cancel(false);
			_resetTask = null;
		}
	}
	
	public static DailyRewardManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DailyRewardManager INSTANCE = new DailyRewardManager();
	}
}
