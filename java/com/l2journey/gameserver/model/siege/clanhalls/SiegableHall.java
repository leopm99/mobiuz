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
import java.util.Calendar;
import java.util.logging.Level;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Door;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.residences.ClanHall;
import com.l2journey.gameserver.model.siege.SiegeClan;
import com.l2journey.gameserver.model.siege.SiegeClanType;
import com.l2journey.gameserver.model.zone.type.SiegableHallZone;
import com.l2journey.gameserver.model.zone.type.SiegeZone;
import com.l2journey.gameserver.network.serverpackets.SiegeInfo;

/**
 * @author BiggBoss
 */
public class SiegableHall extends ClanHall
{
	private static final String SQL_SAVE = "UPDATE siegable_clanhall SET ownerId=?, nextSiege=? WHERE clanHallId=?";
	
	private Calendar _nextSiege;
	private final long _siegeLength;
	private final int[] _scheduleConfig =
	{
		7,
		0,
		0,
		12,
		0
	};
	
	private SiegeStatus _status = SiegeStatus.REGISTERING;
	private SiegeZone _siegeZone;
	
	private ClanHallSiegeEngine _siege;
	
	public SiegableHall(StatSet set)
	{
		super(set);
		_siegeLength = set.getLong("siegeLength");
		final String[] rawSchConfig = set.getString("scheduleConfig").split(";");
		if (rawSchConfig.length == 5)
		{
			for (int i = 0; i < 5; i++)
			{
				try
				{
					_scheduleConfig[i] = Integer.parseInt(rawSchConfig[i]);
				}
				catch (Exception _)
				{
					LOGGER.warning("SiegableHall - " + getName() + ": Wrong schedule_config parameters!");
				}
			}
		}
		else
		{
			LOGGER.warning(getName() + ": Wrong schedule_config value in siegable_halls table, using default (7 days)");
		}
		
		_nextSiege = Calendar.getInstance();
		final long nextSiege = set.getLong("nextSiege");
		if ((nextSiege - System.currentTimeMillis() - 3600000) < 0)
		{
			updateNextSiege();
		}
		else
		{
			_nextSiege.setTimeInMillis(nextSiege);
		}
		
		if (getOwnerId() != 0)
		{
			_isFree = false;
			loadFunctions();
		}
	}
	
	public void spawnDoor()
	{
		spawnDoor(false);
	}
	
	public void spawnDoor(boolean isDoorWeak)
	{
		for (Door door : getDoors())
		{
			if (door.isDead())
			{
				door.doRevive();
				if (isDoorWeak)
				{
					door.setCurrentHp(door.getMaxHp() / 2);
				}
				else
				{
					door.setCurrentHp(door.getMaxHp());
				}
			}
			
			if (door.isOpen())
			{
				door.closeMe();
			}
		}
	}
	
	@Override
	public void updateDb()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(SQL_SAVE))
		{
			ps.setInt(1, getOwnerId());
			ps.setLong(2, getNextSiegeTime());
			ps.setInt(3, getId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "Exception: SiegableHall.updateDb(): " + e.getMessage());
		}
	}
	
	public void setSiege(ClanHallSiegeEngine siegable)
	{
		_siege = siegable;
		_siegeZone.setSiegeInstance(siegable);
	}
	
	public ClanHallSiegeEngine getSiege()
	{
		return _siege;
	}
	
	public Calendar getSiegeDate()
	{
		return _nextSiege;
	}
	
	public long getNextSiegeTime()
	{
		return _nextSiege.getTimeInMillis();
	}
	
	public long getSiegeLength()
	{
		return _siegeLength;
	}
	
	public void setNextSiegeDate(long date)
	{
		_nextSiege.setTimeInMillis(date);
	}
	
	public void setNextSiegeDate(Calendar c)
	{
		_nextSiege = c;
	}
	
	public void updateNextSiege()
	{
		final Calendar callendar = Calendar.getInstance();
		callendar.add(Calendar.DAY_OF_YEAR, _scheduleConfig[0]);
		callendar.add(Calendar.MONTH, _scheduleConfig[1]);
		callendar.add(Calendar.YEAR, _scheduleConfig[2]);
		callendar.set(Calendar.HOUR_OF_DAY, _scheduleConfig[3]);
		callendar.set(Calendar.MINUTE, _scheduleConfig[4]);
		callendar.set(Calendar.SECOND, 0);
		setNextSiegeDate(callendar);
		updateDb();
	}
	
	public void addAttacker(Clan clan)
	{
		if (_siege != null)
		{
			_siege.getAttackers().put(clan.getId(), new SiegeClan(clan.getId(), SiegeClanType.ATTACKER));
		}
	}
	
	public void removeAttacker(Clan clan)
	{
		if (_siege != null)
		{
			_siege.getAttackers().remove(clan.getId());
		}
	}
	
	public boolean isRegistered(Clan clan)
	{
		return (_siege != null) && _siege.checkIsAttacker(clan);
	}
	
	public SiegeStatus getSiegeStatus()
	{
		return _status;
	}
	
	public boolean isRegistering()
	{
		return _status == SiegeStatus.REGISTERING;
	}
	
	public boolean isInSiege()
	{
		return _status == SiegeStatus.RUNNING;
	}
	
	public boolean isWaitingBattle()
	{
		return _status == SiegeStatus.WAITING_BATTLE;
	}
	
	public void updateSiegeStatus(SiegeStatus status)
	{
		_status = status;
	}
	
	public SiegeZone getSiegeZone()
	{
		return _siegeZone;
	}
	
	public void setSiegeZone(SiegeZone zone)
	{
		_siegeZone = zone;
	}
	
	public void updateSiegeZone(boolean active)
	{
		_siegeZone.setActive(active);
	}
	
	public void showSiegeInfo(Player player)
	{
		player.sendPacket(new SiegeInfo(this, player));
	}
	
	@Override
	public boolean isSiegableHall()
	{
		return true;
	}
	
	@Override
	public SiegableHallZone getZone()
	{
		return (SiegableHallZone) super.getZone();
	}
}
