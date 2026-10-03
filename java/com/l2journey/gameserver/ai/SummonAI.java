/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.l2journey.gameserver.ai;

import java.util.concurrent.Future;

import com.l2journey.Config;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.GeoData;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Summon;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.pathfinding.PathFinding;

public class SummonAI extends PlayableAI implements Runnable
{
	private static final int AVOID_RADIUS = 70;
	
	private volatile boolean _thinking; // to prevent recursive thinking
	private volatile boolean _startFollow = _actor.asSummon().getFollowStatus();
	private Creature _lastAttack = null;
	
	private volatile boolean _startAvoid = false;
	private Future<?> _avoidTask = null;
	
	public SummonAI(Summon creature)
	{
		super(creature);
	}
	
	@Override
	protected void onIntentionAttack(Creature target)
	{
		if ((Config.PATHFINDING > 0) && (PathFinding.getInstance().findPath(_actor.getX(), _actor.getY(), _actor.getZ(), target.getX(), target.getY(), target.getZ(), _actor.getInstanceId(), false) == null))
		{
			return;
		}
		
		super.onIntentionAttack(target);
	}
	
	@Override
	protected void onIntentionIdle()
	{
		stopFollow();
		_startFollow = false;
		onIntentionActive();
	}
	
	@Override
	protected void onIntentionActive()
	{
		if (_startFollow)
		{
			setIntention(Intention.FOLLOW, _actor.asSummon().getOwner());
		}
		else
		{
			super.onIntentionActive();
		}
	}
	
	@Override
	synchronized void changeIntention(Intention intention, Object arg0, Object arg1)
	{
		switch (intention)
		{
			case ACTIVE, FOLLOW:
			{
				startAvoidTask();
				break;
			}
			default:
			{
				stopAvoidTask();
			}
		}
		
		super.changeIntention(intention, arg0, arg1);
	}
	
	private void thinkAttack()
	{
		if (checkTargetLostOrDead(getAttackTarget()))
		{
			setAttackTarget(null);
			return;
		}
		
		if (maybeMoveToPawn(getAttackTarget(), _actor.getPhysicalAttackRange()))
		{
			return;
		}
		
		clientStopMoving(null);
		_actor.doAttack(getAttackTarget());
	}
	
	private void thinkCast()
	{
		if (checkTargetLost(getCastTarget()))
		{
			setCastTarget(null);
			return;
		}
		
		final boolean val = _startFollow;
		if (maybeMoveToPawn(getCastTarget(), _actor.getMagicalAttackRange(_skill)))
		{
			return;
		}
		
		clientStopMoving(null);
		final Summon summon = _actor.asSummon();
		summon.setFollowStatus(false);
		setIntention(Intention.IDLE);
		_startFollow = val;
		_actor.doCast(_skill);
	}
	
	private void thinkPickUp()
	{
		if (checkTargetLost(getTarget()) || maybeMoveToPawn(getTarget(), 36))
		{
			return;
		}
		
		setIntention(Intention.IDLE);
		_actor.asSummon().doPickupItem(getTarget());
	}
	
	private void thinkInteract()
	{
		if (checkTargetLost(getTarget()) || maybeMoveToPawn(getTarget(), 36))
		{
			return;
		}
		
		setIntention(Intention.IDLE);
	}
	
	@Override
	public void onActionThink()
	{
		if (_thinking || _actor.isCastingNow() || _actor.isAllSkillsDisabled())
		{
			return;
		}
		
		_thinking = true;
		
		try
		{
			switch (getIntention())
			{
				case ATTACK:
				{
					thinkAttack();
					break;
				}
				case CAST:
				{
					thinkCast();
					break;
				}
				case PICK_UP:
				{
					thinkPickUp();
					break;
				}
				case INTERACT:
				{
					thinkInteract();
					break;
				}
			}
		}
		finally
		{
			_thinking = false;
		}
	}
	
	@Override
	protected void onActionFinishCasting()
	{
		if (_lastAttack == null)
		{
			_actor.asSummon().setFollowStatus(_startFollow);
		}
		else
		{
			setIntention(Intention.ATTACK, _lastAttack);
			_lastAttack = null;
		}
	}
	
	@Override
	protected void onActionAttacked(Creature attacker)
	{
		super.onActionAttacked(attacker);
		
		avoidAttack(attacker);
	}
	
	@Override
	protected void onActionEvaded(Creature attacker)
	{
		super.onActionEvaded(attacker);
		
		avoidAttack(attacker);
	}
	
	private void avoidAttack(Creature attacker)
	{
		// trying to avoid if summon near owner
		if ((_actor.asSummon().getOwner() != null) && (_actor.asSummon().getOwner() != attacker) && _actor.asSummon().getOwner().isInsideRadius3D(_actor, 2 * AVOID_RADIUS))
		{
			_startAvoid = true;
		}
	}
	
	@Override
	public void run()
	{
		if (!_startAvoid)
		{
			return;
		}
		_startAvoid = false;
		
		if (_actor.isMoving() || _actor.isDead() || _actor.isMovementDisabled())
		{
			return;
		}
		
		final int ownerX = _actor.asSummon().getOwner().getX();
		final int ownerY = _actor.asSummon().getOwner().getY();
		final double angle = Math.toRadians(Rnd.get(-90, 90)) + Math.atan2(ownerY - _actor.getY(), ownerX - _actor.getX());
		final int targetX = ownerX + (int) (AVOID_RADIUS * Math.cos(angle));
		final int targetY = ownerY + (int) (AVOID_RADIUS * Math.sin(angle));
		if (GeoData.getInstance().canMove(_actor.getX(), _actor.getY(), _actor.getZ(), targetX, targetY, _actor.getZ(), _actor.getInstanceId()))
		{
			moveTo(targetX, targetY, _actor.getZ());
		}
	}
	
	public void notifyFollowStatusChange()
	{
		_startFollow = !_startFollow;
		switch (getIntention())
		{
			case ACTIVE, FOLLOW, IDLE, MOVE_TO, PICK_UP:
			{
				_actor.asSummon().setFollowStatus(_startFollow);
			}
		}
	}
	
	public void setStartFollowController(boolean value)
	{
		_startFollow = value;
	}
	
	@Override
	protected void onIntentionCast(Skill skill, WorldObject target)
	{
		if (getIntention() == Intention.ATTACK)
		{
			_lastAttack = getAttackTarget();
		}
		else
		{
			_lastAttack = null;
		}
		
		super.onIntentionCast(skill, target);
	}
	
	private void startAvoidTask()
	{
		if (_avoidTask == null)
		{
			_avoidTask = ThreadPool.scheduleAtFixedRate(this, 100, 100);
		}
	}
	
	private void stopAvoidTask()
	{
		if (_avoidTask != null)
		{
			_avoidTask.cancel(false);
			_avoidTask = null;
		}
	}
	
	@Override
	public void stopAITask()
	{
		stopAvoidTask();
		super.stopAITask();
	}
}
