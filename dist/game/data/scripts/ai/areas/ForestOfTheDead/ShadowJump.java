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
package ai.areas.ForestOfTheDead;

import java.util.concurrent.ThreadLocalRandom;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.model.actor.Attackable;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;
import com.l2journey.gameserver.network.serverpackets.ValidateLocation;

import ai.AbstractNpcAI;

/**
 * Forest Of The Dead Behavior: - On aggro range enter, the mob blinks next to the player. - This version avoids API/classes that are not present in L2Journey. - It uses only core-safe methods so it compiles more easily.
 * @author Mafias, KingHanker
 */
public class ShadowJump extends AbstractNpcAI
{
	private static final int[] MOB_IDS =
	{
		21585,
		21589
	};
	private static final String TP_LOCK = "SHADOW_JUMP_LOCK";
	private static final long TELEPORT_REUSE = 8000L;
	
	public ShadowJump()
	{
		addAggroRangeEnterId(MOB_IDS);
		addAttackId(MOB_IDS);
	}
	
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if ((npc == null) || (player == null) || player.isDead() || npc.isDead())
		{
			return;
		}
		
		if (npc.getVariables().getBoolean(TP_LOCK, false) || (npc.calculateDistance3D(player) <= 30))
		{
			return;
		}
		
		npc.getVariables().set(TP_LOCK, true);
		jumpToPlayer(npc, player);
		
		ThreadPool.schedule(() -> npc.getVariables().remove(TP_LOCK), TELEPORT_REUSE);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		super.onAttack(npc, attacker, damage, isSummon);
		
		if ((attacker != null) && !npc.getVariables().getBoolean(TP_LOCK, false) && (npc.calculateDistance3D(attacker) > 100))
		{
			npc.getVariables().set(TP_LOCK, true);
			jumpToPlayer(npc, attacker);
			
			ThreadPool.schedule(() -> npc.getVariables().remove(TP_LOCK), TELEPORT_REUSE);
		}
	}
	
	private void jumpToPlayer(Npc npc, Player player)
	{
		ThreadPool.schedule(() ->
		{
			if (npc.isDead() || player.isDead())
			{
				return;
			}
			
			final int x = player.getX() + ThreadLocalRandom.current().nextInt(-80, 81);
			final int y = player.getY() + ThreadLocalRandom.current().nextInt(-80, 81);
			final int z = player.getZ();
			
			npc.broadcastPacket(new MagicSkillUse(npc, npc, 2036, 1, 0, 0));
			
			npc.stopMove(null);
			npc.abortAttack();
			npc.setXYZ(x, y, z);
			npc.broadcastPacket(new ValidateLocation(npc));
			npc.broadcastInfo();
			npc.setTarget(player);
			
			if (npc instanceof Attackable attackable)
			{
				attackable.addDamageHate(player, 0, 9999);
			}
			
			ThreadPool.schedule(() ->
			{
				if (!npc.isDead() && !player.isDead())
				{
					npc.doAttack(player);
				}
			}, 200L);
		}, 250L);
	}
	
	public static void main(String[] args)
	{
		new ShadowJump();
	}
}
