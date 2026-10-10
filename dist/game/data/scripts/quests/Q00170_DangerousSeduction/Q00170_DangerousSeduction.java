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
package quests.Q00170_DangerousSeduction;

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
 * Dangerous Seduction (170)
 * @author malyelfik
 */
public class Q00170_DangerousSeduction extends Quest
{
	// NPC
	private static final int VELLIOR = 30305;
	
	// Monster
	private static final int MERKENIS = 27022;
	
	// Item
	private static final int NIGHTMARE_CRYSTAL = 1046;
	
	// Misc
	private static final int MIN_LEVEL = 21;
	
	public Q00170_DangerousSeduction()
	{
		super(170);
		addStartNpc(VELLIOR);
		addTalkId(VELLIOR);
		addKillId(MERKENIS);
		
		registerQuestItems(NIGHTMARE_CRYSTAL);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		if (event.equalsIgnoreCase("30305-04.htm"))
		{
			qs.startQuest();
			return event;
		}
		return null;
	}
	
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1))
		{
			qs.setCond(2, true);
			giveItems(player, NIGHTMARE_CRYSTAL, 1);
			npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), NpcStringId.SEND_MY_SOUL_TO_LICH_KING_ICARUS));
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
				if ((player.getRace() == Race.DARK_ELF))
				{
					if ((player.getLevel() >= MIN_LEVEL))
					{
						htmltext = "30305-01.htm";
					}
					else
					{
						htmltext = "30305-02.htm";
					}
				}
				else
				{
					htmltext = "30305-03.htm";
				}
				break;
			}
			case State.STARTED:
			{
				if (qs.isCond(1))
				{
					htmltext = "30305-05.html";
				}
				else
				{
					giveAdena(player, 102680, true);
					addExpAndSp(player, 38607, 4018);
					qs.exitQuest(false, true);
					htmltext = "30305-06.html";
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