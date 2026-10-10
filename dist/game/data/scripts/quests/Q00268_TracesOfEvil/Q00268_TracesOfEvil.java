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
package quests.Q00268_TracesOfEvil;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.quest.QuestSound;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.quest.State;

/**
 * Traces of Evil (268)
 * @author xban1x
 */
public class Q00268_TracesOfEvil extends Quest
{
	// NPC
	private static final int KUNAI = 30559;
	// Item
	private static final int CONTAMINATED_KASHA_SPIDER_VENOM = 10869;
	// Monsters
	private static final int[] MONSTERS =
	{
		20474, // Kasha Spider
		20476, // Kasha Fang Spider
		20478, // Kasha Blade Spider
	};
	// Misc
	private static final int MIN_LEVEL = 15;
	
	public Q00268_TracesOfEvil()
	{
		super(268);
		addStartNpc(KUNAI);
		addTalkId(KUNAI);
		addKillId(MONSTERS);
		registerQuestItems(CONTAMINATED_KASHA_SPIDER_VENOM);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && event.equalsIgnoreCase("30559-03.htm"))
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
			giveItems(killer, CONTAMINATED_KASHA_SPIDER_VENOM, 1);
			if (getQuestItemsCount(killer, CONTAMINATED_KASHA_SPIDER_VENOM) >= 30)
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
				htmltext = (player.getLevel() >= MIN_LEVEL) ? "30559-02.htm" : "30559-01.htm";
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = (!hasQuestItems(player, CONTAMINATED_KASHA_SPIDER_VENOM)) ? "30559-04.html" : "30559-05.html";
						break;
					}
					case 2:
					{
						if (getQuestItemsCount(player, CONTAMINATED_KASHA_SPIDER_VENOM) >= 30)
						{
							giveAdena(player, 2474, true);
							addExpAndSp(player, 8738, 409);
							qs.exitQuest(true, true);
							htmltext = "30559-06.html";
						}
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
