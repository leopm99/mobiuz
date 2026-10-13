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

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.util.MinionList;

import ai.AbstractNpcAI;

/**
 * Manages minion's spawn, idle despawn and Teleportation Cube spawn.
 * @author GKR
 */
public class Epidos extends AbstractNpcAI
{
	private static final String CHECK_MINIONS = "check_minions";
	private static final String CHECK_IDLE = "check_idle";

	private static final int[] EPIDOSES =
	{
		25609,
		25610,
		25611,
		25612
	};
	
	private static final int[] MINIONS =
	{
		25605,
		25606,
		25607,
		25608
	};
	
	private static final int[] MINIONS_COUNT =
	{
		3,
		6,
		11
	};
	
	private final Map<Integer, Double> _lastHp = new ConcurrentHashMap<>();
	
	private Epidos()
	{
		addKillId(EPIDOSES);
		addSpawnId(EPIDOSES);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase(CHECK_MINIONS))
		{
			if ((getRandom(1000) > 250) && _lastHp.containsKey(npc.getObjectId()))
			{
				final int hpDecreasePercent = (int) (((_lastHp.get(npc.getObjectId()) - npc.getCurrentHp()) * 100) / npc.getMaxHp());
				int minionsCount = 0;
				final int spawnedMinions = npc.asMonster().getMinionList().countSpawnedMinions();
				
				if ((hpDecreasePercent > 5) && (hpDecreasePercent <= 15) && (spawnedMinions <= 9))
				{
					minionsCount = MINIONS_COUNT[0];
				}
				else if ((((hpDecreasePercent > 1) && (hpDecreasePercent <= 5)) || ((hpDecreasePercent > 15) && (hpDecreasePercent <= 30))) && (spawnedMinions <= 6))
				{
					minionsCount = MINIONS_COUNT[1];
				}
				else if (spawnedMinions == 0)
				{
					minionsCount = MINIONS_COUNT[2];
				}
				
				for (int i = 0; i < minionsCount; i++)
				{
					MinionList.spawnMinion(npc.asMonster(), MINIONS[Arrays.binarySearch(EPIDOSES, npc.getId())]);
				}
				
				_lastHp.put(npc.getObjectId(), npc.getCurrentHp());
			}
			
			startQuestTimer(CHECK_MINIONS, 10000, npc, null);
		}
		else if (event.equalsIgnoreCase(CHECK_IDLE))
		{
			if (npc.getAI().getIntention() == Intention.ACTIVE)
			{
				npc.deleteMe();
			}
			else
			{
				startQuestTimer(CHECK_IDLE, 600000, npc, null);
			}
		}
		return null;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.isInsideRadius3D(-45474, 247450, -13994, 2000))
		{
			addSpawn(32376, -45482, 246277, -14184, 0, false, 0, false);
		}
		
		_lastHp.remove(npc.getObjectId());
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer(CHECK_MINIONS, 10000, npc, null);
		startQuestTimer(CHECK_IDLE, 600000, npc, null);
		_lastHp.put(npc.getObjectId(), (double) npc.getMaxHp());
	}
	
	public static void main(String[] args)
	{
		new Epidos();
	}
}
