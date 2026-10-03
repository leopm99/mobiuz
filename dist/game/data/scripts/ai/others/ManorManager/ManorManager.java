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
package ai.others.ManorManager;

import com.l2journey.Config;
import com.l2journey.gameserver.managers.CastleManorManager;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Merchant;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.ListenerRegisterType;
import com.l2journey.gameserver.model.events.annotations.Id;
import com.l2journey.gameserver.model.events.annotations.RegisterEvent;
import com.l2journey.gameserver.model.events.annotations.RegisterType;
import com.l2journey.gameserver.model.events.holders.actor.npc.OnNpcManorBypass;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.BuyListSeed;
import com.l2journey.gameserver.network.serverpackets.ExShowCropInfo;
import com.l2journey.gameserver.network.serverpackets.ExShowManorDefaultInfo;
import com.l2journey.gameserver.network.serverpackets.ExShowProcureCropDetail;
import com.l2journey.gameserver.network.serverpackets.ExShowSeedInfo;
import com.l2journey.gameserver.network.serverpackets.ExShowSellCropList;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

import ai.AbstractNpcAI;

/**
 * Manor manager AI.
 * @author malyelfik
 */
public class ManorManager extends AbstractNpcAI
{
	private static final int[] NPC =
	{
		35644,
		35645,
		35319,
		35366,
		36456,
		35512,
		35558,
		35229,
		35230,
		35231,
		35277,
		35103,
		35145,
		35187
	};
	
	public ManorManager()
	{
		addStartNpc(NPC);
		addFirstTalkId(NPC);
		addTalkId(NPC);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "manager-help-01.htm", "manager-help-02.htm", "manager-help-03.htm":
			{
				htmltext = event;
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (Config.ALLOW_MANOR)
		{
			final int castleId = npc.getTemplate().getParameters().getInt("manor_id", -1);
			if (!player.isGM() && player.isClanLeader() && (castleId == player.getClan().getCastleId()))
			{
				return "manager-lord.htm";
			}
			
			return "manager.htm";
		}
		
		return getHtm(player, "data/html/npcdefault.htm");
	}
	
	// @formatter:off
	@RegisterEvent(EventType.ON_NPC_MANOR_BYPASS)
	@RegisterType(ListenerRegisterType.NPC)
	@Id({35644, 35645, 35319, 35366, 36456, 35512, 35558, 35229, 35230, 35231, 35277, 35103, 35145, 35187})
	// @formatter:on
	public void onNpcManorBypass(OnNpcManorBypass evt)
	{
		final Player player = evt.getPlayer();
		if (CastleManorManager.getInstance().isUnderMaintenance())
		{
			player.sendPacket(SystemMessageId.THE_MANOR_SYSTEM_IS_CURRENTLY_UNDER_MAINTENANCE);
			return;
		}
		
		final Npc npc = evt.getTarget();
		final int templateId = npc.getTemplate().getParameters().getInt("manor_id", -1);
		final int castleId = (evt.getManorId() == -1) ? templateId : evt.getManorId();
		switch (evt.getRequest())
		{
			case 1: // Seed purchase
			{
				if (templateId != castleId)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.HERE_YOU_CAN_BUY_ONLY_SEEDS_OF_S1_MANOR).addCastleId(templateId));
					return;
				}
				player.sendPacket(new BuyListSeed(player.getAdena(), castleId));
				break;
			}
			case 2: // Crop sales
			{
				player.sendPacket(new ExShowSellCropList(player.getInventory(), castleId));
				break;
			}
			case 3: // Seed info
			{
				player.sendPacket(new ExShowSeedInfo(castleId, evt.isNextPeriod(), false));
				break;
			}
			case 4: // Crop info
			{
				player.sendPacket(new ExShowCropInfo(castleId, evt.isNextPeriod(), false));
				break;
			}
			case 5: // Basic info
			{
				player.sendPacket(new ExShowManorDefaultInfo(false));
				break;
			}
			case 6: // Buy harvester
			{
				((Merchant) npc).showBuyWindow(player, 300000 + npc.getId());
				break;
			}
			case 9: // Edit sales (Crop sales)
			{
				player.sendPacket(new ExShowProcureCropDetail(evt.getManorId()));
				break;
			}
			default:
			{
				LOGGER.warning(getClass().getSimpleName() + ": " + player + " send unknown request id " + evt.getRequest() + "!");
			}
		}
	}
	
	public static void main(String[] args)
	{
		new ManorManager();
	}
}