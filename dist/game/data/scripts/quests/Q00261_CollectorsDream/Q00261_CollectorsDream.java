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
package quests.Q00261_CollectorsDream;

import com.l2journey.Config;
import com.l2journey.gameserver.managers.QuestManager;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.quest.State;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.util.LocationUtil;

import ai.others.NewbieGuide.NewbieGuide;

/**
 * Collector's Dream (261)
 * @author xban1x
 */
public class Q00261_CollectorsDream extends Quest
{
	// NPC
	private static final int ALSHUPES = 30222;
	// Monsters
	private static final int[] MONSTERS =
	{
		20308, // Hook Spider
		20460, // Crimson Spider
		20466, // Pincer Spider
	};
	// Item
	private static final int SPIDER_LEG = 1087;
	// Misc
	private static final int MIN_LEVEL = 15;
	private static final int MAX_LEG_COUNT = 8;
	private static final int GUIDE_MISSION = 41;
	
	public Q00261_CollectorsDream()
	{
		super(261);
		addStartNpc(ALSHUPES);
		addTalkId(ALSHUPES);
		addKillId(MONSTERS);
		registerQuestItems(SPIDER_LEG);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && event.equals("30222-03.htm"))
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
		if ((qs != null) && qs.isCond(1) && LocationUtil.checkIfInRange(Config.ALT_PARTY_RANGE, npc, killer, true) && giveItemRandomly(killer, SPIDER_LEG, 1, MAX_LEG_COUNT, 1, true))
		{
			qs.setCond(2);
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
				htmltext = (player.getLevel() >= MIN_LEVEL) ? "30222-02.htm" : "30222-01.htm";
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = "30222-04.html";
						break;
					}
					case 2:
					{
						if (getQuestItemsCount(player, SPIDER_LEG) >= MAX_LEG_COUNT)
						{
							// Newbie Guide.
							final Quest newbieGuide = QuestManager.getInstance().getQuest(NewbieGuide.class.getSimpleName());
							if (newbieGuide != null)
							{
								final QuestState newbieGuideQs = newbieGuide.getQuestState(player, true);
								if (!haveNRMemo(newbieGuideQs, GUIDE_MISSION))
								{
									setNRMemo(newbieGuideQs, GUIDE_MISSION);
									setNRMemoState(newbieGuideQs, GUIDE_MISSION, 100000);
									showOnScreenMsg(player, NpcStringId.LAST_DUTY_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
								}
								else if (((getNRMemoState(newbieGuideQs, GUIDE_MISSION) % 100000000) / 10000000) != 1)
								{
									setNRMemo(newbieGuideQs, GUIDE_MISSION);
									setNRMemoState(newbieGuideQs, GUIDE_MISSION, getNRMemoState(newbieGuideQs, GUIDE_MISSION) + 10000000);
									showOnScreenMsg(player, NpcStringId.LAST_DUTY_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
								}
							}
							
							giveAdena(player, 1000, true);
							addExpAndSp(player, 2000, 0);
							qs.exitQuest(true, true);
							htmltext = "30222-05.html";
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
