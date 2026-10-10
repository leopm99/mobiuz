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
package com.l2journey.gameserver.model.actor.tasks.creature;

import java.util.List;

import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.skill.Skill;

/**
 * Task dedicated to magic use of character
 * @author xban1x
 */
public class MagicUseTask implements Runnable
{
	private final Creature _creature;
	private List<WorldObject> _targets;
	private final Skill _skill;
	private int _count;
	private int _skillTime;
	private int _phase;
	private final boolean _simultaneously;
	
	public MagicUseTask(Creature creature, List<WorldObject> targets, Skill s, int hit, boolean simultaneous)
	{
		_creature = creature;
		_targets = targets;
		_skill = s;
		_count = 0;
		_phase = 1;
		_skillTime = hit;
		_simultaneously = simultaneous;
	}
	
	@Override
	public void run()
	{
		if (_creature == null)
		{
			return;
		}
		switch (_phase)
		{
			case 1:
			{
				_creature.onMagicLaunchedTimer(this);
				break;
			}
			case 2:
			{
				_creature.onMagicHitTimer(this);
				break;
			}
			case 3:
			{
				_creature.onMagicFinalizer(this);
				break;
			}
			default:
			{
				break;
			}
		}
	}
	
	public int getCount()
	{
		return _count;
	}
	
	public int getPhase()
	{
		return _phase;
	}
	
	public Skill getSkill()
	{
		return _skill;
	}
	
	public int getSkillTime()
	{
		return _skillTime;
	}
	
	public List<WorldObject> getTargets()
	{
		return _targets;
	}
	
	public boolean isSimultaneous()
	{
		return _simultaneously;
	}
	
	public void setCount(int count)
	{
		_count = count;
	}
	
	public void setPhase(int phase)
	{
		_phase = phase;
	}
	
	public void setSkillTime(int skillTime)
	{
		_skillTime = skillTime;
	}
	
	public void setTargets(List<WorldObject> targets)
	{
		_targets = targets;
	}
}