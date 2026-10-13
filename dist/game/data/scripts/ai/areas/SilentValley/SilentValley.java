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
package ai.areas.SilentValley;

import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;

/**
 * Silent Valley AI
 * @author malyelfik
 */
public class SilentValley extends AbstractNpcAI
{
	private static final String CLEAR_EVENT = "CLEAR_EVENT";

	// Skills
	private static final SkillHolder BETRAYAL = new SkillHolder(6033, 1); // Treasure Seeker's Betrayal
	private static final SkillHolder BLAZE = new SkillHolder(4157, 10); // NPC Blaze - Magic
	// Item
	private static final int SACK = 13799; // Treasure Sack of the Ancient Giants
	// Chance
	private static final int SPAWN_CHANCE = 2;
	private static final int CHEST_DIE_CHANCE = 5;
	// Monsters
	private static final int CHEST = 18693; // Treasure Chest of the Ancient Giants
	private static final int GUARD1 = 18694; // Treasure Chest Guard
	private static final int GUARD2 = 18695; // Treasure Chest Guard
	private static final int[] MOBS =
	{
		20965, // Chimera Piece
		20966, // Changed Creation
		20967, // Past Creature
		20968, // Nonexistent Man
		20969, // Giant's Shadow
		20970, // Soldier of Ancient Times
		20971, // Warrior of Ancient Times
		20972, // Shaman of Ancient Times
		20973, // Forgotten Ancient People
	};
	
	private SilentValley()
	{
		addAttackId(MOBS);
		addAttackId(CHEST, GUARD1, GUARD2);
		addEventReceivedId(GUARD1, GUARD2);
		addKillId(MOBS);
		addSpawnId(CHEST, GUARD2);
		addCreatureSeeId(GUARD1, GUARD2);
		addCreatureSeeId(MOBS);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ((npc != null) && !npc.isDead())
		{
			switch (event)
			{
				case "CLEAR":
				{
					npc.doDie(null);
					break;
				}
				case CLEAR_EVENT:
				{
					npc.broadcastEvent("CLEAR_ALL_INSTANT", 2000, null);
					npc.doDie(null);
					break;
				}
				case "SPAWN_CHEST":
				{
					addSpawn(CHEST, npc.getX() - 100, npc.getY(), npc.getZ() - 100, 0, false, 0);
					break;
				}
				default:
				{
					break;
				}
			}
		}
		return null;
	}
	
	@Override
	public void onAttack(Npc npc, Player player, int damage, boolean isSummon)
	{
		switch (npc.getId())
		{
			case CHEST:
			{
				if (!isSummon && npc.isScriptValue(0))
				{
					npc.setScriptValue(1);
					npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.YOU_WILL_BE_CURSED_FOR_SEEKING_THE_TREASURE);
					npc.setTarget(player);
					npc.doCast(BETRAYAL.getSkill());
				}
				else if (isSummon || (getRandom(100) < CHEST_DIE_CHANCE))
				{
					npc.dropItem(player, SACK, 1);
					npc.broadcastEvent("CLEAR_ALL", 2000, null);
					npc.doDie(null);
					cancelQuestTimer(CLEAR_EVENT, npc, null);
				}
				break;
			}
			case GUARD1, GUARD2:
			{
				npc.setTarget(player);
				npc.doCast(BLAZE.getSkill());
				addAttackDesire(npc, player);
				break;
			}
			default:
			{
				if (isSummon)
				{
					addAttackDesire(npc, player);
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (getRandom(1000) < SPAWN_CHANCE)
		{
			final int newZ = npc.getZ() + 100;
			addSpawn(GUARD2, npc.getX() + 100, npc.getY(), newZ, 0, false, 0);
			addSpawn(GUARD1, npc.getX() - 100, npc.getY(), newZ, 0, false, 0);
			addSpawn(GUARD1, npc.getX(), npc.getY() + 100, newZ, 0, false, 0);
			addSpawn(GUARD1, npc.getX(), npc.getY() - 100, newZ, 0, false, 0);
		}
	}
	
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayable())
		{
			final Player player = (creature.isSummon()) ? creature.asSummon().getOwner() : creature.asPlayer();
			if ((npc.getId() == GUARD1) || (npc.getId() == GUARD2))
			{
				npc.setTarget(player);
				npc.doCast(BLAZE.getSkill());
				addAttackDesire(npc, player);
			}
			else if (creature.isAffectedBySkill(BETRAYAL.getSkillId()))
			{
				addAttackDesire(npc, player);
			}
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == CHEST)
		{
			npc.setInvul(true);
			startQuestTimer(CLEAR_EVENT, 300000, npc, null);
		}
		else
		{
			startQuestTimer("SPAWN_CHEST", 10000, npc, null);
		}
	}
	
	@Override
	public String onEventReceived(String eventName, Npc sender, Npc receiver, WorldObject reference)
	{
		if ((receiver != null) && !receiver.isDead())
		{
			switch (eventName)
			{
				case "CLEAR_ALL":
				{
					startQuestTimer("CLEAR", 60000, receiver, null);
					break;
				}
				case "CLEAR_ALL_INSTANT":
				{
					receiver.doDie(null);
					break;
				}
				default:
				{
					break;
				}
			}
		}
		return super.onEventReceived(eventName, sender, receiver, reference);
	}
	
	public static void main(String[] args)
	{
		new SilentValley();
	}
}