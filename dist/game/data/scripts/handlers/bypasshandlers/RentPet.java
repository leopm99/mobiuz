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

import java.util.StringTokenizer;

import com.l2journey.Config;
import com.l2journey.gameserver.handler.IBypassHandler;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Merchant;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2journey.gameserver.network.serverpackets.SetupGauge;

public class RentPet implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"RentPet"
	};
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		if (!(target instanceof Merchant) || !Config.ALLOW_RENTPET || !Config.LIST_PET_RENT_NPC.contains(target.getId()))
		{
			return false;
		}
		
		try
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			if (st.countTokens() < 1)
			{
				final NpcHtmlMessage msg = new NpcHtmlMessage(target.asNpc().getObjectId());
				msg.setHtml("<html><body>Pet Manager:<br>You can rent a wyvern or strider for adena.<br>My prices:<br1><table border=0><tr><td>Ride</td></tr><tr><td>Wyvern</td><td>Strider</td></tr><tr><td><a action=\"bypass -h npc_%objectId%_RentPet 1\">30 sec/1800 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 11\">30 sec/900 adena</a></td></tr><tr><td><a action=\"bypass -h npc_%objectId%_RentPet 2\">1 min/7200 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 12\">1 min/3600 adena</a></td></tr><tr><td><a action=\"bypass -h npc_%objectId%_RentPet 3\">10 min/720000 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 13\">10 min/360000 adena</a></td></tr><tr><td><a action=\"bypass -h npc_%objectId%_RentPet 4\">30 min/6480000 adena</a></td><td><a action=\"bypass -h npc_%objectId%_RentPet 14\">30 min/3240000 adena</a></td></tr></table></body></html>");
				msg.replace("%objectId%", String.valueOf(target.asNpc().getObjectId()));
				player.sendPacket(msg);
			}
			else
			{
				tryRentPet(player, Integer.parseInt(st.nextToken()));
			}
			
			return true;
		}
		catch (Exception _)
		{
			LOGGER.info("Exception in " + getClass().getSimpleName());
		}
		return false;
	}
	
	public static void tryRentPet(Player player, int petValue)
	{
		if ((player == null) || player.hasSummon() || player.isMounted() || player.isRentedPet() || player.isTransformed() || player.isCursedWeaponEquipped())
		{
			return;
		}
		if (!player.disarmWeapons())
		{
			return;
		}
		
		int petId;
		double price = 1;
		final int[] cost =
		{
			1800,
			7200,
			720000,
			6480000
		};
		final int[] ridetime =
		{
			30,
			60,
			600,
			1800
		};
		
		int value = petValue;
		if (value > 10)
		{
			petId = 12526;
			value -= 10;
			price /= 2;
		}
		else
		{
			petId = 12621;
		}
		
		if ((value < 1) || (value > 4))
		{
			return;
		}
		
		price *= cost[value - 1];
		final int time = ridetime[value - 1];
		if (!player.reduceAdena(ItemProcessType.FEE, (long) price, player.getLastFolkNPC(), true))
		{
			return;
		}
		
		player.mount(petId, 0, false);
		player.sendPacket(new SetupGauge(player.getObjectId(), 3, time * 1000));
		player.startRentPet(time);
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
