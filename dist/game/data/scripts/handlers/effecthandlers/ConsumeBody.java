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
package handlers.effecthandlers;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.conditions.Condition;
import com.l2journey.gameserver.model.effects.AbstractEffect;
import com.l2journey.gameserver.model.skill.EffectScope;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.stats.Formulas;
import com.l2journey.gameserver.taskmanagers.DecayTaskManager;

/**
 * Consume Body effect implementation.
 * @author Zoey76
 */
public class ConsumeBody extends AbstractEffect
{
	public ConsumeBody(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		if ((effector == null) || (effected == null) || !effected.isNpc() || !effected.isDead())
		{
			return;
		}
		
		if (skill.hasEffects(EffectScope.START))
		{
			DecayTaskManager.getInstance().cancel(effected);
			ThreadPool.schedule(effected::onDecay, Formulas.calcAtkSpd(effector, skill, (double) (skill.getHitTime()) + skill.getCoolTime()));
		}
		else
		{
			effected.asNpc().endDecayTask();
		}
	}
}
