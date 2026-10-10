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
package quests.Q00151_CureForFever;

import com.l2journey.gameserver.managers.QuestManager;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.quest.State;
import com.l2journey.gameserver.network.NpcStringId;

import ai.others.NewbieGuide.NewbieGuide;

/**
 * Cure for Fever (151)
 * @author malyelfik
 */
public class Q00151_CureForFever extends Quest
{
	// NPCs
	private static final int ELLIAS = 30050;
	private static final int YOHANES = 30032;
	// Monsters
	private static final int[] MOBS =
	{
		20103, // Giant Spider
		20106, // Talon Spider
		20108, // Blade Spider
	};
	// Items
	private static final int ROUND_SHIELD = 102;
	private static final int POISON_SAC = 703;
	private static final int FEVER_MEDICINE = 704;
	// Misc
	private static final int MIN_LEVEL = 15;
	private static final int CHANCE = 0;
	private static final int GUIDE_MISSION = 41;
	
	public Q00151_CureForFever()
	{
		super(151);
		addStartNpc(ELLIAS);
		addTalkId(ELLIAS, YOHANES);
		addKillId(MOBS);
		registerQuestItems(POISON_SAC, FEVER_MEDICINE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && event.equalsIgnoreCase("30050-03.htm"))
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
		if ((qs != null) && qs.isCond(1) && (getRandom(5) == CHANCE))
		{
			giveItems(killer, POISON_SAC, 1);
			qs.setCond(2, true);
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case ELLIAS:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() >= MIN_LEVEL) ? "30050-02.htm" : "30050-01.htm";
						break;
					}
					case State.STARTED:
					{
						if (qs.isCond(3) && hasQuestItems(player, FEVER_MEDICINE))
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
								newbieGuideQs.setState(State.COMPLETED);
							}
							
							giveItems(player, ROUND_SHIELD, 1);
							addExpAndSp(player, 13106, 613);
							qs.exitQuest(false, true);
							htmltext = "30050-06.html";
						}
						else if (qs.isCond(2) && hasQuestItems(player, POISON_SAC))
						{
							htmltext = "30050-05.html";
						}
						else
						{
							htmltext = "30050-04.html";
						}
						break;
					}
					case State.COMPLETED:
					{
						htmltext = getAlreadyCompletedMsg(player);
						break;
					}
					default:
					{
						break;
					}
				}
				break;
			}
			case YOHANES:
			{
				if (qs.isStarted())
				{
					if (qs.isCond(2) && hasQuestItems(player, POISON_SAC))
					{
						qs.setCond(3, true);
						takeItems(player, POISON_SAC, -1);
						giveItems(player, FEVER_MEDICINE, 1);
						htmltext = "30032-01.html";
					}
					else if (qs.isCond(3) && hasQuestItems(player, FEVER_MEDICINE))
					{
						htmltext = "30032-02.html";
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