/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package quests.Q00275_DarkWingedSpies;

import com.l2journey.Config;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.quest.State;
import com.l2journey.gameserver.util.LocationUtil;

/**
 * Dark Winged Spies (275)
 * @author xban1x
 */
public class Q00275_DarkWingedSpies extends Quest
{
	// Npc
	private static final int NERUGA_CHIEF_TANTUS = 30567;
	// Items
	private static final int DARKWING_BAT_FANG = 1478;
	private static final int VARANGKAS_PARASITE = 1479;
	// Monsters
	private static final int DARKWING_BAT = 20316;
	private static final int VARANGKAS_TRACKER = 27043;
	// Misc
	private static final int MIN_LEVEL = 11;
	private static final int FANG_PRICE = 60;
	private static final int MAX_BAT_FANG_COUNT = 70;
	
	public Q00275_DarkWingedSpies()
	{
		super(275);
		addStartNpc(NERUGA_CHIEF_TANTUS);
		addTalkId(NERUGA_CHIEF_TANTUS);
		addKillId(DARKWING_BAT, VARANGKAS_TRACKER);
		addCreatureSeeId(VARANGKAS_TRACKER);
		registerQuestItems(DARKWING_BAT_FANG, VARANGKAS_PARASITE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && event.equals("30567-03.htm"))
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
		if ((qs != null) && qs.isCond(1) && LocationUtil.checkIfInRange(Config.ALT_PARTY_RANGE, npc, killer, true))
		{
			final long count = getQuestItemsCount(killer, DARKWING_BAT_FANG);
			
			switch (npc.getId())
			{
				case DARKWING_BAT:
				{
					if (giveItemRandomly(killer, DARKWING_BAT_FANG, 1, MAX_BAT_FANG_COUNT, 1, true))
					{
						qs.setCond(2);
					}
					else if ((count > 10) && (count < 66) && (getRandom(100) < 10))
					{
						addSpawn(VARANGKAS_TRACKER, killer);
						giveItems(killer, VARANGKAS_PARASITE, 1);
					}
					break;
				}
				case VARANGKAS_TRACKER:
				{
					if ((count < 66) && hasQuestItems(killer, VARANGKAS_PARASITE))
					{
						if (giveItemRandomly(killer, DARKWING_BAT_FANG, 5, MAX_BAT_FANG_COUNT, 1, true))
						{
							qs.setCond(2);
						}
						takeItems(killer, VARANGKAS_PARASITE, -1);
					}
					break;
				}
			}
		}
	}
	
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer())
		{
			npc.setRunning();
			npc.asAttackable().addDamageHate(creature, 0, 1);
			npc.getAI().setIntention(Intention.ATTACK, creature);
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
				if ((talker.getRace() == Race.ORC))
				{
					if ((talker.getLevel() >= MIN_LEVEL))
					{
						htmltext = "30567-02.htm";
					}
					else
					{
						htmltext = "30567-01.htm";
					}
				}
				else
				{
					htmltext = "30567-00.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = "30567-05.html";
						break;
					}
					case 2:
					{
						final long count = getQuestItemsCount(talker, DARKWING_BAT_FANG);
						if (count >= MAX_BAT_FANG_COUNT)
						{
							giveAdena(talker, count * FANG_PRICE, true);
							qs.exitQuest(true, true);
							htmltext = "30567-05.html";
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
