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
package ai.areas.Hellbound.AI.NPC.Hude;

import com.l2journey.gameserver.data.xml.MultisellData;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;

import ai.AbstractNpcAI;
import ai.areas.Hellbound.HellboundEngine;

/**
 * Hude AI.
 * @author DS
 */
public class Hude extends AbstractNpcAI
{
	// NPCs
	private static final int HUDE = 32298;
	// Items
	private static final int BASIC_CERT = 9850;
	private static final int STANDART_CERT = 9851;
	private static final int PREMIUM_CERT = 9852;
	private static final int MARK_OF_BETRAYAL = 9676;
	private static final int LIFE_FORCE = 9681;
	private static final int CONTAINED_LIFE_FORCE = 9682;
	private static final int MAP = 9994;
	private static final int STINGER = 10012;
	
	public Hude()
	{
		addFirstTalkId(HUDE);
		addStartNpc(HUDE);
		addTalkId(HUDE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "scertif":
			{
				if ((HellboundEngine.getInstance().getLevel() > 3) && hasQuestItems(player, BASIC_CERT) && (getQuestItemsCount(player, MARK_OF_BETRAYAL) >= 30) && (getQuestItemsCount(player, STINGER) >= 60))
				{
					takeItems(player, MARK_OF_BETRAYAL, 30);
					takeItems(player, STINGER, 60);
					takeItems(player, BASIC_CERT, 1);
					giveItems(player, STANDART_CERT, 1);
					return "32298-04a.htm";
				}
				return "32298-04b.htm";
			}
			case "pcertif":
			{
				if ((HellboundEngine.getInstance().getLevel() > 6) && hasQuestItems(player, STANDART_CERT) && (getQuestItemsCount(player, LIFE_FORCE) >= 56) && (getQuestItemsCount(player, CONTAINED_LIFE_FORCE) >= 14))
				{
					takeItems(player, LIFE_FORCE, 56);
					takeItems(player, CONTAINED_LIFE_FORCE, 14);
					takeItems(player, STANDART_CERT, 1);
					giveItems(player, PREMIUM_CERT, 1);
					giveItems(player, MAP, 1);
					return "32298-06a.htm";
				}
				return "32298-06b.htm";
			}
			case "multisell1":
			{
				if (hasQuestItems(player, STANDART_CERT) || hasQuestItems(player, PREMIUM_CERT))
				{
					MultisellData.getInstance().separateAndSend(322980001, player, npc, false);
				}
				break;
			}
			case "multisell2":
			{
				if (hasQuestItems(player, PREMIUM_CERT))
				{
					MultisellData.getInstance().separateAndSend(322980002, player, npc, false);
				}
				break;
			}
			default:
			{
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = null;
		if (!hasAtLeastOneQuestItem(player, BASIC_CERT, STANDART_CERT, PREMIUM_CERT))
		{
			htmltext = "32298-01.htm";
		}
		else if (hasQuestItems(player, BASIC_CERT) && !hasAtLeastOneQuestItem(player, STANDART_CERT, PREMIUM_CERT))
		{
			htmltext = "32298-03.htm";
		}
		else if (hasQuestItems(player, STANDART_CERT) && !hasQuestItems(player, PREMIUM_CERT))
		{
			htmltext = "32298-05.htm";
		}
		else if (hasQuestItems(player, PREMIUM_CERT))
		{
			htmltext = "32298-07.htm";
		}
		return htmltext;
	}
}