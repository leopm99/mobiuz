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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.EventsConfig;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.data.sql.CharInfoTable;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;
import com.l2journey.gameserver.util.Broadcast;

/**
 * Manager for the Hitman Event system.<br>
 * Players can place bounties on other players' heads.<br>
 * When a target is killed, the assassin receives the bounty reward.
 * @author L2Journey, KingHanker
 */
public class HitmanManager
{
	private static final Logger LOGGER = Logger.getLogger(HitmanManager.class.getName());
	
	// SQL Queries
	private static final String SELECT_ALL = "SELECT target_id, client_id, target_name, item_id, bounty, pending_delete FROM hitman_list";
	private static final String INSERT_TARGET = "REPLACE INTO hitman_list (target_id, client_id, target_name, item_id, bounty, pending_delete) VALUES (?, ?, ?, ?, ?, ?)";
	private static final String DELETE_TARGET = "DELETE FROM hitman_list WHERE target_id = ?";
	
	// Number formatter for display
	private static final DecimalFormat FORMATTER = new DecimalFormat(",##0,000");
	
	// Target data storage
	private final Map<Integer, HitmanTarget> _targets = new ConcurrentHashMap<>();
	
	// Currency mapping (name -> itemId)
	private final Map<String, Integer> _currency = new HashMap<>();
	
	// Save task
	private ScheduledFuture<?> _saveTask;
	
	/**
	 * Protected constructor - use getInstance()
	 */
	protected HitmanManager()
	{
		if (EventsConfig.HITMAN_ENABLED)
		{
			loadTargets();
			loadCurrency();
			scheduleSaveTask();
			LOGGER.info(getClass().getSimpleName() + ": Hitman Event Manager initialized.");
		}
	}
	
	/**
	 * Load all active targets from database.
	 */
	private void loadTargets()
	{
		_targets.clear();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SELECT_ALL);
			ResultSet rs = ps.executeQuery())
		{
			while (rs.next())
			{
				final int targetId = rs.getInt("target_id");
				final int clientId = rs.getInt("client_id");
				final String targetName = rs.getString("target_name");
				final int itemId = rs.getInt("item_id");
				final long bounty = rs.getLong("bounty");
				final boolean pendingDelete = rs.getInt("pending_delete") == 1;
				
				if (pendingDelete)
				{
					removeTarget(targetId, false);
				}
				else
				{
					_targets.put(targetId, new HitmanTarget(targetId, clientId, targetName, itemId, bounty));
				}
			}
			LOGGER.info(() -> getClass().getSimpleName() + ": Loaded " + _targets.size() + " active assassination targets.");
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Error loading hitman targets.");
		}
	}
	
	/**
	 * Load accepted currency items.
	 */
	private void loadCurrency()
	{
		_currency.clear();
		for (int itemId : EventsConfig.HITMAN_CURRENCY)
		{
			final var template = ItemData.getInstance().getTemplate(itemId);
			if (template != null)
			{
				final String name = template.getName().trim().replace(" ", "_");
				_currency.put(name, itemId);
			}
		}
	}
	
	/**
	 * Schedule the periodic save task.
	 */
	private void scheduleSaveTask()
	{
		final long interval = EventsConfig.HITMAN_SAVE_INTERVAL * 60000L;
		_saveTask = ThreadPool.scheduleAtFixedRate(this::saveAndCleanup, interval, interval);
	}
	
	/**
	 * Save targets and cleanup pending deletes.
	 */
	private void saveAndCleanup()
	{
		// Remove pending delete targets
		for (HitmanTarget target : _targets.values())
		{
			if (target.isPendingDelete())
			{
				removeTarget(target.getTargetId(), true);
			}
		}
		save();
	}
	
	/**
	 * Save all targets to database.
	 */
	public void save()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(INSERT_TARGET))
		{
			for (HitmanTarget target : _targets.values())
			{
				ps.setInt(1, target.getTargetId());
				ps.setInt(2, target.getClientId());
				ps.setString(3, target.getTargetName());
				ps.setInt(4, target.getItemId());
				ps.setLong(5, target.getBounty());
				ps.setInt(6, target.isPendingDelete() ? 1 : 0);
				ps.addBatch();
			}
			ps.executeBatch();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Error saving hitman targets.");
		}
	}
	
	/**
	 * Called when a player enters the world.
	 * @param player the player
	 */
	public void onEnterWorld(Player player)
	{
		if (!EventsConfig.HITMAN_ENABLED)
		{
			return;
		}
		
		// Notify if player has a bounty on their head
		if (_targets.containsKey(player.getObjectId()))
		{
			player.sendMessage("Warning: There is a bounty on your head! Watch your back!");
		}
		
		// Check player's active contracts
		final List<Integer> playerContracts = player.getHitmanTargets();
		if ((playerContracts != null) && !playerContracts.isEmpty())
		{
			for (int charId : new ArrayList<>(playerContracts))
			{
				if (!_targets.containsKey(charId))
				{
					final String targetName = CharInfoTable.getInstance().getNameById(charId);
					player.sendMessage("Your target " + (targetName != null ? targetName : "Unknown") + " has been eliminated by someone else.");
					player.removeHitmanTarget(charId);
				}
				else
				{
					final HitmanTarget target = _targets.get(charId);
					player.sendMessage("Your target " + target.getTargetName() + " is still alive. Bounty: " + formatNumber(target.getBounty()) + " " + getCurrencyName(target.getItemId()));
				}
			}
		}
	}
	
	/**
	 * Called when a player kills another player.
	 * @param killer the killer
	 * @param victim the victim
	 */
	public void onPlayerKill(Player killer, Player victim)
	{
		if (!EventsConfig.HITMAN_ENABLED || (killer == null) || (victim == null) || !_targets.containsKey(victim.getObjectId()))
		{
			return;
		}
		
		final HitmanTarget target = _targets.get(victim.getObjectId());
		
		// Check same team restriction
		if (!EventsConfig.HITMAN_SAME_TEAM)
		{
			final int killerClanId = killer.getClanId();
			final int killerAllyId = killer.getAllyId();
			
			if (((killerClanId != 0) && (killerClanId == victim.getClanId())) || ((killerAllyId != 0) && (killerAllyId == victim.getAllyId())))
			{
				killer.sendMessage("You cannot collect a bounty on a clan/ally member!");
				return;
			}
		}
		
		// Notify the victim
		victim.sendMessage("You have been assassinated by " + killer.getName() + " for a bounty!");
		
		// Notify the contract owner (if online)
		final Player client = World.getInstance().getPlayer(target.getClientId());
		if (client != null)
		{
			client.sendMessage("Your target " + victim.getName() + " has been assassinated by " + killer.getName() + "!");
			client.removeHitmanTarget(victim.getObjectId());
		}
		
		// Notify and reward the assassin
		killer.sendMessage("You have completed the assassination of " + victim.getName() + "!");
		
		rewardAssassin(killer, victim, target.getItemId(), target.getBounty());
		
		// Remove the target
		removeTarget(target.getTargetId(), true);
	}
	
	/**
	 * Reward the assassin with the bounty.
	 * @param assassin the assassin
	 * @param victim the victim
	 * @param itemId the reward item ID
	 * @param bounty the bounty amount
	 */
	private void rewardAssassin(Player assassin, Player victim, int itemId, long bounty)
	{
		final var itemTemplate = ItemData.getInstance().getTemplate(itemId);
		
		if ((itemTemplate != null) && itemTemplate.isStackable())
		{
			// addItem with sendMessage=true already notifies the player
			assassin.addItem(ItemProcessType.REWARD, itemId, bounty, victim, true);
		}
		else
		{
			for (int i = 0; i < bounty; i++)
			{
				assassin.addItem(ItemProcessType.REWARD, itemId, 1, victim, true);
			}
		}
	}
	
	/**
	 * Place a bounty on a player.
	 * @param client the player placing the bounty
	 * @param targetName the name of the target
	 * @param bounty the bounty amount
	 * @param itemId the item ID for payment
	 * @return true if successful
	 */
	public boolean putHitOn(Player client, String targetName, long bounty, int itemId)
	{
		// Check target limit
		final List<Integer> clientTargets = client.getHitmanTargets();
		if ((clientTargets != null) && (clientTargets.size() >= EventsConfig.HITMAN_TARGETS_LIMIT))
		{
			client.sendMessage("You have reached the maximum number of active contracts (" + EventsConfig.HITMAN_TARGETS_LIMIT + ").");
			return false;
		}
		
		// Check minimum bounty
		if (bounty < EventsConfig.HITMAN_MIN_BOUNTY)
		{
			client.sendMessage("Minimum bounty amount is " + formatNumber(EventsConfig.HITMAN_MIN_BOUNTY) + ".");
			return false;
		}
		
		// Check if client has enough items
		if (client.getInventory().getInventoryItemCount(itemId, -1) < bounty)
		{
			client.sendPacket(new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS));
			return false;
		}
		
		// Check if target exists
		final int targetId = CharInfoTable.getInstance().getIdByName(targetName);
		if (targetId <= 0)
		{
			client.sendMessage("Player \"" + targetName + "\" does not exist.");
			return false;
		}
		
		// Cannot put hit on yourself
		if (targetId == client.getObjectId())
		{
			client.sendMessage("You cannot place a bounty on yourself!");
			return false;
		}
		
		// Check if target already has a bounty
		if (_targets.containsKey(targetId))
		{
			client.sendMessage("There is already a bounty on " + targetName + ".");
			return false;
		}
		
		// Take the payment
		client.destroyItemByItemId(ItemProcessType.FEE, itemId, bounty, client, true);
		
		// Create the target entry
		_targets.put(targetId, new HitmanTarget(targetId, client.getObjectId(), targetName, itemId, bounty));
		client.addHitmanTarget(targetId);
		
		// Notify the target if online
		final Player target = World.getInstance().getPlayer(targetId);
		if (target != null)
		{
			target.sendMessage("Someone has placed a bounty on your head! Watch out for assassins!");
		}
		
		// Announce if enabled
		if (EventsConfig.HITMAN_ANNOUNCE)
		{
			final String message = client.getName() + " has placed a bounty of " + formatNumber(bounty) + " " + getCurrencyName(itemId) + " on " + targetName + "!";
			Broadcast.toAllOnlinePlayers(message);
		}
		
		client.sendMessage("You have placed a bounty of " + formatNumber(bounty) + " " + getCurrencyName(itemId) + " on " + targetName + ".");
		return true;
	}
	
	/**
	 * Cancel an assassination contract.
	 * @param client the player who owns the contract
	 * @param targetName the name of the target
	 * @return true if successful
	 */
	public boolean cancelContract(Player client, String targetName)
	{
		final List<Integer> clientTargets = client.getHitmanTargets();
		if ((clientTargets == null) || clientTargets.isEmpty())
		{
			client.sendMessage("You don't have any active contracts.");
			return false;
		}
		
		final int targetId = CharInfoTable.getInstance().getIdByName(targetName);
		if (targetId <= 0)
		{
			client.sendMessage("Player \"" + targetName + "\" does not exist.");
			return false;
		}
		
		if (!clientTargets.contains(targetId))
		{
			client.sendMessage("You don't have a contract on " + targetName + ".");
			return false;
		}
		
		final HitmanTarget target = _targets.get(targetId);
		if ((target == null) || (target.getClientId() != client.getObjectId()))
		{
			client.sendMessage("This is not your contract.");
			return false;
		}
		
		// Remove the contract (no refund)
		client.removeHitmanTarget(targetId);
		removeTarget(targetId, true);
		
		client.sendMessage("You have cancelled the contract on " + targetName + ". No refund will be given.");
		
		// Notify target if online
		final Player targetPlayer = World.getInstance().getPlayer(targetId);
		if (targetPlayer != null)
		{
			targetPlayer.sendMessage("The bounty on your head has been cancelled.");
		}
		
		return true;
	}
	
	/**
	 * Remove a target from the list.
	 * @param targetId the target object ID
	 * @param live whether to also remove from database
	 */
	public void removeTarget(int targetId, boolean live)
	{
		if (live)
		{
			_targets.remove(targetId);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_TARGET))
		{
			ps.setInt(1, targetId);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, e, () -> getClass().getSimpleName() + ": Error removing target " + targetId);
		}
	}
	
	/**
	 * Get a target by object ID.
	 * @param objectId the object ID
	 * @return the target or null
	 */
	public HitmanTarget getTarget(int objectId)
	{
		return _targets.get(objectId);
	}
	
	/**
	 * Check if a player is a hitman target.
	 * @param objectId the object ID
	 * @return true if the player is a target
	 */
	public boolean isTarget(int objectId)
	{
		final HitmanTarget target = _targets.get(objectId);
		return (target != null) && !target.isPendingDelete();
	}
	
	/**
	 * Get all targets.
	 * @return map of targets
	 */
	public Map<Integer, HitmanTarget> getTargets()
	{
		return _targets;
	}
	
	/**
	 * Get all online targets.
	 * @return map of online targets
	 */
	public Map<Integer, HitmanTarget> getOnlineTargets()
	{
		final Map<Integer, HitmanTarget> online = new HashMap<>();
		for (HitmanTarget target : _targets.values())
		{
			if (!target.isPendingDelete() && (World.getInstance().getPlayer(target.getTargetId()) != null))
			{
				online.put(target.getTargetId(), target);
			}
		}
		return online;
	}
	
	/**
	 * Get accepted currency map.
	 * @return currency map (name -> itemId)
	 */
	public Map<String, Integer> getCurrencyMap()
	{
		return _currency;
	}
	
	/**
	 * Get currency item ID by name.
	 * @param name the currency name
	 * @return the item ID or null
	 */
	public Integer getCurrencyId(String name)
	{
		return _currency.get(name);
	}
	
	/**
	 * Get currency name by item ID.
	 * @param itemId the item ID
	 * @return the item name
	 */
	public String getCurrencyName(int itemId)
	{
		final var template = ItemData.getInstance().getTemplate(itemId);
		return template != null ? template.getName() : "Unknown";
	}
	
	/**
	 * Format a number for display.
	 * @param number the number
	 * @return formatted string
	 */
	public static String formatNumber(long number)
	{
		return number > 999 ? FORMATTER.format(number) : String.valueOf(number);
	}
	
	/**
	 * Shutdown the manager.
	 */
	public void shutdown()
	{
		if (_saveTask != null)
		{
			_saveTask.cancel(false);
			_saveTask = null;
		}
		save();
	}
	
	/**
	 * Get the singleton instance.
	 * @return the instance
	 */
	public static HitmanManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final HitmanManager INSTANCE = new HitmanManager();
	}
	
	/**
	 * Represents a hitman target (player with a bounty).
	 */
	public static class HitmanTarget
	{
		private final int _targetId;
		private final int _clientId;
		private final String _targetName;
		private final int _itemId;
		private long _bounty;
		private boolean _pendingDelete;
		
		public HitmanTarget(int targetId, int clientId, String targetName, int itemId, long bounty)
		{
			_targetId = targetId;
			_clientId = clientId;
			_targetName = targetName;
			_itemId = itemId;
			_bounty = bounty;
			_pendingDelete = false;
		}
		
		public int getTargetId()
		{
			return _targetId;
		}
		
		public int getClientId()
		{
			return _clientId;
		}
		
		public String getTargetName()
		{
			return _targetName;
		}
		
		public int getItemId()
		{
			return _itemId;
		}
		
		public long getBounty()
		{
			return _bounty;
		}
		
		public void setBounty(long bounty)
		{
			_bounty = bounty;
		}
		
		public void addBounty(long amount)
		{
			_bounty += amount;
		}
		
		public boolean isPendingDelete()
		{
			return _pendingDelete;
		}
		
		public void setPendingDelete(boolean value)
		{
			_pendingDelete = value;
		}
		
		public boolean isOnline()
		{
			return World.getInstance().getPlayer(_targetId) != null;
		}
	}
}
