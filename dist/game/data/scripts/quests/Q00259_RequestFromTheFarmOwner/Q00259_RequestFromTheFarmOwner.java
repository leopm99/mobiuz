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
package quests.Q00259_RequestFromTheFarmOwner;

import java.util.HashMap;
import java.util.Map;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.holders.ItemHolder;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.quest.QuestSound;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.quest.State;

/**
 * Request from the Farm Owner (259)
 * @author xban1x
 */
public class Q00259_RequestFromTheFarmOwner extends Quest
{
	// Npcs
	private static final int EDMOND = 30497;
	private static final int MARIUS = 30405;
	// Monsters
	private static final int[] MONSTERS =
	{
		20103, // Giant Spider
		20106, // Talon Spider
		20108, // Blade Spider
	};
	// Items
	private static final int SPIDER_SKIN = 1495;
	// Misc
	private static final int MIN_LEVEL = 15;
	private static final int SKIN_COUNT = 10;
	private static final int SKIN_REWARD = 25;
	private static final int SKIN_BONUS = 250;
	private static final Map<String, ItemHolder> CONSUMABLES = new HashMap<>();
	static
	{
		CONSUMABLES.put("30405-04.html", new ItemHolder(1061, 2)); // Greater Healing Potion
		CONSUMABLES.put("30405-05.html", new ItemHolder(17, 250)); // Wooden Arrow
		CONSUMABLES.put("30405-05a.html", new ItemHolder(1835, 60)); // Soulshot: No Grade
		CONSUMABLES.put("30405-05c.html", new ItemHolder(2509, 30)); // Spiritshot: No Grade
	}
	
	public Q00259_RequestFromTheFarmOwner()
	{
		super(259);
		addStartNpc(EDMOND);
		addTalkId(EDMOND, MARIUS);
		addKillId(MONSTERS);
		registerQuestItems(SPIDER_SKIN);
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
			case "30405-03.html", "30405-05b.html", "30405-05d.html", "30497-07.html":
			{
				htmltext = event;
				break;
			}
			case "30405-04.html", "30405-05.html", "30405-05a.html", "30405-05c.html":
			{
				if (getQuestItemsCount(player, SPIDER_SKIN) >= SKIN_COUNT)
				{
					giveItems(player, CONSUMABLES.get(event));
					takeItems(player, SPIDER_SKIN, SKIN_COUNT);
					htmltext = event;
				}
				break;
			}
			case "30405-06.html":
			{
				htmltext = (getQuestItemsCount(player, SPIDER_SKIN) >= SKIN_COUNT) ? event : "30405-07.html";
				break;
			}
			case "30497-03.html":
			{
				qs.startQuest();
				htmltext = event;
				break;
			}
			case "30497-06.html":
			{
				qs.exitQuest(true, true);
				htmltext = event;
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
		if (qs != null)
		{
			giveItems(killer, SPIDER_SKIN, 1);
			playSound(killer, QuestSound.ITEMSOUND_QUEST_ITEMGET);
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final QuestState qs = getQuestState(player, true);
		String htmltext = getNoQuestMsg(player);
		switch (npc.getId())
		{
			case EDMOND:
			{
				switch (qs.getState())
				{
					case State.CREATED:
					{
						htmltext = (player.getLevel() >= MIN_LEVEL) ? "30497-02.htm" : "30497-01.html";
						break;
					}
					case State.STARTED:
					{
						if (hasQuestItems(player, SPIDER_SKIN))
						{
							final long skins = getQuestItemsCount(player, SPIDER_SKIN);
							giveAdena(player, (skins * SKIN_REWARD) + ((skins >= 10) ? SKIN_BONUS : 0), true);
							takeItems(player, SPIDER_SKIN, -1);
							htmltext = "30497-05.html";
						}
						else
						{
							htmltext = "30497-04.html";
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
			case MARIUS:
			{
				htmltext = (getQuestItemsCount(player, SPIDER_SKIN) >= SKIN_COUNT) ? "30405-02.html" : "30405-01.html";
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
