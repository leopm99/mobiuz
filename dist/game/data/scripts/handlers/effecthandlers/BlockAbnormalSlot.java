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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.conditions.Condition;
import com.l2journey.gameserver.model.effects.AbstractEffect;
import com.l2journey.gameserver.model.skill.AbnormalType;
import com.l2journey.gameserver.model.skill.BuffInfo;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.util.BlockedAbnormalSlotRelease;

/**
 * Block Buff Slot effect implementation.
 * @author Zoey76
 */
public class BlockAbnormalSlot extends AbstractEffect
{
	private final Set<AbnormalType> _blockAbnormalSlots;
	
	public BlockAbnormalSlot(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		final String blockBuffSlots = params.getString("slot", null);
		if ((blockBuffSlots != null) && !blockBuffSlots.isEmpty())
		{
			_blockAbnormalSlots = new HashSet<>();
			for (String slot : blockBuffSlots.split(";"))
			{
				_blockAbnormalSlots.add(AbnormalType.getAbnormalType(slot));
			}
		}
		else
		{
			_blockAbnormalSlots = Collections.<AbnormalType> emptySet();
		}
	}
	
	/**
	 * @return the abnormal types this effect blocks
	 */
	public Set<AbnormalType> getBlockAbnormalSlots()
	{
		return _blockAbnormalSlots;
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		final List<Set<AbnormalType>> remaining = new ArrayList<>();
		for (BuffInfo info : effected.getEffectList().getEffects())
		{
			for (AbstractEffect effect : info.getEffects())
			{
				if (effect instanceof BlockAbnormalSlot block)
				{
					remaining.add(block.getBlockAbnormalSlots());
				}
			}
		}
		effected.getEffectList().removeBlockedAbnormalTypes(BlockedAbnormalSlotRelease.slotsToRelease(_blockAbnormalSlots, remaining));
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		effected.getEffectList().addBlockedAbnormalTypes(_blockAbnormalSlots);
	}
}
