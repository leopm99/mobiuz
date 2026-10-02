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
package com.l2journey.gameserver.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.data.xml.TransformData;
import com.l2journey.gameserver.managers.CursedWeaponsManager;
import com.l2journey.gameserver.managers.MapRegionManager;
import com.l2journey.gameserver.model.actor.Attackable;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.groups.PartyMessageType;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.phantom.PhantomCursedHunt;
import com.l2journey.gameserver.model.skill.CommonSkill;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.variables.PlayerVariables;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.Earthquake;
import com.l2journey.gameserver.network.serverpackets.ExRedSky;
import com.l2journey.gameserver.network.serverpackets.SocialAction;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;
import com.l2journey.gameserver.util.Broadcast;

public class CursedWeapon
{
	private static final Logger LOGGER = Logger.getLogger(CursedWeapon.class.getName());

	// _name is the name of the cursed weapon associated with its ID.
	private final String _name;
	// _itemId is the Item ID of the cursed weapon.
	private final int _itemId;
	// _skillId is the skills ID.
	private final int _skillId;
	private final int _skillMaxLevel;
	private int _dropRate;
	private int _duration;
	private int _durationLost;
	private int _disapearChance;
	private int _stageKills;

	// this should be false unless if the cursed weapon is dropped, in that case it would be true.
	private boolean _isDropped = false;
	// this sets the cursed weapon status to true only if a player has the cursed weapon, otherwise this should be false.
	private boolean _isActivated = false;
	private ScheduledFuture<?> _removeTask;

	private int _nbKills = 0;
	long _endTime = 0;

	private int _playerId = 0;
	protected Player _player = null;
	private Item _item = null;
	private int _playerKarma = 0;
	private int _playerPkKills = 0;
	private int _lastKillerId = 0;
	protected int transformationId = 0;

	public CursedWeapon(int itemId, int skillId, String name)
	{
		_name = name;
		_itemId = itemId;
		_skillId = skillId;
		_skillMaxLevel = SkillData.getInstance().getMaxLevel(_skillId);
	}

	public void endOfLife()
	{
		if (_isActivated)
		{
			if ((_player != null) && _player.isOnline())
			{
				// Remove from player
				LOGGER.log(Level.INFO, () -> _name + " being removed from player " + _player.getName() + " online.");
				_player.abortAttack();

				_player.setKarma(_playerKarma);
				_player.setPkKills(_playerPkKills);
				_player.setCursedWeaponEquippedId(0);
				removeSkill();

				// Remove
				_player.getInventory().unEquipItemInBodySlot(ItemTemplate.SLOT_LR_HAND);
				_player.storeMe();

				// Destroy
				_player.getInventory().destroyItemByItemId(ItemProcessType.NONE, _itemId, 1, _player, null);
				_player.sendItemList(true);
				_player.broadcastUserInfo();
			}
			else
			{
				// Remove from Db
				LOGGER.log(Level.INFO, () -> _name + " being removed from the database offline.");

				try (Connection con = DatabaseFactory.getConnection();
					PreparedStatement del = con.prepareStatement("DELETE FROM items WHERE owner_id=? AND item_id=?");
					PreparedStatement ps = con.prepareStatement("UPDATE characters SET karma=?, pkkills=? WHERE charId=?"))
				{
					// Delete the item
					del.setInt(1, _playerId);
					del.setInt(2, _itemId);
					if (del.executeUpdate() != 1)
					{
						LOGGER.log(Level.WARNING, () -> "Error while deleting itemId " + _itemId + " from userId " + _playerId);
					}

					// Restore the karma
					ps.setInt(1, _playerKarma);
					ps.setInt(2, _playerPkKills);
					ps.setInt(3, _playerId);
					if (ps.executeUpdate() != 1)
					{
						LOGGER.log(Level.WARNING, () -> "Error while updating karma & pkkills for userId " + _playerId);
					}
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, "Could not delete.", e);
				}
			}
		}
		// either this cursed weapon is in the inventory of someone who has another cursed weapon equipped,
		// OR this cursed weapon is on the ground.
		else if ((_player != null) && (_player.getInventory().getItemByItemId(_itemId) != null))
		{
			// Destroy
			_player.getInventory().destroyItemByItemId(ItemProcessType.NONE, _itemId, 1, _player, null);
			_player.sendItemList(true);
			_player.broadcastUserInfo();
		}
		// is dropped on the ground
		else if (_item != null)
		{
			_item.decayMe();
			World.getInstance().removeObject(_item);
			LOGGER.log(Level.INFO, () -> _name + " item has been removed from World.");
		}

		// Delete infos from table if any
		CursedWeaponsManager.removeFromDb(_itemId);

		final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
		sm.addItemName(_itemId);
		CursedWeaponsManager.announce(sm);

		// Reset state
		cancelTask();
		_isActivated = false;
		_isDropped = false;
		_endTime = 0;
		_player = null;
		_playerId = 0;
		_playerKarma = 0;
		_playerPkKills = 0;
		_item = null;
		_nbKills = 0;
	}

	private void cancelTask()
	{
		if (_removeTask != null)
		{
			_removeTask.cancel(true);
			_removeTask = null;
		}
	}

	private class RemoveTask implements Runnable
	{
		protected RemoveTask()
		{
		}

		@Override
		public void run()
		{
			if (System.currentTimeMillis() >= _endTime)
			{
				endOfLife();
			}
		}
	}

	private void dropIt(Attackable attackable, Player player)
	{
		dropIt(attackable, player, null, true);
	}

	private void dropIt(Attackable attackable, Player player, Creature killer, boolean fromMonster)
	{
		_isActivated = false;
		if (fromMonster)
		{
			_item = attackable.dropItem(player, _itemId, 1);
			_item.setDropTime(0); // Prevent item from being removed by ItemsAutoDestroy

			// RedSky and Earthquake
			final ExRedSky rs = new ExRedSky(10);
			final Earthquake eq = new Earthquake(player.getX(), player.getY(), player.getZ(), 14, 3);
			Broadcast.toAllOnlinePlayers(rs);
			Broadcast.toAllOnlinePlayers(eq);

			final MapRegion mapRegion = MapRegionManager.getInstance().getMapRegion(player.getX(), player.getY());
			LOGGER.log(Level.INFO, () -> _name + " dropped from " + attackable.getName() + " near " + mapRegion.getTown() + " by " + player.getName() + ".");
		}
		else
		{
			_item = _player.getInventory().getItemByItemId(_itemId);
			_lastKillerId = ((killer != null) && killer.isPlayer()) ? killer.getObjectId() : 0;
			_player.dropItem(ItemProcessType.DEATH, _item, killer, true);
			_player.setKarma(_playerKarma);
			_player.setPkKills(_playerPkKills);
			_player.setCursedWeaponEquippedId(0);
			removeSkill();
			_player.abortAttack();
		}
		_isDropped = true;
		final SystemMessage sm = new SystemMessage(SystemMessageId.S2_WAS_DROPPED_IN_THE_S1_REGION);
		if (fromMonster)
		{
			sm.addZoneName(player.getX(), player.getY(), player.getZ()); // Region Name
		}
		else
		{
			sm.addZoneName(_player.getX(), _player.getY(), _player.getZ()); // Region Name
		}
		sm.addItemName(_itemId);
		CursedWeaponsManager.announce(sm); // i.e. "ItemName was dropped in the Hot Spring region"
	}

	public void cursedOnLogin()
	{
		doTransform();
		giveSkill();

		final SystemMessage msg = new SystemMessage(SystemMessageId.S2_S_OWNER_HAS_LOGGED_INTO_THE_S1_REGION);
		msg.addZoneName(_player.getX(), _player.getY(), _player.getZ());
		msg.addItemName(_player.getCursedWeaponEquippedId());
		CursedWeaponsManager.announce(msg);

		final CursedWeapon cw = CursedWeaponsManager.getInstance().getCursedWeapon(_player.getCursedWeaponEquippedId());
		final SystemMessage msg2 = new SystemMessage(SystemMessageId.S1_HAS_S2_MINUTE_S_OF_USAGE_TIME_REMAINING);
		final int timeLeft = (int) (cw.getTimeLeft() / 60000);
		msg2.addItemName(_player.getCursedWeaponEquippedId());
		msg2.addInt(timeLeft);
		_player.sendPacket(msg2);
	}

	/**
	 * Yesod:<br>
	 * Rebind the passive skill belonging to the CursedWeapon. Invoke this method if the weapon owner switches to a subclass.
	 */
	public void giveSkill()
	{
		int level = 1 + (_nbKills / _stageKills);
		if (level > _skillMaxLevel)
		{
			level = _skillMaxLevel;
		}

		final Skill skill = SkillData.getInstance().getSkill(_skillId, level);
		_player.addSkill(skill, false);

		// Void Burst, Void Flow
		_player.addSkill(CommonSkill.VOID_BURST.getSkill(), false);
		_player.addTransformSkill(CommonSkill.VOID_BURST.getSkill());
		_player.addSkill(CommonSkill.VOID_FLOW.getSkill(), false);
		_player.addTransformSkill(CommonSkill.VOID_FLOW.getSkill());
		_player.sendSkillList();
	}

	public void doTransform()
	{
		if (_itemId == 8689) // Akamanah
		{
			transformationId = 302;
		}
		else if (_itemId == 8190) // Zariche
		{
			transformationId = 301;
		}

		if (_player.isTransformed() || _player.isInStance())
		{
			_player.stopTransformation(true);

			ThreadPool.schedule(() -> TransformData.getInstance().transformPlayer(transformationId, _player), 500);
		}
		else
		{
			TransformData.getInstance().transformPlayer(transformationId, _player);
		}
	}

	public void removeSkill()
	{
		_player.removeSkill(_skillId);
		_player.removeSkill(CommonSkill.VOID_BURST.getSkill().getId());
		_player.removeSkill(CommonSkill.VOID_FLOW.getSkill().getId());
		_player.untransform();
		_player.sendSkillList();
	}

	public void reActivate()
	{
		_isActivated = true;
		if ((_endTime - System.currentTimeMillis()) <= 0)
		{
			endOfLife();
		}
		else
		{
			_removeTask = ThreadPool.scheduleAtFixedRate(new RemoveTask(), _durationLost * 12000L, _durationLost * 12000L);
		}
	}

	public boolean checkDrop(Attackable attackable, Player player)
	{
		if (Rnd.get(100000) < _dropRate)
		{
			// Drop the item
			dropIt(attackable, player);

			// Start the Life Task
			_endTime = System.currentTimeMillis() + (_duration * 60000);
			_removeTask = ThreadPool.scheduleAtFixedRate(new RemoveTask(), _durationLost * 12000L, _durationLost * 12000L);
			return true;
		}
		return false;
	}

	public void activate(Player player, Item item)
	{
		// If the player is mounted, attempt to unmount first.
		// Only allow picking up the cursed weapon if unmounting is successful.
		if (player.isMounted() && !player.dismount())
		{
			// TODO: Verify the following system message, may still be custom.
			player.sendPacket(SystemMessageId.YOU_HAVE_FAILED_TO_PICK_UP_S1);
			player.dropItem(ItemProcessType.DROP, item, null, true);
			return;
		}

		_isActivated = true;

		// Player holding it data
		_player = player;
		_playerId = _player.getObjectId();
		_playerKarma = _player.getKarma();
		_playerPkKills = _player.getPkKills();
		_lastKillerId = 0;
		saveData();

		try
		{
			final PlayerVariables vars = _player.getVariables();
			if (_itemId == 8190) // Zariche
			{
				vars.set("HAS_EVER_USED_ZARICHE", 1);
			}
			else if (_itemId == 8689) // Akamanah
			{
				vars.set("HAS_EVER_USED_AKAMANAH", 1);
			}
			vars.storeMe();
		}
		catch (Exception _)
		{
			// ignore, non-critical
		}

		// Change player stats
		_player.setCursedWeaponEquippedId(_itemId);
		_player.setKarma(9999999);
		_player.setPkKills(0);
		if (_player.isInParty())
		{
			_player.getParty().removePartyMember(_player, PartyMessageType.EXPELLED);
		}

		// Disable All Skills
		// Do Transform
		doTransform();
		// Add skill
		giveSkill();

		// Equip with the weapon
		_item = item;
		_player.getInventory().equipItem(_item);
		SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EQUIPPED_YOUR_S1);
		sm.addItemName(_item);
		_player.sendPacket(sm);

		// Fully heal player
		_player.setCurrentHpMp(_player.getMaxHp(), _player.getMaxMp());
		_player.setCurrentCp(_player.getMaxCp());

		// Refresh inventory
		_player.sendItemList(false);

		// Refresh player stats
		_player.broadcastUserInfo();

		ThreadPool.schedule(() -> _player.broadcastPacket(new SocialAction(_player.getObjectId(), 17)), 300);

		sm = new SystemMessage(SystemMessageId.THE_OWNER_OF_S2_HAS_APPEARED_IN_THE_S1_REGION);
		sm.addZoneName(_player.getX(), _player.getY(), _player.getZ()); // Region Name
		sm.addItemName(_item);
		CursedWeaponsManager.announce(sm);

		_isDropped = false;
		PhantomCursedHunt.onWeaponActivated(this, _player);
	}

	public void saveData()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement del = con.prepareStatement("DELETE FROM cursed_weapons WHERE itemId = ?");
			PreparedStatement ps = con.prepareStatement("INSERT INTO cursed_weapons (itemId, charId, playerKarma, playerPkKills, nbKills, endTime) VALUES (?, ?, ?, ?, ?, ?)"))
		{
			// Delete previous datas
			del.setInt(1, _itemId);
			del.executeUpdate();

			if (_isActivated)
			{
				ps.setInt(1, _itemId);
				ps.setInt(2, _playerId);
				ps.setInt(3, _playerKarma);
				ps.setInt(4, _playerPkKills);
				ps.setInt(5, _nbKills);
				ps.setLong(6, _endTime);
				ps.executeUpdate();
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "CursedWeapon: Failed to save data.", e);
		}
	}

	public void dropIt(Creature killer)
	{
		if (Rnd.get(100) <= _disapearChance)
		{
			// Remove it
			endOfLife();
		}
		else
		{
			// Unequip & Drop
			dropIt(null, null, killer, false);
			// Reset player stats
			_player.setKarma(_playerKarma);
			_player.setPkKills(_playerPkKills);
			_player.setCursedWeaponEquippedId(0);
			removeSkill();

			_player.abortAttack();

			_player.broadcastUserInfo();
		}
	}

	public void increaseKills()
	{
		_nbKills++;

		if ((_player != null) && _player.isOnline())
		{
			_player.setPkKills(_nbKills);
			_player.updateUserInfo();
			if (((_nbKills % _stageKills) == 0) && (_nbKills <= (_stageKills * (_skillMaxLevel - 1))))
			{
				giveSkill();
			}
		}
		// Reduce time-to-live
		_endTime -= _durationLost * 60000;
		saveData();
	}

	public void setDisapearChance(int disapearChance)
	{
		_disapearChance = disapearChance;
	}

	public void setDropRate(int dropRate)
	{
		_dropRate = dropRate;
	}

	public void setDuration(int duration)
	{
		_duration = duration;
	}

	public void setDurationLost(int durationLost)
	{
		_durationLost = durationLost;
	}

	public void setStageKills(int stageKills)
	{
		_stageKills = stageKills;
	}

	public void setNbKills(int nbKills)
	{
		_nbKills = nbKills;
	}

	public void setPlayerId(int playerId)
	{
		_playerId = playerId;
	}

	public void setPlayerKarma(int playerKarma)
	{
		_playerKarma = playerKarma;
	}

	public void setPlayerPkKills(int playerPkKills)
	{
		_playerPkKills = playerPkKills;
	}

	public void setActivated(boolean isActivated)
	{
		_isActivated = isActivated;
	}

	public void setDropped(boolean isDropped)
	{
		_isDropped = isDropped;
	}

	public void setEndTime(long endTime)
	{
		_endTime = endTime;
	}

	public void setPlayer(Player player)
	{
		_player = player;
	}

	public void setItem(Item item)
	{
		_item = item;
	}

	public boolean isActivated()
	{
		return _isActivated;
	}

	public boolean isDropped()
	{
		return _isDropped;
	}

	public long getEndTime()
	{
		return _endTime;
	}

	public String getName()
	{
		return _name;
	}

	public int getItemId()
	{
		return _itemId;
	}

	public int getSkillId()
	{
		return _skillId;
	}

	public int getPlayerId()
	{
		return _playerId;
	}

	public Player getPlayer()
	{
		return _player;
	}

	public int getPlayerKarma()
	{
		return _playerKarma;
	}

	public int getPlayerPkKills()
	{
		return _playerPkKills;
	}

	public int getNbKills()
	{
		return _nbKills;
	}

	public int getStageKills()
	{
		return _stageKills;
	}

	public boolean isActive()
	{
		return _isActivated || _isDropped;
	}

	public int getLevel()
	{
		if (_nbKills > (_stageKills * _skillMaxLevel))
		{
			return _skillMaxLevel;
		}
		return (_nbKills / _stageKills);
	}

	public long getTimeLeft()
	{
		return _endTime - System.currentTimeMillis();
	}

	public void goTo(Player player)
	{
		if (player == null)
		{
			return;
		}

		if (_isActivated && (_player != null))
		{
			// Go to player holding the weapon
			player.teleToLocation(_player.getLocation(), true);
		}
		else if (_isDropped && (_item != null))
		{
			// Go to item on the ground
			player.teleToLocation(_item.getLocation(), true);
		}
		else
		{
			player.sendMessage(_name + " isn't in the World.");
		}
	}

	public Location getWorldPosition()
	{
		if (_isActivated && (_player != null))
		{
			return _player.getLocation();
		}

		if (_isDropped && (_item != null))
		{
			return _item.getLocation();
		}

		return null;
	}

	public Item getItem()
	{
		return _item;
	}

	public int getLastKillerId()
	{
		return _lastKillerId;
	}

	public long getDuration()
	{
		return _duration;
	}
}
