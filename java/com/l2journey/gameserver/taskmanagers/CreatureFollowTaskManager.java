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

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.GeoData;
import com.l2journey.gameserver.ai.CreatureAI;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.zone.ZoneId;

/**
 * @author Mobius
 */
public class CreatureFollowTaskManager
{
	// Tick interval (ms) used for Playable followers (players/summons) on the "normal" follow.
	// Kept much lower than the NPC/pet interval so a fixed-destination MoveToLocation is recalculated
	// often enough to look smooth, without falling back to MoveToPawn (which caused observer desync).
	private static final int PLAYABLE_FOLLOW_INTERVAL = 300;
	
	protected static final Map<Creature, Integer> NORMAL_FOLLOW_CREATURES = new ConcurrentHashMap<>();
	protected static final Map<Creature, Integer> PLAYABLE_FOLLOW_CREATURES = new ConcurrentHashMap<>();
	protected static final Map<Creature, Integer> ATTACK_FOLLOW_CREATURES = new ConcurrentHashMap<>();
	protected static boolean _workingNormal = false;
	protected static boolean _workingPlayable = false;
	protected static boolean _workingAttack = false;
	
	protected CreatureFollowTaskManager()
	{
		ThreadPool.schedulePriorityTaskAtFixedRate(new CreatureFollowNormalTask(), 1000, 1000);
		ThreadPool.schedulePriorityTaskAtFixedRate(new CreatureFollowPlayableTask(), PLAYABLE_FOLLOW_INTERVAL, PLAYABLE_FOLLOW_INTERVAL);
		ThreadPool.schedulePriorityTaskAtFixedRate(new CreatureFollowAttackTask(), 500, 500);
	}
	
	protected class CreatureFollowNormalTask implements Runnable
	{
		@Override
		public void run()
		{
			if (_workingNormal)
			{
				return;
			}
			_workingNormal = true;
			
			if (!NORMAL_FOLLOW_CREATURES.isEmpty())
			{
				for (Entry<Creature, Integer> entry : NORMAL_FOLLOW_CREATURES.entrySet())
				{
					follow(entry.getKey(), entry.getValue());
				}
			}
			
			_workingNormal = false;
		}
	}
	
	protected class CreatureFollowPlayableTask implements Runnable
	{
		@Override
		public void run()
		{
			if (_workingPlayable)
			{
				return;
			}
			_workingPlayable = true;
			
			if (!PLAYABLE_FOLLOW_CREATURES.isEmpty())
			{
				for (Entry<Creature, Integer> entry : PLAYABLE_FOLLOW_CREATURES.entrySet())
				{
					follow(entry.getKey(), entry.getValue());
				}
			}
			
			_workingPlayable = false;
		}
	}
	
	protected class CreatureFollowAttackTask implements Runnable
	{
		@Override
		public void run()
		{
			if (_workingAttack)
			{
				return;
			}
			_workingAttack = true;
			
			if (!ATTACK_FOLLOW_CREATURES.isEmpty())
			{
				for (Entry<Creature, Integer> entry : ATTACK_FOLLOW_CREATURES.entrySet())
				{
					follow(entry.getKey(), entry.getValue());
				}
			}
			
			_workingAttack = false;
		}
	}
	
	protected void follow(Creature creature, int range)
	{
		try
		{
			if (creature.hasAI())
			{
				final CreatureAI ai = creature.getAI();
				if (ai != null)
				{
					final WorldObject followTarget = ai.getFollowTarget();
					// Check if creature and target are on different floors inside multi-floor zones (e.g., Tower of Insolence)
					if ((followTarget == null) || (creature.isInsideZone(ZoneId.MULTI_FLOOR) && followTarget.isCreature() && GeoData.getInstance().areOnDifferentFloors(creature.getX(), creature.getY(), creature.getZ(), followTarget.getX(), followTarget.getY(), followTarget.getZ())))
					{
						if (creature.isSummon())
						{
							creature.asSummon().setFollowStatus(false);
						}
						ai.setIntention(Intention.IDLE);
						return;
					}
					
					final int followRange = range == -1 ? Rnd.get(60, 80) : range;
					if (!creature.isInsideRadius3D(followTarget, followRange))
					{
						if (!creature.isInsideRadius3D(followTarget, 3000))
						{
							// If the target is too far (maybe also teleported).
							if (creature.isSummon())
							{
								creature.asSummon().setFollowStatus(false);
							}
							ai.setIntention(Intention.IDLE);
							return;
						}
						ai.moveToPawn(followTarget, followRange);
					}
				}
				else
				{
					remove(creature);
				}
			}
			else
			{
				remove(creature);
			}
		}
		catch (Exception e)
		{
			// Ignore.
		}
	}
	
	public boolean isFollowing(Creature creature)
	{
		return NORMAL_FOLLOW_CREATURES.containsKey(creature) || PLAYABLE_FOLLOW_CREATURES.containsKey(creature) || ATTACK_FOLLOW_CREATURES.containsKey(creature);
	}
	
	public void addNormalFollow(Creature creature, int range)
	{
		NORMAL_FOLLOW_CREATURES.putIfAbsent(creature, range);
		follow(creature, range);
	}
	
	/**
	 * Same as {@link #addNormalFollow(Creature, int)} but ticks at {@link #PLAYABLE_FOLLOW_INTERVAL} instead of 1s.
	 * Used for Playable actors (players/summons) so the fixed-destination MoveToLocation is recalculated often
	 * enough to look smooth, without reverting to MoveToPawn (which caused observer desync).
	 * @param creature
	 * @param range
	 */
	public void addPlayableFollow(Creature creature, int range)
	{
		PLAYABLE_FOLLOW_CREATURES.putIfAbsent(creature, range);
		follow(creature, range);
	}
	
	public void addAttackFollow(Creature creature, int range)
	{
		ATTACK_FOLLOW_CREATURES.putIfAbsent(creature, range);
		follow(creature, range);
	}
	
	public void remove(Creature creature)
	{
		NORMAL_FOLLOW_CREATURES.remove(creature);
		PLAYABLE_FOLLOW_CREATURES.remove(creature);
		ATTACK_FOLLOW_CREATURES.remove(creature);
	}
	
	public static CreatureFollowTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CreatureFollowTaskManager INSTANCE = new CreatureFollowTaskManager();
	}
}
