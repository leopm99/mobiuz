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
package com.l2journey.gameserver.model.skill;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.GeoData;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.item.enums.ShotType;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.MagicSkillLaunched;
import com.l2journey.gameserver.util.LocationUtil;

/**
 * Skill Channelizer implementation.
 * @author UnAfraid
 */
public class SkillChannelizer implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(SkillChannelizer.class.getName());
	
	private final Creature _channelizer;
	private List<Creature> _channelized;
	
	private Skill _skill;
	private ScheduledFuture<?> _task = null;
	
	public SkillChannelizer(Creature channelizer)
	{
		_channelizer = channelizer;
	}
	
	public Creature getChannelizer()
	{
		return _channelizer;
	}
	
	public List<Creature> getChannelized()
	{
		return _channelized;
	}
	
	public boolean hasChannelized()
	{
		return _channelized != null;
	}
	
	public void startChanneling(Skill skill)
	{
		// Verify for same status.
		if (isChanneling())
		{
			LOGGER.warning(() -> "Character: " + _channelizer + " is attempting to channel skill but he already does!");
			return;
		}
		
		// Start channeling.
		_skill = skill;
		final long tickInterval = skill.getChannelingTickInterval();
		if (tickInterval > 0)
		{
			_task = ThreadPool.scheduleAtFixedRate(this, skill.getChannelingTickInitialDelay(), tickInterval);
		}
		else
		{
			// A skill without a tick interval channels once, a period of zero would be rejected by the pool.
			_task = ThreadPool.schedule(this, skill.getChannelingTickInitialDelay());
		}
	}
	
	public void stopChanneling()
	{
		// Verify for same status.
		if (!isChanneling())
		{
			LOGGER.warning(() -> "Character: " + _channelizer + " is attempting to stop channel skill but he does not!");
			return;
		}
		
		// Cancel the task and unset it.
		_task.cancel(true);
		_task = null;
		
		// Cancel target channelization and unset it.
		if (_channelized != null)
		{
			for (Creature creature : _channelized)
			{
				creature.getSkillChannelized().removeChannelizer(_skill.getChannelingSkillId(), _channelizer);
			}
			_channelized = null;
		}
		
		// unset skill.
		_skill = null;
	}
	
	public Skill getSkill()
	{
		return _skill;
	}
	
	public boolean isChanneling()
	{
		return _task != null;
	}
	
	@Override
	public void run()
	{
		if (!isChanneling())
		{
			return;
		}
		
		final Skill skill = _skill;
		if (skill == null)
		{
			return;
		}
		
		try
		{
			if (skill.getMpPerChanneling() > 0)
			{
				// Validate mana per tick.
				if (_channelizer.getCurrentMp() < skill.getMpPerChanneling())
				{
					if (_channelizer.isPlayer())
					{
						_channelizer.sendPacket(SystemMessageId.YOUR_SKILL_WAS_DEACTIVATED_DUE_TO_LACK_OF_MP);
					}
					_channelizer.abortCast();
					return;
				}
				
				// Reduce mana per tick
				_channelizer.reduceCurrentMp(skill.getMpPerChanneling());
			}
			
			// Apply channeling skills on the targets.
			if (skill.getChannelingSkillId() > 0)
			{
				final Skill baseSkill = SkillData.getInstance().getSkill(skill.getChannelingSkillId(), 1);
				if (baseSkill == null)
				{
					LOGGER.warning(() -> getClass().getSimpleName() + ": skill " + skill + " couldn't find effect id skill: " + skill.getChannelingSkillId() + " !");
					_channelizer.abortCast();
					return;
				}
				
				final List<Creature> targetList = new LinkedList<>();
				for (WorldObject chars : skill.getTargetList(_channelizer))
				{
					if (chars.isCreature())
					{
						targetList.add(chars.asCreature());
						chars.asCreature().getSkillChannelized().addChannelizer(skill.getChannelingSkillId(), _channelizer);
					}
				}
				
				if (targetList.isEmpty())
				{
					return;
				}
				_channelized = targetList;
				
				for (Creature creature : _channelized)
				{
					if (!LocationUtil.checkIfInRange(skill.getEffectRange(), _channelizer, creature, true))
					{
					}
					else if (!GeoData.getInstance().canSeeTarget(_channelizer, creature))
					{
					}
					else
					{
						final int maxSkillLevel = SkillData.getInstance().getMaxLevel(skill.getChannelingSkillId());
						final int skillLevel = Math.min(creature.getSkillChannelized().getChannerlizersSize(skill.getChannelingSkillId()), maxSkillLevel);
						if (skillLevel == 0)
						{
							continue;
						}
						
						final BuffInfo info = creature.getEffectList().getBuffInfoBySkillId(skill.getChannelingSkillId());
						if ((info == null) || (info.getSkill().getLevel() < skillLevel))
						{
							final Skill channelingSkill = SkillData.getInstance().getSkill(skill.getChannelingSkillId(), skillLevel);
							if (channelingSkill == null)
							{
								LOGGER.warning(() -> getClass().getSimpleName() + ": Non existent channeling skill requested: " + skill);
								_channelizer.abortCast();
								return;
							}
							
							// Update PvP status
							if (creature.isPlayable() && _channelizer.isPlayer() && channelingSkill.isBad())
							{
								_channelizer.asPlayer().updatePvPStatus(creature);
							}
							
							channelingSkill.applyEffects(_channelizer, creature);
							
							// Reduce shots.
							if (skill.useSpiritShot())
							{
								_channelizer.setChargedShot(_channelizer.isChargedShot(ShotType.BLESSED_SPIRITSHOTS) ? ShotType.BLESSED_SPIRITSHOTS : ShotType.SPIRITSHOTS, false);
							}
							else
							{
								_channelizer.setChargedShot(ShotType.SOULSHOTS, false);
							}
							
							// Shots are re-charged every cast.
							_channelizer.rechargeShots(skill.useSoulShot(), skill.useSpiritShot());
						}
						_channelizer.broadcastPacket(new MagicSkillLaunched(_channelizer, skill.getId(), skill.getLevel(), creature));
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "Error while channelizing skill: " + skill + " channelizer: " + _channelizer + " channelized: " + _channelized + "; ");
		}
	}
}
