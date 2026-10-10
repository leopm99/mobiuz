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
package ai.areas.Gracia.AI.NPC.Nemo;

import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.managers.QuestManager;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.network.NpcStringId;

import ai.AbstractNpcAI;
import ai.areas.Gracia.AI.Maguen;

/**
 * Nemo AI.
 * @author St3eT
 */
public class Nemo extends AbstractNpcAI
{
	// NPC
	private static final int NEMO = 32735; // Nemo
	private static final int MAGUEN = 18839; // Wild Maguen
	// Items
	private static final int COLLECTOR = 15487; // Maguen Plasma Collector
	// Misc
	private static final int MAXIMUM_MAGUEN = 18; // Maximum maguens in one time
	
	public Nemo()
	{
		addStartNpc(NEMO);
		addFirstTalkId(NEMO);
		addTalkId(NEMO);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "32735-01.html":
			{
				htmltext = event;
				break;
			}
			case "giveCollector":
			{
				if (hasQuestItems(player, COLLECTOR))
				{
					htmltext = "32735-03.html";
				}
				else if (!player.isInventoryUnder90(false))
				{
					htmltext = "32735-04.html";
				}
				else
				{
					htmltext = "32735-02.html";
					giveItems(player, COLLECTOR, 1);
				}
				break;
			}
			case "summonMaguen":
			{
				if ((player.getVariables().getInt("TEST_MAGUEN", 0) == 0) && (npc.getScriptValue() < MAXIMUM_MAGUEN))
				{
					final Npc maguen = addSpawn(MAGUEN, npc.getLocation(), true, 60000, true);
					maguen.getVariables().set("SUMMON_PLAYER", player);
					maguen.getVariables().set("SPAWNED_NPC", npc);
					maguen.getVariables().set("TEST_MAGUEN", 1);
					player.getVariables().set("TEST_MAGUEN", 1);
					maguen.setTitle(player.getName());
					maguen.setRunning();
					maguen.getAI().setIntention(Intention.FOLLOW, player);
					maguen.broadcastStatusUpdate();
					showOnScreenMsg(player, NpcStringId.MAGUEN_APPEARANCE, 2, 4000);
					maguenAi().startQuestTimer("DIST_CHECK_TIMER", 1000, maguen, player);
					npc.setScriptValue(npc.getScriptValue() + 1);
					htmltext = "32735-05.html";
				}
				else
				{
					htmltext = "32735-06.html";
				}
				break;
			}
			case "DECREASE_COUNT":
			{
				final Npc spawnedNpc = npc.getVariables().getObject("SPAWNED_NPC", Npc.class);
				if ((spawnedNpc != null) && (spawnedNpc.getScriptValue() > 0))
				{
					player.getVariables().remove("TEST_MAGUEN");
					spawnedNpc.setScriptValue(spawnedNpc.getScriptValue() - 1);
				}
			}
			default:
			{
				break;
			}
		}
		return htmltext;
	}
	
	private Quest maguenAi()
	{
		return QuestManager.getInstance().getQuest(Maguen.class.getSimpleName());
	}
}