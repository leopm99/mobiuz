/*
 * Copyright (C) 2020 L2J DataPack
 *
 * This file is part of L2J DataPack.
 *
 * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.voicedcommandhandlers; // Mantenha o seu pacote original

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;

import com.l2journey.Config;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.data.SpawnTable;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.data.xml.NpcData;
import com.l2journey.gameserver.handler.IVoicedCommandHandler;
import com.l2journey.gameserver.managers.GrandBossManager;
import com.l2journey.gameserver.managers.SiegeManager;
import com.l2journey.gameserver.managers.ZoneManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.Spawn;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.model.zone.type.NoSummonFriendZone;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;
import com.l2journey.gameserver.network.serverpackets.SetupGauge;
import com.l2journey.gameserver.util.Broadcast;

public class ToMob implements IVoicedCommandHandler
{
	private static final Map<Integer, EscapeFinalizer> PENDING = new ConcurrentHashMap<>();
	private static final String[] VOICED_COMMANDS =
	{
		Config.TOMOB_COMMAND
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (activeChar == null) { return false; }
		synchronized (activeChar)
		{
			return execute(command, activeChar, target);
		}
	}

	private boolean execute(String command, Player activeChar, String target)
	{
		if (PENDING.containsKey(activeChar.getObjectId()))
		{
			activeChar.sendMessage("A teleport is already in progress.");
			return false;
		}
		
		if (!Config.TOMOB_COMMAND_ENABLED)
		{
			activeChar.sendMessage("This command is disabled!");
			return false;
		}
		
		if (command.equalsIgnoreCase(VOICED_COMMANDS[0]))
		{
			if ((target == null) || target.isEmpty())
			{
				activeChar.sendMessage("Usage: ." + VOICED_COMMANDS[0] + " <monster_name>");
				return true;
			}
			
			final NpcTemplate mobTemplate = NpcData.getInstance().getTemplateByName(target);
			if (mobTemplate == null)
			{
				activeChar.sendMessage("Monster not found.");
				return true;
			}
			
			final Set<Spawn> spawns = SpawnTable.getInstance().getSpawns(mobTemplate.getId());
			if (spawns.isEmpty())
			{
				activeChar.sendMessage("This monster does not have a defined spawn location.");
				return true;
			}
			
			final List<Location> spawnLocations = new ArrayList<>(spawns);
			final Location selected = spawnLocations.get(ThreadLocalRandom.current().nextInt(spawnLocations.size()));
			final Location destination = new Location(selected.getX(), selected.getY(), selected.getZ(), selected.getHeading(), selected.getInstanceId());
			
			if (!checkTravelConditions(activeChar, destination)) { return false; }

			if ((Config.TOMOB_TP_TIME < 0) || (Config.TOMOB_TP_TIME > (Integer.MAX_VALUE / 1000)))
			{
				activeChar.sendMessage("Invalid teleport time configuration.");
				return false;
			}
			final int teleportTimer = Config.TOMOB_TP_TIME * 1000;
			final int itemId = Config.TOMOB_TP_ITEM_ID;
			final int price = Config.TOMOB_FREE_TP ? 0 : Config.TOMOB_TP_ITEM_COUNT;
			if ((price < 0) || ((price > 0) && ((ItemData.getInstance().getTemplate(itemId) == null) || !activeChar.destroyItemByItemId(ItemProcessType.FEE, itemId, price, activeChar, true))))
			{
				activeChar.sendMessage("Not enough currency for teleportation.");
				return false;
			}
			final EscapeFinalizer task = new EscapeFinalizer(activeChar, destination, itemId, price);
			PENDING.put(activeChar.getObjectId(), task);
			try
			{
				activeChar.getAI().setIntention(Intention.IDLE);
				activeChar.setTarget(activeChar);
				activeChar.disableAllSkills();
				Broadcast.toSelfAndKnownPlayers(activeChar, new MagicSkillUse(activeChar, activeChar, 1050, 1, teleportTimer, 0));
				activeChar.sendPacket(new SetupGauge(activeChar.getObjectId(), SetupGauge.BLUE, teleportTimer));
				if (ThreadPool.schedule(task, teleportTimer) == null) { throw new IllegalStateException("Teleport scheduler unavailable"); }
			}
			catch (RuntimeException e)
			{
				PENDING.remove(activeChar.getObjectId(), task);
				activeChar.enableAllSkills();
				task.refund();
				java.util.logging.Logger.getLogger(ToMob.class.getName()).log(java.util.logging.Level.WARNING, "Could not schedule mob teleport", e);
				return false;
			}
			if (price > 0) { activeChar.sendMessage(price + " " + ItemData.getInstance().getTemplate(itemId).getName() + " consumed."); }

			activeChar.sendMessage("Teleporting to " + mobTemplate.getName() + "'s zone...");
		}
		return true;
	}
	
	private static boolean checkTravelConditions(Player activeChar, Location destination)
	{
		if (!Config.TOMOB_COMMAND_ENABLED || (Config.TOMOB_PEACE_ZONE_ONLY && !activeChar.isInsideZone(ZoneId.PEACE)))
		{
			activeChar.sendMessage("This teleport is disabled or requires a peace zone.");
			return false;
		}

		if (activeChar.isJailed() || activeChar.isDead() || activeChar.isInCombat() || activeChar.isInDuel() || activeChar.isInOlympiadMode() || activeChar.inObserverMode() || activeChar.isCursedWeaponEquipped() || activeChar.isFestivalParticipant())
		{
			activeChar.sendMessage("You cannot use this command right now.");
			return false;
		}
		
		if (GrandBossManager.getInstance().getZone(activeChar) != null)
		{
			activeChar.sendMessage("You cannot teleport from a Grand Boss zone.");
			return false;
		}
		if (activeChar.isCombatFlagEquipped())
		{
			activeChar.sendMessage("You cannot teleport while holding a Combat Flag.");
			return false;
		}
		if ((SiegeManager.getInstance().getSiege(activeChar) != null) && SiegeManager.getInstance().getSiege(activeChar).isInProgress())
		{
			activeChar.sendMessage("You cannot teleport while in a siege.");
			return false;
		}
		if (activeChar.isInParty() && activeChar.getParty().isInDimensionalRift())
		{
			activeChar.sendMessage("You cannot teleport from the Dimensional Rift.");
			return false;
		}
		if (activeChar.isInsideZone(ZoneId.NO_SUMMON_FRIEND))
		{
			activeChar.sendMessage("You are in an area which blocks teleporting.");
			return false;
		}
		
		if (activeChar.isOnEvent())
		{
			activeChar.sendMessage("You cannot teleport while participating in an event!");
			return false;
		}
		
		if (GrandBossManager.getInstance().getZone(destination) != null)
		{
			activeChar.sendMessage("The monster is in a Grand Boss zone, teleportation is forbidden.");
			return false;
		}
		if ((SiegeManager.getInstance().getSiege(destination) != null) && SiegeManager.getInstance().getSiege(destination).isInProgress())
		{
			activeChar.sendMessage("The monster is in a siege zone, you cannot teleport there.");
			return false;
		}
		if (ZoneManager.getInstance().getZone(destination, NoSummonFriendZone.class) != null)
		{
			activeChar.sendMessage("The monster is in an area which blocks summoning.");
			return false;
		}
		
		return true;
	}

	static class EscapeFinalizer implements Runnable
	{
		private final Player _activeChar;
		private final Location _destination;
		private final int _instanceId;
		private final int _itemId;
		private final int _price;

		EscapeFinalizer(Player player, Location destination, int itemId, int price)
		{
			_activeChar = player;
			_destination = destination;
			_instanceId = player.getInstanceId();
			_itemId = itemId;
			_price = price;
		}

		private void refund()
		{
			if (_price > 0) { _activeChar.addItem(ItemProcessType.REFUND, _itemId, _price, _activeChar, true); }
		}

		@Override
		public void run()
		{
			synchronized (_activeChar)
			{
				if (!PENDING.remove(_activeChar.getObjectId(), this)) { return; }
				boolean teleported = false;
				try
				{
					if (!_activeChar.isOnline() || (_activeChar.getInstanceId() != _instanceId) || !checkTravelConditions(_activeChar, _destination)) { return; }
					_activeChar.teleToLocation(_destination);
					teleported = true;
				}
				finally
				{
					_activeChar.enableAllSkills();
					if (!teleported) { refund(); }
				}
			}
		}
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}