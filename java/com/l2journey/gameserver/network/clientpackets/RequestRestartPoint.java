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
package com.l2journey.gameserver.network.clientpackets;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.data.sql.ClanHallTable;
import com.l2journey.gameserver.managers.CHSiegeManager;
import com.l2journey.gameserver.managers.CastleManager;
import com.l2journey.gameserver.managers.FortManager;
import com.l2journey.gameserver.managers.MapRegionManager;
import com.l2journey.gameserver.managers.TerritoryWarManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.TeleportWhereType;
import com.l2journey.gameserver.model.actor.instance.SiegeFlag;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.listeners.AbstractEventListener;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.quest.Event;
import com.l2journey.gameserver.model.residences.ClanHall;
import com.l2journey.gameserver.model.siege.Castle;
import com.l2journey.gameserver.model.siege.Fort;
import com.l2journey.gameserver.model.siege.SiegeClan;
import com.l2journey.gameserver.model.siege.clanhalls.SiegableHall;
import com.l2journey.gameserver.network.PacketLogger;

/**
 * @version $Revision: 1.7.2.3.2.6 $ $Date: 2005/03/27 15:29:30 $
 */
public class RequestRestartPoint extends ClientPacket
{
	protected int _requestedPointType;
	protected boolean _continuation;
	
	@Override
	protected void readImpl()
	{
		_requestedPointType = readInt();
	}
	
	class DeathTask implements Runnable
	{
		final Player _player;
		
		DeathTask(Player player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			portPlayer(_player);
		}
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if ((player == null) || !player.canRevive())
		{
			return;
		}
		
		if (player.isFakeDeath())
		{
			player.stopFakeDeath(true);
			return;
		}
		else if (!player.isDead())
		{
			return;
		}
		
		// Custom event resurrection management.
		if (player.isOnEvent())
		{
			for (AbstractEventListener listener : player.getListeners(EventType.ON_CREATURE_DEATH))
			{
				if (listener.getOwner() instanceof Event)
				{
					((Event) listener.getOwner()).notifyEvent("ResurrectPlayer", null, player);
					return;
				}
			}
		}
		
		final Castle castle = CastleManager.getInstance().getCastle(player.getX(), player.getY(), player.getZ());
		if ((castle != null) && castle.getSiege().isInProgress() && (player.getClan() != null) && castle.getSiege().checkIsAttacker(player.getClan()))
		{
			// Schedule respawn delay for attacker
			ThreadPool.schedule(new DeathTask(player), castle.getSiege().getAttackerRespawnDelay());
			if (castle.getSiege().getAttackerRespawnDelay() > 0)
			{
				player.sendMessage("You will be re-spawned in " + (castle.getSiege().getAttackerRespawnDelay() / 1000) + " seconds");
			}
			return;
		}
		
		portPlayer(player);
	}
	
	protected void portPlayer(Player player)
	{
		Location loc = null;
		Castle castle = null;
		Fort fort = null;
		SiegableHall hall = null;
		final boolean isInDefense = false;
		int instanceId = 0;
		
		// force jail
		if (player.isJailed())
		{
			_requestedPointType = 27;
		}
		else if (player.isFestivalParticipant())
		{
			_requestedPointType = 5;
		}
		
		final Clan clan = player.getClan();
		switch (_requestedPointType)
		{
			case 1: // to clanhall
			{
				if ((clan == null) || (clan.getHideoutId() == 0))
				{
					PacketLogger.warning("Player [" + player.getName() + "] called RestartPointPacket - To Clanhall and he doesn't have Clanhall!");
					return;
				}
				loc = MapRegionManager.getInstance().getTeleToLocation(player, TeleportWhereType.CLANHALL);
				if ((ClanHallTable.getInstance().getClanHallByOwner(clan) != null) && (ClanHallTable.getInstance().getClanHallByOwner(clan).getFunction(ClanHall.FUNC_RESTORE_EXP) != null))
				{
					player.restoreExp(ClanHallTable.getInstance().getClanHallByOwner(clan).getFunction(ClanHall.FUNC_RESTORE_EXP).getLevel());
				}
				break;
			}
			case 2: // to castle
			{
				castle = CastleManager.getInstance().getCastle(player);
				if ((castle != null) && castle.getSiege().isInProgress())
				{
					// Siege in progress
					if (castle.getSiege().checkIsDefender(clan))
					{
						loc = MapRegionManager.getInstance().getTeleToLocation(player, TeleportWhereType.CASTLE);
					}
					else if (castle.getSiege().checkIsAttacker(clan))
					{
						loc = MapRegionManager.getInstance().getTeleToLocation(player, TeleportWhereType.TOWN);
					}
					else
					{
						PacketLogger.warning("Player [" + player.getName() + "] called RestartPointPacket - To Castle and he doesn't have Castle!");
						return;
					}
				}
				else
				{
					if ((clan == null) || (clan.getCastleId() == 0))
					{
						return;
					}
					loc = MapRegionManager.getInstance().getTeleToLocation(player, TeleportWhereType.CASTLE);
				}
				if ((CastleManager.getInstance().getCastleByOwner(clan) != null) && (CastleManager.getInstance().getCastleByOwner(clan).getFunction(Castle.FUNC_RESTORE_EXP) != null))
				{
					player.restoreExp(CastleManager.getInstance().getCastleByOwner(clan).getFunction(Castle.FUNC_RESTORE_EXP).getLvl());
				}
				break;
			}
			case 3: // to fortress
			{
				if (((clan == null) || (clan.getFortId() == 0)) && !isInDefense)
				{
					PacketLogger.warning("Player [" + player.getName() + "] called RestartPointPacket - To Fortress and he doesn't have Fortress!");
					return;
				}
				loc = MapRegionManager.getInstance().getTeleToLocation(player, TeleportWhereType.FORTRESS);
				if ((FortManager.getInstance().getFortByOwner(clan) != null) && (FortManager.getInstance().getFortByOwner(clan).getFunction(Fort.FUNC_RESTORE_EXP) != null))
				{
					player.restoreExp(FortManager.getInstance().getFortByOwner(clan).getFunction(Fort.FUNC_RESTORE_EXP).getLevel());
				}
				break;
			}
			case 4: // to siege HQ
			{
				SiegeClan siegeClan = null;
				castle = CastleManager.getInstance().getCastle(player);
				fort = FortManager.getInstance().getFort(player);
				hall = CHSiegeManager.getInstance().getNearbyClanHall(player);
				SiegeFlag flag = TerritoryWarManager.getInstance().getHQForClan(clan);
				if (flag == null)
				{
					flag = TerritoryWarManager.getInstance().getFlagForClan(clan);
				}
				
				if ((castle != null) && castle.getSiege().isInProgress())
				{
					siegeClan = castle.getSiege().getAttackerClan(clan);
				}
				else if ((fort != null) && fort.getSiege().isInProgress())
				{
					siegeClan = fort.getSiege().getAttackerClan(clan);
				}
				else if ((hall != null) && hall.isInSiege())
				{
					siegeClan = hall.getSiege().getAttackerClan(clan);
				}
				if (((siegeClan == null) || siegeClan.getFlag().isEmpty()) && (flag == null))
				{
					// Check if clan hall has inner spawns loc
					if (hall != null)
					{
						loc = hall.getSiege().getInnerSpawnLoc(player);
						if (loc != null)
						{
							break;
						}
					}
					
					return;
				}
				loc = MapRegionManager.getInstance().getTeleToLocation(player, TeleportWhereType.SIEGEFLAG);
				break;
			}
			case 5: // Fixed or Player is a festival participant
			{
				if (!player.isGM() && !player.isFestivalParticipant() && !player.getInventory().haveItemForSelfResurrection())
				{
					PacketLogger.warning("Player [" + player.getName() + "] called RestartPointPacket - Fixed and he isn't festival participant!");
					return;
				}
				if (player.isGM() || player.destroyItemByItemId(ItemProcessType.FEE, 10649, 1, player, false) || player.destroyItemByItemId(ItemProcessType.FEE, 13300, 1, player, false) || player.destroyItemByItemId(ItemProcessType.FEE, 13128, 1, player, false))
				{
					player.doRevive(100.00);
				}
				else
				// Festival Participant
				{
					instanceId = player.getInstanceId();
					loc = new Location(player);
				}
				break;
			}
			case 6: // TODO: agathion ress
			{
				break;
			}
			case 27: // to jail
			{
				if (!player.isJailed())
				{
					return;
				}
				loc = new Location(-114356, -249645, -2984);
				break;
			}
			default:
			{
				loc = MapRegionManager.getInstance().getTeleToLocation(player, TeleportWhereType.TOWN);
				break;
			}
		}
		
		// Teleport and revive
		if (loc != null)
		{
			player.setInstanceId(instanceId);
			player.setIn7sDungeon(false);
			player.setIsPendingRevive(true);
			player.teleToLocation(loc, true);
		}
	}
}
