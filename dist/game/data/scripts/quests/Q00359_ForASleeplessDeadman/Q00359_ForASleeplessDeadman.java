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
package quests.Q00359_ForASleeplessDeadman;

import java.util.HashMap;
import java.util.Map;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.quest.QuestState;

/**
 * For a Sleepless Deadman (359)
 * @author Adry_85
 */
public class Q00359_ForASleeplessDeadman extends Quest
{
	// NPC
	private static final int ORVEN = 30857;
	// Item
	private static final int REMAINS_OF_ADEN_RESIDENTS = 5869;
	// Misc
	private static final int MIN_LEVEL = 60;
	private static final int REMAINS_COUNT = 60;
	// Rewards
	private static final int[] REWARDS =
	{
		5494, // Sealed Dark Crystal Shield Fragment
		5495, // Sealed Shield of Nightmare Fragment
		6341, // Sealed Phoenix Earring Gemstone
		6342, // Sealed Majestic Earring Gemstone
		6343, // Sealed Phoenix Necklace Beads
		6344, // Sealed Majestic Necklace Beads
		6345, // Sealed Phoenix Ring Gemstone
		6346, // Sealed Majestic Ring Gemstone
	};
	// Mobs
	private static final Map<Integer, Double> MOBS = new HashMap<>();
	static
	{
		MOBS.put(21006, 0.365); // doom_servant
		MOBS.put(21007, 0.392); // doom_guard
		MOBS.put(21008, 0.503); // doom_archer
	}
	
	public Q00359_ForASleeplessDeadman()
	{
		super(359);
		addStartNpc(ORVEN);
		addTalkId(ORVEN);
		addKillId(MOBS.keySet());
		registerQuestItems(REMAINS_OF_ADEN_RESIDENTS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "30857-02.htm", "30857-03.htm", "30857-04.htm":
			{
				htmltext = event;
				break;
			}
			case "30857-05.htm":
			{
				qs.setMemoState(1);
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "30857-10.html":
			{
				rewardItems(player, REWARDS[getRandom(REWARDS.length)], 4);
				qs.exitQuest(true, true);
				htmltext = event;
				break;
			}
			default:
			{
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState qs = getRandomPartyMemberState(player, 1, 3, npc);
		if ((qs != null) && giveItemRandomly(qs.getPlayer(), npc, REMAINS_OF_ADEN_RESIDENTS, 1, REMAINS_COUNT, MOBS.get(npc.getId()), true))
		{
			qs.setCond(2, true);
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		if (qs.isCreated())
		{
			htmltext = (player.getLevel() >= MIN_LEVEL) ? "30857-01.htm" : "30857-06.html";
		}
		else if (qs.isStarted())
		{
			if (qs.isMemoState(1))
			{
				if (getQuestItemsCount(player, REMAINS_OF_ADEN_RESIDENTS) < REMAINS_COUNT)
				{
					htmltext = "30857-07.html";
				}
				else
				{
					takeItems(player, REMAINS_OF_ADEN_RESIDENTS, -1);
					qs.setMemoState(2);
					qs.setCond(3, true);
					htmltext = "30857-08.html";
				}
			}
			else if (qs.isMemoState(2))
			{
				htmltext = "30857-09.html";
			}
		}
		return htmltext;
	}
}
