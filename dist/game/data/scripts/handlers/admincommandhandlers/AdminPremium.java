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
package handlers.admincommandhandlers;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import com.l2journey.Config;
import com.l2journey.EventsConfig;
import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.handler.IAdminCommandHandler;
import com.l2journey.gameserver.managers.PcCafePointsManager;
import com.l2journey.gameserver.managers.PremiumManager;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Mobius
 */
public class AdminPremium implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_premium_menu",
		"admin_premium_add1",
		"admin_premium_add2",
		"admin_premium_add3",
		"admin_premium_info",
		"admin_premium_remove"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.equals("admin_premium_menu"))
		{
			AdminHtml.showAdminHtml(activeChar, "premium_menu.htm");
		}
		else if (command.startsWith("admin_premium_add1"))
		{
			try
			{
				addPremiumStatus(activeChar, 1, command.substring(19));
			}
			catch (StringIndexOutOfBoundsException _)
			{
				activeChar.sendSysMessage("Please enter a valid account name.");
			}
		}
		else if (command.startsWith("admin_premium_add2"))
		{
			try
			{
				addPremiumStatus(activeChar, 2, command.substring(19));
			}
			catch (StringIndexOutOfBoundsException _)
			{
				activeChar.sendSysMessage("Please enter a valid account name.");
			}
		}
		else if (command.startsWith("admin_premium_add3"))
		{
			try
			{
				addPremiumStatus(activeChar, 3, command.substring(19));
			}
			catch (StringIndexOutOfBoundsException _)
			{
				activeChar.sendSysMessage("Please enter a valid account name.");
			}
		}
		else if (command.startsWith("admin_premium_info"))
		{
			try
			{
				viewPremiumInfo(activeChar, command.substring(19));
			}
			catch (StringIndexOutOfBoundsException _)
			{
				activeChar.sendSysMessage("Please enter a valid account name.");
			}
		}
		else if (command.startsWith("admin_premium_remove"))
		{
			try
			{
				removePremium(activeChar, command.substring(21));
			}
			catch (StringIndexOutOfBoundsException _)
			{
				activeChar.sendSysMessage("Please enter a valid account name.");
			}
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 0);
		html.setHtml(HtmCache.getInstance().getHtm(activeChar, "data/html/admin/premium_menu.htm"));
		activeChar.sendPacket(html);
		return true;
	}
	
	private void addPremiumStatus(Player admin, int months, String accountName)
	{
		if (!Config.PREMIUM_SYSTEM_ENABLED)
		{
			admin.sendMessage("Premium system is disabled.");
			return;
		}
		
		// TODO: Add check if account exists XD
		PremiumManager.getInstance().addPremiumTime(accountName, months * 30, TimeUnit.DAYS);
		admin.sendMessage("Account " + accountName + " will now have premium status until " + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(PremiumManager.getInstance().getPremiumExpiration(accountName)) + ".");
		if (EventsConfig.PC_CAFE_RETAIL_LIKE)
		{
			for (Player player : World.getInstance().getPlayers())
			{
				if (player.getAccountName().matches(accountName))
				{
					PcCafePointsManager.getInstance().run(player);
					break;
				}
			}
		}
	}
	
	private void viewPremiumInfo(Player admin, String accountName)
	{
		if (!Config.PREMIUM_SYSTEM_ENABLED)
		{
			admin.sendMessage("Premium system is disabled.");
			return;
		}
		
		if (PremiumManager.getInstance().getPremiumExpiration(accountName) > 0)
		{
			admin.sendMessage("Account " + accountName + " has premium status until " + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(PremiumManager.getInstance().getPremiumExpiration(accountName)) + ".");
		}
		else
		{
			admin.sendMessage("Account " + accountName + " has no premium status.");
		}
	}
	
	private void removePremium(Player admin, String accountName)
	{
		if (!Config.PREMIUM_SYSTEM_ENABLED)
		{
			admin.sendMessage("Premium system is disabled.");
			return;
		}
		
		if (PremiumManager.getInstance().getPremiumExpiration(accountName) > 0)
		{
			PremiumManager.getInstance().removePremiumStatus(accountName, true);
			admin.sendMessage("Account " + accountName + " has no longer premium status.");
		}
		else
		{
			admin.sendMessage("Account " + accountName + " has no premium status.");
		}
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}