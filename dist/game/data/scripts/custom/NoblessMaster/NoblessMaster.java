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
package custom.NoblessMaster;

import com.l2journey.Config;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.quest.QuestSound;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

import ai.AbstractNpcAI;

/**
 * @author Kinghanker
 */
public class NoblessMaster extends AbstractNpcAI
{
	private static final String HTML_N_36605_BLOCKED = "36605-blocked.htm";

	// Item
	private static final int NOBLESS_TIARA = 7694;
	
	private NoblessMaster()
	{
		addStartNpc(Config.NOBLESS_MASTER_NPCID);
		addTalkId(Config.NOBLESS_MASTER_NPCID);
		addFirstTalkId(Config.NOBLESS_MASTER_NPCID);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		if (!Config.NOBLESS_MASTER_ENABLED)
		{
			return "36605-disabled.htm";
		}
		
		if (event.equals("noblesse"))
		{
			if (player.isNoble())
			{
				return "36605-1.htm";
			}
			
			if (player.getLevel() < Config.NOBLESS_MASTER_LEVEL_REQUIREMENT)
			{
				return "36605-3.htm";
			}
			
			if (player.getPkKills() > 0)
			{
				player.sendMessage("You cannot become a Noblesse while flagged as a PK.");
				return HTML_N_36605_BLOCKED;
			}
			
			if (player.isInCombat())
			{
				player.sendMessage("You cannot become a Noblesse while in combat.");
				return HTML_N_36605_BLOCKED;
			}
			
			if (player.isOnEvent() || player.isRegisteredOnEvent() || player.isInOlympiadMode() || player.isInDuel())
			{
				player.sendMessage("You cannot become a Noblesse while participating in an event.");
				return HTML_N_36605_BLOCKED;
			}
			
			if (player.isTransformed())
			{
				player.sendMessage("You cannot become a Noblesse while transformed.");
				return HTML_N_36605_BLOCKED;
			}
			
			if (player.isSubClassActive())
			{
				player.sendMessage("You must be on your main class to become a Noblesse.");
				return HTML_N_36605_BLOCKED;
			}
			
			if (getQuestItemsCount(player, Config.NOBLESS_COIN) < Config.NOBLESS_COIN_COUNT)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S2_UNIT_S_OF_THE_ITEM_S1_IS_ARE_REQUIRED);
				sm.addItemName(Config.NOBLESS_COIN);
				sm.addLong(Config.NOBLESS_COIN_COUNT);
				player.sendPacket(sm);
				return "36605-2.htm";
			}
			
			if (Config.NOBLESS_MASTER_REWARD_TIARA)
			{
				giveItems(player, NOBLESS_TIARA, 1);
			}
			
			takeItems(player, Config.NOBLESS_COIN, Config.NOBLESS_COIN_COUNT);
			player.setNoble(true);
			player.sendPacket(QuestSound.ITEMSOUND_QUEST_FINISH.getPacket());
			return "36605-4.htm";
		}
		
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (!Config.NOBLESS_MASTER_ENABLED)
		{
			return "36605-disabled.htm";
		}
		return "36605.htm";
	}
	
	public static void main(String[] args)
	{
		new NoblessMaster();
	}
}
