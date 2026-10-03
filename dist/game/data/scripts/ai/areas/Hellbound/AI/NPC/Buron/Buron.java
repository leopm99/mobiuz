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
package ai.areas.Hellbound.AI.NPC.Buron;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;

import ai.AbstractNpcAI;
import ai.areas.Hellbound.HellboundEngine;

/**
 * Buron AI.
 * @author DS
 */
public class Buron extends AbstractNpcAI
{
	private static final int BURON = 32345;
	private static final int HELMET = 9669;
	private static final int TUNIC = 9670;
	private static final int PANTS = 9671;
	private static final int DARION_BADGE = 9674;
	
	public Buron()
	{
		addFirstTalkId(BURON);
		addStartNpc(BURON);
		addTalkId(BURON);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext;
		if ("Rumor".equalsIgnoreCase(event))
		{
			htmltext = "32345-" + HellboundEngine.getInstance().getLevel() + "r.htm";
		}
		else
		{
			if (HellboundEngine.getInstance().getLevel() < 2)
			{
				htmltext = "32345-lowlvl.htm";
			}
			else
			{
				if (getQuestItemsCount(player, DARION_BADGE) >= 10)
				{
					takeItems(player, DARION_BADGE, 10);
					if (event.equalsIgnoreCase("Tunic"))
					{
						player.addItem(ItemProcessType.QUEST, TUNIC, 1, npc, true);
					}
					else if (event.equalsIgnoreCase("Helmet"))
					{
						player.addItem(ItemProcessType.QUEST, HELMET, 1, npc, true);
					}
					else if (event.equalsIgnoreCase("Pants"))
					{
						player.addItem(ItemProcessType.QUEST, PANTS, 1, npc, true);
					}
					htmltext = null;
				}
				else
				{
					htmltext = "32345-noitems.htm";
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		getQuestState(player, true);
		switch (HellboundEngine.getInstance().getLevel())
		{
			case 1:
			{
				return "32345-01.htm";
			}
			case 2, 3, 4:
			{
				return "32345-02.htm";
			}
			default:
			{
				return "32345-01a.htm";
			}
		}
	}
}