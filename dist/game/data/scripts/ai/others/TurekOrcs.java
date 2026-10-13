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
package ai.others;

import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;

/**
 * Turek Orcs AI - flee and return with assistance
 * @author GKR
 */

public class TurekOrcs extends AbstractNpcAI
{
	private static final String STATE = "state";
	private static final String FLEEX = "fleeX";
	private static final String FLEEY = "fleeY";
	private static final String FLEEZ = "fleeZ";

	// NPCs
	private static final int[] MOBS =
	{
		20494, // Turek War Hound
		20495, // Turek Orc Warlord
		20497, // Turek Orc Skirmisher
		20498, // Turek Orc Supplier
		20499, // Turek Orc Footman
		20500, // Turek Orc Sentinel
	};
	
	private TurekOrcs()
	{
		addAttackId(MOBS);
		addEventReceivedId(MOBS);
		addMoveFinishedId(MOBS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("checkState") && !npc.isDead() && (npc.getAI().getIntention() != Intention.ATTACK))
		{
			if ((npc.getCurrentHp() > (npc.getMaxHp() * 0.7)) && (npc.getVariables().getInt(STATE) == 2))
			{
				npc.getVariables().set(STATE, 3);
				npc.asAttackable().returnHome();
			}
			else
			{
				npc.getVariables().remove(STATE);
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (!npc.getVariables().hasVariable("isHit"))
		{
			npc.getVariables().set("isHit", 1);
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.5)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.3)) && (attacker.getCurrentHp() > (attacker.getMaxHp() * 0.25)) && npc.hasAIValue(FLEEX) && npc.hasAIValue(FLEEY) && npc.hasAIValue(FLEEZ) && (npc.getVariables().getInt(STATE) == 0) && (getRandom(100) < 10))
		{
			// Say and flee
			npc.broadcastSay(ChatType.GENERAL, NpcStringId.getNpcStringId(getRandom(1000007, 1000027)));
			npc.disableCoreAI(true); // to avoid attacking behaviour, while flee
			npc.setRunning();
			npc.getAI().setIntention(Intention.MOVE_TO, new Location(npc.getAIValue(FLEEX), npc.getAIValue(FLEEY), npc.getAIValue(FLEEZ)));
			npc.getVariables().set(STATE, 1);
			npc.getVariables().set("attacker", attacker.getObjectId());
		}
	}
	
	@Override
	public String onEventReceived(String eventName, Npc sender, Npc receiver, WorldObject reference)
	{
		if (eventName.equals("WARNING") && !receiver.isDead() && (receiver.getAI().getIntention() != Intention.ATTACK) && (reference != null))
		{
			final Player player = reference.asPlayer();
			if ((player != null) && !player.isDead())
			{
				receiver.getVariables().set(STATE, 3);
				receiver.setRunning();
				receiver.asAttackable().addDamageHate(player, 0, 99999);
				receiver.getAI().setIntention(Intention.ATTACK, player);
			}
		}
		return super.onEventReceived(eventName, sender, receiver, reference);
	}
	
	@Override
	public void onMoveFinished(Npc npc)
	{
		// NPC reaches flee point
		if (npc.getVariables().getInt(STATE) == 1)
		{
			if ((npc.getX() == npc.getAIValue(FLEEX)) && (npc.getY() == npc.getAIValue(FLEEY)))
			{
				npc.disableCoreAI(false);
				startQuestTimer("checkState", 15000, npc, null);
				npc.getVariables().set(STATE, 2);
				npc.broadcastEvent("WARNING", 400, World.getInstance().getPlayer(npc.getVariables().getInt("attacker")));
			}
			else
			{
				npc.getAI().setIntention(Intention.MOVE_TO, new Location(npc.getAIValue(FLEEX), npc.getAIValue(FLEEY), npc.getAIValue(FLEEZ)));
			}
		}
		else if ((npc.getVariables().getInt(STATE) == 3) && npc.staysInSpawnLoc())
		{
			npc.disableCoreAI(false);
			npc.getVariables().remove(STATE);
		}
	}
	
	public static void main(String[] args)
	{
		new TurekOrcs();
	}
}
