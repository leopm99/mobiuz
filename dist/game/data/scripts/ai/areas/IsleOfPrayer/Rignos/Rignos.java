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
package ai.areas.IsleOfPrayer.Rignos;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;

import ai.AbstractNpcAI;

/**
 * Rignos AI.
 * @author St3eT
 */
public class Rignos extends AbstractNpcAI
{
	// NPC
	private static final int RIGNOS = 32349; // Rignos
	// Item
	private static final int STAMP = 10013; // Race Stamp
	private static final int KEY = 9694; // Secret Key
	// Skill
	private static final SkillHolder TIMER = new SkillHolder(5239, 5); // Event Timer
	// Misc
	private static final int MIN_LV = 78;
	
	private Rignos()
	{
		addStartNpc(RIGNOS);
		addTalkId(RIGNOS);
		addFirstTalkId(RIGNOS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "32349-03.html":
			{
				return event;
			}
			case "startRace":
			{
				if (npc.isScriptValue(0))
				{
					npc.setScriptValue(1);
					startQuestTimer("TIME_OUT", 1800000, npc, null);
					TIMER.getSkill().applyEffects(player, player);
					if (player.hasSummon())
					{
						TIMER.getSkill().applyEffects(player.getSummon(), player.getSummon());
					}
					
					if (hasQuestItems(player, STAMP))
					{
						takeItems(player, STAMP, -1);
					}
				}
				break;
			}
			case "exchange":
			{
				if (getQuestItemsCount(player, STAMP) >= 4)
				{
					giveItems(player, KEY, 3);
					takeItems(player, STAMP, -1);
				}
				break;
			}
			case "TIME_OUT":
			{
				npc.setScriptValue(0);
				break;
			}
			default:
			{
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = (npc.isScriptValue(0) && (player.getLevel() >= MIN_LV)) ? "32349.html" : "32349-02.html";
		if (getQuestItemsCount(player, STAMP) >= 4)
		{
			htmltext = "32349-01.html";
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new Rignos();
	}
}