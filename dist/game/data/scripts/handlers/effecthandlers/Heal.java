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

import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.conditions.Condition;
import com.l2journey.gameserver.model.effects.AbstractEffect;
import com.l2journey.gameserver.model.effects.EffectType;
import com.l2journey.gameserver.model.item.enums.ShotType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.item.type.CrystalType;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.skillVariation.ServitorShareConditions;
import com.l2journey.gameserver.model.stats.Formulas;
import com.l2journey.gameserver.model.stats.Stat;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * Heal effect implementation.
 * @author UnAfraid, KingHanker
 */
public class Heal extends AbstractEffect
{
	private final double _power;
	
	public Heal(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_power = params.getDouble("power", 0);
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
		if ((effected == null) || effected.isDead() || effected.isDoor() || effected.isInvul())
		{
			return;
		}
		
		double amount = _power;
		double staticShotBonus = 0;
		int mAtkMul = 1;
		final boolean sps = skill.isMagic() && effector.isChargedShot(ShotType.SPIRITSHOTS);
		final boolean bss = skill.isMagic() && effector.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
		if (((sps || bss) && (effector.isPlayer() && effector.asPlayer().isMageClass())) || effector.isSummon())
		{
			staticShotBonus = skill.getMpConsume(); // static bonus for spiritshots
			mAtkMul = bss ? 4 : 2;
			staticShotBonus *= bss ? 2.4 : 1.0;
		}
		else if ((sps || bss) && effector.isNpc())
		{
			staticShotBonus = 2.4 * skill.getMpConsume(); // always blessed spiritshots
			mAtkMul = 4;
		}
		else
		{
			// no static bonus
			// grade dynamic bonus
			final Item weaponInst = effector.getActiveWeaponInstance();
			if (weaponInst != null)
			{
				if (weaponInst.getTemplate().getCrystalType() == CrystalType.S84)
				{
					mAtkMul = 4;
				}
				else if (weaponInst.getTemplate().getCrystalType() == CrystalType.S80)
				{
					mAtkMul = 2;
				}
				else
				{
					mAtkMul = 1;
				}
			}
			// shot dynamic bonus
			mAtkMul = bss ? mAtkMul * 4 : mAtkMul + 1;
		}
		
		if (!skill.isStatic())
		{
			amount += staticShotBonus + Math.sqrt(mAtkMul * effector.getMAtk(effector, null));
			amount = effected.calcStat(Stat.HEAL_EFFECT, amount, null, null);
			// Heal critic, since CT2.3 Gracia Final
			if (skill.isMagic() && Formulas.calcMCrit(effector.getMCriticalHit(effected, skill)))
			{
				amount *= 3;
			}
		}
		
		final double maxRecoverableHp = ServitorShareConditions.getMaxServitorRecoverableHp(effected);
		
		// Prevents overheal and negative amount.
		amount = Math.max(Math.min(amount, maxRecoverableHp - effected.getCurrentHp()), 0);
		
		if (amount != 0)
		{
			effected.setCurrentHp(amount + effected.getCurrentHp());
		}
		
		if (effected.isPlayer())
		{
			if (skill.getId() == 4051)
			{
				effected.sendPacket(SystemMessageId.REJUVENATING_HP);
			}
			else
			{
				if (effector.isPlayer() && (effector != effected))
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S2_HP_HAS_BEEN_RESTORED_BY_C1);
					sm.addString(effector.getName());
					sm.addInt((int) amount);
					effected.sendPacket(sm);
				}
				else
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HP_HAS_BEEN_RESTORED);
					sm.addInt((int) amount);
					effected.sendPacket(sm);
				}
			}
		}
	}
}
