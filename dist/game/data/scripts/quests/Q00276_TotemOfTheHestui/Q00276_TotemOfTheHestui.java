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
package quests.Q00276_TotemOfTheHestui;

import java.util.ArrayList;
import java.util.List;

import com.l2journey.Config;
import com.l2journey.gameserver.managers.QuestManager;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.item.holders.ItemHolder;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.quest.State;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.util.LocationUtil;

import ai.others.NewbieGuide.NewbieGuide;

/**
 * Totem of the Hestui (276)
 * @author xban1x
 */
public class Q00276_TotemOfTheHestui extends Quest
{
	// NPC
	private static final int TANAPI = 30571;
	// Items
	private static final int KASHA_PARASITE = 1480;
	private static final int KASHA_CRYSTAL = 1481;
	// Monsters
	private static final int KASHA_BEAR = 20479;
	private static final int KASHA_BEAR_TOTEM = 27044;
	// Rewards
	private static final int[] REWARDS =
	{
		29,
		1500,
	};
	// Misc
	private static final List<ItemHolder> SPAWN_CHANCES = new ArrayList<>();
	static
	{
		SPAWN_CHANCES.add(new ItemHolder(79, 100));
		SPAWN_CHANCES.add(new ItemHolder(69, 20));
		SPAWN_CHANCES.add(new ItemHolder(59, 15));
		SPAWN_CHANCES.add(new ItemHolder(49, 10));
		SPAWN_CHANCES.add(new ItemHolder(39, 2));
	}
	private static final int MIN_LEVEL = 15;
	private static final int GUIDE_MISSION = 41;
	
	public Q00276_TotemOfTheHestui()
	{
		super(276);
		addStartNpc(TANAPI);
		addTalkId(TANAPI);
		addKillId(KASHA_BEAR, KASHA_BEAR_TOTEM);
		registerQuestItems(KASHA_PARASITE, KASHA_CRYSTAL);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && event.equals("30571-03.htm"))
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
		if ((qs != null) && qs.isCond(1) && LocationUtil.checkIfInRange(Config.ALT_PARTY_RANGE, killer, npc, true))
		{
			switch (npc.getId())
			{
				case KASHA_BEAR:
				{
					final long chance1 = getQuestItemsCount(killer, KASHA_PARASITE);
					final int chance2 = getRandom(100);
					boolean chance3 = true;
					for (ItemHolder spawnChance : SPAWN_CHANCES)
					{
						if ((chance1 >= spawnChance.getId()) && (chance2 <= spawnChance.getCount()))
						{
							addSpawn(KASHA_BEAR_TOTEM, killer);
							takeItems(killer, KASHA_PARASITE, -1);
							chance3 = false;
							break;
						}
					}
					if (chance3)
					{
						giveItemRandomly(killer, KASHA_PARASITE, 1, 0, 1, true);
					}
					break;
				}
				case KASHA_BEAR_TOTEM:
				{
					if (giveItemRandomly(killer, KASHA_CRYSTAL, 1, 1, 1, true))
					{
						qs.setCond(2);
					}
					break;
				}
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
						htmltext = "30571-02.htm";
					}
					else
					{
						htmltext = "30571-01.htm";
					}
				}
				else
				{
					htmltext = "30571-00.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = "30571-04.html";
						break;
					}
					case 2:
					{
						if (hasQuestItems(player, KASHA_CRYSTAL))
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
							
							for (int reward : REWARDS)
							{
								rewardItems(player, reward, 1);
							}
							qs.exitQuest(true, true);
							htmltext = "30571-05.html";
						}
						break;
					}
				}
				break;
			}
		}
		return htmltext;
	}
}
