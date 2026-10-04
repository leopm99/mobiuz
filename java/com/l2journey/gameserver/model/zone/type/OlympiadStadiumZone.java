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
package com.l2journey.gameserver.model.zone.type;

import java.util.ArrayList;
import java.util.List;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.managers.InstanceManager;
import com.l2journey.gameserver.managers.ZoneManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.TeleportWhereType;
import com.l2journey.gameserver.model.actor.instance.Door;
import com.l2journey.gameserver.model.actor.instance.OlympiadManager;
import com.l2journey.gameserver.model.olympiad.OlympiadGameTask;
import com.l2journey.gameserver.model.zone.AbstractZoneSettings;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.model.zone.ZoneRespawn;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.ExOlympiadMatchEnd;
import com.l2journey.gameserver.network.serverpackets.ExOlympiadUserInfo;
import com.l2journey.gameserver.network.serverpackets.ServerPacket;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * An olympiad stadium
 * @author durgus, DS
 */
public class OlympiadStadiumZone extends ZoneRespawn
{
	private List<Location> _spectatorLocations;
	
	public OlympiadStadiumZone(int id)
	{
		super(id);
		AbstractZoneSettings settings = ZoneManager.getSettings(getName());
		if (settings == null)
		{
			settings = new Settings();
		}
		setSettings(settings);
		
		_checkAffected = true;
	}
	
	public class Settings extends AbstractZoneSettings
	{
		private OlympiadGameTask _task = null;
		
		protected Settings()
		{
		}
		
		public OlympiadGameTask getOlympiadTask()
		{
			return _task;
		}
		
		protected void setTask(OlympiadGameTask task)
		{
			_task = task;
		}
		
		@Override
		public void clear()
		{
			_task = null;
		}
	}
	
	@Override
	public Settings getSettings()
	{
		return (Settings) super.getSettings();
	}
	
	public void registerTask(OlympiadGameTask task)
	{
		getSettings().setTask(task);
	}
	
	public void openDoors()
	{
		for (Door door : InstanceManager.getInstance().getInstance(getInstanceId()).getDoors())
		{
			if ((door != null) && !door.isOpen())
			{
				door.openMe();
			}
		}
	}
	
	public void closeDoors()
	{
		for (Door door : InstanceManager.getInstance().getInstance(getInstanceId()).getDoors())
		{
			if ((door != null) && door.isOpen())
			{
				door.closeMe();
			}
		}
	}
	
	public void spawnBuffers()
	{
		for (Npc buffer : InstanceManager.getInstance().getInstance(getInstanceId()).getNpcs())
		{
			if ((buffer instanceof OlympiadManager) && !buffer.isSpawned())
			{
				buffer.spawnMe();
			}
		}
	}
	
	public void deleteBuffers()
	{
		for (Npc buffer : InstanceManager.getInstance().getInstance(getInstanceId()).getNpcs())
		{
			if ((buffer instanceof OlympiadManager) && buffer.isSpawned())
			{
				buffer.decayMe();
			}
		}
	}
	
	public void broadcastStatusUpdate(Player player)
	{
		final ExOlympiadUserInfo packet = new ExOlympiadUserInfo(player);
		for (Player target : getPlayersInside())
		{
			if ((target != null) && (target.inObserverMode() || (target.getOlympiadSide() != player.getOlympiadSide())) && (target.getInstanceId() == player.getInstanceId()))
			{
				target.sendPacket(packet);
			}
		}
	}
	
	public void broadcastPacketToObservers(ServerPacket packet)
	{
		for (Player creature : getPlayersInside())
		{
			if ((creature != null) && creature.inObserverMode() && (creature.getInstanceId() == getInstanceId()))
			{
				creature.sendPacket(packet);
			}
		}
	}
	
	@Override
	public void broadcastPacket(ServerPacket packet)
	{
		for (Player creature : getPlayersInside())
		{
			if ((creature != null) && (creature.getInstanceId() == getInstanceId()))
			{
				creature.sendPacket(packet);
			}
		}
	}
	
	@Override
	protected boolean isAffected(Creature creature)
	{
		if (super.isAffected(creature))
		{
			return !(creature.getInstanceId() != getInstanceId());
		}
		
		return false;
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		creature.setInsideZone(ZoneId.PVP, true);
		
		if (creature.isPlayer() && (getSettings().getOlympiadTask() != null) && getSettings().getOlympiadTask().isBattleStarted())
		{
			creature.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_A_COMBAT_ZONE);
			getSettings().getOlympiadTask().getGame().sendOlympiadInfo(creature);
		}
		
		if (!creature.isPlayable())
		{
			return;
		}
		
		final Player player = creature.asPlayer();
		if (player != null)
		{
			// only participants, observers and GMs allowed
			if (!player.isGM() && !player.isInOlympiadMode() && !player.inObserverMode())
			{
				ThreadPool.execute(new KickPlayer(player));
			}
			else
			{
				// check for pet
				if (player.hasPet())
				{
					player.getSummon().unSummon(player);
				}
			}
		}
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		creature.setInsideZone(ZoneId.PVP, false);
		
		if (creature.isPlayer() && (getSettings().getOlympiadTask() != null) && getSettings().getOlympiadTask().isBattleStarted())
		{
			creature.sendPacket(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
			creature.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
		}
	}
	
	public void updateZoneStatusForCharactersInside()
	{
		if (getSettings().getOlympiadTask() == null)
		{
			return;
		}
		
		final boolean battleStarted = getSettings().getOlympiadTask().isBattleStarted();
		final SystemMessage sm = battleStarted ? new SystemMessage(SystemMessageId.YOU_HAVE_ENTERED_A_COMBAT_ZONE) : new SystemMessage(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
		for (Creature creature : getCharactersInside())
		{
			if ((creature == null) || (creature.getInstanceId() != getInstanceId()))
			{
				continue;
			}
			
			if (battleStarted)
			{
				creature.setInsideZone(ZoneId.PVP, true);
				if (creature.isPlayer())
				{
					creature.sendPacket(sm);
				}
			}
			else
			{
				creature.setInsideZone(ZoneId.PVP, false);
				if (creature.isPlayer())
				{
					creature.sendPacket(sm);
					creature.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
				}
			}
			creature.broadcastInfo();
		}
	}
	
	private static class KickPlayer implements Runnable
	{
		private Player _player;
		
		public KickPlayer(Player player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			if (_player == null)
			{
				return;
			}
			
			if (_player.hasSummon())
			{
				_player.getSummon().unSummon(_player);
			}
			
			_player.teleToLocation(TeleportWhereType.TOWN);
			_player.setInstanceId(0);
			_player = null;
		}
	}
	
	@Override
	public void parseLoc(int x, int y, int z, String type)
	{
		if ((type != null) && type.equals("spectatorSpawn"))
		{
			if (_spectatorLocations == null)
			{
				_spectatorLocations = new ArrayList<>();
			}
			_spectatorLocations.add(new Location(x, y, z));
		}
		else
		{
			super.parseLoc(x, y, z, type);
		}
	}
	
	public List<Location> getSpectatorSpawns()
	{
		return _spectatorLocations;
	}
}