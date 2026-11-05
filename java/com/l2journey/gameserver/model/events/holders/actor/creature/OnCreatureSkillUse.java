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
package com.l2journey.gameserver.model.events.holders.actor.creature;

import java.util.Collection;

import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.holders.IBaseEvent;
import com.l2journey.gameserver.model.skill.Skill;

/**
 * An instantly executed event when Creature is attacked by Creature.
 * @author UnAfraid
 */
public class OnCreatureSkillUse implements IBaseEvent
{
	private Creature _caster;
	private Skill _skill;
	private boolean _simultaneously;
	private Creature _target;
	private Collection<WorldObject> _targets;
	
	public OnCreatureSkillUse()
	{
	}
	
	public synchronized Creature getCaster()
	{
		return _caster;
	}
	
	public synchronized void setCaster(Creature caster)
	{
		_caster = caster;
	}
	
	public synchronized Skill getSkill()
	{
		return _skill;
	}
	
	public synchronized void setSkill(Skill skill)
	{
		_skill = skill;
	}
	
	public synchronized boolean isSimultaneously()
	{
		return _simultaneously;
	}
	
	public synchronized void setSimultaneously(boolean simultaneously)
	{
		_simultaneously = simultaneously;
	}
	
	public synchronized Creature getTarget()
	{
		return _target;
	}
	
	public synchronized void setTarget(Creature target)
	{
		_target = target;
	}
	
	public synchronized Collection<WorldObject> getTargets()
	{
		return _targets;
	}
	
	public synchronized void setTargets(Collection<WorldObject> targets)
	{
		_targets = targets;
	}
	
	@Override
	public EventType getType()
	{
		return EventType.ON_CREATURE_SKILL_USE;
	}
}