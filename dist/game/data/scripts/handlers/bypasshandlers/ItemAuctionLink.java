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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Level;

import com.l2journey.Config;
import com.l2journey.gameserver.handler.IBypassHandler;
import com.l2journey.gameserver.managers.ItemAuctionManager;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.itemauction.ItemAuction;
import com.l2journey.gameserver.model.itemauction.ItemAuctionInstance;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.ExItemAuctionInfoPacket;

public class ItemAuctionLink implements IBypassHandler
{
	private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss dd.MM.yyyy");
	
	private static final String[] COMMANDS =
	{
		"ItemAuction"
	};
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		if (!target.isNpc())
		{
			return false;
		}
		
		if (!Config.ALT_ITEM_AUCTION_ENABLED)
		{
			player.sendPacket(SystemMessageId.IT_IS_NOT_AN_AUCTION_PERIOD);
			return true;
		}
		
		final ItemAuctionInstance au = ItemAuctionManager.getInstance().getManagerInstance(target.getId());
		if (au == null)
		{
			return false;
		}
		
		try
		{
			final StringTokenizer st = new StringTokenizer(command);
			st.nextToken(); // bypass "ItemAuction"
			if (!st.hasMoreTokens())
			{
				return false;
			}
			
			final String cmd = st.nextToken();
			if ("show".equalsIgnoreCase(cmd))
			{
				if (!player.getClient().getFloodProtectors().canUseItemAuction() || player.isItemAuctionPolling())
				{
					return false;
				}
				
				final ItemAuction currentAuction = au.getCurrentAuction();
				final ItemAuction nextAuction = au.getNextAuction();
				if (currentAuction == null)
				{
					player.sendPacket(SystemMessageId.IT_IS_NOT_AN_AUCTION_PERIOD);
					
					if (nextAuction != null)
					{
						player.sendMessage("The next auction will begin on the " + SDF.format(new Date(nextAuction.getStartingTime())) + ".");
					}
					return true;
				}
				
				player.sendPacket(new ExItemAuctionInfoPacket(false, currentAuction, nextAuction));
			}
			else if ("cancel".equalsIgnoreCase(cmd))
			{
				boolean returned = false;
				for (ItemAuction auction : au.getAuctionsByBidder(player.getObjectId()))
				{
					if (auction.cancelBid(player))
					{
						returned = true;
					}
				}
				if (!returned)
				{
					player.sendPacket(SystemMessageId.THERE_ARE_NO_OFFERINGS_I_OWN_OR_I_MADE_A_BID_FOR);
				}
			}
			else
			{
				return false;
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "Exception in " + getClass().getSimpleName());
		}
		
		return true;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
