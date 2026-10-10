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
package ai.areas.StakatoNest;

import java.util.List;

import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Monster;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.skill.AbnormalType;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;
import com.l2journey.gameserver.util.ArrayUtil;
import com.l2journey.gameserver.util.Broadcast;
import com.l2journey.gameserver.util.LocationUtil;

import ai.AbstractNpcAI;

/**
 * Stakato Nest AI.
 * @author Gnacik
 */
public class StakatoNest extends AbstractNpcAI
{
	// @formatter:off
	// List of all mobs just for register
	private static final int[] STAKATO_MOBS =
	{
		18793, 18794, 18795, 18796, 18797, 18798, 22617, 22618, 22619, 22620,
		22621, 22622, 22623, 22624, 22625, 22626, 22627, 22628, 22629, 22630,
		22631, 22632, 22633, 25667
	};
	// Coocons
	private static final int[] COCOONS =
	{
		18793, 18794, 18795, 18796, 18797, 18798
	};
	// @formatter:on
	// Cannibalistic Stakato Leader
	private static final int STAKATO_LEADER = 22625;
	
	// Spike Stakato Nurse
	private static final int STAKATO_NURSE = 22630;
	// Spike Stakato Nurse (Changed)
	private static final int STAKATO_NURSE_2 = 22631;
	// Spiked Stakato Baby
	private static final int STAKATO_BABY = 22632;
	// Spiked Stakato Captain
	private static final int STAKATO_CAPTAIN = 22629;
	
	// Female Spiked Stakato
	private static final int STAKATO_FEMALE = 22620;
	// Male Spiked Stakato
	private static final int STAKATO_MALE = 22621;
	// Male Spiked Stakato (Changed)
	private static final int STAKATO_MALE_2 = 22622;
	// Spiked Stakato Guard
	private static final int STAKATO_GUARD = 22619;
	
	// Cannibalistic Stakato Chief
	private static final int STAKATO_CHIEF = 25667;
	// Growth Accelerator
	private static final int GROWTH_ACCELERATOR = 2905;
	// Small Stakato Cocoon
	private static final int SMALL_COCOON = 14833;
	// Large Stakato Cocoon
	private static final int LARGE_COCOON = 14834;
	
	private StakatoNest()
	{
		registerMobs(STAKATO_MOBS);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if ((npc.getId() == STAKATO_LEADER) && (npc.getEffectList().getBuffInfoByAbnormalType(AbnormalType.SILENCE) == null) && (getRandom(1000) < 100) && (npc.getCurrentHp() < (npc.getMaxHp() * 0.3)))
		{
			final Monster follower = checkMinion(npc);
			if (follower != null)
			{
				final double hp = follower.getCurrentHp();
				if (hp > (follower.getMaxHp() * 0.3))
				{
					npc.abortAttack();
					npc.abortCast();
					npc.setHeading(LocationUtil.calculateHeadingFrom(npc, follower));
					npc.doCast(SkillData.getInstance().getSkill(4484, 1));
					npc.setCurrentHp(npc.getCurrentHp() + hp);
					follower.doDie(follower);
					follower.deleteMe();
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Monster monster;
		switch (npc.getId())
		{
			case STAKATO_NURSE:
			{
				monster = checkMinion(npc);
				if (monster != null)
				{
					Broadcast.toSelfAndKnownPlayers(npc, new MagicSkillUse(npc, 2046, 1, 1000, 0));
					for (int i = 0; i < 3; i++)
					{
						final Npc spawned = addSpawn(STAKATO_CAPTAIN, monster, true);
						addAttackDesire(spawned, killer);
					}
				}
				break;
			}
			case STAKATO_BABY:
			{
				monster = npc.asMonster().getLeader();
				if ((monster != null) && !monster.isDead())
				{
					startQuestTimer("nurse_change", 5000, monster, killer);
				}
				break;
			}
			case STAKATO_MALE:
			{
				monster = checkMinion(npc);
				if (monster != null)
				{
					Broadcast.toSelfAndKnownPlayers(npc, new MagicSkillUse(npc, 2046, 1, 1000, 0));
					for (int i = 0; i < 3; i++)
					{
						final Npc spawned = addSpawn(STAKATO_GUARD, monster, true);
						addAttackDesire(spawned, killer);
					}
				}
				break;
			}
			case STAKATO_FEMALE:
			{
				monster = npc.asMonster().getLeader();
				if ((monster != null) && !monster.isDead())
				{
					startQuestTimer("male_change", 5000, monster, killer);
				}
				break;
			}
			case STAKATO_CHIEF:
			{
				if (killer.isInParty())
				{
					final List<Player> party = killer.getParty().getMembers();
					for (Player member : party)
					{
						giveCocoon(member, npc);
					}
				}
				else
				{
					giveCocoon(killer, npc);
				}
				break;
			}
			default:
			{
				break;
			}
		}
	}
	
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, List<WorldObject> targets, boolean isSummon)
	{
		if (ArrayUtil.contains(COCOONS, npc.getId()) && targets.contains(npc) && (skill.getId() == GROWTH_ACCELERATOR))
		{
			npc.doDie(caster);
			final Npc spawned = addSpawn(STAKATO_CHIEF, npc.getX(), npc.getY(), npc.getZ(), LocationUtil.calculateHeadingFrom(npc, caster), false, 0, true);
			addAttackDesire(spawned, caster);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ((npc == null) || (player == null) || npc.isDead())
		{
			return null;
		}
		
		int npcId = 0;
		switch (event)
		{
			case "nurse_change":
			{
				npcId = STAKATO_NURSE_2;
				break;
			}
			case "male_change":
			{
				npcId = STAKATO_MALE_2;
				break;
			}
			default:
			{
				break;
			}
		}
		if (npcId > 0)
		{
			npc.getSpawn().decreaseCount(npc);
			final Npc spawned = addSpawn(npcId, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 0, true);
			addAttackDesire(spawned, player);
			npc.deleteMe();
		}
		return super.onEvent(event, npc, player);
	}
	
	private static Monster checkMinion(Npc npc)
	{
		final Monster mob = npc.asMonster();
		if (mob.hasMinions())
		{
			final List<Monster> minion = mob.getMinionList().getSpawnedMinions();
			if ((minion != null) && !minion.isEmpty() && (minion.get(0) != null) && !minion.get(0).isDead())
			{
				return minion.get(0);
			}
		}
		return null;
	}
	
	private void giveCocoon(Player player, Npc npc)
	{
		player.addItem(ItemProcessType.REWARD, ((getRandom(100) > 80) ? LARGE_COCOON : SMALL_COCOON), 1, npc, true);
	}
	
	public static void main(String[] args)
	{
		new StakatoNest();
	}
}
