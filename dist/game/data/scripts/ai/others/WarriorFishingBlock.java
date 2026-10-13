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
package ai.others;

import com.l2journey.gameserver.ai.Action;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;

/**
 * Warrior Fishing Block AI.
 * @author Zoey76
 */
public class WarriorFishingBlock extends AbstractNpcAI
{
	private static final String DESPAWN = "DESPAWN";
	private static final String SPAWN = "SPAWN";

	// Monsters
	private static final int[] MONSTERS =
	{
		18319, // Caught Frog
		18320, // Caught Undine
		18321, // Caught Rakul
		18322, // Caught Sea Giant
		18323, // Caught Sea Horse Soldier
		18324, // Caught Homunculus
		18325, // Caught Flava
		18326, // Caught Gigantic Eye
	};
	// NPC Strings
	private static final NpcStringId[] NPC_STRINGS_ON_SPAWN =
	{
		NpcStringId.CROAK_CROAK_FOOD_LIKE_S1_IN_THIS_PLACE,
		NpcStringId.S1_HOW_LUCKY_I_AM,
		NpcStringId.PRAY_THAT_YOU_CAUGHT_A_WRONG_FISH_S1
	};
	private static final NpcStringId[] NPC_STRINGS_ON_ATTACK =
	{
		NpcStringId.DO_YOU_KNOW_WHAT_A_FROG_TASTES_LIKE,
		NpcStringId.I_WILL_SHOW_YOU_THE_POWER_OF_A_FROG,
		NpcStringId.I_WILL_SWALLOW_AT_A_MOUTHFUL
	};
	private static final NpcStringId[] NPC_STRINGS_ON_KILL =
	{
		NpcStringId.UGH_NO_CHANCE_HOW_COULD_THIS_ELDER_PASS_AWAY_LIKE_THIS,
		NpcStringId.CROAK_CROAK_A_FROG_IS_DYING,
		NpcStringId.A_FROG_TASTES_BAD_YUCK
	};
	// Misc
	private static final int CHANCE_TO_SHOUT_ON_ATTACK = 33;
	private static final int DESPAWN_TIME = 50; // 50 seconds to despawn
	
	public WarriorFishingBlock()
	{
		addAttackId(MONSTERS);
		addKillId(MONSTERS);
		addSpawnId(MONSTERS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case SPAWN:
			{
				final WorldObject obj = npc.getTarget();
				if ((obj == null) || !obj.isPlayer())
				{
					npc.decayMe();
				}
				else
				{
					final Player target = obj.asPlayer();
					npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(NPC_STRINGS_ON_SPAWN), target.getName());
					npc.asAttackable().addDamageHate(target, 0, 2000);
					npc.getAI().notifyAction(Action.ATTACKED, target);
					npc.addAttackerToAttackByList(target);
					
					startQuestTimer(DESPAWN, DESPAWN_TIME * 1000, npc, target);
				}
				break;
			}
			case DESPAWN:
			{
				npc.decayMe();
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
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (getRandom(100) < CHANCE_TO_SHOUT_ON_ATTACK)
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(NPC_STRINGS_ON_ATTACK));
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(NPC_STRINGS_ON_KILL));
		cancelQuestTimer(DESPAWN, npc, killer);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		cancelQuestTimer(SPAWN, npc, null);
		cancelQuestTimer(DESPAWN, npc, null);
		startQuestTimer(SPAWN, 2000, npc, null);
	}
	
	public static void main(String[] args)
	{
		new WarriorFishingBlock();
	}
}
