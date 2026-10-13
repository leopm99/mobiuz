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
package ai.areas.MonasteryOfSilence;

import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;

import ai.AbstractNpcAI;

/**
 * Etis van Etina (Head of the Embryo) AI.
 * @author Altur
 */
public class EtisVanEtina extends AbstractNpcAI
{
	private static final String PHANTOMSALIVE = "phantomsAlive";
	private static final String PHASE = "phase";

	// NPCs
	private static final int ETIS_VAN_ETINA = 18949;
	private static final int PHANTOM_REAL = 18950;
	private static final int PHANTOM_FAKE = 18951;
	// Misc
	private static final String THINK_TIMER = "THINK";
	private static final String EVT_PHANTOM_REAL_DEAD = "ETIS_PHANTOM_REAL_DEAD";
	private static final String EVT_BOSS_DEAD = "ETIS_BOSS_DEAD";
	private static final int PHASE_NORMAL = 0;
	private static final int PHASE_TRANSFORM = 1;
	private static final int THINK_INTERVAL = 5000;
	private static final int PHANTOM_BROADCAST_RADIUS = 5000;
	
	private EtisVanEtina()
	{
		addSpawnId(ETIS_VAN_ETINA);
		addKillId(ETIS_VAN_ETINA);
		addEventReceivedId(ETIS_VAN_ETINA);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		npc.getVariables().set(PHASE, PHASE_NORMAL);
		npc.getVariables().set(PHANTOMSALIVE, false);
		startQuestTimer(THINK_TIMER, THINK_INTERVAL, npc, null);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals(THINK_TIMER) && (npc != null) && !npc.isDead())
		{
			final int phase = npc.getVariables().getInt(PHASE, PHASE_NORMAL);
			if ((phase == PHASE_NORMAL) && (npc.getCurrentHp() < (npc.getMaxRecoverableHp() * 0.5)))
			{
				npc.getVariables().set(PHASE, PHASE_TRANSFORM);
			}
			
			if ((npc.getVariables().getInt(PHASE, PHASE_NORMAL) == PHASE_TRANSFORM) && !npc.getVariables().getBoolean(PHANTOMSALIVE, false))
			{
				final Npc dr = addSpawn(PHANTOM_REAL, npc.getLocation(), true, 0, false, npc.getInstanceId());
				final Npc df = addSpawn(PHANTOM_FAKE, npc.getLocation(), true, 0, false, npc.getInstanceId());
				final WorldObject target = npc.getTarget();
				if ((target != null) && target.isPlayer())
				{
					final int targetId = target.getObjectId();
					if (dr != null)
					{
						dr.getVariables().set("etisTargetId", targetId);
					}
					
					if (df != null)
					{
						df.getVariables().set("etisTargetId", targetId);
					}
				}
				npc.getVariables().set(PHANTOMSALIVE, true);
			}
			
			startQuestTimer(THINK_TIMER, THINK_INTERVAL, npc, null);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public String onEventReceived(String eventName, Npc sender, Npc receiver, WorldObject reference)
	{
		if (eventName.equals(EVT_PHANTOM_REAL_DEAD) && (receiver != null) && (receiver.getId() == ETIS_VAN_ETINA) && !receiver.isDead())
		{
			receiver.getVariables().set(PHANTOMSALIVE, false);
		}
		
		return super.onEventReceived(eventName, sender, receiver, reference);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		cancelQuestTimer(THINK_TIMER, npc, null);
		npc.broadcastEvent(EVT_BOSS_DEAD, PHANTOM_BROADCAST_RADIUS, null);
	}
	
	public static void main(String[] args)
	{
		new EtisVanEtina();
	}
}
