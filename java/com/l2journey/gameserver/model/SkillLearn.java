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
package com.l2journey.gameserver.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.l2journey.Config;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;
import com.l2journey.gameserver.model.actor.enums.player.SocialClass;
import com.l2journey.gameserver.model.item.holders.ItemHolder;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;

/**
 * @author Zoey76
 */
public class SkillLearn
{
	private final String _skillName;
	private final int _skillId;
	private final int _skillLevel;
	private final int _getLevel;
	private final boolean _autoGet;
	private final int _levelUpSp;
	private final Set<ItemHolder> _requiredItems = HashSet.newHashSet(1);
	private final Set<Race> _races = EnumSet.noneOf(Race.class);
	private final Set<SkillHolder> _preReqSkills = HashSet.newHashSet(1);
	private SocialClass _socialClass;
	private final boolean _residenceSkill;
	private final Set<Integer> _residenceIds = HashSet.newHashSet(1);
	private final List<SubClassData> _subClassLvlNumber = new ArrayList<>(1);
	private final boolean _learnedByNpc;
	private final boolean _learnedByFS;
	
	public class SubClassData
	{
		private final int slot;
		private final int level;
		
		public SubClassData(int pSlot, int pLvl)
		{
			slot = pSlot;
			level = pLvl;
		}
		
		/**
		 * @return the sub-class slot.
		 */
		public int getSlot()
		{
			return slot;
		}
		
		/**
		 * @return the required sub-class level.
		 */
		public int getLvl()
		{
			return level;
		}
	}
	
	/**
	 * Constructor for SkillLearn.
	 * @param set the set with the SkillLearn data.
	 */
	public SkillLearn(StatSet set)
	{
		_skillName = set.getString("skillName");
		_skillId = set.getInt("skillId");
		_skillLevel = set.getInt("skillLevel");
		_getLevel = set.getInt("getLevel");
		_autoGet = set.getBoolean("autoGet", false);
		_levelUpSp = set.getInt("levelUpSp", 0);
		_residenceSkill = set.getBoolean("residenceSkill", false);
		_learnedByNpc = set.getBoolean("learnedByNpc", false);
		_learnedByFS = set.getBoolean("learnedByFS", false);
	}
	
	/**
	 * @return the name of this skill.
	 */
	public String getName()
	{
		return _skillName;
	}
	
	/**
	 * @return the ID of this skill.
	 */
	public int getSkillId()
	{
		return _skillId;
	}
	
	/**
	 * @return the level of this skill.
	 */
	public int getSkillLevel()
	{
		return _skillLevel;
	}
	
	/**
	 * @return the minimum level required to acquire this skill.
	 */
	public int getGetLevel()
	{
		return _getLevel;
	}
	
	/**
	 * @return the amount of SP/Clan Reputation to acquire this skill.
	 */
	public int getLevelUpSp()
	{
		return _levelUpSp;
	}
	
	/**
	 * @return {@code true} if the skill is auto-get, this skill is automatically delivered.
	 */
	public boolean isAutoGet()
	{
		return _autoGet;
	}
	
	/**
	 * @return the set with the item holders required to acquire this skill.
	 */
	public Set<ItemHolder> getRequiredItems()
	{
		return _requiredItems;
	}
	
	/**
	 * Adds a required item holder to learn this skill.
	 * @param item the required item holder.
	 */
	public void addRequiredItem(ItemHolder item)
	{
		_requiredItems.add(item);
	}
	
	/**
	 * @return a set with the races that can acquire this skill.
	 */
	public Set<Race> getRaces()
	{
		return _races;
	}
	
	/**
	 * Adds a required race to learn this skill.
	 * @param race the required race.
	 */
	public void addRace(Race race)
	{
		_races.add(race);
	}
	
	/**
	 * @return the set of skill holders required to acquire this skill.
	 */
	public Set<SkillHolder> getPreReqSkills()
	{
		return _preReqSkills;
	}
	
	/**
	 * Adds a required skill holder to learn this skill.
	 * @param skill the required skill holder.
	 */
	public void addPreReqSkill(SkillHolder skill)
	{
		_preReqSkills.add(skill);
	}
	
	/**
	 * @return the social class required to get this skill.
	 */
	public SocialClass getSocialClass()
	{
		return _socialClass;
	}
	
	/**
	 * Sets the social class if hasn't been set before.
	 * @param socialClass the social class to set.
	 */
	public void setSocialClass(SocialClass socialClass)
	{
		if (_socialClass == null)
		{
			_socialClass = socialClass;
		}
	}
	
	/**
	 * @return {@code true} if this skill is a Residence skill.
	 */
	public boolean isResidencialSkill()
	{
		return _residenceSkill;
	}
	
	/**
	 * @return a set with the Ids where this skill is available.
	 */
	public Set<Integer> getResidenceIds()
	{
		return _residenceIds;
	}
	
	/**
	 * Adds a required residence Id.
	 * @param id the residence Id to add.
	 */
	public void addResidenceId(Integer id)
	{
		_residenceIds.add(id);
	}
	
	/**
	 * @return a list with Sub-Class conditions, amount of subclasses and level.
	 */
	public List<SubClassData> getSubClassConditions()
	{
		return _subClassLvlNumber;
	}
	
	/**
	 * Adds a required residence Id.
	 * @param slot the sub-class slot.
	 * @param level the required sub-class level.
	 */
	public void addSubclassConditions(int slot, int level)
	{
		_subClassLvlNumber.add(new SubClassData(slot, level));
	}
	
	/**
	 * @return {@code true} if this skill is learned from Npc.
	 */
	public boolean isLearnedByNpc()
	{
		return _learnedByNpc;
	}
	
	/**
	 * @return {@code true} if this skill is learned by Forgotten Scroll.
	 */
	public boolean isLearnedByFS()
	{
		return _learnedByFS;
	}
	
	/**
	 * Used for AltGameSkillLearn mod.<br>
	 * If the alternative skill learn system is enabled and the player is learning a skill from a different class apply a fee.<br>
	 * If the player is learning a skill from other class type (mage learning warrior skills or vice versa) the fee is higher.
	 * @param playerClass the player class Id.
	 * @param learningClass the skill learning player class Id.
	 * @return the amount of SP required to acquire this skill, by calculating the cost for the alternative skill learn system.
	 */
	public int getCalculatedLevelUpSp(PlayerClass playerClass, PlayerClass learningClass)
	{
		if ((playerClass == null) || (learningClass == null))
		{
			return _levelUpSp;
		}
		
		int levelUpSp = _levelUpSp;
		// If the alternative skill learn system is enabled and the player is learning a skill from a different class apply a fee.
		if (Config.ALT_GAME_SKILL_LEARN && (playerClass != learningClass))
		{
			// If the player is learning a skill from other class type (mage learning warrior skills or vice versa) the fee is higher.
			if (playerClass.isMage() != learningClass.isMage())
			{
				levelUpSp *= 3;
			}
			else
			{
				levelUpSp *= 2;
			}
		}
		return levelUpSp;
	}
}
