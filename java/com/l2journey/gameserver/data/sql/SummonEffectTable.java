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
package com.l2journey.gameserver.data.sql;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.Summon;
import com.l2journey.gameserver.model.actor.instance.Pet;
import com.l2journey.gameserver.model.actor.instance.Servitor;
import com.l2journey.gameserver.model.skill.Skill;

/**
 * @author Nyaran
 */
public class SummonEffectTable
{
	/** Servitors **/
	// Map tree
	// -> key: charObjectId, value: classIndex Map
	// --> key: classIndex, value: servitors Map
	// ---> key: servitorSkillId, value: Effects list
	private final Map<Integer, Map<Integer, Map<Integer, Collection<SummonEffect>>>> _servitorEffects = new ConcurrentHashMap<>();
	
	private Map<Integer, Collection<SummonEffect>> getServitorEffects(Player owner)
	{
		final Map<Integer, Map<Integer, Collection<SummonEffect>>> servitorMap = _servitorEffects.get(owner.getObjectId());
		if (servitorMap == null)
		{
			return null;
		}
		return servitorMap.get(owner.getClassIndex());
	}
	
	private Collection<SummonEffect> getServitorEffects(Player owner, int referenceSkill)
	{
		final Map<Integer, Collection<SummonEffect>> effects = getServitorEffects(owner);
		if (effects == null)
		{
			return null;
		}
		return effects.get(referenceSkill);
	}
	
	private boolean containsOwner(Player owner)
	{
		return _servitorEffects.getOrDefault(owner.getObjectId(), Collections.emptyMap()).containsKey(owner.getClassIndex());
	}
	
	private void removeEffects(Collection<SummonEffect> effects, int skillId)
	{
		if ((effects != null) && !effects.isEmpty())
		{
			for (SummonEffect effect : effects)
			{
				final Skill skill = effect.getSkill();
				if ((skill != null) && (skill.getId() == skillId))
				{
					effects.remove(effect);
				}
			}
		}
	}
	
	private void applyEffects(Summon summon, Collection<SummonEffect> summonEffects)
	{
		if (summonEffects == null)
		{
			return;
		}
		for (SummonEffect se : summonEffects)
		{
			if (se != null)
			{
				se.getSkill().applyEffects(summon, summon, false, se.getEffectCurTime());
			}
		}
	}
	
	public boolean containsSkill(Player owner, int referenceSkill)
	{
		return containsOwner(owner) && getServitorEffects(owner).containsKey(referenceSkill);
	}
	
	public void clearServitorEffects(Player owner, int referenceSkill)
	{
		final Map<Integer, Collection<SummonEffect>> effects = getServitorEffects(owner);
		if (effects == null)
		{
			return;
		}
		final Collection<SummonEffect> skillEffects = effects.get(referenceSkill);
		if (skillEffects != null)
		{
			skillEffects.clear();
		}
	}
	
	public void addServitorEffect(Player owner, int referenceSkill, Skill skill, int effectCurTime)
	{
		final Map<Integer, Collection<SummonEffect>> effects = _servitorEffects.computeIfAbsent(owner.getObjectId(), _ -> new ConcurrentHashMap<>()).computeIfAbsent(owner.getClassIndex(), _ -> new ConcurrentHashMap<>());
		effects.computeIfAbsent(referenceSkill, _ -> ConcurrentHashMap.newKeySet()).add(new SummonEffect(skill, effectCurTime));
	}
	
	public void removeServitorEffects(Player owner, int referenceSkill, int skillId)
	{
		removeEffects(getServitorEffects(owner, referenceSkill), skillId);
	}
	
	public void applyServitorEffects(Servitor servitor, Player owner, int referenceSkill)
	{
		applyEffects(servitor, getServitorEffects(owner, referenceSkill));
	}
	
	/** Pets **/
	private final Map<Integer, Collection<SummonEffect>> _petEffects = new ConcurrentHashMap<>(); // key: petItemObjectId, value: Effects list
	
	public void addPetEffect(int controlObjectId, Skill skill, int effectCurTime)
	{
		_petEffects.computeIfAbsent(controlObjectId, _ -> ConcurrentHashMap.newKeySet()).add(new SummonEffect(skill, effectCurTime));
	}
	
	public boolean containsPetId(int controlObjectId)
	{
		return _petEffects.containsKey(controlObjectId);
	}
	
	public void applyPetEffects(Pet pet, int controlObjectId)
	{
		applyEffects(pet, _petEffects.get(controlObjectId));
	}
	
	public void clearPetEffects(int controlObjectId)
	{
		_petEffects.getOrDefault(controlObjectId, Collections.emptyList()).clear();
	}
	
	public void removePetEffects(int controlObjectId, int skillId)
	{
		removeEffects(_petEffects.get(controlObjectId), skillId);
	}
	
	private class SummonEffect
	{
		Skill _skill;
		int _effectCurTime;
		
		public SummonEffect(Skill skill, int effectCurTime)
		{
			_skill = skill;
			_effectCurTime = effectCurTime;
		}
		
		public Skill getSkill()
		{
			return _skill;
		}
		
		public int getEffectCurTime()
		{
			return _effectCurTime;
		}
	}
	
	public static SummonEffectTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SummonEffectTable INSTANCE = new SummonEffectTable();
	}
}
