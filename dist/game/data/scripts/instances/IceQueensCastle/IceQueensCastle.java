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
package instances.IceQueensCastle;

import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.managers.InstanceManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.instancezone.InstanceWorld;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.enums.Movie;

import instances.AbstractInstance;
import quests.Q10285_MeetingSirra.Q10285_MeetingSirra;

/**
 * Ice Queen's Castle instance zone.
 * @author Adry_85
 */
public class IceQueensCastle extends AbstractInstance
{
	private static final String ATTACK_KNIGHT = "ATTACK_KNIGHT";

	// NPCs
	private static final int FREYA = 18847;
	private static final int BATTALION_LEADER = 18848;
	private static final int LEGIONNAIRE = 18849;
	private static final int MERCENARY_ARCHER = 18926;
	private static final int ARCHERY_KNIGHT = 22767;
	private static final int JINIA = 32781;
	// Locations
	private static final Location START_LOC = new Location(114000, -112357, -11200, 0, 0);
	private static final Location EXIT_LOC = new Location(113883, -108777, -848, 0, 0);
	private static final Location FREYA_LOC = new Location(114730, -114805, -11200, 50, 0);
	// Skill
	private static final SkillHolder ETHERNAL_BLIZZARD = new SkillHolder(6276, 1);
	// Misc
	private static final int TEMPLATE_ID = 137;
	private static final int ICE_QUEEN_DOOR = 23140101;
	private static final int MIN_LV = 82;
	
	private IceQueensCastle()
	{
		addStartNpc(JINIA);
		addTalkId(JINIA);
		addSpawnId(FREYA);
		addSpellFinishedId(FREYA);
		addCreatureSeeId(BATTALION_LEADER, LEGIONNAIRE, MERCENARY_ARCHER);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case ATTACK_KNIGHT:
			{
				World.getInstance().forEachVisibleObject(npc, Creature.class, character ->
				{
					if ((character.getId() == ARCHERY_KNIGHT) && !character.isDead() && !character.asAttackable().isDecayed())
					{
						npc.setRunning();
						npc.getAI().setIntention(Intention.ATTACK, character);
						npc.asAttackable().addDamageHate(character, 0, 999999);
					}
				});
				startQuestTimer(ATTACK_KNIGHT, 3000, npc, null);
				break;
			}
			case "TIMER_MOVING":
			{
				if (npc != null)
				{
					npc.getAI().setIntention(Intention.MOVE_TO, FREYA_LOC);
				}
				break;
			}
			case "TIMER_BLIZZARD":
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.I_CAN_NO_LONGER_STAND_BY);
				npc.stopMove(null);
				npc.setTarget(player);
				npc.doCast(ETHERNAL_BLIZZARD.getSkill());
				break;
			}
			case "TIMER_SCENE_21":
			{
				if (npc != null)
				{
					playMovie(player, Movie.SC_BOSS_FREYA_FORCED_DEFEAT);
					startQuestTimer("TIMER_PC_LEAVE", 24000, null, player);
					npc.deleteMe();
				}
				break;
			}
			case "TIMER_PC_LEAVE":
			{
				final QuestState qs = player.getQuestState(Q10285_MeetingSirra.class.getSimpleName());
				if ((qs != null))
				{
					qs.setMemoState(3);
					qs.setCond(10, true);
					final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
					world.removeAllowed(player);
					player.setInstanceId(0);
					player.teleToLocation(EXIT_LOC, 0);
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
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer() && npc.isScriptValue(0))
		{
			World.getInstance().forEachVisibleObject(npc, Creature.class, character ->
			{
				if ((character.getId() == ARCHERY_KNIGHT) && !character.isDead() && !character.asAttackable().isDecayed())
				{
					npc.setRunning();
					npc.getAI().setIntention(Intention.ATTACK, character);
					npc.asAttackable().addDamageHate(character, 0, 999999);
					npc.setScriptValue(1);
					startQuestTimer(ATTACK_KNIGHT, 5000, npc, null);
				}
			});
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.S1_MAY_THE_PROTECTION_OF_THE_GODS_BE_UPON_YOU, creature.getName());
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("TIMER_MOVING", 60000, npc, null);
		startQuestTimer("TIMER_BLIZZARD", 180000, npc, null);
	}
	
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		final InstanceWorld world = InstanceManager.getInstance().getWorld(npc);
		if (world != null)
		{
			final Player leader = world.getParameters().getObject("player", Player.class);
			if ((skill == ETHERNAL_BLIZZARD.getSkill()) && (leader != null))
			{
				startQuestTimer("TIMER_SCENE_21", 1000, npc, leader);
			}
		}
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		enterInstance(talker, TEMPLATE_ID);
		return super.onTalk(npc, talker);
	}
	
	@Override
	public void onEnterInstance(Player player, InstanceWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player);
			world.setParameter("player", player);
			world.openDoor(ICE_QUEEN_DOOR);
		}
		teleportPlayer(player, START_LOC, world.getInstanceId(), false);
	}
	
	@Override
	protected boolean checkConditions(Player player)
	{
		if (player.getLevel() < MIN_LV)
		{
			player.sendPacket(SystemMessageId.C1_S_LEVEL_DOES_NOT_CORRESPOND_TO_THE_REQUIREMENTS_FOR_ENTRY);
			return false;
		}
		return true;
	}
	
	public static void main(String[] args)
	{
		new IceQueensCastle();
	}
}
