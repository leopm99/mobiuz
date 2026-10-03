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
package ai.areas.Hellbound.AI.NPC.Kief;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;

import ai.AbstractNpcAI;
import ai.areas.Hellbound.HellboundEngine;

/**
 * Kief AI.
 * @author DS
 */
public class Kief extends AbstractNpcAI
{
	// NPCs
	private static final int KIEF = 32354;
	// Items
	private static final int BOTTLE = 9672; // Magic Bottle
	private static final int DARION_BADGE = 9674; // Darion's Badge
	private static final int DIM_LIFE_FORCE = 9680; // Dim Life Force
	private static final int LIFE_FORCE = 9681; // Life Force
	private static final int CONTAINED_LIFE_FORCE = 9682; // Contained Life Force
	private static final int STINGER = 10012; // Scorpion Poison Stinger
	
	public Kief()
	{
		addFirstTalkId(KIEF);
		addStartNpc(KIEF);
		addTalkId(KIEF);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "32354-11g.htm":
			{
				htmltext = event;
				break;
			}
			case "Badges":
			{
				switch (HellboundEngine.getInstance().getLevel())
				{
					case 2, 3:
					{
						if (hasQuestItems(player, DARION_BADGE))
						{
							HellboundEngine.getInstance().updateTrust((int) getQuestItemsCount(player, DARION_BADGE) * 10, true);
							takeItems(player, DARION_BADGE, -1);
							return "32354-10.htm";
						}
						break;
					}
					default:
					{
						htmltext = "32354-10a.htm";
						break;
					}
				}
				break;
			}
			case "Bottle":
			{
				if (HellboundEngine.getInstance().getLevel() >= 7)
				{
					if (getQuestItemsCount(player, STINGER) >= 20)
					{
						takeItems(player, STINGER, 20);
						giveItems(player, BOTTLE, 1);
						htmltext = "32354-11h.htm";
					}
					else
					{
						htmltext = "32354-11i.htm";
					}
				}
				break;
			}
			case "dlf":
			{
				if (HellboundEngine.getInstance().getLevel() == 7)
				{
					if (hasQuestItems(player, DIM_LIFE_FORCE))
					{
						HellboundEngine.getInstance().updateTrust((int) getQuestItemsCount(player, DIM_LIFE_FORCE) * 20, true);
						takeItems(player, DIM_LIFE_FORCE, -1);
						htmltext = "32354-11a.htm";
					}
					else
					{
						htmltext = "32354-11b.htm";
					}
				}
				break;
			}
			case "lf":
			{
				if (HellboundEngine.getInstance().getLevel() == 7)
				{
					if (hasQuestItems(player, LIFE_FORCE))
					{
						HellboundEngine.getInstance().updateTrust((int) getQuestItemsCount(player, LIFE_FORCE) * 80, true);
						takeItems(player, LIFE_FORCE, -1);
						htmltext = "32354-11c.htm";
					}
					else
					{
						htmltext = "32354-11d.htm";
					}
				}
				break;
			}
			case "clf":
			{
				if (HellboundEngine.getInstance().getLevel() == 7)
				{
					if (hasQuestItems(player, CONTAINED_LIFE_FORCE))
					{
						HellboundEngine.getInstance().updateTrust((int) getQuestItemsCount(player, CONTAINED_LIFE_FORCE) * 200, true);
						takeItems(player, CONTAINED_LIFE_FORCE, -1);
						htmltext = "32354-11e.htm";
					}
					else
					{
						htmltext = "32354-11f.htm";
					}
				}
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		switch (HellboundEngine.getInstance().getLevel())
		{
			case 1:
			{
				return "32354-01.htm";
			}
			case 2, 3:
			{
				return "32354-01a.htm";
			}
			case 4:
			{
				return "32354-01e.htm";
			}
			case 5:
			{
				return "32354-01d.htm";
			}
			case 6:
			{
				return "32354-01b.htm";
			}
			case 7:
			{
				return "32354-01c.htm";
			}
			default:
			{
				return "32354-01f.htm";
			}
		}
	}
}