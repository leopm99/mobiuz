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
package com.l2journey.gameserver.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import com.l2journey.Config;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.data.xml.NpcData;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.holders.npc.MinionHolder;
import com.l2journey.gameserver.model.actor.instance.Monster;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;

/**
 * @author luisantonioa, DS, Mobius
 */
public class MinionList
{
	protected final Monster _master;
	private final List<Monster> _spawnedMinions = new CopyOnWriteArrayList<>();
	private final List<ScheduledFuture<?>> _respawnTasks = new CopyOnWriteArrayList<>();
	
	public MinionList(Monster master)
	{
		if (master == null)
		{
			throw new NullPointerException("MinionList: Master is null!");
		}
		_master = master;
	}
	
	/**
	 * @return list of the spawned (alive) minions.
	 */
	public List<Monster> getSpawnedMinions()
	{
		return _spawnedMinions;
	}
	
	/**
	 * Manage the spawn of Minions.<br>
	 * <br>
	 * <b><u>Actions</u>:</b><br>
	 * <li>Get the Minion data of all Minions that must be spawn</li>
	 * <li>For each Minion type, spawn the amount of Minion needed</li><br>
	 * @param minions
	 */
	public void spawnMinions(List<MinionHolder> minions)
	{
		if (_master.isAlikeDead() || (minions == null))
		{
			return;
		}
		
		int minionCount;
		int minionId;
		int minionsToSpawn;
		for (MinionHolder minion : minions)
		{
			minionCount = minion.getCount();
			minionId = minion.getId();
			minionsToSpawn = minionCount - countSpawnedMinionsById(minionId);
			if (minionsToSpawn > 0)
			{
				for (int i = 0; i < minionsToSpawn; i++)
				{
					spawnMinion(minionId);
				}
			}
		}
	}
	
	/**
	 * Called on the minion spawn and added them in the list of the spawned minions.
	 * @param minion
	 */
	public void onMinionSpawn(Monster minion)
	{
		_spawnedMinions.add(minion);
	}
	
	/**
	 * Called on the master death/delete.
	 * @param force - When true, force delete of the spawned minions. By default minions are deleted only for raidbosses.
	 */
	public void onMasterDie(boolean force)
	{
		if (_master.isRaid() || force || Config.FORCE_DELETE_MINIONS)
		{
			if (!_spawnedMinions.isEmpty())
			{
				for (Monster minion : _spawnedMinions)
				{
					if (minion != null)
					{
						minion.setLeader(null);
						minion.deleteMe();
					}
				}
				_spawnedMinions.clear();
			}
			
			if (!_respawnTasks.isEmpty())
			{
				for (ScheduledFuture<?> task : _respawnTasks)
				{
					if ((task != null) && !task.isCancelled() && !task.isDone())
					{
						task.cancel(true);
					}
				}
				_respawnTasks.clear();
			}
		}
	}
	
	/**
	 * Called on the minion death/delete. Removed minion from the list of the spawned minions and reuse if possible.
	 * @param minion
	 * @param respawnTime (ms) enable respawning of this minion while master is alive. -1 - use default value: 0 (disable) for mobs and config value for raids.
	 */
	public void onMinionDie(Monster minion, int respawnTime)
	{
		// Prevent memory leaks.
		if (respawnTime == 0)
		{
			minion.setLeader(null);
		}
		_spawnedMinions.remove(minion);
		
		final int time = respawnTime < 0 ? _master.isRaid() ? (int) Config.RAID_MINION_RESPAWN_TIMER : 0 : respawnTime;
		if ((time > 0) && !_master.isAlikeDead())
		{
			_respawnTasks.add(ThreadPool.schedule(new MinionRespawnTask(minion), time));
		}
	}
	
	/**
	 * Called if master/minion was attacked. Master and all free minions receive aggro against attacker.
	 * @param caller
	 * @param attacker
	 */
	public void onAssist(Creature caller, Creature attacker)
	{
		if (attacker == null)
		{
			return;
		}
		
		if (!_master.isAlikeDead() && !_master.isInCombat())
		{
			_master.addDamageHate(attacker, 0, 1);
		}
		
		final boolean callerIsMaster = caller == _master;
		int aggro = callerIsMaster ? 10 : 1;
		if (_master.isRaid())
		{
			aggro *= 10;
		}
		
		for (Monster minion : _spawnedMinions)
		{
			if ((minion != null) && !minion.isDead() && (callerIsMaster || !minion.isInCombat()))
			{
				minion.addDamageHate(attacker, 0, aggro);
			}
		}
	}
	
	/**
	 * Called from onTeleported() of the master Alive and able to move minions teleported to master.
	 */
	public void onMasterTeleported()
	{
		final int offset = 200;
		final int minRadius = (int) _master.getCollisionRadius() + 30;
		for (Monster minion : _spawnedMinions)
		{
			if ((minion != null) && !minion.isDead() && !minion.isMovementDisabled())
			{
				int newX = Rnd.get(minRadius * 2, offset * 2); // x
				int newY = Rnd.get(newX, offset * 2); // distance
				newY = (int) Math.sqrt((double) ((newY * newY)) - (newX * newX)); // y
				if (newX > (offset + minRadius))
				{
					newX = (_master.getX() + newX) - offset;
				}
				else
				{
					newX = (_master.getX() - newX) + minRadius;
				}
				if (newY > (offset + minRadius))
				{
					newY = (_master.getY() + newY) - offset;
				}
				else
				{
					newY = (_master.getY() - newY) + minRadius;
				}
				
				minion.teleToLocation(new Location(newX, newY, _master.getZ()));
			}
		}
	}
	
	private void spawnMinion(int minionId)
	{
		if (minionId == 0)
		{
			return;
		}
		spawnMinion(_master, minionId);
	}
	
	private class MinionRespawnTask implements Runnable
	{
		private final Monster _minion;
		
		public MinionRespawnTask(Monster minion)
		{
			_minion = minion;
		}
		
		@Override
		public void run()
		{
			// minion can be already spawned or deleted
			if (!_master.isAlikeDead() && _master.isSpawned() && !_minion.isSpawned())
			{
				// _minion.refreshID();
				initializeNpc(_master, _minion);
				
				// assist master
				if (!_master.getAggroList().isEmpty())
				{
					_minion.getAggroList().putAll(_master.getAggroList());
					_minion.getAI().setIntention(Intention.ATTACK, _minion.getAggroList().keySet().stream().findFirst().get());
				}
			}
		}
	}
	
	/**
	 * Init a Minion and add it in the world as a visible object.<br>
	 * <br>
	 * <b><u>Actions</u>:</b><br>
	 * <li>Get the template of the Minion to spawn</li>
	 * <li>Create and Init the Minion and generate its Identifier</li>
	 * <li>Set the Minion HP, MP and Heading</li>
	 * <li>Set the Minion leader to this RaidBoss</li>
	 * <li>Init the position of the Minion and add it in the world as a visible object</li><br>
	 * @param master Monster used as master for this minion
	 * @param minionId The NpcTemplate Identifier of the Minion to spawn
	 * @return
	 */
	public static Monster spawnMinion(Monster master, int minionId)
	{
		// Get the template of the Minion to spawn
		final NpcTemplate minionTemplate = NpcData.getInstance().getTemplate(minionId);
		if (minionTemplate == null)
		{
			return null;
		}
		return initializeNpc(master, new Monster(minionTemplate));
	}
	
	protected static Monster initializeNpc(Monster master, Monster minion)
	{
		minion.stopAllEffects();
		minion.setDead(false);
		minion.setDecayed(false);
		
		// Set the Minion HP, MP and Heading
		minion.setCurrentHpMp(minion.getMaxHp(), minion.getMaxMp());
		minion.setHeading(master.getHeading());
		
		// Set the Minion leader to this RaidBoss
		minion.setLeader(master);
		
		// move monster to masters instance
		minion.setInstanceId(master.getInstanceId());
		
		// Init the position of the Minion and add it in the world as a visible object
		final int offset = 200;
		final int minRadius = (int) master.getCollisionRadius() + 30;
		int newX = Rnd.get(minRadius * 2, offset * 2); // x
		int newY = Rnd.get(newX, offset * 2); // distance
		newY = (int) Math.sqrt((double) ((newY * newY)) - (newX * newX)); // y
		if (newX > (offset + minRadius))
		{
			newX = (master.getX() + newX) - offset;
		}
		else
		{
			newX = (master.getX() - newX) + minRadius;
		}
		if (newY > (offset + minRadius))
		{
			newY = (master.getY() + newY) - offset;
		}
		else
		{
			newY = (master.getY() - newY) + minRadius;
		}
		
		minion.spawnMe(newX, newY, master.getZ());
		
		// Make sure info is broadcasted in instances
		if (minion.getInstanceId() > 0)
		{
			minion.broadcastInfo();
		}
		
		return minion;
	}
	
	// Statistics part
	
	private final int countSpawnedMinionsById(int minionId)
	{
		int count = 0;
		for (Monster minion : _spawnedMinions)
		{
			if ((minion != null) && (minion.getId() == minionId))
			{
				count++;
			}
		}
		return count;
	}
	
	public int countSpawnedMinions()
	{
		return _spawnedMinions.size();
	}
	
	public long lazyCountSpawnedMinionsGroups()
	{
		return _spawnedMinions.stream().distinct().count();
	}
}
