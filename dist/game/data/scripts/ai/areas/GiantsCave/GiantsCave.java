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
package ai.areas.GiantsCave;

import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Attackable;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;

/**
 * Giant's Cave AI.
 * @author Gnacik, St3eT
 */
public class GiantsCave extends AbstractNpcAI
{
	private static final String ATTACK = "ATTACK";
	private static final String CLEAR = "CLEAR";

	// NPC
	private static final int[] SCOUTS =
	{
		22668, // Gamlin (Scout)
		22669, // Leogul (Scout)
	};
	
	private GiantsCave()
	{
		addAttackId(SCOUTS);
		addAggroRangeEnterId(SCOUTS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals(ATTACK) && (player != null) && (npc != null) && !npc.isDead())
		{
			if (npc.getId() == SCOUTS[0]) // Gamlin
			{
				npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.INTRUDER_DETECTED);
			}
			else
			{
				npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.OH_GIANTS_AN_INTRUDER_HAS_BEEN_DISCOVERED);
			}
			
			World.getInstance().forEachVisibleObjectInRange(npc, Attackable.class, 450, characters ->
			{
				if ((getRandomBoolean()))
				{
					addAttackDesire(characters, player);
				}
			});
		}
		else if (event.equals(CLEAR) && (npc != null) && !npc.isDead())
		{
			npc.setScriptValue(0);
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.isScriptValue(0))
		{
			npc.setScriptValue(1);
			startQuestTimer(ATTACK, 6000, npc, attacker);
			startQuestTimer(CLEAR, 120000, npc, null);
		}
	}
	
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if (npc.isScriptValue(0))
		{
			npc.setScriptValue(1);
			if (getRandomBoolean())
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.YOU_GUYS_ARE_DETECTED);
			}
			else
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.WHAT_KIND_OF_CREATURES_ARE_YOU);
			}
			startQuestTimer(ATTACK, 6000, npc, player);
			startQuestTimer(CLEAR, 120000, npc, null);
		}
	}
	
	public static void main(String[] args)
	{
		new GiantsCave();
	}
}