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
package ai.areas.PlainsOfLizardman;

import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Attackable;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Playable;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;

import ai.AbstractNpcAI;

/**
 * Plains of Lizardmen AI.
 * @author Gnacik, malyelfik
 */
public class PlainsOfLizardman extends AbstractNpcAI
{
	// NPCs
	private static final int INVISIBLE_NPC = 18919;
	private static final int TANTA_GUARD = 18862;
	private static final int FANTASY_MUSHROOM = 18864;
	private static final int STICKY_MUSHROOM = 18865;
	private static final int RAINBOW_FROG = 18866;
	private static final int ENERGY_PLANT = 18868;
	private static final int TANTA_SCOUT = 22768;
	private static final int TANTA_MAGICIAN = 22773;
	private static final int TANTA_SUMMONER = 22774;
	private static final int[] TANTA_LIZARDMEN =
	{
		22768, // Tanta Lizardman Scout
		22769, // Tanta Lizardman Warrior
		22770, // Tanta Lizardman Soldier
		22771, // Tanta Lizardman Berserker
		22772, // Tanta Lizardman Archer
		22773, // Tanta Lizardman Magician
		22774, // Tanta Lizardman Summoner
	};
	// Skills
	private static final SkillHolder STUN_EFFECT = new SkillHolder(6622, 1);
	private static final SkillHolder DEMOTIVATION_HEX = new SkillHolder(6425, 1);
	private static final SkillHolder FANTASY_MUSHROOM_SKILL = new SkillHolder(6427, 1);
	private static final SkillHolder RAINBOW_FROG_SKILL = new SkillHolder(6429, 1);
	private static final SkillHolder STICKY_MUSHROOM_SKILL = new SkillHolder(6428, 1);
	private static final SkillHolder ENERGY_PLANT_SKILL = new SkillHolder(6430, 1);
	// Misc
	private static final double HP_PERCENTAGE = 0.60;
	// Buffs
	private static final SkillHolder[] BUFFS =
	{
		new SkillHolder(6625, 1), // Energy of Life
		new SkillHolder(6626, 2), // Energy of Life's Power
		new SkillHolder(6627, 3), // Energy of Life's Highest Power
		new SkillHolder(6628, 1), // Energy of Mana
		new SkillHolder(6629, 2), // Energy of Mana's Power
		new SkillHolder(6630, 3), // Energy of Mana's Highest Power
		new SkillHolder(6631, 1), // Energy of Power
		new SkillHolder(6633, 1), // Energy of Attack Speed
		new SkillHolder(6635, 1), // Energy of Crt Rate
		new SkillHolder(6636, 1), // Energy of Moving Speed
		new SkillHolder(6638, 1), // Aura of Mystery
		new SkillHolder(6639, 1), // Bane of Auras - Damage
		new SkillHolder(6640, 1), // Energizing Aura
		new SkillHolder(6674, 1), // Energy of Range Increment
	};
	// Misc
	// @formatter:off
	private static final int[] BUFF_LIST =
	{
		6, 7, 8, 11, 13
	};
	// @formatter:on
	
	private PlainsOfLizardman()
	{
		addAttackId(FANTASY_MUSHROOM, RAINBOW_FROG, STICKY_MUSHROOM, ENERGY_PLANT, TANTA_SUMMONER);
		addKillId(TANTA_LIZARDMEN);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("fantasy_mushroom") && (npc != null) && (player != null))
		{
			npc.doCast(FANTASY_MUSHROOM_SKILL.getSkill());
			World.getInstance().forEachVisibleObjectInRange(npc, Attackable.class, 200, monster ->
			{
				npc.setTarget(monster);
				npc.doCast(STUN_EFFECT.getSkill());
				addAttackDesire(monster, player);
			});
			npc.doDie(player);
		}
		return null;
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		switch (npc.getId())
		{
			case TANTA_SUMMONER:
			{
				if ((npc.getCurrentHp() < (npc.getMaxHp() * HP_PERCENTAGE)) && npc.isScriptValue(0))
				{
					npc.setScriptValue(1);
					npc.doCast(DEMOTIVATION_HEX.getSkill());
					addAttackDesire(addSpawn(TANTA_SCOUT, npc.getX(), npc.getY(), npc.getZ(), 0, false, 0, false), attacker);
					addAttackDesire(addSpawn(TANTA_SCOUT, npc.getX(), npc.getY(), npc.getZ(), 0, false, 0, false), attacker);
				}
				break;
			}
			case RAINBOW_FROG:
			{
				castSkill(npc, attacker, RAINBOW_FROG_SKILL);
				break;
			}
			case ENERGY_PLANT:
			{
				castSkill(npc, attacker, ENERGY_PLANT_SKILL);
				break;
			}
			case STICKY_MUSHROOM:
			{
				castSkill(npc, attacker, STICKY_MUSHROOM_SKILL);
				break;
			}
			case FANTASY_MUSHROOM:
			{
				if (npc.isScriptValue(0))
				{
					npc.setScriptValue(1);
					npc.setInvul(true);
					World.getInstance().forEachVisibleObjectInRange(npc, Attackable.class, 1000, monster ->
					{
						if ((monster.getId() == TANTA_MAGICIAN) || (monster.getId() == TANTA_SCOUT))
						{
							monster.setRunning();
							monster.getAI().setIntention(Intention.MOVE_TO, new Location(npc.getX(), npc.getY(), npc.getZ(), 0));
						}
					});
					startQuestTimer("fantasy_mushroom", 4000, npc, attacker);
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
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		// Tanta Guard
		if (getRandom(1000) == 0)
		{
			addAttackDesire(addSpawn(TANTA_GUARD, npc), killer);
		}
		
		// Invisible buff npc
		final int random = getRandom(100);
		final Npc buffer = addSpawn(INVISIBLE_NPC, npc.getLocation(), false, 6000);
		buffer.setTarget(killer);
		
		if (random <= 42)
		{
			castRandomBuff(buffer, 7, 45, BUFFS[0], BUFFS[1], BUFFS[2]);
		}
		if (random <= 11)
		{
			castRandomBuff(buffer, 8, 60, BUFFS[3], BUFFS[4], BUFFS[5]);
			castRandomBuff(buffer, 3, 6, BUFFS[9], BUFFS[10], BUFFS[12]);
		}
		if (random <= 25)
		{
			buffer.doCast(BUFFS[getRandomEntry(BUFF_LIST)].getSkill());
		}
		if (random <= 10)
		{
			buffer.doCast(BUFFS[13].getSkill());
		}
		if (random <= 1)
		{
			final int i = getRandom(100);
			if (i <= 34)
			{
				buffer.doCast(BUFFS[6].getSkill());
				buffer.doCast(BUFFS[7].getSkill());
				buffer.doCast(BUFFS[8].getSkill());
			}
			else if (i < 67)
			{
				buffer.doCast(BUFFS[13].getSkill());
			}
			else
			{
				buffer.doCast(BUFFS[2].getSkill());
				buffer.doCast(BUFFS[5].getSkill());
			}
		}
	}
	
	private void castRandomBuff(Npc npc, int chance1, int chance2, SkillHolder... buffs)
	{
		final int rand = getRandom(100);
		if (rand <= chance1)
		{
			npc.doCast(buffs[2].getSkill());
		}
		else if (rand <= chance2)
		{
			npc.doCast(buffs[1].getSkill());
		}
		else
		{
			npc.doCast(buffs[0].getSkill());
		}
	}
	
	@Override
	protected void castSkill(Npc npc, Playable target, SkillHolder skill)
	{
		npc.doDie(target);
		super.castSkill(addSpawn(INVISIBLE_NPC, npc, false, 6000), target, skill);
	}
	
	public static void main(String[] args)
	{
		new PlainsOfLizardman();
	}
}