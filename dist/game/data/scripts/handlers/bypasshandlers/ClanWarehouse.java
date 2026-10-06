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
package handlers.bypasshandlers;

import java.util.logging.Level;

import com.l2journey.Config;
import com.l2journey.gameserver.handler.IBypassHandler;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.ClanHallManager;
import com.l2journey.gameserver.model.actor.instance.Warehouse;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.clan.ClanAccess;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.enums.WarehouseListType;
import com.l2journey.gameserver.network.serverpackets.ActionFailed;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2journey.gameserver.network.serverpackets.SortedWareHouseWithdrawalList;
import com.l2journey.gameserver.network.serverpackets.WareHouseDepositList;
import com.l2journey.gameserver.network.serverpackets.WareHouseWithdrawalList;

public class ClanWarehouse implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"withdrawc",
		"withdrawsortedc",
		"depositc"
	};
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		if ((!(target instanceof Warehouse) && !(target instanceof ClanHallManager)) || player.isEnchanting())
		{
			return false;
		}
		
		final Clan clan = player.getClan();
		if (clan == null)
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_THE_CLAN_WAREHOUSE);
			return false;
		}
		
		if (clan.getLevel() == 0)
		{
			player.sendPacket(SystemMessageId.ONLY_CLANS_OF_CLAN_LEVEL_1_OR_HIGHER_CAN_USE_A_CLAN_WAREHOUSE);
			return false;
		}
		
		try
		{
			if (command.toLowerCase().startsWith(COMMANDS[0])) // WithdrawC
			{
				if (Config.ENABLE_WAREHOUSESORTING_CLAN)
				{
					final NpcHtmlMessage msg = new NpcHtmlMessage(target.asNpc().getObjectId());
					msg.setFile(player, "data/html/mods/WhSortedC.htm");
					msg.replace("%objectId%", String.valueOf(target.asNpc().getObjectId()));
					player.sendPacket(msg);
				}
				else
				{
					showWithdrawWindow(player, null, (byte) 0);
				}
				return true;
			}
			else if (command.toLowerCase().startsWith(COMMANDS[1])) // WithdrawSortedC
			{
				final String[] param = command.split(" ");
				if (param.length > 2)
				{
					showWithdrawWindow(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.getOrder(param[2]));
				}
				else if (param.length > 1)
				{
					showWithdrawWindow(player, WarehouseListType.valueOf(param[1]), SortedWareHouseWithdrawalList.A2Z);
				}
				else
				{
					showWithdrawWindow(player, WarehouseListType.ALL, SortedWareHouseWithdrawalList.A2Z);
				}
				return true;
			}
			else if (command.toLowerCase().startsWith(COMMANDS[2])) // DepositC
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				player.setActiveWarehouse(clan.getWarehouse());
				player.setInventoryBlockingStatus(true);
				player.sendPacket(new WareHouseDepositList(player, WareHouseDepositList.CLAN));
				return true;
			}
			
			return false;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "Exception in " + getClass().getSimpleName());
		}
		return false;
	}
	
	private void showWithdrawWindow(Player player, WarehouseListType itemtype, byte sortorder)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		if (!player.hasAccess(ClanAccess.ACCESS_WAREHOUSE))
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_THE_CLAN_WAREHOUSE);
			return;
		}
		
		player.setActiveWarehouse(player.getClan().getWarehouse());
		
		if (player.getActiveWarehouse().getSize() == 0)
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_NOT_DEPOSITED_ANY_ITEMS_IN_YOUR_WAREHOUSE);
			return;
		}
		
		for (Item i : player.getActiveWarehouse().getItems())
		{
			if (i.isTimeLimitedItem() && (i.getRemainingTime() <= 0))
			{
				player.getActiveWarehouse().destroyItem(ItemProcessType.DESTROY, i, player, null);
			}
		}
		if (itemtype != null)
		{
			player.sendPacket(new SortedWareHouseWithdrawalList(player, WareHouseWithdrawalList.CLAN, itemtype, sortorder));
		}
		else
		{
			player.sendPacket(new WareHouseWithdrawalList(player, WareHouseWithdrawalList.CLAN));
		}
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
