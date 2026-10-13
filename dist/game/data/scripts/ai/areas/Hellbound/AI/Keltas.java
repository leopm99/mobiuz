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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.Spawn;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Monster;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;

/**
 * Manages Darion's Enforcer's and Darion's Executioner spawn/despawn
 * @author GKR
 */
public class Keltas extends AbstractNpcAI
{
	private static final String DESPAWN = "despawn";

	// NPCs
	private static final int KELTAS = 22341;
	private static final int ENFORCER = 22342;
	private static final int EXECUTIONER = 22343;
	// Locations
	private static final Location[] ENFORCER_SPAWN_POINTS =
	{
		new Location(-24540, 251404, -3320),
		new Location(-24100, 252578, -3060),
		new Location(-24607, 252443, -3074),
		new Location(-23962, 252041, -3275),
		new Location(-24381, 252132, -3090),
		new Location(-23652, 251838, -3370),
		new Location(-23838, 252603, -3095),
		new Location(-23257, 251671, -3360),
		new Location(-27127, 251106, -3523),
		new Location(-27118, 251203, -3523),
		new Location(-27052, 251205, -3523),
		new Location(-26999, 250818, -3523),
		new Location(-29613, 252888, -3523),
		new Location(-29765, 253009, -3523),
		new Location(-29594, 252570, -3523),
		new Location(-29770, 252658, -3523),
		new Location(-27816, 252008, -3527),
		new Location(-27930, 252011, -3523),
		new Location(-28702, 251986, -3523),
		new Location(-27357, 251987, -3527),
		new Location(-28859, 251081, -3527),
		new Location(-28607, 250397, -3523),
		new Location(-28801, 250462, -3523),
		new Location(-29123, 250387, -3472),
		new Location(-25376, 252368, -3257),
		new Location(-25376, 252208, -3257)
	};
	private static final Location[] EXECUTIONER_SPAWN_POINTS =
	{
		new Location(-24419, 251395, -3340),
		new Location(-24912, 252160, -3310),
		new Location(-25027, 251941, -3300),
		new Location(-24127, 252657, -3058),
		new Location(-25120, 252372, -3270),
		new Location(-24456, 252651, -3060),
		new Location(-24844, 251614, -3295),
		new Location(-28675, 252008, -3523),
		new Location(-27943, 251238, -3523),
		new Location(-27827, 251984, -3523),
		new Location(-27276, 251995, -3523),
		new Location(-28769, 251955, -3523),
		new Location(-27969, 251073, -3523),
		new Location(-27233, 250938, -3523),
		new Location(-26835, 250914, -3523),
		new Location(-26802, 251276, -3523),
		new Location(-29671, 252781, -3527),
		new Location(-29536, 252831, -3523),
		new Location(-29419, 253214, -3523),
		new Location(-27923, 251965, -3523),
		new Location(-28499, 251882, -3527),
		new Location(-28194, 251915, -3523),
		new Location(-28358, 251078, -3527),
		new Location(-28580, 251071, -3527),
		new Location(-28492, 250704, -3523)
	};
	// Misc
	private Monster _spawnedKeltas = null;
	private final Set<Spawn> _spawnedMonsters = Collections.newSetFromMap(new ConcurrentHashMap<>());
	
	public Keltas()
	{
		addKillId(KELTAS);
		addSpawnId(KELTAS);
	}
	
	private void spawnMinions()
	{
		for (Location loc : ENFORCER_SPAWN_POINTS)
		{
			final Monster minion = addSpawn(ENFORCER, loc, false, 0, false).asMonster();
			final Spawn spawn = minion.getSpawn();
			spawn.setRespawnDelay(60);
			spawn.setAmount(1);
			spawn.startRespawn();
			_spawnedMonsters.add(spawn);
		}
		
		for (Location loc : EXECUTIONER_SPAWN_POINTS)
		{
			final Monster minion = addSpawn(EXECUTIONER, loc, false, 0, false).asMonster();
			final Spawn spawn = minion.getSpawn();
			spawn.setRespawnDelay(80);
			spawn.setAmount(1);
			spawn.startRespawn();
			_spawnedMonsters.add(spawn);
		}
	}
	
	private void despawnMinions()
	{
		if (_spawnedMonsters.isEmpty())
		{
			return;
		}
		
		for (Spawn spawn : _spawnedMonsters)
		{
			spawn.stopRespawn();
			final Npc minion = spawn.getLastSpawn();
			if ((minion != null) && !minion.isDead())
			{
				minion.deleteMe();
			}
		}
		_spawnedMonsters.clear();
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase(DESPAWN))
		{
			final Npc keltas = _spawnedKeltas;
			if ((keltas != null) && !keltas.isDead())
			{
				keltas.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.THAT_IS_IT_FOR_TODAY_LET_S_RETREAT_EVERYONE_PULL_BACK);
				keltas.deleteMe();
				keltas.getSpawn().decreaseCount(keltas);
				despawnMinions();
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		cancelQuestTimers(DESPAWN);
		despawnMinions();
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		_spawnedKeltas = npc.asMonster();
		_spawnedKeltas.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.GUYS_SHOW_THEM_OUR_POWER);
		spawnMinions();
		startQuestTimer(DESPAWN, 1800000, null, null);
	}
}