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
package com.l2journey.gameserver.model.zone.type;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.enums.creature.InstanceType;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.enums.SkillFinishType;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.model.zone.ZoneType;
import com.l2journey.gameserver.network.serverpackets.EtcStatusUpdate;

/**
 * Another type of damage zone with skills.
 * @author Kerberos
 */
public class EffectZone extends ZoneType
{
	int _chance;
	private int _initialDelay;
	private int _reuse;
	protected boolean _bypassConditions;
	private boolean _isShowDangerIcon;
	private boolean _removeEffectsOnExit;
	protected Map<Integer, Integer> _skills;
	protected volatile Future<?> _task;
	
	public EffectZone(int id)
	{
		super(id);
		_chance = 100;
		_initialDelay = 0;
		_reuse = 30000;
		setTargetType(InstanceType.Playable); // default only playable
		_bypassConditions = false;
		_isShowDangerIcon = true;
		_removeEffectsOnExit = false;
	}
	
	@Override
	public void setParameter(String name, String value)
	{
		switch (name)
		{
			case "chance":
			{
				_chance = Integer.parseInt(value);
				break;
			}
			case "initialDelay":
			{
				_initialDelay = Integer.parseInt(value);
				break;
			}
			case "reuse":
			{
				_reuse = Integer.parseInt(value);
				break;
			}
			case "bypassSkillConditions":
			{
				_bypassConditions = Boolean.parseBoolean(value);
				break;
			}
			case "maxDynamicSkillCount":
			{
				_skills = new ConcurrentHashMap<>(Integer.parseInt(value));
				break;
			}
			case "showDangerIcon":
			{
				_isShowDangerIcon = Boolean.parseBoolean(value);
				break;
			}
			case "skillIdLvl":
			{
				final String[] propertySplit = value.split(";");
				_skills = new ConcurrentHashMap<>(propertySplit.length);
				for (String skill : propertySplit)
				{
					final String[] skillSplit = skill.split("-");
					if (skillSplit.length != 2)
					{
						LOGGER.warning(() -> getClass().getSimpleName() + ": invalid config property -> skillsIdLvl \"" + skill + "\"");
					}
					else
					{
						try
						{
							_skills.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
						}
						catch (NumberFormatException _)
						{
							if (!skill.isEmpty())
							{
								LOGGER.warning(getClass().getSimpleName() + ": invalid config property -> skillsIdLvl \"" + skillSplit[0] + "\"" + skillSplit[1]);
							}
						}
					}
				}
				break;
			}
			case "removeEffectsOnExit":
			{
				_removeEffectsOnExit = Boolean.parseBoolean(value);
				break;
			}
			default:
			{
				super.setParameter(name, value);
			}
		}
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		if (_skills != null)
		{
			Future<?> task = _task;
			if (task == null)
			{
				synchronized (this)
				{
					task = _task;
					if (task == null)
					{
						_task = task = ThreadPool.scheduleAtFixedRate(new ApplySkill(), _initialDelay, _reuse);
					}
				}
			}
		}
		
		if (creature.isPlayer())
		{
			creature.setInsideZone(ZoneId.ALTERED, true);
			if (_isShowDangerIcon)
			{
				creature.setInsideZone(ZoneId.DANGER_AREA, true);
				creature.sendPacket(new EtcStatusUpdate(creature.asPlayer()));
			}
		}
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		if (creature.isPlayer())
		{
			creature.setInsideZone(ZoneId.ALTERED, false);
			if (_isShowDangerIcon)
			{
				creature.setInsideZone(ZoneId.DANGER_AREA, false);
				if (!creature.isInsideZone(ZoneId.DANGER_AREA))
				{
					creature.sendPacket(new EtcStatusUpdate(creature.asPlayer()));
				}
			}
			if (_removeEffectsOnExit && (_skills != null))
			{
				for (Entry<Integer, Integer> e : _skills.entrySet())
				{
					final Skill skill = SkillData.getInstance().getSkill(e.getKey().intValue(), e.getValue().intValue());
					if ((skill != null) && creature.isAffectedBySkill(skill.getId()))
					{
						creature.stopSkillEffects(SkillFinishType.REMOVED, skill.getId());
					}
				}
			}
		}
		
		if (getCharactersInside().isEmpty() && (_task != null))
		{
			_task.cancel(true);
			_task = null;
		}
	}
	
	public int getChance()
	{
		return _chance;
	}
	
	public void addSkill(int skillId, int skillLevel)
	{
		if (skillLevel < 1) // remove skill
		{
			removeSkill(skillId);
			return;
		}
		
		if (_skills == null)
		{
			synchronized (this)
			{
				if (_skills == null)
				{
					_skills = new ConcurrentHashMap<>(3);
				}
			}
		}
		_skills.put(skillId, skillLevel);
	}
	
	public void removeSkill(int skillId)
	{
		if (_skills != null)
		{
			_skills.remove(skillId);
		}
	}
	
	public void clearSkills()
	{
		if (_skills != null)
		{
			_skills.clear();
		}
	}
	
	public int getSkillLevel(int skillId)
	{
		if ((_skills == null) || !_skills.containsKey(skillId))
		{
			return 0;
		}
		return _skills.get(skillId);
	}
	
	/**
	 * Atomically increments the skill level and returns the new value. Thread-safe for concurrent access from multiple scripts.
	 * @param skillId the skill ID to increment
	 * @return the new skill level after incrementing
	 */
	public int incrementSkillLevel(int skillId)
	{
		if (_skills == null)
		{
			synchronized (this)
			{
				if (_skills == null)
				{
					_skills = new ConcurrentHashMap<>(3);
				}
			}
		}
		return _skills.merge(skillId, 1, Integer::sum);
	}
	
	/**
	 * Atomically decrements the skill level and returns the new value. If the level reaches 0 or below, the skill is removed. Thread-safe for concurrent access from multiple scripts.
	 * @param skillId the skill ID to decrement
	 * @return the new skill level after decrementing (0 if removed)
	 */
	public int decrementSkillLevel(int skillId)
	{
		if ((_skills == null) || !_skills.containsKey(skillId))
		{
			return 0;
		}
		final int newLevel = _skills.merge(skillId, -1, Integer::sum);
		if (newLevel < 1)
		{
			_skills.remove(skillId);
			return 0;
		}
		return newLevel;
	}
	
	private class ApplySkill implements Runnable
	{
		protected ApplySkill()
		{
			if (_skills == null)
			{
				throw new IllegalStateException("No skills defined.");
			}
		}
		
		@Override
		public void run()
		{
			if (getCharactersInside().isEmpty())
			{
				if (_task != null)
				{
					_task.cancel(false);
					_task = null;
				}
				return;
			}
			
			if (!isEnabled())
			{
				return;
			}
			
			for (Creature character : getCharactersInside())
			{
				if ((character != null) && !character.isDead() && (Rnd.get(100) < _chance))
				{
					for (Entry<Integer, Integer> e : _skills.entrySet())
					{
						final Skill skill = SkillData.getInstance().getSkill(e.getKey().intValue(), e.getValue().intValue());
						if (((skill != null) && (_bypassConditions || skill.checkCondition(character, character, false))) && (!character.isAffectedBySkill(skill.getId())))
						{
							if (getChance() == 100)
							{
								skill.applyEffectsWithoutSuccessCheck(character, character);
							}
							else
							{
								skill.applyEffects(character, character);
							}
						}
					}
				}
			}
		}
	}
}