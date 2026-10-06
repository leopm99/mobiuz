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
package ai.bosses.Sailren;

import com.l2journey.gameserver.managers.GlobalVariablesManager;
import com.l2journey.gameserver.managers.ZoneManager;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.TeleportWhereType;
import com.l2journey.gameserver.model.actor.instance.RaidBoss;
import com.l2journey.gameserver.model.groups.Party;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.model.zone.type.NoRestartZone;
import com.l2journey.gameserver.network.serverpackets.SpecialCamera;

import ai.AbstractNpcAI;

/**
 * Sailren AI.
 * @author St3eT
 */
public class Sailren extends AbstractNpcAI
{
	// NPCs
	private static final int STATUE = 32109; // Shilen's Stone Statue
	private static final int MOVIE_NPC = 32110; // Invisible NPC for movie
	private static final int SAILREN = 29065; // Sailren
	private static final int VELOCIRAPTOR = 22218; // Velociraptor
	private static final int PTEROSAUR = 22199; // Pterosaur
	private static final int TREX = 22217; // Tyrannosaurus
	private static final int CUBIC = 32107; // Teleportation Cubic
	// Item
	private static final int GAZKH = 8784; // Gazkh
	// Skill
	private static final SkillHolder ANIMATION = new SkillHolder(5090, 1);
	// Zone
	private static final NoRestartZone zone = ZoneManager.getInstance().getZoneById(70049, NoRestartZone.class);
	// Misc
	private static final int RESPAWN = 1; // Respawn time (in hours)
	private static final int MAX_TIME = 3200; // Max time for Sailren fight (in minutes)
	private static Status STATUS = Status.ALIVE;
	private static int _killCount = 0;
	private static long _lastAttack = 0;
	
	private enum Status
	{
		ALIVE,
		IN_FIGHT,
		DEAD
	}
	
	private Sailren()
	{
		addStartNpc(STATUE, CUBIC);
		addTalkId(STATUE, CUBIC);
		addFirstTalkId(STATUE);
		addKillId(VELOCIRAPTOR, PTEROSAUR, TREX, SAILREN);
		addAttackId(VELOCIRAPTOR, PTEROSAUR, TREX, SAILREN);
		
		final long remain = GlobalVariablesManager.getInstance().getLong("SailrenRespawn", 0) - System.currentTimeMillis();
		if (remain > 0)
		{
			STATUS = Status.DEAD;
			startQuestTimer("CLEAR_STATUS", remain, null, null);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "32109-01.html", "32109-01a.html", "32109-02a.html", "32109-03a.html":
			{
				return event;
			}
			case "enter":
			{
				String htmltext = null;
				final Party party = player.getParty();
				if (party == null)
				{
					htmltext = "32109-01.html";
				}
				else if (STATUS == Status.DEAD)
				{
					htmltext = "32109-04.html";
				}
				else if (STATUS == Status.IN_FIGHT)
				{
					htmltext = "32109-05.html";
				}
				else if (!party.isLeader(player))
				{
					htmltext = "32109-03.html";
				}
				else if (!hasQuestItems(player, GAZKH))
				{
					htmltext = "32109-02.html";
				}
				else
				{
					takeItems(player, GAZKH, 1);
					STATUS = Status.IN_FIGHT;
					_lastAttack = System.currentTimeMillis();
					for (Player member : party.getMembers())
					{
						if (member.isInsideRadius3D(npc, 1000))
						{
							member.teleToLocation(27549, -6638, -2008);
						}
					}
					startQuestTimer("SPAWN_VELOCIRAPTOR", 60000, null, null);
					startQuestTimer("TIME_OUT", MAX_TIME * 1000, null, null);
					startQuestTimer("CHECK_ATTACK", 120000, null, null);
				}
				return htmltext;
			}
			case "teleportOut":
			{
				player.teleToLocation(TeleportWhereType.TOWN);
				break;
			}
			case "SPAWN_VELOCIRAPTOR":
			{
				for (int i = 0; i < 3; i++)
				{
					addSpawn(VELOCIRAPTOR, 27313 + getRandom(150), -6766 + getRandom(150), -1975, 0, false, 0);
				}
				break;
			}
			case "SPAWN_SAILREN":
			{
				final RaidBoss sailren = (RaidBoss) addSpawn(SAILREN, 27549, -6638, -2008, 0, false, 0);
				final Npc movieNpc = addSpawn(MOVIE_NPC, sailren.getX(), sailren.getY(), sailren.getZ() + 30, 0, false, 26000);
				sailren.setInvul(true);
				sailren.setImmobilized(true);
				zone.broadcastPacket(new SpecialCamera(movieNpc, 60, 110, 30, 4000, 1500, 20000, 0, 65, 1, 0, 0));
				startQuestTimer("ATTACK", 24600, sailren, null);
				startQuestTimer("ANIMATION", 2000, movieNpc, null);
				startQuestTimer("CAMERA_1", 4100, movieNpc, null);
				break;
			}
			case "ANIMATION":
			{
				if (npc != null)
				{
					npc.setTarget(npc);
					npc.doCast(ANIMATION.getSkill());
					startQuestTimer("ANIMATION", 2000, npc, null);
				}
				break;
			}
			case "CAMERA_1":
			{
				zone.broadcastPacket(new SpecialCamera(npc, 100, 180, 30, 3000, 1500, 20000, 0, 50, 1, 0, 0));
				startQuestTimer("CAMERA_2", 3000, npc, null);
				break;
			}
			case "CAMERA_2":
			{
				zone.broadcastPacket(new SpecialCamera(npc, 150, 270, 25, 3000, 1500, 20000, 0, 30, 1, 0, 0));
				startQuestTimer("CAMERA_3", 3000, npc, null);
				break;
			}
			case "CAMERA_3":
			{
				zone.broadcastPacket(new SpecialCamera(npc, 160, 360, 20, 3000, 1500, 20000, 10, 15, 1, 0, 0));
				startQuestTimer("CAMERA_4", 3000, npc, null);
				break;
			}
			case "CAMERA_4":
			{
				zone.broadcastPacket(new SpecialCamera(npc, 160, 450, 10, 3000, 1500, 20000, 0, 10, 1, 0, 0));
				startQuestTimer("CAMERA_5", 3000, npc, null);
				break;
			}
			case "CAMERA_5":
			{
				zone.broadcastPacket(new SpecialCamera(npc, 160, 560, 0, 3000, 1500, 20000, 0, 10, 1, 0, 0));
				startQuestTimer("CAMERA_6", 7000, npc, null);
				break;
			}
			case "CAMERA_6":
			{
				zone.broadcastPacket(new SpecialCamera(npc, 70, 560, 0, 500, 1500, 7000, -15, 20, 1, 0, 0));
				break;
			}
			case "ATTACK":
			{
				npc.setInvul(false);
				npc.setImmobilized(false);
				break;
			}
			case "CLEAR_STATUS":
			{
				STATUS = Status.ALIVE;
				break;
			}
			case "TIME_OUT":
			{
				if (STATUS == Status.IN_FIGHT)
				{
					STATUS = Status.ALIVE;
				}
				for (Creature creature : zone.getCharactersInside())
				{
					if (creature != null)
					{
						if (creature.isPlayer())
						{
							creature.teleToLocation(TeleportWhereType.TOWN);
						}
						else if (creature.isNpc())
						{
							creature.deleteMe();
						}
					}
				}
				break;
			}
			case "CHECK_ATTACK":
			{
				if (!zone.getPlayersInside().isEmpty() && ((_lastAttack + 600000) < System.currentTimeMillis()))
				{
					cancelQuestTimer("TIME_OUT", null, null);
					notifyEvent("TIME_OUT", null, null);
				}
				else
				{
					startQuestTimer("CHECK_ATTACK", 120000, null, null);
				}
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (zone.isCharacterInZone(attacker))
		{
			_lastAttack = System.currentTimeMillis();
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (zone.isCharacterInZone(killer))
		{
			switch (npc.getId())
			{
				case SAILREN:
				{
					STATUS = Status.DEAD;
					addSpawn(CUBIC, 27644, -6638, -2008, 0, false, 300000);
					final long respawnTime = RESPAWN * 3600000L;
					GlobalVariablesManager.getInstance().set("SailrenRespawn", System.currentTimeMillis() + respawnTime);
					cancelQuestTimer("CHECK_ATTACK", null, null);
					cancelQuestTimer("TIME_OUT", null, null);
					startQuestTimer("CLEAR_STATUS", respawnTime, null, null);
					startQuestTimer("TIME_OUT", 300000, null, null);
					break;
				}
				case VELOCIRAPTOR:
				{
					_killCount++;
					if (_killCount == 3)
					{
						final Npc pterosaur = addSpawn(PTEROSAUR, 27313, -6766, -1975, 0, false, 0);
						addAttackDesire(pterosaur, killer);
						_killCount = 0;
					}
					break;
				}
				case PTEROSAUR:
				{
					final Npc trex = addSpawn(TREX, 27313, -6766, -1975, 0, false, 0);
					addAttackDesire(trex, killer);
					break;
				}
				case TREX:
				{
					startQuestTimer("SPAWN_SAILREN", 180000, null, null);
					break;
				}
			}
		}
	}
	
	@Override
	public void unload(boolean removeFromList)
	{
		if (STATUS == Status.IN_FIGHT)
		{
			LOGGER.info(getClass().getSimpleName() + ": Script is being unloaded while Sailren is active, clearing zone.");
			notifyEvent("TIME_OUT", null, null);
		}
		super.unload(removeFromList);
	}
	
	public static void main(String[] args)
	{
		new Sailren();
	}
}
