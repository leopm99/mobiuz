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
package ai.areas.MonasteryOfSilence;

import java.util.List;

import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Attackable;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.effects.EffectType;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;

/**
 * Monastery of Silence AI.
 * @author Kerberos, nonom
 */
public class MonasteryOfSilence extends AbstractNpcAI
{
	private static final String TRAINING = "TRAINING";
	private static final String DO_CAST = "DO_CAST";

	// NPCs
	private static final int CAPTAIN = 18910; // Solina Knight Captain
	private static final int KNIGHT = 18909; // Solina Knights
	private static final int SCARECROW = 18912; // Scarecrow
	private static final int GUIDE = 22789; // Guide Solina
	private static final int SEEKER = 22790; // Seeker Solina
	private static final int SAVIOR = 22791; // Savior Solina
	private static final int ASCETIC = 22793; // Ascetic Solina
	private static final int[] DIVINITY_CLAN =
	{
		22794, // Divinity Judge
		22795, // Divinity Manager
	};
	// Skills
	private static final SkillHolder ORDEAL_STRIKE = new SkillHolder(6303, 1); // Trial of the Coup
	private static final SkillHolder LEADER_STRIKE = new SkillHolder(6304, 1); // Shock
	private static final SkillHolder SAVER_STRIKE = new SkillHolder(6305, 1); // Sacred Gnosis
	private static final SkillHolder SAVER_BLEED = new SkillHolder(6306, 1); // Solina Strike
	private static final SkillHolder LEARNING_MAGIC = new SkillHolder(6308, 1); // Opus of the Wave
	private static final SkillHolder STUDENT_CANCEL = new SkillHolder(6310, 1); // Loss of Quest
	private static final SkillHolder WARRIOR_THRUSTING = new SkillHolder(6311, 1); // Solina Thrust
	private static final SkillHolder KNIGHT_BLESS = new SkillHolder(6313, 1); // Solina Bless
	// Misc
	private static final NpcStringId[] DIVINITY_MSG =
	{
		NpcStringId.S1_WHY_WOULD_YOU_CHOOSE_THE_PATH_OF_DARKNESS,
		NpcStringId.S1_HOW_DARE_YOU_DEFY_THE_WILL_OF_EINHASAD
	};
	private static final NpcStringId[] SOLINA_KNIGHTS_MSG =
	{
		NpcStringId.PUNISH_ALL_THOSE_WHO_TREAD_FOOTSTEPS_IN_THIS_PLACE,
		NpcStringId.WE_ARE_THE_SWORD_OF_TRUTH_THE_SWORD_OF_SOLINA,
		NpcStringId.WE_RAISE_OUR_BLADES_FOR_THE_GLORY_OF_SOLINA
	};
	
	private MonasteryOfSilence()
	{
		addSkillSeeId(DIVINITY_CLAN);
		addAttackId(KNIGHT, CAPTAIN, GUIDE, SEEKER, ASCETIC);
		addNpcHateId(GUIDE, SEEKER, SAVIOR, ASCETIC);
		addAggroRangeEnterId(GUIDE, SEEKER, SAVIOR, ASCETIC);
		addSpawnId(SCARECROW);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case TRAINING:
			{
				World.getInstance().forEachVisibleObjectInRange(npc, Npc.class, 400, character ->
				{
					if ((getRandom(100) < 30) && !character.isDead() && !character.isInCombat())
					{
						if ((character.getId() == CAPTAIN) && (getRandom(100) < 10) && npc.isScriptValue(0))
						{
							character.broadcastSay(ChatType.NPC_GENERAL, SOLINA_KNIGHTS_MSG[getRandom(SOLINA_KNIGHTS_MSG.length)]);
							character.setScriptValue(1);
							startQuestTimer("TIMER", 10000, character, null);
						}
						else if (character.getId() == KNIGHT)
						{
							character.setRunning();
							character.asAttackable().addDamageHate(npc, 0, 100);
							character.getAI().setIntention(Intention.ATTACK, npc, null);
						}
					}
				});
				break;
			}
			case DO_CAST:
			{
				if ((npc != null) && (player != null) && (getRandom(100) < 3))
				{
					if (npc.checkDoCastConditions(STUDENT_CANCEL.getSkill()))
					{
						npc.setTarget(player);
						npc.doCast(STUDENT_CANCEL.getSkill());
					}
					npc.setScriptValue(0);
				}
				break;
			}
			case "TIMER":
			{
				if (npc != null)
				{
					npc.setScriptValue(0);
				}
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
	public void onAttack(Npc npc, Player player, int damage, boolean isSummon)
	{
		final Attackable mob = npc.asAttackable();
		
		switch (npc.getId())
		{
			case KNIGHT:
			{
				if ((getRandom(100) < 10) && (mob.getMostHated() == player) && mob.checkDoCastConditions(WARRIOR_THRUSTING.getSkill()))
				{
					npc.setTarget(player);
					npc.doCast(WARRIOR_THRUSTING.getSkill());
				}
				break;
			}
			case CAPTAIN:
			{
				if ((getRandom(100) < 20) && (npc.getCurrentHp() < (npc.getMaxHp() * 0.5)) && npc.isScriptValue(0))
				{
					if (npc.checkDoCastConditions(KNIGHT_BLESS.getSkill()))
					{
						npc.setTarget(npc);
						npc.doCast(KNIGHT_BLESS.getSkill());
					}
					npc.setScriptValue(1);
					npc.broadcastSay(ChatType.GENERAL, NpcStringId.FOR_THE_GLORY_OF_SOLINA);
					addAttackDesire(addSpawn(KNIGHT, npc), player);
				}
				break;
			}
			case GUIDE:
			{
				if ((getRandom(100) < 3) && (mob.getMostHated() == player) && npc.checkDoCastConditions(ORDEAL_STRIKE.getSkill()))
				{
					npc.setTarget(player);
					npc.doCast(ORDEAL_STRIKE.getSkill());
				}
				break;
			}
			case SEEKER:
			{
				if ((getRandom(100) < 33) && (mob.getMostHated() == player) && npc.checkDoCastConditions(SAVER_STRIKE.getSkill()))
				{
					npc.setTarget(npc);
					npc.doCast(SAVER_STRIKE.getSkill());
				}
				break;
			}
			case ASCETIC:
			{
				if ((mob.getMostHated() == player) && npc.isScriptValue(0))
				{
					npc.setScriptValue(1);
					startQuestTimer(DO_CAST, 20000, npc, player);
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
	public boolean onNpcHate(Attackable mob, Player player, boolean isSummon)
	{
		return player.getActiveWeaponInstance() != null;
	}
	
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if (player.getActiveWeaponInstance() != null)
		{
			SkillHolder skill = null;
			switch (npc.getId())
			{
				case GUIDE:
				{
					if (getRandom(100) < 3)
					{
						skill = LEADER_STRIKE;
					}
					break;
				}
				case SEEKER:
				{
					skill = SAVER_BLEED;
					break;
				}
				case SAVIOR:
				{
					skill = LEARNING_MAGIC;
					break;
				}
				case ASCETIC:
				{
					if (getRandom(100) < 3)
					{
						skill = STUDENT_CANCEL;
					}
					
					if (npc.isScriptValue(0))
					{
						npc.setScriptValue(1);
						startQuestTimer(DO_CAST, 20000, npc, player);
					}
					break;
				}
				default:
				{
					break;
				}
			}
			
			if ((skill != null) && npc.checkDoCastConditions(skill.getSkill()))
			{
				npc.setTarget(player);
				npc.doCast(skill.getSkill());
			}
			
			if (!npc.isInCombat())
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.YOU_CANNOT_CARRY_A_WEAPON_WITHOUT_AUTHORIZATION);
			}
			
			addAttackDesire(npc, player);
		}
	}
	
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, List<WorldObject> targets, boolean isSummon)
	{
		if (skill.hasEffectType(EffectType.AGGRESSION) && (!targets.isEmpty()))
		{
			for (WorldObject obj : targets)
			{
				if (obj.equals(npc))
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, DIVINITY_MSG[getRandom(DIVINITY_MSG.length)], caster.getName());
					addAttackDesire(npc, caster);
					break;
				}
			}
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setInvul(true);
		npc.disableCoreAI(true);
		cancelQuestTimer(TRAINING, npc, null);
		startQuestTimer(TRAINING, 30000, npc, null, true);
	}
	
	public static void main(String[] args)
	{
		new MonasteryOfSilence();
	}
}
