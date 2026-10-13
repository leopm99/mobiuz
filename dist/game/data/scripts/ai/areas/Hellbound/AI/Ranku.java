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
package ai.areas.Hellbound.AI;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Monster;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.util.MinionList;

import ai.AbstractNpcAI;

/**
 * Ranku's AI.
 * @author GKR
 */
public class Ranku extends AbstractNpcAI
{
	private static final String CHECKUP = "checkup";

	// NPCs
	private static final int RANKU = 25542;
	private static final int MINION = 32305;
	private static final int MINION_2 = 25543;
	// Misc
	private static final Set<Integer> MY_TRACKING_SET = ConcurrentHashMap.newKeySet();
	
	public Ranku()
	{
		addAttackId(RANKU);
		addKillId(RANKU, MINION);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase(CHECKUP) && (npc.getId() == RANKU) && !npc.isDead())
		{
			for (Monster minion : npc.asMonster().getMinionList().getSpawnedMinions())
			{
				if ((minion != null) && !minion.isDead() && MY_TRACKING_SET.contains(minion.getObjectId()))
				{
					final Player killer = getRandomEntry(World.getInstance().getVisibleObjects(minion, Player.class));
					minion.reduceCurrentHp(minion.getMaxHp() / 100, killer, null);
				}
			}
			startQuestTimer(CHECKUP, 1000, npc, null);
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (npc.getId() == RANKU)
		{
			for (Monster minion : npc.asMonster().getMinionList().getSpawnedMinions())
			{
				if ((minion != null) && !minion.isDead() && !MY_TRACKING_SET.contains(minion.getObjectId()))
				{
					minion.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.DON_T_KILL_ME_PLEASE_SOMETHING_S_STRANGLING_ME);
					startQuestTimer(CHECKUP, 1000, npc, null);
					MY_TRACKING_SET.add(minion.getObjectId());
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() == MINION)
		{
			if (MY_TRACKING_SET.contains(npc.getObjectId()))
			{
				MY_TRACKING_SET.remove(npc.getObjectId());
			}
			
			final Monster master = npc.asMonster().getLeader();
			if ((master != null) && !master.isDead())
			{
				final Monster minion2 = MinionList.spawnMinion(master, MINION_2);
				minion2.teleToLocation(npc.getLocation());
			}
		}
		else if (npc.getId() == RANKU)
		{
			for (Monster minion : npc.asMonster().getMinionList().getSpawnedMinions())
			{
				if (MY_TRACKING_SET.contains(minion.getObjectId()))
				{
					MY_TRACKING_SET.remove(minion.getObjectId());
				}
			}
		}
	}
}