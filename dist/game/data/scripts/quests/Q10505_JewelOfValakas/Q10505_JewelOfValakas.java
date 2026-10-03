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
package quests.Q10505_JewelOfValakas;

import com.l2journey.Config;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.quest.QuestSound;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.quest.State;
import com.l2journey.gameserver.util.LocationUtil;

/**
 * Jewel of Valakas (10505)
 * @author Zoey76
 */
public class Q10505_JewelOfValakas extends Quest
{
	// NPC
	private static final int KLEIN = 31540;
	// Monster
	private static final int VALAKAS = 29028;
	// Items
	private static final int EMPTY_CRYSTAL = 21906;
	private static final int FILLED_CRYSTAL_VALAKAS_ENERGY = 21908;
	private static final int JEWEL_OF_VALAKAS = 21896;
	private static final int VACUALITE_FLOATING_STONE = 7267;
	// Misc
	private static final int MIN_LEVEL = 83;
	
	public Q10505_JewelOfValakas()
	{
		super(10505);
		addStartNpc(KLEIN);
		addTalkId(KLEIN);
		addKillId(VALAKAS);
		registerQuestItems(EMPTY_CRYSTAL, FILLED_CRYSTAL_VALAKAS_ENERGY);
	}
	
	@Override
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1) && LocationUtil.checkIfInRange(Config.ALT_PARTY_RANGE, npc, player, false))
		{
			takeItems(player, EMPTY_CRYSTAL, -1);
			giveItems(player, FILLED_CRYSTAL_VALAKAS_ENERGY, 1);
			playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
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
		
		String htmltext = null;
		if ((player.getLevel() >= MIN_LEVEL) && hasQuestItems(player, VACUALITE_FLOATING_STONE))
		{
			switch (event)
			{
				case "31540-05.htm", "31540-06.htm":
				{
					htmltext = event;
					break;
				}
				case "31540-07.html":
				{
					qs.startQuest();
					giveItems(player, EMPTY_CRYSTAL, 1);
					htmltext = event;
					break;
				}
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		executeForEachPlayer(killer, npc, isSummon, true, true);
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
				if (player.getLevel() < MIN_LEVEL)
				{
					htmltext = "31540-02.html";
				}
				else if (!hasQuestItems(player, VACUALITE_FLOATING_STONE))
				{
					htmltext = "31540-04.html";
				}
				else
				{
					htmltext = "31540-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						if (hasQuestItems(player, EMPTY_CRYSTAL))
						{
							htmltext = "31540-08.html";
						}
						else
						{
							giveItems(player, EMPTY_CRYSTAL, 1);
							htmltext = "31540-09.html";
						}
						break;
					}
					case 2:
					{
						giveItems(player, JEWEL_OF_VALAKAS, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						qs.exitQuest(false, true);
						htmltext = "31540-10.html";
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = "31540-03.html";
				break;
			}
		}
		return htmltext;
	}
}
