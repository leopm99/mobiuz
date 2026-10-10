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

import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.conditions.Condition;
import com.l2journey.gameserver.model.effects.AbstractEffect;
import com.l2journey.gameserver.model.effects.EffectType;
import com.l2journey.gameserver.model.item.Weapon;
import com.l2journey.gameserver.model.item.enums.ShotType;
import com.l2journey.gameserver.model.item.type.WeaponType;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.stats.BaseStat;
import com.l2journey.gameserver.model.stats.Formulas;
import com.l2journey.gameserver.model.stats.Stat;

/**
 * Energy Attack effect implementation.
 * @author NosBit
 */
public class EnergyDamage extends AbstractEffect
{
	private final double _power;
	private final int _criticalChance;
	private final boolean _ignoreShieldDefence;
	
	public EnergyDamage(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_power = params.getDouble("power", 0);
		_criticalChance = params.getInt("criticalChance", 0);
		_ignoreShieldDefence = params.getBoolean("ignoreShieldDefence", false);
	}
	
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		// TODO: Verify this on retail
		return !Formulas.calcPhysicalSkillEvasion(effector, effected, skill);
	}
	
	@Override
	public EffectType getEffectType()
	{
		return EffectType.PHYSICAL_ATTACK;
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		final Player attacker = effector.isPlayer() ? effector.asPlayer() : null;
		if (attacker == null)
		{
			return;
		}
		
		double attack = attacker.getPAtk(effected);
		double defence = effected.getPDef(attacker);
		if (!_ignoreShieldDefence)
		{
			final byte shield = Formulas.calcShldUse(attacker, effected, skill, true);
			switch (shield)
			{
				case Formulas.SHIELD_DEFENSE_FAILED:
				{
					break;
				}
				case Formulas.SHIELD_DEFENSE_SUCCEED:
				{
					defence += effected.getShldDef();
					break;
				}
				case Formulas.SHIELD_DEFENSE_PERFECT_BLOCK:
				{
					defence = -1;
					break;
				}
				default:
				{
					break;
				}
			}
		}
		
		double damage = 1;
		boolean critical = false;
		if (defence != -1)
		{
			final double damageMultiplier = Formulas.calcWeaponTraitBonus(attacker, effected) * Formulas.calcAttributeBonus(attacker, effected, skill) * Formulas.calcGeneralTraitBonus(attacker, effected, skill.getTraitType(), true);
			final boolean ss = skill.useSoulShot() && attacker.isChargedShot(ShotType.SOULSHOTS);
			final double ssBoost = ss ? 2 : 1.0;
			double weaponTypeBoost;
			final Weapon weapon = attacker.getActiveWeaponItem();
			if ((weapon != null) && ((weapon.getItemType() == WeaponType.BOW) || (weapon.getItemType() == WeaponType.CROSSBOW)))
			{
				weaponTypeBoost = 70;
			}
			else
			{
				weaponTypeBoost = 77;
			}
			
			// charge count should be the count before casting the skill but since its reduced before calling effects
			// we add skill consume charges to current charges
			final double energyChargesBoost = (((attacker.getCharges() + skill.getChargeConsumeCount()) - 1) * 0.2) + 1;
			attack += _power;
			attack *= ssBoost;
			attack *= energyChargesBoost;
			attack *= weaponTypeBoost;
			if (effected.isPlayer())
			{
				defence *= effected.getStat().calcStat(Stat.PVP_PHYS_SKILL_DEF, 1.0);
			}
			
			damage = attack / defence;
			damage *= damageMultiplier;
			if (effected.isPlayer())
			{
				damage *= attacker.getStat().calcStat(Stat.PVP_PHYS_SKILL_DMG, 1.0);
				damage = attacker.getStat().calcStat(Stat.PHYSICAL_SKILL_POWER, damage);
			}
			
			critical = (BaseStat.STR.calcBonus(attacker) * _criticalChance) > (Rnd.nextDouble() * 100);
			if (critical)
			{
				damage *= 2;
			}
		}
		
		if (damage > 0)
		{
			attacker.sendDamageMessage(effected, (int) damage, false, critical, false);
			effected.reduceCurrentHp(damage, attacker, skill);
			effected.notifyDamageReceived(damage, attacker, skill, critical, false);
			
			// Check if damage should be reflected
			Formulas.calcDamageReflected(attacker, effected, skill, critical);
		}
	}
}