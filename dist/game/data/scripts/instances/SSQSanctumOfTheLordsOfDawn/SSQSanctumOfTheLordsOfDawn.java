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
package instances.SSQSanctumOfTheLordsOfDawn;

import com.l2journey.gameserver.managers.InstanceManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.instancezone.InstanceWorld;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.enums.Movie;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;

import instances.AbstractInstance;
import quests.Q00195_SevenSignsSecretRitualOfThePriests.Q00195_SevenSignsSecretRitualOfThePriests;

/**
 * Sanctum of the Lords of Dawn instance zone.
 * @author Adry_85
 */
public class SSQSanctumOfTheLordsOfDawn extends AbstractInstance
{
	private static final String SAVE_POINT1 = "save_point1";
	private static final String SAVE_POINT2 = "save_point2";
	private static final String SAVE_POINT3 = "save_point3";
	private static final String SAVE_POINT4 = "save_point4";
	private static final String DOORST = "doorst";

	// NPCs
	private static final int GUARDS_OF_THE_DAWN = 18834;
	private static final int GUARDS_OF_THE_DAWN_2 = 18835;
	private static final int GUARDS_OF_THE_DAWN_3 = 27351;
	private static final int LIGHT_OF_DAWN = 32575;
	private static final int PASSWORD_ENTRY_DEVICE = 32577;
	private static final int IDENTITY_CONFIRM_DEVICE = 32578;
	private static final int DARKNESS_OF_DAWN = 32579;
	private static final int SHELF = 32580;
	// Item
	private static final int IDENTITY_CARD = 13822;
	// Skill
	private static final SkillHolder GUARD_SKILL = new SkillHolder(5978, 1);
	// Locations
	private static final Location ENTER = new Location(-76161, 213401, -7120, 0, 0);
	private static final Location EXIT = new Location(-12585, 122305, -2989, 0, 0);
	// Misc
	private static final int TEMPLATE_ID = 111;
	private static int DOOR_ONE = 17240001;
	private static int DOOR_TWO = 17240003;
	private static int DOOR_THREE = 17240005;
	private static final Location[] SAVE_POINT =
	{
		new Location(-75775, 213415, -7120),
		new Location(-74959, 209240, -7472),
		new Location(-77699, 208905, -7640),
		new Location(-79939, 205857, -7888),
	};
	
	private SSQSanctumOfTheLordsOfDawn()
	{
		addStartNpc(LIGHT_OF_DAWN);
		addTalkId(LIGHT_OF_DAWN, IDENTITY_CONFIRM_DEVICE, PASSWORD_ENTRY_DEVICE, DARKNESS_OF_DAWN, SHELF);
		addAggroRangeEnterId(GUARDS_OF_THE_DAWN, GUARDS_OF_THE_DAWN_2, GUARDS_OF_THE_DAWN_3);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "spawn":
			{
				final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(player);
				if (world != null)
				{
					world.spawnGroup("high_priest_of_dawn");
					player.sendPacket(SystemMessageId.BY_USING_THE_SKILL_OF_EINHASAD_S_HOLY_SWORD_DEFEAT_THE_EVIL_LILIMS);
				}
				break;
			}
			case "teleportPlayer":
			{
				switch (npc.getId())
				{
					case GUARDS_OF_THE_DAWN:
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.INTRUDER_PROTECT_THE_PRIESTS_OF_DAWN);
						break;
					}
					case GUARDS_OF_THE_DAWN_2:
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.HOW_DARE_YOU_INTRUDE_WITH_THAT_TRANSFORMATION_GET_LOST);
						break;
					}
					case GUARDS_OF_THE_DAWN_3:
					{
						npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.WHO_ARE_YOU_A_NEW_FACE_LIKE_YOU_CAN_T_APPROACH_THIS_PLACE);
						break;
					}
					default:
					{
						break;
					}
				}
				
				final InstanceWorld world = InstanceManager.getInstance().getWorld(npc);
				if (world != null)
				{
					boolean teleported = false;
					for (Npc monster : world.getParameters().getList(SAVE_POINT1, Npc.class))
					{
						if ((monster != null) && (monster.getObjectId() == npc.getObjectId()))
						{
							teleported = true;
							player.teleToLocation(SAVE_POINT[0], player.getInstanceId(), 0);
							break;
						}
					}
					if (!teleported)
					{
						for (Npc monster : world.getParameters().getList(SAVE_POINT2, Npc.class))
						{
							if ((monster != null) && (monster.getObjectId() == npc.getObjectId()))
							{
								teleported = true;
								player.teleToLocation(SAVE_POINT[1], player.getInstanceId(), 0);
								break;
							}
						}
					}
					if (!teleported)
					{
						for (Npc monster : world.getParameters().getList(SAVE_POINT3, Npc.class))
						{
							if ((monster != null) && (monster.getObjectId() == npc.getObjectId()))
							{
								teleported = true;
								player.teleToLocation(SAVE_POINT[2], player.getInstanceId(), 0);
								break;
							}
						}
					}
					if (!teleported)
					{
						for (Npc monster : world.getParameters().getList(SAVE_POINT4, Npc.class))
						{
							if ((monster != null) && (monster.getObjectId() == npc.getObjectId()))
							{
								player.teleToLocation(SAVE_POINT[3], player.getInstanceId(), 0);
								break;
							}
						}
					}
				}
			}
			default:
			{
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onEnterInstance(Player player, InstanceWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player);
			world.setParameter(SAVE_POINT1, world.spawnGroup(SAVE_POINT1));
			world.setParameter(SAVE_POINT2, world.spawnGroup(SAVE_POINT2));
			world.setParameter(SAVE_POINT3, world.spawnGroup(SAVE_POINT3));
			world.setParameter(SAVE_POINT4, world.spawnGroup(SAVE_POINT4));
		}
		teleportPlayer(player, ENTER, world.getInstanceId());
	}
	
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		switch (npc.getId())
		{
			case LIGHT_OF_DAWN:
			{
				final QuestState qs = talker.getQuestState(Q00195_SevenSignsSecretRitualOfThePriests.class.getSimpleName());
				if ((qs != null) && qs.isCond(3) && hasQuestItems(talker, IDENTITY_CARD) && (talker.getTransformationId() == 113))
				{
					enterInstance(talker, TEMPLATE_ID);
					return "32575-01.html";
				}
				return "32575-02.html";
			}
			case IDENTITY_CONFIRM_DEVICE:
			{
				final InstanceWorld world = InstanceManager.getInstance().getWorld(npc);
				if (world != null)
				{
					if (hasQuestItems(talker, IDENTITY_CARD) && (talker.getTransformationId() == 113))
					{
						final int doorst = world.getParameters().getInt(DOORST, 0);
						if (doorst == 0)
						{
							world.openDoor(DOOR_ONE);
							talker.sendPacket(SystemMessageId.BY_USING_THE_INVISIBLE_SKILL_SNEAK_INTO_THE_DAWN_S_DOCUMENT_STORAGE);
							talker.sendPacket(SystemMessageId.MALE_GUARDS_CAN_DETECT_THE_CONCEALMENT_BUT_THE_FEMALE_GUARDS_CANNOT);
							talker.sendPacket(SystemMessageId.FEMALE_GUARDS_NOTICE_THE_DISGUISES_FROM_FAR_AWAY_BETTER_THAN_THE_MALE_GUARDS_DO_SO_BEWARE);
							world.setParameter(DOORST, doorst + 1);
							npc.decayMe();
						}
						else if (doorst == 1)
						{
							world.openDoor(DOOR_TWO);
							world.setParameter(DOORST, doorst + 1);
							npc.decayMe();
							for (Player plr : world.getAllowed())
							{
								if (plr != null)
								{
									playMovie(plr, Movie.SSQ_RITUAL_OF_PRIEST);
									startQuestTimer("spawn", 35000, null, talker);
								}
							}
						}
						return "32578-01.html";
					}
					return "32578-02.html";
				}
				break;
			}
			case PASSWORD_ENTRY_DEVICE:
			{
				final InstanceWorld world = InstanceManager.getInstance().getWorld(npc);
				if (world != null)
				{
					world.openDoor(DOOR_THREE);
					return "32577-01.html";
				}
				break;
			}
			case DARKNESS_OF_DAWN:
			{
				final InstanceWorld world = InstanceManager.getInstance().getPlayerWorld(talker);
				world.removeAllowed(talker);
				talker.teleToLocation(EXIT, 0);
				return "32579-01.html";
			}
			case SHELF:
			{
				final InstanceWorld world = InstanceManager.getInstance().getWorld(npc);
				InstanceManager.getInstance().getInstance(world.getInstanceId()).setDuration(300000);
				talker.teleToLocation(-75925, 213399, -7128);
				return "32580-01.html";
			}
			default:
			{
				break;
			}
		}
		return "";
	}
	
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		npc.disableCoreAI(true); // disable core AI
		npc.broadcastPacket(new MagicSkillUse(npc, player, GUARD_SKILL.getSkillId(), 1, 2000, 1));
		startQuestTimer("teleportPlayer", 2000, npc, player);
	}
	
	public static void main(String[] args)
	{
		new SSQSanctumOfTheLordsOfDawn();
	}
}
