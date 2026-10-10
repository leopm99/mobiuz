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
package quests.Q00280_TheFoodChain;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.l2journey.Config;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.holders.ItemHolder;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.quest.State;
import com.l2journey.gameserver.util.LocationUtil;

/**
 * The Food Chain (280)
 * @author xban1x
 */
public class Q00280_TheFoodChain extends Quest
{
	// Npc
	private static final int BIXON = 32175;
	// Items
	private static final int GREY_KELTIR_TOOTH = 9809;
	private static final int BLACK_WOLF_TOOTH = 9810;
	// Monsters
	private static final Map<Integer, Integer> MONSTER_ITEM = new HashMap<>();
	private static final Map<Integer, List<ItemHolder>> MONSTER_CHANCE = new HashMap<>();
	static
	{
		MONSTER_ITEM.put(22229, GREY_KELTIR_TOOTH);
		MONSTER_ITEM.put(22230, GREY_KELTIR_TOOTH);
		MONSTER_ITEM.put(22231, GREY_KELTIR_TOOTH);
		MONSTER_ITEM.put(22232, BLACK_WOLF_TOOTH);
		MONSTER_ITEM.put(22233, BLACK_WOLF_TOOTH);
		MONSTER_CHANCE.put(22229, Arrays.asList(new ItemHolder(1000, 1)));
		MONSTER_CHANCE.put(22230, Arrays.asList(new ItemHolder(500, 1), new ItemHolder(1000, 2)));
		MONSTER_CHANCE.put(22231, Arrays.asList(new ItemHolder(1000, 2)));
		MONSTER_CHANCE.put(22232, Arrays.asList(new ItemHolder(1000, 3)));
		MONSTER_CHANCE.put(22233, Arrays.asList(new ItemHolder(500, 3), new ItemHolder(1000, 4)));
	}
	// Rewards
	private static final int[] REWARDS =
	{
		28,
		35,
		41,
		48,
		116,
	};
	// Misc
	private static final int MIN_LEVEL = 3;
	private static final int TEETH_COUNT = 25;
	
	public Q00280_TheFoodChain()
	{
		super(280);
		addStartNpc(BIXON);
		addTalkId(BIXON);
		addKillId(MONSTER_ITEM.keySet());
		registerQuestItems(GREY_KELTIR_TOOTH, BLACK_WOLF_TOOTH);
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
			case "32175-03.htm":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "32175-06.html":
			{
				if (hasAtLeastOneQuestItem(player, getRegisteredItemIds()))
				{
					final long greyTeeth = getQuestItemsCount(player, GREY_KELTIR_TOOTH);
					final long blackTeeth = getQuestItemsCount(player, BLACK_WOLF_TOOTH);
					giveAdena(player, 2 * (greyTeeth + blackTeeth), true);
					takeItems(player, -1, GREY_KELTIR_TOOTH, BLACK_WOLF_TOOTH);
					htmltext = event;
				}
				else
				{
					htmltext = "32175-07.html";
				}
				break;
			}
			case "32175-08.html":
			{
				htmltext = event;
				break;
			}
			case "32175-09.html":
			{
				qs.exitQuest(true, true);
				htmltext = event;
				break;
			}
			case "32175-11.html":
			{
				final long greyTeeth = getQuestItemsCount(player, GREY_KELTIR_TOOTH);
				final long blackTeeth = getQuestItemsCount(player, BLACK_WOLF_TOOTH);
				if ((greyTeeth + blackTeeth) >= TEETH_COUNT)
				{
					if (greyTeeth >= TEETH_COUNT)
					{
						takeItems(player, GREY_KELTIR_TOOTH, TEETH_COUNT);
					}
					else
					{
						takeItems(player, GREY_KELTIR_TOOTH, greyTeeth);
						takeItems(player, BLACK_WOLF_TOOTH, TEETH_COUNT - greyTeeth);
					}
					rewardItems(player, REWARDS[getRandom(5)], 1);
					htmltext = event;
				}
				else
				{
					htmltext = "32175-10.html";
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
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final QuestState qs = getQuestState(killer, false);
		if ((qs != null) && LocationUtil.checkIfInRange(Config.ALT_PARTY_RANGE, npc, killer, true))
		{
			final int chance = getRandom(1000);
			for (ItemHolder dropChance : MONSTER_CHANCE.get(npc.getId()))
			{
				if (chance < dropChance.getId())
				{
					giveItemRandomly(killer, MONSTER_ITEM.get(npc.getId()), dropChance.getCount(), 0, 1, true);
					break;
				}
			}
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		final QuestState qs = getQuestState(talker, true);
		String htmltext = getNoQuestMsg(talker);
		
		switch (qs.getState())
		{
			case State.CREATED:
			{
				htmltext = (talker.getLevel() >= MIN_LEVEL) ? "32175-01.htm" : "32175-02.htm";
				break;
			}
			case State.STARTED:
			{
				if (hasAtLeastOneQuestItem(talker, getRegisteredItemIds()))
				{
					htmltext = "32175-05.html";
				}
				else
				{
					htmltext = "32175-04.html";
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