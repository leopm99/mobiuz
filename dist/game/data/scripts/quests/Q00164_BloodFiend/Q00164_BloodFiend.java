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
package quests.Q00164_BloodFiend;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.quest.State;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.NpcSay;

/**
 * Blood Fiend (164)
 * @author xban1x
 */
public class Q00164_BloodFiend extends Quest
{
	// NPC
	private static final int CREAMEES = 30149;
	// Monster
	private static final int KIRUNAK = 27021;
	// Item
	private static final int KIRUNAK_SKULL = 1044;
	// Misc
	private static final int MIN_LEVEL = 21;
	
	public Q00164_BloodFiend()
	{
		super(164);
		addStartNpc(CREAMEES);
		addTalkId(CREAMEES);
		addKillId(KIRUNAK);
		registerQuestItems(KIRUNAK_SKULL);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && event.equals("30149-04.htm"))
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
			npc.broadcastPacket(new NpcSay(npc, ChatType.GENERAL, NpcStringId.I_HAVE_FULFILLED_MY_CONTRACT_WITH_TRADER_CREAMEES));
			giveItems(killer, KIRUNAK_SKULL, 1);
			qs.setCond(2, true);
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
				if ((player.getRace() != Race.DARK_ELF))
				{
					if (player.getLevel() >= MIN_LEVEL)
					{
						htmltext = "30149-03.htm";
					}
					else
					{
						htmltext = "30149-02.htm";
					}
				}
				else
				{
					htmltext = "30149-00.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(2) && hasQuestItems(player, KIRUNAK_SKULL))
				{
					giveAdena(player, 42130, true);
					addExpAndSp(player, 35637, 1854);
					qs.exitQuest(false, true);
					htmltext = "30149-06.html";
				}
				else
				{
					htmltext = "30149-05.html";
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
		return htmltext;
	}
}
