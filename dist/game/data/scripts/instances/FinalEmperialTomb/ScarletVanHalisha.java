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
package instances.FinalEmperialTomb;

import java.util.ArrayList;
import java.util.List;

import com.l2journey.gameserver.GeoData;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Decoy;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.util.LocationUtil;

import ai.AbstractNpcAI;

/**
 * @author Micr0, Zerox
 */
public class ScarletVanHalisha extends AbstractNpcAI
{
	// NPCs
	private static final int HALISHA2 = 29046;
	private static final int HALISHA3 = 29047;
	
	private Creature _target;
	private Skill _skill;
	private long _lastRangedSkillTime;
	private final int _rangedSkillMinCoolTime = 60000; // 1 minute
	
	public ScarletVanHalisha()
	{
		addAttackId(HALISHA2, HALISHA3);
		addKillId(HALISHA2, HALISHA3);
		addSpellFinishedId(HALISHA2, HALISHA3);
		registerMobs(HALISHA2, HALISHA3);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "attack":
			{
				if (npc != null)
				{
					getSkillAI(npc);
				}
				break;
			}
			case "random_target":
			{
				_target = getRandomTarget(npc, null);
				break;
			}
			default:
			{
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		getSkillAI(npc);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		startQuestTimer("random_Target", 5000, npc, null, true);
		startQuestTimer("attack", 500, npc, null, true);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		cancelQuestTimers("attack");
		cancelQuestTimers("random_Target");
	}
	
	private Skill getRndSkills(Npc npc)
	{
		switch (npc.getId())
		{
			case HALISHA2:
			{
				if (getRandom(100) < 10)
				{
					return SkillData.getInstance().getSkill(5015, 2);
				}
				else if (getRandom(100) < 10)
				{
					return SkillData.getInstance().getSkill(5015, 5);
				}
				else if (getRandom(100) < 2)
				{
					return SkillData.getInstance().getSkill(5016, 1);
				}
				else
				{
					return SkillData.getInstance().getSkill(5014, 2);
				}
			}
			case HALISHA3:
			{
				if (getRandom(100) < 10)
				{
					return SkillData.getInstance().getSkill(5015, 3);
				}
				else if (getRandom(100) < 10)
				{
					return SkillData.getInstance().getSkill(5015, 6);
				}
				else if (getRandom(100) < 10)
				{
					return SkillData.getInstance().getSkill(5015, 2);
				}
				else if (((_lastRangedSkillTime + _rangedSkillMinCoolTime) < System.currentTimeMillis()) && (getRandom(100) < 10))
				{
					return SkillData.getInstance().getSkill(5019, 1);
				}
				else if (((_lastRangedSkillTime + _rangedSkillMinCoolTime) < System.currentTimeMillis()) && (getRandom(100) < 10))
				{
					return SkillData.getInstance().getSkill(5018, 1);
				}
				else if (getRandom(100) < 2)
				{
					return SkillData.getInstance().getSkill(5016, 1);
				}
				else
				{
					return SkillData.getInstance().getSkill(5014, 3);
				}
			}
			default:
			{
				break;
			}
		}
		return SkillData.getInstance().getSkill(5014, 1);
	}
	
	private synchronized void getSkillAI(Npc npc)
	{
		if (npc.isInvul() || npc.isCastingNow())
		{
			return;
		}
		if ((getRandom(100) < 30) || (_target == null) || _target.isDead())
		{
			_skill = getRndSkills(npc);
			_target = getRandomTarget(npc, _skill);
		}
		final Creature target = _target;
		Skill skill = _skill;
		if (skill == null)
		{
			skill = getRndSkills(npc);
		}
		
		if (npc.isPhysicalMuted())
		{
			return;
		}
		
		if ((target == null) || target.isDead())
		{
			npc.setCastingNow(false);
			return;
		}
		
		if (LocationUtil.checkIfInRange(skill.getCastRange(), npc, target, true))
		{
			npc.getAI().setIntention(Intention.IDLE);
			npc.setTarget(target);
			npc.setCastingNow(true);
			_target = null;
			npc.doCast(skill);
		}
		else
		{
			npc.getAI().setIntention(Intention.FOLLOW, target, null);
			npc.getAI().setIntention(Intention.ATTACK, target, null);
			npc.setCastingNow(false);
		}
	}
	
	private Creature getRandomTarget(Npc npc, Skill skill)
	{
		final List<Creature> result = new ArrayList<>();
		for (WorldObject obj : World.getInstance().getVisibleObjects(npc, WorldObject.class))
		{
			if (obj.isPlayable() || (obj instanceof Decoy))
			{
				if (obj.isPlayer() && obj.asPlayer().isInvisible())
				{
					continue;
				}
				
				if (((obj.asCreature().getZ() < (npc.getZ() - 100)) && (obj.asCreature().getZ() > (npc.getZ() + 100))) || !GeoData.getInstance().canSeeTarget(obj, npc))
				{
					continue;
				}
			}
			if (obj.isPlayable() || (obj instanceof Decoy))
			{
				int skillRange = 150;
				if (skill != null)
				{
					switch (skill.getId())
					{
						case 5014:
						{
							skillRange = 150;
							break;
						}
						case 5015:
						{
							skillRange = 400;
							break;
						}
						case 5016:
						{
							skillRange = 200;
							break;
						}
						case 5018, 5019:
						{
							_lastRangedSkillTime = System.currentTimeMillis();
							skillRange = 550;
							break;
						}
						default:
						{
							break;
						}
					}
				}
				if (LocationUtil.checkIfInRange(skillRange, npc, obj, true) && !obj.asCreature().isDead())
				{
					result.add(obj.asCreature());
				}
			}
		}
		if (!result.isEmpty() && (!result.isEmpty()))
		{
			return getRandomEntry(result);
		}
		return null;
	}
	
	public static void main(String[] args)
	{
		new ScarletVanHalisha();
	}
}