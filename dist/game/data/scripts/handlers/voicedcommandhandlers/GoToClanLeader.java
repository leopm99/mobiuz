/*
 * Copyright (C) 2004-2020 L2J DataPack
 * * This file is part of L2J DataPack.
 * * L2J DataPack is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * * L2J DataPack is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.voicedcommandhandlers;

import com.l2journey.Config;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.handler.IVoicedCommandHandler;
import com.l2journey.gameserver.managers.CastleManager;
import com.l2journey.gameserver.managers.GrandBossManager;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;

/**
 * Voice Command - Go To Clan Leader (.gotocl)
 * @author swarlog
 */
public class GoToClanLeader implements IVoicedCommandHandler
{
	private static final String[] _voicedCommands =
	{
		"gotocl",
		"gotopl"
	};
	
	
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if ((activeChar == null) || !("gotocl".equals(command) || "gotopl".equals(command))) { return false; }
		final boolean clan = command.equals("gotocl");
		if (clan ? !Config.ALLOW_GOTOCL_COMMAND : !Config.ALLOW_GOTOPL_COMMAND)
		{
			activeChar.sendMessage("This command is disabled!");
			return false;
		}
		synchronized (activeChar)
		{
			if (clan && (activeChar.getClan() == null))
			{
				activeChar.sendMessage("You are not in a clan.");
				return false;
			}
			if (!clan && (activeChar.getParty() == null))
			{
				activeChar.sendMessage("You are not in a party.");
				return false;
			}
			final Player leader = World.getInstance().getPlayer(clan ? activeChar.getClan().getLeaderId() : activeChar.getParty().getLeaderObjectId());
			if (!checkConditions(activeChar, leader)) { return false; }
			final int itemId = Config.TOCL_ITEM_CONSUME_ID;
			final long price = Config.TOCL_ITEM_CONSUME_COUNT;
			if ((price < 0) || ((price > 0) && ((itemId <= 0) || !activeChar.destroyItemByItemId(ItemProcessType.FEE, itemId, price, activeChar, true))))
			{
				activeChar.sendMessage("Not enough currency to perform the teleportation.");
				return false;
			}
			activeChar.teleToLocation(leader.getLocation());
			activeChar.sendMessage("You have been teleported to your leader!");
			if (price > 0)
			{
				final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
				activeChar.sendMessage(price + " " + (item == null ? "Item #" + itemId : item.getName()) + " consumed.");
			}
			return true;
		}
	}

	/**
	 * Checks all conditions for both the player and the clan leader before teleporting.
	 * @param activeChar The player using the command.
	 * @param leader The clan leader to teleport to.
	 * @return {@code true} if all conditions are met, {@code false} otherwise.
	 */
	private boolean checkConditions(Player activeChar, Player leader)
	{
		
		if ((leader == null) || !leader.isOnline())
		{
			activeChar.sendMessage("Your leader is not online.");
			return false;
		}
		
		if (activeChar == leader)
		{
			activeChar.sendMessage("You cannot teleport to yourself.");
			return false;
		}
		
		if (isPlayerInInvalidState(activeChar))
		{
			return false;
		}
		
		if (isPlayerInInvalidState(leader))
		{
			activeChar.sendMessage("Your leader is currently in a state where you cannot teleport to him.");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Performs a series of common state checks for a player.
	 * @param player The player to check.
	 * @return {@code true} if the player is in an invalid state, {@code false} otherwise.
	 */
	private boolean isPlayerInInvalidState(Player player)
	{
		if (player.isJailed())
		{
			player.sendMessage("Action not allowed while in jail.");
			return true;
		}
		if (player.isDead() || player.isAlikeDead())
		{
			player.sendMessage("Action not allowed while dead.");
			return true;
		}
		if (player.isInOlympiadMode())
		{
			player.sendMessage("Action not allowed during Olympiad.");
			return true;
		}
		if (GrandBossManager.getInstance().getZone(player) != null)
		{
			player.sendMessage("Action not allowed inside a Boss Zone.");
			return true;
		}
		if (player.isCombatFlagEquipped())
		{
			player.sendMessage("Action not allowed while holding a Combat Flag or Territory Ward.");
			return true;
		}
		if (player.isCursedWeaponEquipped())
		{
			player.sendMessage("Action not allowed while holding a Cursed Weapon.");
			return true;
		}
		if (player.isRegisteredOnEvent() || player.isOnEvent())
		{
			player.sendMessage("Action not allowed during an event.");
			return true;
		}
		if (player.isInDuel())
		{
			player.sendMessage("Action not allowed during a duel.");
			return true;
		}
		if (player.isInParty() && player.getParty().isInDimensionalRift())
		{
			player.sendMessage("Action not allowed while in the Dimensional Rift.");
			return true;
		}
		if (player.inObserverMode())
		{
			player.sendMessage("Action not allowed while in Observation Mode.");
			return true;
		}
		if ((player.getClan() != null) && (CastleManager.getInstance().getCastleByOwner(player.getClan()) != null) && CastleManager.getInstance().getCastleByOwner(player.getClan()).getSiege().isInProgress())
		{
			player.sendMessage("Action not allowed during a siege.");
			return true;
		}
		if (player.isInCombat())
		{
			player.sendMessage("You cannot teleport while in combat.");
			return true;
		}
		
		return false;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return _voicedCommands;
	}
}