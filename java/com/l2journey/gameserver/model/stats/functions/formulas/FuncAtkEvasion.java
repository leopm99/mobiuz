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
package com.l2journey.gameserver.model.stats.functions.formulas;

import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.stats.Stat;
import com.l2journey.gameserver.model.stats.functions.AbstractFunction;

/**
 * @author UnAfraid
 */
public class FuncAtkEvasion extends AbstractFunction
{
	private static final FuncAtkEvasion _fae_instance = new FuncAtkEvasion();
	
	public static AbstractFunction getInstance()
	{
		return _fae_instance;
	}
	
	private FuncAtkEvasion()
	{
		super(Stat.EVASION_RATE, 1, null, 0, null);
	}
	
	@Override
	public double calc(Creature effector, Creature effected, Skill skill, double initVal)
	{
		final int level = effector.getLevel();
		double value = initVal;
		if (effector.isPlayer())
		{
			// [Square(DEX)] * 6 + level;
			value += (Math.sqrt(effector.getDEX()) * 6) + level;
			double diff = level - 69.0;
			if (level >= 78)
			{
				diff *= 1.2;
			}
			if (level >= 70)
			{
				value += diff;
			}
		}
		else
		{
			// [Square(DEX)] * 6 + level;
			value += (Math.sqrt(effector.getDEX()) * 6) + level;
			if (level > 69)
			{
				value += (level - 69) + 2;
			}
		}
		return value;
	}
}