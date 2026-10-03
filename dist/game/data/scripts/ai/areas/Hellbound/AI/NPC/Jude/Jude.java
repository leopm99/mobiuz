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
package ai.areas.Hellbound.AI.NPC.Jude;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;

import ai.AbstractNpcAI;
import ai.areas.Hellbound.HellboundEngine;

/**
 * Jude AI.
 * @author DS
 */
public class Jude extends AbstractNpcAI
{
	// NPCs
	private static final int JUDE = 32356;
	private static final int NATIVE_TREASURE = 9684;
	private static final int RING_OF_WIND_MASTERY = 9677;
	
	public Jude()
	{
		addFirstTalkId(JUDE);
		addStartNpc(JUDE);
		addTalkId(JUDE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ("TreasureSacks".equalsIgnoreCase(event))
		{
			if ((HellboundEngine.getInstance().getLevel() == 3) && (getQuestItemsCount(player, NATIVE_TREASURE) >= 40))
			{
				takeItems(player, NATIVE_TREASURE, 40);
				giveItems(player, RING_OF_WIND_MASTERY, 1);
				return "32356-02.htm";
			}
			return "32356-02a.htm";
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		switch (HellboundEngine.getInstance().getLevel())
		{
			case 0, 1, 2:
			{
				return "32356-01.htm";
			}
			case 3, 4:
			{
				return "32356-01c.htm";
			}
			case 5:
			{
				return "32356-01a.htm";
			}
			default:
			{
				return "32356-01b.htm";
			}
		}
	}
}