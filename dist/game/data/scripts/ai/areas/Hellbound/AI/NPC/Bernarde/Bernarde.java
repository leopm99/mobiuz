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
package ai.areas.Hellbound.AI.NPC.Bernarde;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;

import ai.AbstractNpcAI;
import ai.areas.Hellbound.HellboundEngine;

/**
 * Bernarde AI.
 * @author DS
 */
public class Bernarde extends AbstractNpcAI
{
	// NPCs
	private static final int BERNARDE = 32300;
	// Misc
	private static final int NATIVE_TRANSFORM = 101;
	// Items
	private static final int HOLY_WATER = 9673;
	private static final int DARION_BADGE = 9674;
	private static final int TREASURE = 9684;
	
	public Bernarde()
	{
		addFirstTalkId(BERNARDE);
		addStartNpc(BERNARDE);
		addTalkId(BERNARDE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "HolyWater":
			{
				if ((HellboundEngine.getInstance().getLevel() == 2) //
					&& (player.getInventory().getInventoryItemCount(DARION_BADGE, -1, false) >= 5) //
					&& player.exchangeItemsById(ItemProcessType.QUEST, npc, DARION_BADGE, 5, HOLY_WATER, 1, true))
				{
					return "32300-02b.htm";
				}
				return "32300-02c.htm";
			}
			case "Treasure":
			{
				if ((HellboundEngine.getInstance().getLevel() == 3) && hasQuestItems(player, TREASURE))
				{
					HellboundEngine.getInstance().updateTrust((int) (getQuestItemsCount(player, TREASURE) * 1000), true);
					takeItems(player, TREASURE, -1);
					return "32300-02d.htm";
				}
				return "32300-02e.htm";
			}
			case "rumors":
			{
				return "32300-" + HellboundEngine.getInstance().getLevel() + "r.htm";
			}
		}
		return event;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		switch (HellboundEngine.getInstance().getLevel())
		{
			case 0, 1:
			{
				return isTransformed(player) ? "32300-01a.htm" : "32300-01.htm";
			}
			case 2:
			{
				return isTransformed(player) ? "32300-02.htm" : "32300-03.htm";
			}
			case 3:
			{
				return isTransformed(player) ? "32300-01c.htm" : "32300-03.htm";
			}
			case 4:
			{
				return isTransformed(player) ? "32300-01d.htm" : "32300-03.htm";
			}
			default:
			{
				return isTransformed(player) ? "32300-01f.htm" : "32300-03.htm";
			}
		}
	}
	
	private static boolean isTransformed(Player player)
	{
		return player.isTransformed() && (player.getTransformation().getId() == NATIVE_TRANSFORM);
	}
}