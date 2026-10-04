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
package com.l2journey.gameserver.model.actor.instance;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.managers.ZoneManager;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.InstanceType;
import com.l2journey.gameserver.model.actor.enums.creature.TrapAction;
import com.l2journey.gameserver.model.actor.tasks.npc.trap.TrapTask;
import com.l2journey.gameserver.model.actor.tasks.npc.trap.TrapTriggerTask;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.events.EventDispatcher;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.holders.actor.trap.OnTrapAction;
import com.l2journey.gameserver.model.item.Weapon;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.olympiad.OlympiadGameManager;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.AbstractNpcInfo.TrapInfo;
import com.l2journey.gameserver.network.serverpackets.ServerPacket;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;
import com.l2journey.gameserver.taskmanagers.DecayTaskManager;

/**
 * Trap instance.
 * @author nBd
 */
public class Trap extends Npc
{
	private static final int TICK = 1000; // 1s
	private boolean _hasLifeTime;
	private boolean _isInArena = false;
	private boolean _isTriggered;
	private final int _lifeTime;
	private Player _owner;
	private final Set<Integer> _playersWhoDetectedMe = new HashSet<>();
	private final SkillHolder _skill;
	private int _remainingTime;
	// Tasks
	private ScheduledFuture<?> _trapTask = null;
	
	/**
	 * Creates a trap.
	 * @param template the trap NPC template
	 * @param instanceId the instance ID
	 * @param lifeTime the life time
	 */
	public Trap(NpcTemplate template, int instanceId, int lifeTime)
	{
		super(template);
		setInstanceType(InstanceType.Trap);
		setInstanceId(instanceId);
		setName(template.getName());
		setInvul(false);
		
		_owner = null;
		_isTriggered = false;
		_skill = getTemplate().getParameters().getObject("trap_skill", SkillHolder.class);
		_hasLifeTime = lifeTime >= 0;
		_lifeTime = lifeTime != 0 ? lifeTime : 30000;
		_remainingTime = _lifeTime;
		if (_skill != null)
		{
			_trapTask = ThreadPool.scheduleAtFixedRate(new TrapTask(this), TICK, TICK);
		}
	}
	
	/**
	 * Creates a trap.
	 * @param template the trap NPC template
	 * @param owner the owner
	 * @param lifeTime the life time
	 */
	public Trap(NpcTemplate template, Player owner, int lifeTime)
	{
		this(template, owner.getInstanceId(), lifeTime);
		_owner = owner;
	}
	
	@Override
	public void broadcastPacket(ServerPacket packet)
	{
		World.getInstance().forEachVisibleObject(this, Player.class, player ->
		{
			if (_isTriggered || canBeSeen(player))
			{
				player.sendPacket(packet);
			}
		});
	}
	
	/**
	 * Verify if the character can see the trap.
	 * @param creature The creature to verify
	 * @return {@code true} if the character can see the trap, {@code false} otherwise
	 */
	public boolean canBeSeen(Creature creature)
	{
		if ((creature != null) && _playersWhoDetectedMe.contains(creature.getObjectId()))
		{
			return true;
		}
		
		if ((_owner == null) || (creature == null))
		{
			return false;
		}
		if (creature == _owner)
		{
			return true;
		}
		
		// observers can't see trap
		// olympiad competitors can't see trap
		if ((creature.isPlayer()) && (creature.asPlayer().inObserverMode() || (_owner.isInOlympiadMode() && creature.asPlayer().isInOlympiadMode() && (creature.asPlayer().getOlympiadSide() != _owner.getOlympiadSide()))))
		{
			return false;
		}
		
		if (_isInArena || (_owner.isInParty() && creature.isInParty() && (_owner.getParty().getLeaderObjectId() == creature.getParty().getLeaderObjectId())))
		{
			return true;
		}
		return false;
	}
	
	public boolean checkTarget(Creature target)
	{
		// Range seems to be reduced from Freya(300) to H5(150)
		if (!target.isInsideRadius2D(this, 150) || !Skill.checkForAreaOffensiveSkills(this, target, _skill.getSkill(), _isInArena))
		{
			return false;
		}
		
		// observers
		final Player player = target.asPlayer();
		if (target.isPlayer() && player.inObserverMode())
		{
			return false;
		}
		
		// olympiad own team and their summons not attacked
		if ((_owner != null) && _owner.isInOlympiadMode() && (player != null) && player.isInOlympiadMode() && (player.getOlympiadSide() == _owner.getOlympiadSide()))
		{
			return false;
		}
		
		if (_isInArena)
		{
			return true;
		}
		
		// trap owned by players not attack non-flagged players
		if (_owner != null)
		{
			if (target.isAttackable())
			{
				return true;
			}
			
			if ((player == null) || ((player.getPvpFlag() == 0) && (player.getKarma() == 0)))
			{
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean deleteMe()
	{
		if (_owner != null)
		{
			_owner.setTrap(null);
			_owner = null;
		}
		return super.deleteMe();
	}
	
	@Override
	public Player asPlayer()
	{
		return _owner;
	}
	
	@Override
	public Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	@Override
	public int getKarma()
	{
		return _owner != null ? _owner.getKarma() : 0;
	}
	
	/**
	 * Get the owner of this trap.
	 * @return the owner
	 */
	public Player getOwner()
	{
		return _owner;
	}
	
	public byte getPvpFlag()
	{
		return _owner != null ? _owner.getPvpFlag() : 0;
	}
	
	@Override
	public Item getSecondaryWeaponInstance()
	{
		return null;
	}
	
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	public Skill getSkill()
	{
		return _skill.getSkill();
	}
	
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return !canBeSeen(attacker);
	}
	
	@Override
	public boolean isTrap()
	{
		return true;
	}
	
	/**
	 * Checks is triggered
	 * @return True if trap is triggered.
	 */
	public boolean isTriggered()
	{
		return _isTriggered;
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_isInArena = isInsideZone(ZoneId.PVP) && !isInsideZone(ZoneId.SIEGE);
		_playersWhoDetectedMe.clear();
	}
	
	@Override
	public void sendDamageMessage(Creature target, int damage, boolean mcrit, boolean pcrit, boolean miss)
	{
		if (miss || (_owner == null))
		{
			return;
		}
		
		if (_owner.isInOlympiadMode() && target.isPlayer() && target.asPlayer().isInOlympiadMode() && (target.asPlayer().getOlympiadGameId() == _owner.getOlympiadGameId()))
		{
			OlympiadGameManager.getInstance().notifyCompetitorDamage(getOwner(), damage);
		}
		
		if (target.isInvul() && !target.isNpc())
		{
			_owner.sendPacket(SystemMessageId.THE_ATTACK_HAS_BEEN_BLOCKED);
		}
		else
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_DONE_S3_POINTS_OF_DAMAGE_TO_C2);
			sm.addString(getName());
			sm.addString(target.getName());
			sm.addInt(damage);
			_owner.sendPacket(sm);
		}
	}
	
	@Override
	public void sendInfo(Player player)
	{
		if (_isTriggered || canBeSeen(player))
		{
			player.sendPacket(new TrapInfo(this, player));
		}
	}
	
	public void setDetected(Creature detector)
	{
		if (_isInArena)
		{
			if (detector.isPlayable())
			{
				sendInfo(detector.asPlayer());
			}
			return;
		}
		
		if ((_owner != null) && (_owner.getPvpFlag() == 0) && (_owner.getKarma() == 0))
		{
			return;
		}
		
		_playersWhoDetectedMe.add(detector.getObjectId());
		
		// Notify to scripts
		if (EventDispatcher.getInstance().hasListener(EventType.ON_TRAP_ACTION, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnTrapAction(this, detector, TrapAction.TRAP_DETECTED), this);
		}
		
		if (detector.isPlayable())
		{
			sendInfo(detector.asPlayer());
		}
	}
	
	public void stopDecay()
	{
		DecayTaskManager.getInstance().cancel(this);
	}
	
	/**
	 * Trigger the trap.
	 * @param target the target
	 */
	public void triggerTrap(Creature target)
	{
		if (_trapTask != null)
		{
			_trapTask.cancel(true);
			_trapTask = null;
		}
		
		_isTriggered = true;
		broadcastPacket(new TrapInfo(this, null));
		setTarget(target);
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_TRAP_ACTION, this))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnTrapAction(this, target, TrapAction.TRAP_TRIGGERED), this);
		}
		
		ThreadPool.schedule(new TrapTriggerTask(this), 500);
	}
	
	public void unSummon()
	{
		if (_trapTask != null)
		{
			_trapTask.cancel(true);
			_trapTask = null;
		}
		
		if (_owner != null)
		{
			_owner.setTrap(null);
			_owner = null;
		}
		
		if (isSpawned() && !isDead())
		{
			ZoneManager.getInstance().getRegion(this).removeFromZones(this);
			deleteMe();
		}
	}
	
	@Override
	public void updateAbnormalEffect()
	{
	}
	
	public boolean hasLifeTime()
	{
		return _hasLifeTime;
	}
	
	public void setHasLifeTime(boolean value)
	{
		_hasLifeTime = value;
	}
	
	public int getRemainingTime()
	{
		return _remainingTime;
	}
	
	public void setRemainingTime(int time)
	{
		_remainingTime = time;
	}
	
	public int getLifeTime()
	{
		return _lifeTime;
	}
}
