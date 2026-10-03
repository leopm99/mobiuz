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
package com.l2journey.gameserver.model;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.ai.ControllableMobAI;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.data.SpawnTable;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.ControllableMob;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;

/**
 * @author littlecrow
 */
public class MobGroup
{
	private final NpcTemplate _npcTemplate;
	private final int _groupId;
	private final int _maxMobCount;
	
	private List<ControllableMob> _mobs;
	
	public MobGroup(int groupId, NpcTemplate npcTemplate, int maxMobCount)
	{
		_groupId = groupId;
		_npcTemplate = npcTemplate;
		_maxMobCount = maxMobCount;
	}
	
	public int getActiveMobCount()
	{
		return getMobs().size();
	}
	
	public int getGroupId()
	{
		return _groupId;
	}
	
	public int getMaxMobCount()
	{
		return _maxMobCount;
	}
	
	public List<ControllableMob> getMobs()
	{
		if (_mobs == null)
		{
			_mobs = new CopyOnWriteArrayList<>();
		}
		return _mobs;
	}
	
	public String getStatus()
	{
		try
		{
			switch (((ControllableMobAI) getMobs().get(0).getAI()).getAlternateAI())
			{
				case ControllableMobAI.AI_NORMAL:
				{
					return "Idle";
				}
				case ControllableMobAI.AI_FORCEATTACK:
				{
					return "Force Attacking";
				}
				case ControllableMobAI.AI_FOLLOW:
				{
					return "Following";
				}
				case ControllableMobAI.AI_CAST:
				{
					return "Casting";
				}
				case ControllableMobAI.AI_ATTACK_GROUP:
				{
					return "Attacking Group";
				}
				default:
				{
					return "Idle";
				}
			}
		}
		catch (Exception _)
		{
			return "Unspawned";
		}
	}
	
	public NpcTemplate getTemplate()
	{
		return _npcTemplate;
	}
	
	public boolean isGroupMember(ControllableMob mobInst)
	{
		for (ControllableMob groupMember : getMobs())
		{
			if (groupMember == null)
			{
				continue;
			}
			
			if (groupMember.getObjectId() == mobInst.getObjectId())
			{
				return true;
			}
		}
		
		return false;
	}
	
	public void spawnGroup(int x, int y, int z)
	{
		if (!getMobs().isEmpty())
		{
			return;
		}
		
		try
		{
			for (int i = 0; i < _maxMobCount; i++)
			{
				final GroupSpawn spawn = new GroupSpawn(_npcTemplate);
				final int signX = Rnd.nextBoolean() ? -1 : 1;
				final int signY = Rnd.nextBoolean() ? -1 : 1;
				final int randX = Rnd.get(MobGroupTable.RANDOM_RANGE);
				final int randY = Rnd.get(MobGroupTable.RANDOM_RANGE);
				spawn.setXYZ(x + (signX * randX), y + (signY * randY), z);
				spawn.stopRespawn();
				
				SpawnTable.getInstance().addSpawn(spawn);
				getMobs().add((ControllableMob) spawn.doGroupSpawn());
			}
		}
		catch (Exception _)
		{
			// Ignore.
		}
	}
	
	public void spawnGroup(Player player)
	{
		spawnGroup(player.getX(), player.getY(), player.getZ());
	}
	
	public void teleportGroup(Player player)
	{
		removeDead();
		
		for (ControllableMob mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			if (!mobInst.isDead())
			{
				final int x = player.getX() + Rnd.get(50);
				final int y = player.getY() + Rnd.get(50);
				mobInst.teleToLocation(new Location(x, y, player.getZ()), true);
				((ControllableMobAI) mobInst.getAI()).follow(player);
			}
		}
	}
	
	public ControllableMob getRandomMob()
	{
		removeDead();
		return getMobs().isEmpty() ? null : getMobs().get(Rnd.get(getMobs().size()));
	}
	
	public void unspawnGroup()
	{
		removeDead();
		
		if (getMobs().isEmpty())
		{
			return;
		}
		
		for (ControllableMob mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			if (!mobInst.isDead())
			{
				mobInst.deleteMe();
			}
			
			SpawnTable.getInstance().removeSpawn(mobInst.getSpawn());
		}
		
		getMobs().clear();
	}
	
	public void killGroup(Player player)
	{
		removeDead();
		
		for (ControllableMob mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			if (!mobInst.isDead())
			{
				mobInst.reduceCurrentHp(mobInst.getMaxHp() + 1, player, null);
			}
			
			SpawnTable.getInstance().removeSpawn(mobInst.getSpawn());
		}
		
		getMobs().clear();
	}
	
	public void setAttackRandom()
	{
		removeDead();
		
		for (ControllableMob mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			final ControllableMobAI ai = (ControllableMobAI) mobInst.getAI();
			ai.setAlternateAI(ControllableMobAI.AI_NORMAL);
			ai.setIntention(Intention.ACTIVE);
		}
	}
	
	public void setAttackTarget(Creature target)
	{
		removeDead();
		
		for (ControllableMob mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			((ControllableMobAI) mobInst.getAI()).forceAttack(target);
		}
	}
	
	public void setIdleMode()
	{
		removeDead();
		
		for (ControllableMob mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			((ControllableMobAI) mobInst.getAI()).stop();
		}
	}
	
	public void returnGroup(Creature creature)
	{
		setIdleMode();
		
		for (ControllableMob mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			((ControllableMobAI) mobInst.getAI()).move(creature.getX() + ((Rnd.nextBoolean() ? -1 : 1) * Rnd.get(MobGroupTable.RANDOM_RANGE)), creature.getY() + ((Rnd.nextBoolean() ? -1 : 1) * Rnd.get(MobGroupTable.RANDOM_RANGE)), creature.getZ());
		}
	}
	
	public void setFollowMode(Creature creature)
	{
		removeDead();
		
		for (ControllableMob mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			((ControllableMobAI) mobInst.getAI()).follow(creature);
		}
	}
	
	public void setCastMode()
	{
		removeDead();
		
		for (ControllableMob mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			((ControllableMobAI) mobInst.getAI()).setAlternateAI(ControllableMobAI.AI_CAST);
		}
	}
	
	public void setNoMoveMode(boolean enabled)
	{
		removeDead();
		
		for (ControllableMob mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			((ControllableMobAI) mobInst.getAI()).setNotMoving(enabled);
		}
	}
	
	protected void removeDead()
	{
		final List<ControllableMob> deadMobs = new LinkedList<>();
		for (ControllableMob mobInst : getMobs())
		{
			if ((mobInst != null) && mobInst.isDead())
			{
				deadMobs.add(mobInst);
			}
		}
		
		getMobs().removeAll(deadMobs);
	}
	
	public void setInvul(boolean invulState)
	{
		removeDead();
		
		for (ControllableMob mobInst : getMobs())
		{
			if (mobInst != null)
			{
				mobInst.setInvul(invulState);
			}
		}
	}
	
	public void setAttackGroup(MobGroup otherGrp)
	{
		removeDead();
		
		for (ControllableMob mobInst : getMobs())
		{
			if (mobInst == null)
			{
				continue;
			}
			
			final ControllableMobAI ai = (ControllableMobAI) mobInst.getAI();
			ai.forceAttackGroup(otherGrp);
			ai.setIntention(Intention.ACTIVE);
		}
	}
}