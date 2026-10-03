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
package quests.Q10504_JewelOfAntharas;

import com.l2journey.Config;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.quest.QuestSound;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.quest.State;
import com.l2journey.gameserver.util.LocationUtil;

/**
 * Jewel of Antharas (10504)
 * @author Zoey76
 */
public class Q10504_JewelOfAntharas extends Quest
{
	// NPC
	private static final int THEODRIC = 30755;
	// Monster
	private static final int ANTHARAS = 29068;
	// Items
	private static final int CLEAR_CRYSTAL = 21905;
	private static final int FILLED_CRYSTAL_ANTHARAS_ENERGY = 21907;
	private static final int JEWEL_OF_ANTHARAS = 21898;
	private static final int PORTAL_STONE = 3865;
	// Misc
	private static final int MIN_LEVEL = 84;
	
	public Q10504_JewelOfAntharas()
	{
		super(10504);
		addStartNpc(THEODRIC);
		addTalkId(THEODRIC);
		addKillId(ANTHARAS);
		registerQuestItems(CLEAR_CRYSTAL, FILLED_CRYSTAL_ANTHARAS_ENERGY);
	}
	
	@Override
	public void actionForEachPlayer(Player player, Npc npc, boolean isSummon)
	{
		final QuestState qs = getQuestState(player, false);
		if ((qs != null) && qs.isCond(1) && LocationUtil.checkIfInRange(Config.ALT_PARTY_RANGE, npc, player, false))
		{
			takeItems(player, CLEAR_CRYSTAL, -1);
			giveItems(player, FILLED_CRYSTAL_ANTHARAS_ENERGY, 1);
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
		if ((player.getLevel() >= MIN_LEVEL) && hasQuestItems(player, PORTAL_STONE))
		{
			switch (event)
			{
				case "30755-05.htm", "30755-06.htm":
				{
					htmltext = event;
					break;
				}
				case "30755-07.html":
				{
					qs.startQuest();
					giveItems(player, CLEAR_CRYSTAL, 1);
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
					htmltext = "30755-02.html";
				}
				else if (!hasQuestItems(player, PORTAL_STONE))
				{
					htmltext = "30755-04.html";
				}
				else
				{
					htmltext = "30755-01.htm";
				}
				break;
			}
			case State.STARTED:
			{
				switch (qs.getCond())
				{
					case 1:
					{
						if (hasQuestItems(player, CLEAR_CRYSTAL))
						{
							htmltext = "30755-08.html";
						}
						else
						{
							giveItems(player, CLEAR_CRYSTAL, 1);
							htmltext = "30755-09.html";
						}
						break;
					}
					case 2:
					{
						giveItems(player, JEWEL_OF_ANTHARAS, 1);
						playSound(player, QuestSound.ITEMSOUND_QUEST_ITEMGET);
						qs.exitQuest(false, true);
						htmltext = "30755-10.html";
						break;
					}
				}
				break;
			}
			case State.COMPLETED:
			{
				htmltext = "30755-03.html";
				break;
			}
		}
		return htmltext;
	}
}
