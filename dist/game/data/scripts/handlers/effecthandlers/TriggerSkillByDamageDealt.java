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
import com.l2journey.gameserver.handler.ITargetTypeHandler;
import com.l2journey.gameserver.handler.TargetHandler;
import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.enums.creature.InstanceType;
import com.l2journey.gameserver.model.conditions.Condition;
import com.l2journey.gameserver.model.effects.AbstractEffect;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.holders.actor.creature.OnCreatureDamageDealt;
import com.l2journey.gameserver.model.events.listeners.ConsumerEventListener;
import com.l2journey.gameserver.model.item.type.WeaponType;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.model.skill.targets.TargetType;

/**
 * Trigger skill by damage dealt effect implementation.
 * @author Zealar
 */
public class TriggerSkillByDamageDealt extends AbstractEffect
{
	private final int _minAttackerLevel;
	private final int _maxAttackerLevel;
	private final int _minDamage;
	private final int _chance;
	private final SkillHolder _skill;
	private final TargetType _targetType;
	private final InstanceType _attackerType;
	private int _allowWeapons;
	private final boolean _isCritical;
	private final boolean _allowNormalAttack;
	private final boolean _allowSkillAttack;
	
	/**
	 * @param attachCond
	 * @param applyCond
	 * @param set
	 * @param params
	 */
	
	public TriggerSkillByDamageDealt(Condition attachCond, Condition applyCond, StatSet set, StatSet params)
	{
		super(attachCond, applyCond, set, params);
		
		_minAttackerLevel = params.getInt("minAttackerLevel", 1);
		_maxAttackerLevel = params.getInt("maxAttackerLevel", 127);
		_minDamage = params.getInt("minDamage", 1);
		_chance = params.getInt("chance", 100);
		_skill = new SkillHolder(params.getInt("skillId"), params.getInt("skillLevel", 1));
		_targetType = params.getEnum("targetType", TargetType.class, TargetType.SELF);
		_attackerType = params.getEnum("attackerType", InstanceType.class, InstanceType.Creature);
		_isCritical = params.getBoolean("isCritical", false);
		_allowNormalAttack = params.getBoolean("allowNormalAttack", true);
		_allowSkillAttack = params.getBoolean("allowSkillAttack", false);
		
		if (params.getString("allowWeapons", "ALL").equalsIgnoreCase("ALL"))
		{
			_allowWeapons = 0;
		}
		else
		{
			for (String s : params.getString("allowWeapons").split(","))
			{
				_allowWeapons |= WeaponType.valueOf(s).mask();
			}
		}
	}
	
	private void onAttackEvent(OnCreatureDamageDealt event)
	{
		if (event.isDamageOverTime() || (_chance == 0) || ((_skill.getSkillId() == 0) || (_skill.getSkillLevel() == 0)) || (!_allowNormalAttack && !_allowSkillAttack))
		{
			return;
		}
		
		// Check if there is dependancy on critical.
		if (_isCritical != event.isCritical())
		{
			return;
		}
		
		final ITargetTypeHandler targetHandler = TargetHandler.getInstance().getHandler(_targetType);
		if (targetHandler == null)
		{
			LOGGER.warning("Handler for target type: " + _targetType + " does not exist.");
			return;
		}
		
		// When no skill attacks are allowed.
		// When no normal attacks are allowed.
		if ((!_allowSkillAttack && (event.getSkill() != null)) || (!_allowNormalAttack && (event.getSkill() == null)))
		{
			return;
		}
		
		if ((event.getAttacker() == event.getTarget()) || (event.getAttacker().getLevel() < _minAttackerLevel) || (event.getAttacker().getLevel() > _maxAttackerLevel))
		{
			return;
		}
		
		if ((event.getDamage() < _minDamage) || (Rnd.get(100) > _chance) || !event.getAttacker().getInstanceType().isType(_attackerType))
		{
			return;
		}
		
		if ((_allowWeapons > 0) && ((event.getAttacker().getActiveWeaponItem() == null) || ((event.getAttacker().getActiveWeaponItem().getItemType().mask() & _allowWeapons) == 0)))
		{
			return;
		}
		
		final Skill triggerSkill = _skill.getSkill();
		for (WorldObject triggerTarget : targetHandler.getTargetList(triggerSkill, event.getAttacker(), false, event.getTarget()))
		{
			if ((triggerTarget == null) || !triggerTarget.isCreature())
			{
				continue;
			}
			
			final Creature targetChar = triggerTarget.asCreature();
			if (!targetChar.isInvul())
			{
				event.getAttacker().makeTriggerCast(triggerSkill, targetChar);
			}
		}
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.removeListenerIf(EventType.ON_CREATURE_DAMAGE_DEALT, listener -> listener.getOwner() == this);
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill)
	{
		effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_DAMAGE_DEALT, this::onAttackEvent, this));
	}
}
