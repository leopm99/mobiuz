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
package quests.Q00272_WrathOfAncestors;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.quest.QuestSound;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.quest.State;

/**
 * Wrath of Ancestors (272)
 * @author xban1x
 */
public class Q00272_WrathOfAncestors extends Quest
{
	// NPC
	private static final int LIVINA = 30572;
	// Items
	private static final int GRAVE_ROBBERS_HEAD = 1474;
	// Monsters
	private static final int[] MONSTERS =
	{
		20319, // Goblin Grave Robber
		20320, // Goblin Tomb Raider Leader
	};
	// Misc
	private static final int MIN_LEVEL = 5;
	
	public Q00272_WrathOfAncestors()
	{
		super(272);
		addStartNpc(LIVINA);
		addTalkId(LIVINA);
		addKillId(MONSTERS);
		registerQuestItems(GRAVE_ROBBERS_HEAD);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && event.equalsIgnoreCase("30572-04.htm"))
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
			giveItems(killer, GRAVE_ROBBERS_HEAD, 1);
			if (getQuestItemsCount(killer, GRAVE_ROBBERS_HEAD) >= 50)
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
				if ((player.getRace() == Race.ORC))
				{
					if ((player.getLevel() >= MIN_LEVEL))
					{
						htmltext = "30572-03.htm";
					}
					else
					{
						htmltext = "30572-02.htm";
					}
				}
				else
				{
					htmltext = "30572-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = "30572-05.html";
						break;
					}
					case 2:
					{
						giveAdena(player, 1500, true);
						qs.exitQuest(true, true);
						htmltext = "30572-06.html";
						break;
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