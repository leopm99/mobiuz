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
package ai.bosses.Orfen;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.Config;
import com.l2journey.commons.time.TimeUtil;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.managers.GrandBossManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.Spawn;
import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Attackable;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.GrandBoss;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.zone.type.BossZone;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.NpcSay;
import com.l2journey.gameserver.network.serverpackets.PlaySound;

import ai.AbstractNpcAI;

/**
 * Orfen's AI
 * @author Emperorc
 */
public class Orfen extends AbstractNpcAI
{
	private static final String CHECK_MINION_LOC = "check_minion_loc";
	private static final String CHECK_ORFEN_POS = "check_orfen_pos";
	private static final String ORFEN_UNLOCK = "orfen_unlock";
	private static final String SPAWN_MINION = "spawn_minion";

	private static final Location[] POS =
	{
		new Location(43728, 17220, -4342),
		new Location(55024, 17368, -5412),
		new Location(53504, 21248, -5486),
		new Location(53248, 24576, -5262)
	};
	
	private static final NpcStringId[] TEXT =
	{
		NpcStringId.S1_STOP_KIDDING_YOURSELF_ABOUT_YOUR_OWN_POWERLESSNESS,
		NpcStringId.S1_I_LL_MAKE_YOU_FEEL_WHAT_TRUE_FEAR_IS,
		NpcStringId.YOU_RE_REALLY_STUPID_TO_HAVE_CHALLENGED_ME_S1_GET_READY,
		NpcStringId.S1_DO_YOU_THINK_THAT_S_GOING_TO_WORK
	};
	
	private static final int ORFEN = 29014;
	private static final int RAIKEL_LEOS = 29016;
	private static final int RIBA_IREN = 29018;
	
	private static boolean _hasTeleported;
	private static final Collection<Attackable> MINIONS = ConcurrentHashMap.newKeySet();
	private static BossZone ZONE;
	
	private static final byte ALIVE = 0;
	private static final byte DEAD = 1;
	
	private Orfen()
	{
		final int[] mobs =
		{
			ORFEN,
			RAIKEL_LEOS,
			RIBA_IREN
		};
		registerMobs(mobs);
		_hasTeleported = false;
		ZONE = GrandBossManager.getInstance().getZone(POS[0]);
		final StatSet info = GrandBossManager.getInstance().getStatSet(ORFEN);
		if (GrandBossManager.getInstance().getStatus(ORFEN) == DEAD)
		{
			// load the unlock date and time for Orfen from DB
			final long temp = info.getLong("respawn_time") - System.currentTimeMillis();
			// if Orfen is locked until a certain time, mark it so and start the unlock timer
			// the unlock time has not yet expired.
			if (temp > 0)
			{
				startQuestTimer(ORFEN_UNLOCK, temp, null, null);
			}
			else
			{
				// the time has already expired while the server was offline. Immediately spawn Orfen.
				final int i = getRandom(10);
				Location loc;
				if (i < 4)
				{
					loc = POS[1];
				}
				else if (i < 7)
				{
					loc = POS[2];
				}
				else
				{
					loc = POS[3];
				}
				final GrandBoss orfen = (GrandBoss) addSpawn(ORFEN, loc, false, 0);
				GrandBossManager.getInstance().setStatus(ORFEN, ALIVE);
				spawnBoss(orfen);
			}
		}
		else
		{
			final int loc_x = info.getInt("loc_x");
			final int loc_y = info.getInt("loc_y");
			final int loc_z = info.getInt("loc_z");
			final int heading = info.getInt("heading");
			final double hp = info.getDouble("currentHP");
			final double mp = info.getDouble("currentMP");
			final GrandBoss orfen = (GrandBoss) addSpawn(ORFEN, loc_x, loc_y, loc_z, heading, false, 0);
			orfen.setCurrentHpMp(hp, mp);
			spawnBoss(orfen);
		}
	}
	
	public void setSpawnPoint(Npc npc, int index)
	{
		npc.asAttackable().clearAggroList();
		npc.getAI().setIntention(Intention.IDLE, null, null);
		final Spawn spawn = npc.getSpawn();
		spawn.setLocation(POS[index]);
		npc.teleToLocation(POS[index], false);
	}
	
	public void spawnBoss(GrandBoss npc)
	{
		GrandBossManager.getInstance().addBoss(npc);
		npc.broadcastPacket(new PlaySound(1, "BS01_A", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		startQuestTimer(CHECK_ORFEN_POS, 10000, npc, null, true);
		// Spawn minions
		final int x = npc.getX();
		final int y = npc.getY();
		Attackable mob;
		mob = addSpawn(RAIKEL_LEOS, x + 100, y + 100, npc.getZ(), 0, false, 0).asAttackable();
		mob.setIsRaidMinion(true);
		MINIONS.add(mob);
		mob = addSpawn(RAIKEL_LEOS, x + 100, y - 100, npc.getZ(), 0, false, 0).asAttackable();
		mob.setIsRaidMinion(true);
		MINIONS.add(mob);
		mob = addSpawn(RAIKEL_LEOS, x - 100, y + 100, npc.getZ(), 0, false, 0).asAttackable();
		mob.setIsRaidMinion(true);
		MINIONS.add(mob);
		mob = addSpawn(RAIKEL_LEOS, x - 100, y - 100, npc.getZ(), 0, false, 0).asAttackable();
		mob.setIsRaidMinion(true);
		MINIONS.add(mob);
		startQuestTimer(CHECK_MINION_LOC, 10000, npc, null, true);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase(ORFEN_UNLOCK))
		{
			final int i = getRandom(10);
			Location loc;
			if (i < 4)
			{
				loc = POS[1];
			}
			else if (i < 7)
			{
				loc = POS[2];
			}
			else
			{
				loc = POS[3];
			}
			final GrandBoss orfen = (GrandBoss) addSpawn(ORFEN, loc, false, 0);
			GrandBossManager.getInstance().setStatus(ORFEN, ALIVE);
			spawnBoss(orfen);
		}
		else if (event.equalsIgnoreCase(CHECK_ORFEN_POS))
		{
			if ((_hasTeleported && (npc.getCurrentHp() > (npc.getMaxHp() * 0.95))) || (!ZONE.isInsideZone(npc) && !_hasTeleported))
			{
				setSpawnPoint(npc, getRandom(3) + 1);
				_hasTeleported = false;
			}
			else if (_hasTeleported && !ZONE.isInsideZone(npc))
			{
				setSpawnPoint(npc, 0);
			}
		}
		else if (event.equalsIgnoreCase(CHECK_MINION_LOC))
		{
			for (Attackable mob : MINIONS)
			{
				if (!npc.isInsideRadius2D(mob, 3000))
				{
					mob.teleToLocation(npc.getLocation());
					npc.asAttackable().clearAggroList();
					npc.getAI().setIntention(Intention.IDLE, null, null);
				}
			}
		}
		else if (event.equalsIgnoreCase("despawn_minions"))
		{
			for (Attackable mob : MINIONS)
			{
				if (mob != null)
				{
					mob.decayMe();
				}
			}
			MINIONS.clear();
		}
		else if (event.equalsIgnoreCase(SPAWN_MINION))
		{
			final Attackable mob = addSpawn(RAIKEL_LEOS, npc.getX(), npc.getY(), npc.getZ(), 0, false, 0).asAttackable();
			mob.setIsRaidMinion(true);
			MINIONS.add(mob);
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, List<WorldObject> targets, boolean isSummon)
	{
		if (npc.isDead())
		{
			return;
		}
		
		if (npc.getId() == ORFEN)
		{
			final Creature originalCaster = isSummon ? caster.getSummon() : caster;
			if ((skill.getEffectPoint() > 0) && (getRandom(5) == 0) && npc.isInsideRadius2D(originalCaster, 1000))
			{
				final NpcSay packet = new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), TEXT[getRandom(4)]);
				packet.addStringParameter(caster.getName());
				npc.broadcastPacket(packet);
				originalCaster.teleToLocation(npc.getLocation());
				npc.setTarget(originalCaster);
				npc.doCast(SkillData.getInstance().getSkill(4064, 1));
			}
		}
	}
	
	@Override
	public void onFactionCall(Npc npc, Npc caller, Player attacker, boolean isSummon)
	{
		if ((caller == null) || (npc == null) || npc.isCastingNow())
		{
			return;
		}
		
		final int npcId = npc.getId();
		final int callerId = caller.getId();
		if ((npcId == RAIKEL_LEOS) && (getRandom(20) == 0))
		{
			npc.setTarget(attacker);
			npc.doCast(SkillData.getInstance().getSkill(4067, 4));
		}
		else if (npcId == RIBA_IREN)
		{
			int chance = 1;
			if (callerId == ORFEN)
			{
				chance = 9;
			}
			if ((callerId != RIBA_IREN) && (caller.getCurrentHp() < (caller.getMaxHp() / 2.0)) && (getRandom(10) < chance))
			{
				npc.getAI().setIntention(Intention.IDLE, null, null);
				npc.setTarget(caller);
				npc.doCast(SkillData.getInstance().getSkill(4516, 1));
			}
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.isDead())
		{
			return;
		}
		
		final int npcId = npc.getId();
		if (npcId == ORFEN)
		{
			if (!_hasTeleported && ((npc.getCurrentHp() - damage) < (npc.getMaxHp() / 2)))
			{
				_hasTeleported = true;
				setSpawnPoint(npc, 0);
			}
			else if (npc.isInsideRadius2D(attacker, 1000) && !npc.isInsideRadius2D(attacker, 300) && (getRandom(10) == 0))
			{
				final NpcSay packet = new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npcId, TEXT[getRandom(3)]);
				packet.addStringParameter(attacker.getName());
				npc.broadcastPacket(packet);
				attacker.teleToLocation(npc.getLocation());
				npc.setTarget(attacker);
				npc.doCast(SkillData.getInstance().getSkill(4064, 1));
			}
		}
		else if ((npcId == RIBA_IREN) && !npc.isCastingNow() && ((npc.getCurrentHp() - damage) < (npc.getMaxHp() / 2.0)))
		{
			npc.setTarget(attacker);
			npc.doCast(SkillData.getInstance().getSkill(4516, 1));
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() == ORFEN)
		{
			npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			GrandBossManager.getInstance().setStatus(ORFEN, DEAD);
			
			final long baseIntervalMillis = Config.ORFEN_SPAWN_INTERVAL * 3600000L;
			final long randomRangeMillis = Config.ORFEN_SPAWN_RANDOM * 3600000L;
			final long respawnTime = baseIntervalMillis + getRandom(-randomRangeMillis, randomRangeMillis);
			
			// Next respawn time.
			final long nextRespawnTime = System.currentTimeMillis() + respawnTime;
			LOGGER.info("Orfen will respawn at: " + TimeUtil.getDateTimeString(nextRespawnTime));
			
			startQuestTimer(ORFEN_UNLOCK, respawnTime, null, null);
			// also save the respawn time so that the info is maintained past reboots
			final StatSet info = GrandBossManager.getInstance().getStatSet(ORFEN);
			info.set("respawn_time", System.currentTimeMillis() + respawnTime);
			GrandBossManager.getInstance().setStatSet(ORFEN, info);
			cancelQuestTimer(CHECK_MINION_LOC, npc, null);
			cancelQuestTimer(CHECK_ORFEN_POS, npc, null);
			startQuestTimer("despawn_minions", 20000, null, null);
			cancelQuestTimers(SPAWN_MINION);
		}
		else if ((GrandBossManager.getInstance().getStatus(ORFEN) == ALIVE) && (npc.getId() == RAIKEL_LEOS))
		{
			MINIONS.remove(npc);
			startQuestTimer(SPAWN_MINION, 360000, npc, null);
		}
	}
	
	public static void main(String[] args)
	{
		new Orfen();
	}
}
