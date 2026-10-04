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
package com.l2journey.gameserver.model.skill.skillVariation;

import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.effects.EffectFlag;

/**
 * tility class for Servitor Share effect conditions
 * @author KingHanker
 */
public final class ServitorShareConditions
{
	private ServitorShareConditions()
	{
	}

	/**
	 * Gets max recoverable MP considering Servitor Share effect
	 * @param creature the target creature
	 * @return max MP if Servitor Share is active, max recoverable MP otherwise
	 */
	public static double getMaxServitorRecoverableMp(Creature creature)
	{
		return (creature.isSummon() && (creature.asSummon().getOwner() != null) && creature.asSummon().getOwner().isAffected(EffectFlag.SERVITOR_SHARE)) ? creature.getMaxMp() : creature.getMaxRecoverableMp();
	}
	
	/**
	 * Gets max recoverable HP considering Servitor Share effect
	 * @param creature the target creature
	 * @return max HP if Servitor Share is active, max recoverable HP otherwise
	 */
	public static double getMaxServitorRecoverableHp(Creature creature)
	{
		return (creature.isSummon() && (creature.asSummon().getOwner() != null) && creature.asSummon().getOwner().isAffected(EffectFlag.SERVITOR_SHARE)) ? creature.getMaxHp() : creature.getMaxRecoverableHp();
	}
}