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
package ai.areas.Hellbound.AI.Zones.AnomicFoundry;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.data.SpawnTable;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.Spawn;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Monster;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;
import ai.areas.Hellbound.HellboundEngine;

/**
 * Anomic Foundry.
 * @author GKR, KingHanker
 */
public class AnomicFoundry extends AbstractNpcAI
{
	// NPCs
	private static int LABORER = 22396;
	private static int FOREMAN = 22397;
	private static int LESSER_EVIL = 22398;
	private static int GREATER_EVIL = 22399;
	// Misc
	private final Map<Integer, Integer> _atkIndex = new ConcurrentHashMap<>();
	// npcId, x, y, z, heading, max count
	//@formatter:off
	private static int[][] SPAWNS =
	{
		{LESSER_EVIL, 27883, 248613, -3209, -13248, 5},
		{LESSER_EVIL, 26142, 246442, -3216, 7064, 5},
		{LESSER_EVIL, 27335, 246217, -3668, -7992, 5},
		{LESSER_EVIL, 28486, 245913, -3698, 0, 10},
		{GREATER_EVIL, 28684, 244118, -3700, -22560, 10},
	};
	//@formatter:on
	
	private int respawnTime = 60000;
	private static final int respawnMin = 20000;
	private static final int respawnMax = 300000;
	
	private final int[] _spawned =
	{
		0,
		0,
		0,
		0,
		0
	};
	
	public AnomicFoundry()
	{
		addAggroRangeEnterId(LABORER);
		addAttackId(LABORER);
		addKillId(LABORER, LESSER_EVIL, GREATER_EVIL);
		addSpawnId(LABORER, LESSER_EVIL, GREATER_EVIL);
		addTeleportId(LABORER, LESSER_EVIL, GREATER_EVIL);
		startQuestTimer("make_spawn_1", respawnTime, null, null);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("make_spawn_1"))
		{
			if (HellboundEngine.getInstance().getLevel() >= 10)
			{
				final int idx = getRandom(3);
				if (_spawned[idx] < SPAWNS[idx][5])
				{
					addSpawn(SPAWNS[idx][0], SPAWNS[idx][1], SPAWNS[idx][2], SPAWNS[idx][3], SPAWNS[idx][4], false, 0, false);
					respawnTime += 10000;
				}
				startQuestTimer("make_spawn_1", respawnTime, null, null);
			}
		}
		else if (event.equalsIgnoreCase("make_spawn_2"))
		{
			if (_spawned[4] < SPAWNS[4][5])
			{
				addSpawn(SPAWNS[4][0], SPAWNS[4][1], SPAWNS[4][2], SPAWNS[4][3], SPAWNS[4][4], false, 0, false);
			}
		}
		else if (event.equalsIgnoreCase("return_laborer"))
		{
			if ((npc != null) && !npc.isDead())
			{
				npc.asAttackable().returnHome();
			}
		}
		else if (event.equalsIgnoreCase("reset_respawn_time"))
		{
			respawnTime = 60000;
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if (getRandom(10000) < 2000)
		{
			requestHelp(npc, player, 500, FOREMAN);
			requestHelp(npc, player, 500, LESSER_EVIL);
			requestHelp(npc, player, 500, GREATER_EVIL);
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		int atkIndex = _atkIndex.containsKey(npc.getObjectId()) ? _atkIndex.get(npc.getObjectId()) : 0;
		if (atkIndex == 0)
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.ENEMY_INVASION_HURRY_UP);
			cancelQuestTimer("return_laborer", npc, null);
			startQuestTimer("return_laborer", 60000, npc, null);
			if (respawnTime > respawnMin)
			{
				respawnTime -= 5000;
			}
			else if ((respawnTime <= respawnMin) && (getQuestTimer("reset_respawn_time", null, null) == null))
			{
				startQuestTimer("reset_respawn_time", 600000, null, null);
			}
		}
		
		if (getRandom(10000) < 2000)
		{
			atkIndex++;
			_atkIndex.put(npc.getObjectId(), atkIndex);
			requestHelp(npc, attacker, 1000 * atkIndex, FOREMAN);
			requestHelp(npc, attacker, 1000 * atkIndex, LESSER_EVIL);
			requestHelp(npc, attacker, 1000 * atkIndex, GREATER_EVIL);
			if (getRandom(10) < 1)
			{
				npc.getAI().setIntention(Intention.MOVE_TO, new Location((npc.getX() + getRandom(-800, 800)), (npc.getY() + getRandom(-800, 800)), npc.getZ(), npc.getHeading()));
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (getSpawnGroup(npc) >= 0)
		{
			_spawned[getSpawnGroup(npc)]--;
			SpawnTable.getInstance().removeSpawn(npc.getSpawn());
		}
		else if (npc.getId() == LABORER)
		{
			if (getRandom(10000) < 8000)
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.PROCESS_SHOULDN_T_BE_DELAYED_BECAUSE_OF_ME);
				if (respawnTime < respawnMax)
				{
					respawnTime += 10000;
				}
				else if ((respawnTime >= respawnMax) && (getQuestTimer("reset_respawn_time", null, null) == null))
				{
					startQuestTimer("reset_respawn_time", 600000, null, null);
				}
			}
			_atkIndex.remove(npc.getObjectId());
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		SpawnTable.getInstance().addSpawn(npc.getSpawn());
		if (getSpawnGroup(npc) >= 0)
		{
			_spawned[getSpawnGroup(npc)]++;
		}
		
		if (npc.getId() == LABORER)
		{
			npc.setRandomWalking(false);
		}
	}
	
	@Override
	protected void onTeleport(Npc npc)
	{
		if ((getSpawnGroup(npc) >= 0) && (getSpawnGroup(npc) <= 2))
		{
			_spawned[getSpawnGroup(npc)]--;
			SpawnTable.getInstance().removeSpawn(npc.getSpawn());
			npc.scheduleDespawn(100);
			if (_spawned[3] < SPAWNS[3][5])
			{
				addSpawn(SPAWNS[3][0], SPAWNS[3][1], SPAWNS[3][2], SPAWNS[3][3], SPAWNS[3][4], false, 0, false);
			}
		}
		else if (getSpawnGroup(npc) == 3)
		{
			startQuestTimer("make_spawn_2", respawnTime * 2, null, null);
			_spawned[3]--;
			SpawnTable.getInstance().removeSpawn(npc.getSpawn());
			npc.scheduleDespawn(100);
		}
	}
	
	private static int getSpawnGroup(Npc npc)
	{
		final int coordX = npc.getSpawn().getX();
		final int coordY = npc.getSpawn().getY();
		final int npcId = npc.getId();
		for (int i = 0; i < 5; i++)
		{
			if ((SPAWNS[i][0] == npcId) && (SPAWNS[i][1] == coordX) && (SPAWNS[i][2] == coordY))
			{
				return i;
			}
		}
		return -1;
	}
	
	private void requestHelp(Npc requester, Player agressor, int range, int helperId)
	{
		if ((agressor == null) || agressor.isDead())
		{
			return;
		}
		
		Collection<Spawn> spawns = SpawnTable.getInstance().getSpawns(helperId);
		if ((spawns == null) || spawns.isEmpty())
		{
			return;
		}
		
		for (Spawn spawn : spawns)
		{
			final Npc lastSpawn = spawn.getLastSpawn();
			if ((lastSpawn == null) || !lastSpawn.isMonster())
			{
				continue;
			}
			
			final Monster monster = (Monster) lastSpawn;
			if (!monster.isDead() && monster.isInsideRadius3D(requester, range))
			{
				monster.addDamageHate(agressor, 0, 1000);
			}
		}
	}
}