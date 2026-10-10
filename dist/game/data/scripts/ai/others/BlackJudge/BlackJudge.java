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
package ai.others.BlackJudge;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.EtcStatusUpdate;

import ai.AbstractNpcAI;

/**
 * Black Judge AI.
 * @author St3eT
 */
public class BlackJudge extends AbstractNpcAI
{
	// NPC
	private static final int BLACK_JUDGE = 30981;
	// Misc
	// @formatter:off
	private static final int[] COSTS =
	{
		3600, 8640, 25200, 50400, 86400, 144000
	};
	// @formatter:on
	
	private BlackJudge()
	{
		addStartNpc(BLACK_JUDGE);
		addTalkId(BLACK_JUDGE);
		addFirstTalkId(BLACK_JUDGE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		final int level = ((player.getExpertiseLevel() < 5) ? player.getExpertiseLevel() : 5);
		switch (event)
		{
			case "remove_info":
			{
				htmltext = "30981-0" + (level + 1) + ".html";
				break;
			}
			case "remove_dp":
			{
				if (player.getDeathPenaltyBuffLevel() > 0)
				{
					final int cost = COSTS[level];
					if (player.getAdena() >= cost)
					{
						takeItems(player, Inventory.ADENA_ID, cost);
						player.setDeathPenaltyBuffLevel(player.getDeathPenaltyBuffLevel() - 1);
						player.sendPacket(SystemMessageId.YOUR_DEATH_PENALTY_HAS_BEEN_LIFTED);
						player.sendPacket(new EtcStatusUpdate(player));
					}
					else
					{
						htmltext = "30981-07.html";
					}
				}
				else
				{
					htmltext = "30981-08.html";
				}
				break;
			}
			default:
			{
				break;
			}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new BlackJudge();
	}
}