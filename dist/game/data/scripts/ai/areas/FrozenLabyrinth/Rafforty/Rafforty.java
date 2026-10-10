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
package ai.areas.FrozenLabyrinth.Rafforty;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;

import ai.AbstractNpcAI;

/**
 * Rafforty AI.
 * @author malyelfik, Gladicek
 */
public class Rafforty extends AbstractNpcAI
{
	// NPC
	private static final int RAFFORTY = 32020;
	// Items
	private static final int NECKLACE = 16025;
	private static final int BLESSED_NECKLACE = 16026;
	private static final int BOTTLE = 16027;
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		switch (event)
		{
			case "32020-01.html":
			{
				if (!hasQuestItems(player, NECKLACE))
				{
					htmltext = "32020-02.html";
				}
				break;
			}
			case "32020-04.html":
			{
				if (!hasQuestItems(player, BOTTLE))
				{
					htmltext = "32020-05.html";
				}
				break;
			}
			case "32020-07.html":
			{
				if (!hasQuestItems(player, BOTTLE, NECKLACE))
				{
					return "32020-08.html";
				}
				takeItems(player, NECKLACE, 1);
				takeItems(player, BOTTLE, 1);
				giveItems(player, BLESSED_NECKLACE, 1);
				break;
			}
			default:
			{
				break;
			}
		}
		return htmltext;
	}
	
	private Rafforty()
	{
		addStartNpc(RAFFORTY);
		addFirstTalkId(RAFFORTY);
		addTalkId(RAFFORTY);
	}
	
	public static void main(String[] args)
	{
		new Rafforty();
	}
}