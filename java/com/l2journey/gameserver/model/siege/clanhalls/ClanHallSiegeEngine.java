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
package com.l2journey.gameserver.model.siege.clanhalls;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.data.sql.ClanTable;
import com.l2journey.gameserver.managers.CHSiegeManager;
import com.l2journey.gameserver.managers.MapRegionManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.Spawn;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.siege.Siegable;
import com.l2journey.gameserver.model.siege.SiegeClan;
import com.l2journey.gameserver.model.siege.SiegeClanType;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.NpcSay;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;
import com.l2journey.gameserver.util.Broadcast;

/**
 * @author BiggBoss
 */
public abstract class ClanHallSiegeEngine extends Quest implements Siegable
{
	private static final String SQL_LOAD_ATTACKERS = "SELECT attacker_id FROM clanhall_siege_attackers WHERE clanhall_id = ?";
	private static final String SQL_SAVE_ATTACKERS = "INSERT INTO clanhall_siege_attackers VALUES (?,?)";
	private static final String SQL_LOAD_GUARDS = "SELECT npcId, x, y, z, heading, respawnDelay FROM clanhall_siege_guards WHERE clanHallId = ?";
	
	public static final int FORTRESS_RESSISTANCE = 21;
	public static final int DEVASTATED_CASTLE = 34;
	public static final int BANDIT_STRONGHOLD = 35;
	public static final int RAINBOW_SPRINGS = 62;
	public static final int BEAST_FARM = 63;
	public static final int FORTRESS_OF_DEAD = 64;
	
	protected static final Logger LOGGER = Logger.getLogger(ClanHallSiegeEngine.class.getName());
	
	private final Map<Integer, SiegeClan> _attackers = new ConcurrentHashMap<>();
	private Collection<Spawn> _guards;
	
	public SiegableHall _hall;
	public ScheduledFuture<?> _siegeTask;
	public boolean _missionAccomplished = false;
	
	public ClanHallSiegeEngine(int hallId)
	{
		super(-1);
		_hall = CHSiegeManager.getInstance().getSiegableHall(hallId);
		_hall.setSiege(this);
		
		_siegeTask = ThreadPool.schedule(new PrepareOwner(), _hall.getNextSiegeTime() - System.currentTimeMillis() - 3600000);
		LOGGER.config(_hall.getName() + " siege scheduled for " + _hall.getSiegeDate().getTime() + ".");
		loadAttackers();
	}
	
	public void loadAttackers()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SQL_LOAD_ATTACKERS))
		{
			ps.setInt(1, _hall.getId());
			try (ResultSet rset = ps.executeQuery())
			{
				while (rset.next())
				{
					final int id = rset.getInt("attacker_id");
					_attackers.put(id, new SiegeClan(id, SiegeClanType.ATTACKER));
				}
			}
		}
		catch (Exception _)
		{
			LOGGER.warning(getName() + ": Could not load siege attackers!");
		}
	}
	
	public void saveAttackers()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM clanhall_siege_attackers WHERE clanhall_id = ?"))
		{
			ps.setInt(1, _hall.getId());
			ps.execute();
			
			if (!_attackers.isEmpty())
			{
				try (PreparedStatement insert = con.prepareStatement(SQL_SAVE_ATTACKERS))
				{
					for (SiegeClan clan : _attackers.values())
					{
						insert.setInt(1, _hall.getId());
						insert.setInt(2, clan.getClanId());
						insert.addBatch();
						insert.clearParameters();
					}
					insert.executeBatch();
				}
			}
			LOGGER.config(getName() + ": Successfully saved attackers to database.");
		}
		catch (Exception _)
		{
			LOGGER.warning(getName() + ": Couldn't save attacker list!");
		}
	}
	
	public void loadGuards()
	{
		if (_guards != null)
		{
			_guards.forEach(Spawn::startRespawn);
			return;
		}
		
		_guards = new ArrayList<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SQL_LOAD_GUARDS))
		{
			ps.setInt(1, _hall.getId());
			final ResultSet rset = ps.executeQuery();
			while (rset.next())
			{
				final Spawn spawn = new Spawn(rset.getInt("npcId"));
				spawn.setXYZ(rset.getInt("x"), rset.getInt("y"), rset.getInt("z"));
				spawn.setHeading(rset.getInt("heading"));
				spawn.setRespawnDelay(rset.getInt("respawnDelay"));
				spawn.setAmount(1);
				_guards.add(spawn);
			}
		}
		catch (Exception _)
		{
			LOGGER.warning(getName() + ": Could not load siege guards!");
		}
	}
	
	private void spawnSiegeGuards()
	{
		for (Spawn guard : _guards)
		{
			guard.init();
			if (guard.getRespawnDelay() == 0)
			{
				guard.stopRespawn();
			}
		}
	}
	
	private void unSpawnSiegeGuards()
	{
		if (_guards != null)
		{
			for (Spawn guard : _guards)
			{
				guard.stopRespawn();
				if (guard.getLastSpawn() != null)
				{
					guard.getLastSpawn().deleteMe();
				}
			}
		}
	}
	
	@Override
	public Set<Npc> getFlag(Clan clan)
	{
		Set<Npc> result = null;
		final SiegeClan sClan = getAttackerClan(clan);
		if (sClan != null)
		{
			result = sClan.getFlag();
		}
		return result;
	}
	
	public Map<Integer, SiegeClan> getAttackers()
	{
		return _attackers;
	}
	
	@Override
	public boolean checkIsAttacker(Clan clan)
	{
		if (clan == null)
		{
			return false;
		}
		return _attackers.containsKey(clan.getId());
	}
	
	@Override
	public boolean checkIsDefender(Clan clan)
	{
		return false;
	}
	
	@Override
	public SiegeClan getAttackerClan(int clanId)
	{
		return _attackers.get(clanId);
	}
	
	@Override
	public SiegeClan getAttackerClan(Clan clan)
	{
		return getAttackerClan(clan.getId());
	}
	
	@Override
	public Collection<SiegeClan> getAttackerClans()
	{
		return Collections.unmodifiableCollection(_attackers.values());
	}
	
	@Override
	public List<Player> getAttackersInZone()
	{
		final List<Player> attackers = new ArrayList<>();
		for (Player pc : _hall.getSiegeZone().getPlayersInside())
		{
			final Clan clan = pc.getClan();
			if ((clan != null) && _attackers.containsKey(clan.getId()))
			{
				attackers.add(pc);
			}
		}
		return attackers;
	}
	
	@Override
	public SiegeClan getDefenderClan(int clanId)
	{
		return null;
	}
	
	@Override
	public SiegeClan getDefenderClan(Clan clan)
	{
		return null;
	}
	
	@Override
	public List<SiegeClan> getDefenderClans()
	{
		return null;
	}
	
	public void prepareOwner()
	{
		if (_hall.getOwnerId() > 0)
		{
			final SiegeClan clan = new SiegeClan(_hall.getOwnerId(), SiegeClanType.ATTACKER);
			_attackers.put(clan.getClanId(), new SiegeClan(clan.getClanId(), SiegeClanType.ATTACKER));
		}
		
		_hall.free();
		_hall.banishForeigners();
		final SystemMessage msg = new SystemMessage(SystemMessageId.THE_REGISTRATION_TERM_FOR_S1_HAS_ENDED);
		msg.addString(getName());
		Broadcast.toAllOnlinePlayers(msg);
		_hall.updateSiegeStatus(SiegeStatus.WAITING_BATTLE);
		
		_siegeTask = ThreadPool.schedule(new SiegeStarts(), 3600000);
	}
	
	@Override
	public void startSiege()
	{
		if ((_attackers.size() < 1) && (_hall.getId() != 21)) // Fortress of resistance don't have attacker list
		{
			onSiegeEnds();
			_attackers.clear();
			_hall.updateNextSiege();
			_siegeTask = ThreadPool.schedule(new PrepareOwner(), _hall.getSiegeDate().getTimeInMillis());
			_hall.updateSiegeStatus(SiegeStatus.WAITING_BATTLE);
			final SystemMessage sm = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_BEEN_CANCELED_DUE_TO_LACK_OF_INTEREST);
			sm.addString(_hall.getName());
			Broadcast.toAllOnlinePlayers(sm);
			return;
		}
		
		_hall.spawnDoor();
		loadGuards();
		spawnSiegeGuards();
		_hall.updateSiegeZone(true);
		
		final byte state = 1;
		for (SiegeClan sClan : _attackers.values())
		{
			final Clan clan = ClanTable.getInstance().getClan(sClan.getClanId());
			if (clan == null)
			{
				continue;
			}
			
			for (Player pc : clan.getOnlineMembers(0))
			{
				pc.setSiegeState(state);
				pc.broadcastUserInfo();
				pc.setInHideoutSiege(true);
			}
		}
		
		_hall.updateSiegeStatus(SiegeStatus.RUNNING);
		onSiegeStarts();
		_siegeTask = ThreadPool.schedule(new SiegeEnds(), _hall.getSiegeLength());
	}
	
	@Override
	public void endSiege()
	{
		final SystemMessage end = new SystemMessage(SystemMessageId.THE_S1_SIEGE_HAS_FINISHED);
		end.addString(_hall.getName());
		Broadcast.toAllOnlinePlayers(end);
		
		final Clan winner = getWinner();
		SystemMessage finalMsg = null;
		if (_missionAccomplished && (winner != null))
		{
			_hall.setOwner(winner);
			winner.setHideoutId(_hall.getId());
			finalMsg = new SystemMessage(SystemMessageId.CLAN_S1_IS_VICTORIOUS_OVER_S2_S_CASTLE_SIEGE);
			finalMsg.addString(winner.getName());
		}
		else
		{
			finalMsg = new SystemMessage(SystemMessageId.THE_SIEGE_OF_S1_HAS_ENDED_IN_A_DRAW);
		}
		finalMsg.addString(_hall.getName());
		Broadcast.toAllOnlinePlayers(finalMsg);
		_missionAccomplished = false;
		_hall.updateSiegeZone(false);
		_hall.updateNextSiege();
		_hall.spawnDoor(false);
		_hall.banishForeigners();
		
		final byte state = 0;
		for (SiegeClan sClan : _attackers.values())
		{
			final Clan clan = ClanTable.getInstance().getClan(sClan.getClanId());
			if (clan == null)
			{
				continue;
			}
			
			for (Player player : clan.getOnlineMembers(0))
			{
				player.setSiegeState(state);
				player.broadcastUserInfo();
				player.setInHideoutSiege(false);
			}
		}
		
		// Update pvp flag for winners when siege zone becomes inactive
		for (Creature chr : _hall.getSiegeZone().getCharactersInside())
		{
			if ((chr != null) && chr.isPlayer())
			{
				chr.asPlayer().startPvPFlag();
			}
		}
		
		_attackers.clear();
		
		onSiegeEnds();
		
		_siegeTask = ThreadPool.schedule(new PrepareOwner(), _hall.getNextSiegeTime() - System.currentTimeMillis() - 3600000);
		LOGGER.config("Siege of " + _hall.getName() + " scheduled for " + _hall.getSiegeDate().getTime() + ".");
		_hall.updateSiegeStatus(SiegeStatus.REGISTERING);
		unSpawnSiegeGuards();
	}
	
	@Override
	public void updateSiege()
	{
		cancelSiegeTask();
		_siegeTask = ThreadPool.schedule(new PrepareOwner(), _hall.getNextSiegeTime() - 3600000);
		LOGGER.config(_hall.getName() + " siege scheduled for " + _hall.getSiegeDate().getTime() + ".");
	}
	
	public void cancelSiegeTask()
	{
		if (_siegeTask != null)
		{
			_siegeTask.cancel(false);
		}
	}
	
	@Override
	public Calendar getSiegeDate()
	{
		return _hall.getSiegeDate();
	}
	
	@Override
	public boolean giveFame()
	{
		return Config.CHS_ENABLE_FAME;
	}
	
	@Override
	public int getFameAmount()
	{
		return Config.CHS_FAME_AMOUNT;
	}
	
	@Override
	public int getFameFrequency()
	{
		return Config.CHS_FAME_FREQUENCY;
	}
	
	public void broadcastNpcSay(Npc npc, ChatType type, NpcStringId messageId)
	{
		final NpcSay npcSay = new NpcSay(npc.getObjectId(), type, npc.getId(), messageId);
		final int sourceRegion = MapRegionManager.getInstance().getMapRegionLocId(npc);
		for (Player pc : World.getInstance().getPlayers())
		{
			if ((pc != null) && (MapRegionManager.getInstance().getMapRegionLocId(pc) == sourceRegion))
			{
				pc.sendPacket(npcSay);
			}
		}
	}
	
	public Location getInnerSpawnLoc(Player player)
	{
		return null;
	}
	
	public boolean canPlantFlag()
	{
		return true;
	}
	
	public boolean doorIsAutoAttackable()
	{
		return true;
	}
	
	public void onSiegeStarts()
	{
	}
	
	public void onSiegeEnds()
	{
	}
	
	public abstract Clan getWinner();
	
	public class PrepareOwner implements Runnable
	{
		@Override
		public void run()
		{
			prepareOwner();
		}
	}
	
	public class SiegeStarts implements Runnable
	{
		@Override
		public void run()
		{
			startSiege();
		}
	}
	
	public class SiegeEnds implements Runnable
	{
		@Override
		public void run()
		{
			endSiege();
		}
	}
}