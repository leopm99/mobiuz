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
package quests.Q00608_SlayTheEnemyCommanderKetra;

import com.l2journey.Config;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.quest.State;
import com.l2journey.gameserver.util.LocationUtil;

/**
 * Slay the Enemy Commander! (Ketra) (608)
 * @author malyelfik
 */
public class Q00608_SlayTheEnemyCommanderKetra extends Quest
{
	// NPC
	private static final int KADUN = 31370;
	// Monster
	private static final int MOS = 25312;
	// Items
	private static final int MOS_HEAD = 7236;
	private static final int WISDOM_TOTEM = 7220;
	private static final int KETRA_ALLIANCE_FOUR = 7214;
	// Misc
	private static final int MIN_LEVEL = 75;
	
	public Q00608_SlayTheEnemyCommanderKetra()
	{
		super(608);
		addStartNpc(KADUN);
		addTalkId(KADUN);
		addKillId(MOS);
		registerQuestItems(MOS_HEAD);
	}
	
	@Override
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1) && LocationUtil.checkIfInRange(Config.ALT_PARTY_RANGE, npc, player, false))
		{
			giveItems(player, MOS_HEAD, 1);
			qs.setCond(2, true);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		String htmltext = event;
		switch (event)
		{
			case "31370-04.htm":
			{
				qs.startQuest();
				break;
			}
			case "31370-07.html":
			{
				if (hasQuestItems(player, MOS_HEAD) && qs.isCond(2))
				{
					giveItems(player, WISDOM_TOTEM, 1);
					addExpAndSp(player, 10000, 0);
					qs.exitQuest(true, true);
				}
				else
				{
					htmltext = getNoQuestMsg(player);
				}
				break;
			}
			default:
			{
				htmltext = null;
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		executeForEachPlayer(killer, npc, isSummon, true, false);
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
				if ((player.getLevel() >= MIN_LEVEL))
				{
					if ((hasQuestItems(player, KETRA_ALLIANCE_FOUR)))
					{
						htmltext = "31370-01.htm";
					}
					else
					{
						htmltext = "31370-02.htm";
					}
				}
				else
				{
					htmltext = "31370-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				htmltext = (qs.isCond(2) && hasQuestItems(player, MOS_HEAD)) ? "31370-05.html" : "31370-06.html";
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