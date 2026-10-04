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
package com.l2journey.gameserver.model.actor.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.data.enums.CategoryType;
import com.l2journey.gameserver.data.xml.PetDataTable;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.model.PetData;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.InstanceType;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.effects.EffectType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.skill.BuffInfo;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;

public class BabyPet extends Pet
{
	private static final int BUFF_CONTROL = 5771;
	private static final int AWAKENING = 5753;
	private static final int SWITCH_STATE = 6054;
	
	protected List<SkillHolder> _buffs = null;
	protected SkillHolder _majorHeal = null;
	protected SkillHolder _minorHeal = null;
	protected SkillHolder _recharge = null;
	
	private Future<?> _castTask;
	
	protected boolean _bufferMode = true;
	
	/**
	 * Creates a baby pet.
	 * @param template the baby pet NPC template
	 * @param owner the owner
	 * @param control the summoning item
	 */
	public BabyPet(NpcTemplate template, Player owner, Item control)
	{
		super(template, owner, control);
		setInstanceType(InstanceType.BabyPet);
	}
	
	/**
	 * Creates a baby pet.
	 * @param template the baby pet NPC template
	 * @param owner the owner
	 * @param control the summoning item
	 * @param level the level
	 */
	public BabyPet(NpcTemplate template, Player owner, Item control, byte level)
	{
		super(template, owner, control, level);
		setInstanceType(InstanceType.BabyPet);
	}
	
	@Override
	public void onSpawn()
	{
		super.onSpawn();
		
		double healPower = 0;
		
		final PetData petData = PetDataTable.getInstance().getPetData(getId());
		if (petData == null)
		{
			startCastTask();
			return;
		}
		
		if (_buffs != null)
		{
			_buffs.clear();
		}
		
		for (PetData.PetSkillLearn psl : petData.getAvailableSkills())
		{
			final int minLevel = psl.getMinLevel();
			final int currentLevel = getLevel();
			
			if (currentLevel < minLevel)
			{
				continue;
			}
			
			final int id = psl.getSkillId();
			final int level = petData.getAvailableLevel(id, currentLevel);
			if (level == 0)
			{
				continue;
			}
			
			final Skill skill = SkillData.getInstance().getSkill(id, level);
			if ((skill == null) || (skill.getId() == BUFF_CONTROL) || (skill.getId() == AWAKENING) || (skill.getId() == SWITCH_STATE))
			{
				continue;
			}
			
			if (skill.hasEffectType(EffectType.MANAHEAL_BY_LEVEL))
			{
				_recharge = new SkillHolder(skill);
				continue;
			}
			
			if (skill.hasEffectType(EffectType.HEAL))
			{
				if (healPower == 0)
				{
					_majorHeal = new SkillHolder(skill);
					_minorHeal = _majorHeal;
					healPower = skill.getPower();
				}
				else
				{
					if (skill.getPower() > healPower)
					{
						_majorHeal = new SkillHolder(skill);
					}
					else
					{
						_minorHeal = new SkillHolder(skill);
					}
				}
				continue;
			}
			
			if (skill.isContinuous() && !skill.isDebuff())
			{
				if (_buffs == null)
				{
					_buffs = new ArrayList<>();
				}
				_buffs.add(new SkillHolder(skill));
			}
		}
		startCastTask();
	}
	
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		stopCastTask();
		abortCast();
		return true;
	}
	
	@Override
	public synchronized void unSummon(Player owner)
	{
		stopCastTask();
		abortCast();
		super.unSummon(owner);
	}
	
	@Override
	public void doRevive()
	{
		super.doRevive();
		startCastTask();
	}
	
	@Override
	public void onDecay()
	{
		super.onDecay();
		
		if (_buffs != null)
		{
			_buffs.clear();
		}
	}
	
	private void startCastTask()
	{
		if ((_majorHeal != null) || (_buffs != null) || ((_recharge != null) && (_castTask == null) && !isDead()))
		{
			_castTask = ThreadPool.scheduleAtFixedRate(new CastTask(this), 3000, 2000);
		}
	}
	
	@Override
	public void switchMode()
	{
		_bufferMode = !_bufferMode;
		
		if (getOwner() != null)
		{
			if (_bufferMode)
			{
				getOwner().sendMessage("Pet in support mode");
			}
			else
			{
				getOwner().sendMessage("Support mode disabled");
			}
		}
	}
	
	/**
	 * Verify if this pet is in support mode.
	 * @return {@code true} if this baby pet is in support mode, {@code false} otherwise
	 */
	public boolean isInSupportMode()
	{
		return _bufferMode;
	}
	
	private void stopCastTask()
	{
		if (_castTask != null)
		{
			_castTask.cancel(false);
			_castTask = null;
		}
	}
	
	protected void castSkill(Skill skill)
	{
		// casting automatically stops any other action (such as autofollow or a move-to).
		// We need to gather the necessary info to restore the previous state.
		final boolean previousFollowStatus = getFollowStatus();
		
		// pet not following and owner outside cast range
		if (!previousFollowStatus && !isInsideRadius3D(getOwner(), skill.getCastRange()))
		{
			return;
		}
		
		setTarget(getOwner());
		useMagic(skill, false, false);
		
		// calling useMagic changes the follow status, if the babypet actually casts
		// (as opposed to failing due some factors, such as too low MP, etc).
		// if the status has actually been changed, revert it. Else, allow the pet to
		// continue whatever it was trying to do.
		// NOTE: This is important since the pet may have been told to attack a target.
		// reverting the follow status will abort this attack! While aborting the attack
		// in order to heal is natural, it is not acceptable to abort the attack on its own,
		// merely because the timer stroke and without taking any other action...
		if (previousFollowStatus != getFollowStatus())
		{
			setFollowStatus(previousFollowStatus);
		}
	}
	
	private class CastTask implements Runnable
	{
		private final BabyPet _baby;
		private final List<Skill> _currentBuffs = new ArrayList<>();
		
		public CastTask(BabyPet baby)
		{
			_baby = baby;
		}
		
		@Override
		public void run()
		{
			final Player owner = _baby.getOwner();
			// If the owner doesn't meet the conditions avoid casting.
			if ((owner == null) || owner.isDead() || owner.isInvul())
			{
				return;
			}
			
			// If the pet doesn't meet the conditions avoid casting.
			if (_baby.isDead() || _baby.isCastingNow() || _baby.isBetrayed() || _baby.isMuted() || _baby.isOutOfControl() || !_bufferMode || (_baby.getAI().getIntention() == Intention.CAST))
			{
				return;
			}
			
			Skill skill = null;
			if (_majorHeal != null)
			{
				// If the owner's HP is more than 80% for Baby Pets and 70% for Improved Baby pets, do nothing.
				// If the owner's HP is very low, under 15% for Baby pets and under 30% for Improved Baby Pets, have 75% chances of using a strong heal.
				// Otherwise, have 25% chances for weak heal.
				final double hpPercent = owner.getCurrentHp() / owner.getMaxHp();
				final boolean isImprovedBaby = isInCategory(CategoryType.BABY_PET_GROUP);
				if ((isImprovedBaby && (hpPercent < 0.3)) || (!isImprovedBaby && (hpPercent < 0.15)))
				{
					skill = _majorHeal.getSkill();
					if (!_baby.isSkillDisabled(skill) && (Rnd.get(100) <= 75) && (_baby.getCurrentMp() >= skill.getMpConsume()))
					{
						castSkill(skill);
						return;
					}
				}
				else if ((_majorHeal.getSkill() != _minorHeal.getSkill()) && ((isImprovedBaby && (hpPercent < 0.7)) || (!isImprovedBaby && (hpPercent < 0.8))))
				{
					// Cast _minorHeal only if it's different than _majorHeal, then pet has two heals available.
					skill = _minorHeal.getSkill();
					if (!_baby.isSkillDisabled(skill) && (Rnd.get(100) <= 25) && (_baby.getCurrentMp() >= skill.getMpConsume()))
					{
						castSkill(skill);
						return;
					}
				}
			}
			
			// Buff Control is not active
			if (!_baby.isAffectedBySkill(BUFF_CONTROL))
			{
				// searching for usable buffs
				if ((_buffs != null) && !_buffs.isEmpty())
				{
					for (SkillHolder buff : _buffs)
					{
						skill = buff.getSkill();
						if (_baby.isSkillDisabled(skill) || (_baby.getCurrentMp() < skill.getMpConsume()))
						{
							continue;
						}
						
						// Check if skill requires items and if pet has them
						if (((skill.getItemConsumeId() > 0) && (skill.getItemConsumeCount() > 0)) && (_baby.getInventory().getInventoryItemCount(skill.getItemConsumeId(), -1) < skill.getItemConsumeCount()))
						{
							// Item missing, skip this skill
							continue;
						}
						
						// Check if owner already has the buff
						final BuffInfo ownerBuffInfo = owner.getEffectList().getBuffInfoByAbnormalType(skill.getAbnormalType());
						// Check if owner has the buff blocked
						if (((ownerBuffInfo != null) && (skill.getAbnormalLevel() <= ownerBuffInfo.getSkill().getAbnormalLevel())) || ((owner.getEffectList().getBlockedAbnormalTypes() != null) && owner.getEffectList().getBlockedAbnormalTypes().contains(skill.getAbnormalType())))
						{
							continue;
						}
						
						// Check if pet itself already has the buff
						final BuffInfo petBuffInfo = _baby.getEffectList().getBuffInfoByAbnormalType(skill.getAbnormalType());
						// Check if pet has the buff blocked
						if (((petBuffInfo != null) && (skill.getAbnormalLevel() <= petBuffInfo.getSkill().getAbnormalLevel())) || ((_baby.getEffectList().getBlockedAbnormalTypes() != null) && _baby.getEffectList().getBlockedAbnormalTypes().contains(skill.getAbnormalType())))
						{
							continue;
						}
						
						_currentBuffs.add(skill);
					}
				}
				
				if (!_currentBuffs.isEmpty())
				{
					skill = _currentBuffs.get(Rnd.get(_currentBuffs.size()));
					castSkill(skill);
					_currentBuffs.clear();
					return;
				}
			}
			
			// buffs/heal not casted, trying recharge, if exist recharge casted only if owner in combat stance.
			if ((_recharge != null) && owner.isInCombat() && ((owner.getCurrentMp() / owner.getMaxMp()) < 0.6) && (Rnd.get(100) <= 60))
			{
				skill = _recharge.getSkill();
				if (!_baby.isSkillDisabled(skill) && (_baby.getCurrentMp() >= skill.getMpConsume()))
				{
					castSkill(skill);
				}
			}
		}
	}
}
