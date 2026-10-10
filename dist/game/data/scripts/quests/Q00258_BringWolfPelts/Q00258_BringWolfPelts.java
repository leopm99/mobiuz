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
package quests.Q00258_BringWolfPelts;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.quest.QuestSound;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.quest.State;

/**
 * Bring Wolf Pelts (258)
 * @author xban1x
 */
public class Q00258_BringWolfPelts extends Quest
{
	// Npc
	private static final int LECTOR = 30001;
	// Item
	private static final int WOLF_PELT = 702;
	// Monsters
	private static final int[] MONSTERS =
	{
		20120, // Wolf
		20442, // Elder Wolf
	};
	// Rewards
	private static final Map<Integer, Integer> REWARDS = new HashMap<>();
	static
	{
		REWARDS.put(390, 1); // Cotton Shirt
		REWARDS.put(29, 6); // Leather Pants
		REWARDS.put(22, 9); // Leather Shirt
		REWARDS.put(1119, 13); // Short Leather Gloves
		REWARDS.put(426, 16); // Tunic
	}
	// Misc
	private static final int MIN_LEVEL = 3;
	private static final int WOLF_PELT_COUNT = 40;
	
	public Q00258_BringWolfPelts()
	{
		super(258);
		addStartNpc(LECTOR);
		addTalkId(LECTOR);
		addKillId(MONSTERS);
		registerQuestItems(WOLF_PELT);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && event.equalsIgnoreCase("30001-03.html"))
		{
			qs.startQuest();
			return event;
		}
		return null;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && qs.isCond(1))
		{
			giveItems(killer, WOLF_PELT, 1);
			if (getQuestItemsCount(killer, WOLF_PELT) >= WOLF_PELT_COUNT)
			{
				qs.setCond(2, true);
			}
			else
			{
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (qs.getState())
		{
			case State.CREATED:
			{
				htmltext = (player.getLevel() >= MIN_LEVEL) ? "30001-02.htm" : "30001-01.html";
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = "30001-04.html";
						break;
					}
					case 2:
					{
						if (getQuestItemsCount(player, WOLF_PELT) >= WOLF_PELT_COUNT)
						{
							final int chance = getRandom(16);
							for (Entry<Integer, Integer> reward : REWARDS.entrySet())
							{
								if (chance < reward.getValue())
								{
									giveItems(player, reward.getKey(), 1);
									break;
								}
							}
							qs.exitQuest(true, true);
							htmltext = "30001-05.html";
							break;
						}
					}
					default:
					{
						break;
					}
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
}
