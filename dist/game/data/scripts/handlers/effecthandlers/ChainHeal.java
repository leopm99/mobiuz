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
package handlers.effecthandlers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.conditions.Condition;
import com.l2journey.gameserver.model.effects.AbstractEffect;
import com.l2journey.gameserver.model.effects.EffectType;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.skillVariation.ServitorShareConditions;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * Chain Heal effect implementation. Heals targets with decreasing power based on their position in the target list. The most injured target gets full power, subsequent targets get progressively less.
 * @author L2Journey, KingHanker
 */
public class ChainHeal extends AbstractEffect
{
	private static final double DECREASE_RATE = 0.10; // 10% decrease per target
	private static final Map<Integer, SkillCounter> SKILL_COUNTERS = new ConcurrentHashMap<>();
	
	private final int _power;
	
	/**
	 * Helper class to track the number of times a skill has been applied.
	 */
	private static class SkillCounter
	{
		private int count = 0;
		private long timestamp = System.currentTimeMillis();
		
		public synchronized int getAndIncrement()
		{
			// Reset counter if more than 1 second has passed (new skill use)
			if ((System.currentTimeMillis() - timestamp) > 1000)
			{
				count = 0;
				timestamp = System.currentTimeMillis();
			}
			return count++;
		}
	}
	
	public ChainHeal(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_power = params.getInt("power", 30);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.HEAL;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		if ((effected == null) || effected.isDead() || effected.isDoor())
		{
			return;
		}
		
		// Get skill use identifier (effector object ID + skill ID)
		final int skillKey = (effector.getObjectId() * 10000) + skill.getId();
		
		// Get or create counter for this skill use
		final SkillCounter counter = SKILL_COUNTERS.computeIfAbsent(skillKey, k -> new SkillCounter());
		
		// Get the target index (increments automatically)
		final int targetIndex = counter.getAndIncrement();
		
		// Calculate power with progressive decrease
		// First target (index 0) gets full power
		// Each subsequent target gets 10% less
		// Round to ensure integer percentages (30%, 27%, 24%, 21%, etc.)
		final double adjustedPower = Math.rint(Math.max(_power * (1.0 - (targetIndex * DECREASE_RATE)), _power * 0.1)); // Minimum 10% of original power
		final boolean full = (adjustedPower == 100.0);
		
		final double maxHp = ServitorShareConditions.getMaxServitorRecoverableHp(effected);
		double amount = full ? maxHp : (maxHp * adjustedPower) / 100.0;
		
		amount = Math.max(Math.min(amount, maxHp - effected.getCurrentHp()), 0);
		
		if (amount != 0)
		{
			effected.setCurrentHp(amount + effected.getCurrentHp());
		}
		
		SystemMessage sm;
		if (effector.getObjectId() != effected.getObjectId())
		{
			sm = new SystemMessage(SystemMessageId.S2_HP_HAS_BEEN_RESTORED_BY_C1);
			sm.addString(effector.getName());
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.S1_HP_HAS_BEEN_RESTORED);
		}
		sm.addInt((int) amount);
		effected.sendPacket(sm);
		
		// Clean up old counters periodically
		if (targetIndex == 0)
		{
			cleanupOldCounters();
		}
	}
	
	/**
	 * Removes counters that are older than 2 seconds to prevent memory leaks.
	 */
	private void cleanupOldCounters()
	{
		final long currentTime = System.currentTimeMillis();
		SKILL_COUNTERS.entrySet().removeIf(entry -> (currentTime - entry.getValue().timestamp) > 2000);
	}
}
