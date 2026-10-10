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
package quests.Q00293_TheHiddenVeins;

import com.l2journey.gameserver.managers.QuestManager;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.item.holders.ItemHolder;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.quest.QuestSound;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.quest.State;
import com.l2journey.gameserver.network.NpcStringId;

import ai.others.NewbieGuide.NewbieGuide;

/**
 * The Hidden Veins (293)
 * @author xban1x
 */
public class Q00293_TheHiddenVeins extends Quest
{
	// NPCs
	private static final int FILAUR = 30535;
	private static final int CHICHIRIN = 30539;
	// Monsters
	private static final int[] MONSTERS =
	{
		20446,
		20447,
		20448,
	};
	// Items
	private static final int CHRYSOLITE_ORE = 1488;
	private static final int TORN_MAP_FRAGMENT = 1489;
	private static final int HIDDEN_ORE_MAP = 1490;
	private static final ItemHolder SOULSHOTS_NO_GRADE_FOR_ROOKIES = new ItemHolder(5789, 6000);
	// Misc
	private static final int MIN_LEVEL = 6;
	private static final int REQUIRED_TORN_MAP_FRAGMENT = 4;
	private static final int GUIDE_MISSION = 41;
	
	public Q00293_TheHiddenVeins()
	{
		super(293);
		addStartNpc(FILAUR);
		addTalkId(FILAUR, CHICHIRIN);
		addKillId(MONSTERS);
		registerQuestItems(CHRYSOLITE_ORE, TORN_MAP_FRAGMENT, HIDDEN_ORE_MAP);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		String htmltext = null;
		if (qs == null)
		{
			return htmltext;
		}
		
		switch (event)
		{
			case "30535-04.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "30535-07.html":
			{
				qs.exitQuest(true, true);
				htmltext = event;
				break;
			}
			case "30535-08.html":
			{
				htmltext = event;
				break;
			}
			case "30539-03.html":
			{
				if (getQuestItemsCount(player, TORN_MAP_FRAGMENT) >= REQUIRED_TORN_MAP_FRAGMENT)
				{
					giveItems(player, HIDDEN_ORE_MAP, 1);
					takeItems(player, TORN_MAP_FRAGMENT, REQUIRED_TORN_MAP_FRAGMENT);
					htmltext = event;
				}
				else
				{
					htmltext = "30539-02.html";
				}
			}
			default:
			{
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if (qs != null)
		{
			final int chance = getRandom(100);
			if (chance > 50)
			{
				giveItems(killer, CHRYSOLITE_ORE, 1);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
			else if (chance < 5)
			{
				giveItems(killer, TORN_MAP_FRAGMENT, 1);
				playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
			}
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case FILAUR:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						if ((player.getRace() == Race.DWARF))
						{
							if ((player.getLevel() >= MIN_LEVEL))
							{
								htmltext = "30535-03.htm";
							}
							else
							{
								htmltext = "30535-02.htm";
							}
						}
						else
						{
							htmltext = "30535-01.htm";
						}
						break;
					}
					case State.STARTED:
					{
						if (hasAtLeastOneQuestItem(player, CHRYSOLITE_ORE, HIDDEN_ORE_MAP))
						{
							final long ores = getQuestItemsCount(player, CHRYSOLITE_ORE);
							final long maps = getQuestItemsCount(player, HIDDEN_ORE_MAP);
							giveAdena(player, (ores * 5) + (maps * 500) + (((ores + maps) >= 10) ? 2000 : 0), true);
							takeItems(player, -1, CHRYSOLITE_ORE, HIDDEN_ORE_MAP);
							
							if ((player.getLevel() < 25) && (getOneTimeQuestFlag(player, 57) == 0))
							{
								if (!player.isMageClass())
								{
									giveItems(player, SOULSHOTS_NO_GRADE_FOR_ROOKIES);
									playSound(player, "tutorial_voice_026");
								}
								
								setOneTimeQuestFlag(player, 57, 1);
							}
							
							// Newbie Guide.
							final Quest newbieGuide = QuestManager.getInstance().getQuest(NewbieGuide.class.getSimpleName());
							if (newbieGuide != null)
							{
								final QuestState newbieGuideQs = newbieGuide.getQuestState(player, true);
								if (!haveNRMemo(newbieGuideQs, GUIDE_MISSION))
								{
									setNRMemo(newbieGuideQs, GUIDE_MISSION);
									setNRMemoState(newbieGuideQs, GUIDE_MISSION, 1000);
									showOnScreenMsg(player, NpcStringId.ACQUISITION_OF_SOULSHOT_FOR_BEGINNERS_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
								}
								else if (((getNRMemoState(newbieGuideQs, GUIDE_MISSION) % 10000) / 1000) != 1)
								{
									setNRMemo(newbieGuideQs, GUIDE_MISSION);
									setNRMemoState(newbieGuideQs, GUIDE_MISSION, getNRMemoState(newbieGuideQs, GUIDE_MISSION) + 1000);
									showOnScreenMsg(player, NpcStringId.ACQUISITION_OF_SOULSHOT_FOR_BEGINNERS_COMPLETE_N_GO_FIND_THE_NEWBIE_GUIDE, 2, 5000);
								}
							}
							
							if ((ores > 0))
							{
								if ((maps > 0))
								{
									htmltext = "30535-10.html";
								}
								else
								{
									htmltext = "30535-06.html";
								}
							}
							else
							{
								htmltext = "30535-09.html";
							}
						}
						else
						{
							htmltext = "30535-05.html";
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
			case CHICHIRIN:
			{
				htmltext = "30539-01.html";
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
