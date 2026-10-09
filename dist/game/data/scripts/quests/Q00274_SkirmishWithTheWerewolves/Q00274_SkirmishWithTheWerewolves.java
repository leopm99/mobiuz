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
package quests.Q00274_SkirmishWithTheWerewolves;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.quest.QuestSound;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.quest.State;

/**
 * Skirmish with the Werewolves (274)
 * @author xban1x
 */
public class Q00274_SkirmishWithTheWerewolves extends Quest
{
	// NPC
	private static final int BRUKURSE = 30569;
	// Monsters
	private static final int[] MONSTERS =
	{
		20363, // Maraku Werewolf
		20364, // Maraku Werewolf Chieftain
	};
	// Items
	private static final int NECKLACE_OF_COURAGE = 1506;
	private static final int NECKLACE_OF_VALOR = 1507;
	private static final int WEREWOLF_HEAD = 1477;
	private static final int WEREWOLF_TOTEM = 1501;
	// Misc
	private static final int MIN_LEVEL = 9;
	
	public Q00274_SkirmishWithTheWerewolves()
	{
		super(274);
		addStartNpc(BRUKURSE);
		addTalkId(BRUKURSE);
		addKillId(MONSTERS);
		registerQuestItems(WEREWOLF_HEAD, WEREWOLF_TOTEM);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && event.equalsIgnoreCase("30569-04.htm"))
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
			giveItems(killer, WEREWOLF_HEAD, 1);
			if (getRandom(100) <= 5)
			{
				giveItems(killer, WEREWOLF_TOTEM, 1);
			}
			if (getQuestItemsCount(killer, WEREWOLF_HEAD) >= 40)
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
				if (hasAtLeastOneQuestItem(player, NECKLACE_OF_VALOR, NECKLACE_OF_COURAGE))
				{
					if ((player.getRace() == Race.ORC))
					{
						if ((player.getLevel() >= MIN_LEVEL))
						{
							htmltext = "30569-03.htm";
						}
						else
						{
							htmltext = "30569-02.html";
						}
					}
					else
					{
						htmltext = "30569-01.html";
					}
				}
				else
				{
					htmltext = "30569-08.html";
				}
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						htmltext = "30569-05.html";
						break;
					}
					case 2:
					{
						final long heads = getQuestItemsCount(player, WEREWOLF_HEAD);
						if (heads >= 40)
						{
							final long totems = getQuestItemsCount(player, WEREWOLF_TOTEM);
							giveAdena(player, (heads * 30) + (totems * 600) + 2300, true);
							qs.exitQuest(true, true);
							htmltext = (totems > 0) ? "30569-07.html" : "30569-06.html";
						}
					}
				}
				break;
			}
		}
		return htmltext;
	}
}
