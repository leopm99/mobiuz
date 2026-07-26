/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.effecthandlers;

import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.conditions.Condition;
import com.l2journey.gameserver.model.effects.AbstractEffect;
import com.l2journey.gameserver.model.effects.EffectType;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * Sweep Mana Heal effect implementation. Restores MP to the player (effector) upon successful sweep usage.
 * @author KingHanker
 */
public class SweepManaHeal extends AbstractEffect
{
	private final double _power;
	
	public SweepManaHeal(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_power = params.getDouble("power", 0);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.MANAHEAL_BY_LEVEL;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		if ((effector == null) || (effected == null) || effected.isPlayer() || !effected.isAttackable())
		{
			return;
		}
		
		// Calculation
		final double abs = _power;
		final double absorb = ((effector.getCurrentMp() + abs) > effector.getMaxMp() ? effector.getMaxMp() : (effector.getCurrentMp() + abs));
		final int restored = (int) (absorb - effector.getCurrentMp());
		effector.setCurrentMp(absorb);
		// System message
		final SystemMessage sm = new SystemMessage(SystemMessageId.S1_MP_HAS_BEEN_RESTORED);
		sm.addInt(restored);
		effector.sendPacket(sm);
	}
}
