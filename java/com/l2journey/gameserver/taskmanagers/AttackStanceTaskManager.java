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
package com.l2journey.gameserver.taskmanagers;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Cubic;
import com.l2journey.gameserver.network.serverpackets.AutoAttackStop;

/**
 * Attack stance task manager.
 * @author Luca Baldi
 */
public class AttackStanceTaskManager implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(AttackStanceTaskManager.class.getName());
	
	public static final long COMBAT_TIME = 15000;
	
	private static final Map<Creature, Long> CREATURE_ATTACK_STANCES = new ConcurrentHashMap<>();
	private static boolean _working = false;
	
	protected AttackStanceTaskManager()
	{
		ThreadPool.schedulePriorityTaskAtFixedRate(this, 0, 1000);
	}
	
	@Override
	public void run()
	{
		if (_working)
		{
			return;
		}
		_working = true;
		
		if (!CREATURE_ATTACK_STANCES.isEmpty())
		{
			try
			{
				final long currentTime = System.currentTimeMillis();
				final Iterator<Entry<Creature, Long>> iterator = CREATURE_ATTACK_STANCES.entrySet().iterator();
				Entry<Creature, Long> entry;
				Creature creature;
				
				while (iterator.hasNext())
				{
					entry = iterator.next();
					if ((currentTime - entry.getValue()) > COMBAT_TIME)
					{
						creature = entry.getKey();
						if (creature != null)
						{
							creature.broadcastPacket(new AutoAttackStop(creature.getObjectId()));
							creature.getAI().setAutoAttacking(false);
							if (creature.isPlayer())
							{
								final Player player = creature.asPlayer();
								if (player.hasSummon())
								{
									player.getSummon().broadcastPacket(new AutoAttackStop(player.getSummon().getObjectId()));
								}
							}
						}
						iterator.remove();
					}
				}
			}
			catch (Exception e)
			{
				// Unless caught here, players remain in attack positions.
				LOGGER.log(Level.WARNING, e, () -> "Error in AttackStanceTaskManager: " + e.getMessage());
			}
		}
		
		_working = false;
	}
	
	/**
	 * Adds the attack stance task.
	 * @param creature the actor
	 */
	public void addAttackStanceTask(Creature creature)
	{
		if (creature == null)
		{
			return;
		}
		
		if (creature.isPlayable())
		{
			for (Cubic cubic : creature.asPlayer().getCubics().values())
			{
				if (cubic.getId() != Cubic.LIFE_CUBIC)
				{
					cubic.doAction();
				}
			}
		}
		CREATURE_ATTACK_STANCES.put(creature, System.currentTimeMillis());
	}
	
	/**
	 * Removes the attack stance task.
	 * @param creature the actor
	 */
	public void removeAttackStanceTask(Creature creature)
	{
		Creature actor = creature;
		if (actor != null)
		{
			if (actor.isSummon())
			{
				actor = actor.asPlayer();
			}
			CREATURE_ATTACK_STANCES.remove(actor);
		}
	}
	
	/**
	 * Checks for attack stance task.
	 * @param creature the actor
	 * @return {@code true} if the character has an attack stance task, {@code false} otherwise
	 */
	public boolean hasAttackStanceTask(Creature creature)
	{
		Creature actor = creature;
		if (actor != null)
		{
			if (actor.isSummon())
			{
				actor = actor.asPlayer();
			}
			return CREATURE_ATTACK_STANCES.containsKey(actor);
		}
		
		return false;
	}
	
	/**
	 * Gets the single instance of AttackStanceTaskManager.
	 * @return single instance of AttackStanceTaskManager
	 */
	public static AttackStanceTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AttackStanceTaskManager INSTANCE = new AttackStanceTaskManager();
	}
}
