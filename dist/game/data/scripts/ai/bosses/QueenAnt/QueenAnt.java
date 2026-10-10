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
package ai.bosses.QueenAnt;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2journey.Config;
import com.l2journey.commons.time.TimeUtil;
import com.l2journey.commons.util.IXmlReader;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.managers.GrandBossManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Playable;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.GrandBoss;
import com.l2journey.gameserver.model.actor.instance.Monster;
import com.l2journey.gameserver.model.skill.CommonSkill;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.model.zone.type.BossZone;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;
import com.l2journey.gameserver.network.serverpackets.NpcSay;
import com.l2journey.gameserver.network.serverpackets.PlaySound;

import ai.AbstractNpcAI;

/**
 * Queen Ant's AI
 * @author KingHanker, L2Journey Team
 */
public class QueenAnt extends AbstractNpcAI implements IXmlReader
{
	// Explicit LOGGER to resolve ambiguity between Quest.LOGGER and IXmlReader.LOGGER.
	private static final Logger LOGGER = Logger.getLogger(QueenAnt.class.getName());
	
	private static final int QUEEN = 29001;
	private static final int LARVA = 29002;
	private static final int NURSE = 29003;
	private static final int GUARD = 29004;
	private static final int ROYAL = 29005;
	
	private static final int[] MOBS =
	{
		QUEEN,
		LARVA,
		NURSE,
		GUARD,
		ROYAL
	};
	
	private static final Location OUST_LOC_1 = new Location(-19480, 187344, -5600);
	private static final Location OUST_LOC_2 = new Location(-17928, 180912, -5520);
	private static final Location OUST_LOC_3 = new Location(-23808, 182368, -5600);
	
	private static final int QUEEN_X = -21610;
	private static final int QUEEN_Y = 181594;
	private static final int QUEEN_Z = -5734;
	
	// QUEEN Status Tracking :
	private static final byte ALIVE = 0; // Queen Ant is spawned.
	private static final byte DEAD = 1; // Queen Ant has been killed.
	
	private static BossZone _zone;
	
	private static final SkillHolder HEAL1 = new SkillHolder(4020, 1);
	private static final SkillHolder HEAL2 = new SkillHolder(4024, 1);
	
	// Minion spawn data holder, loaded from XML.
	private static final List<MinionSpawn> MINION_SPAWNS = new ArrayList<>();
	
	private static final String[] FIRST_HIT_TEXTS =
	{
		"Who's daring to disturb my sleep?!",
		"You, foolish mortals... how dare you enter my domain!"
	};
	
	private static final String HP_80_TEXT = "Guards! Attack those who threaten the nest!";
	private static final String HP_20_TEXT = "Servants, eliminate the intruders!";
	private static final String LARVA_FIRST_HIT_TEXT = "Protect the Larva! Heal me, my children!";
	
	private static final String[] DEATH_TEXTS =
	{
		"My children... save... yourselves...",
		"The nest... has fallen..."
	};
	
	Monster _queen = null;
	private Monster _larva = null;
	private final Set<Monster> _nurses = ConcurrentHashMap.newKeySet();
	private final Set<Monster> _minions = ConcurrentHashMap.newKeySet();
	private boolean _firstAttacked = false;
	private boolean _hp80Announced = false;
	private boolean _hp20Announced = false;
	private boolean _larvaFirstAttacked = false;
	
	/**
	 * Holds minion spawn data loaded from XML.
	 */
	private static class MinionSpawn
	{
		private final int _npcId;
		private final Location _location;
		private final int _respawnTime;
		
		public MinionSpawn(int npcId, int x, int y, int z, int respawnTime)
		{
			_npcId = npcId;
			_location = new Location(x, y, z);
			_respawnTime = respawnTime;
		}
		
		public int getNpcId()
		{
			return _npcId;
		}
		
		public Location getLocation()
		{
			return _location;
		}
		
		public int getRespawnTime()
		{
			return _respawnTime;
		}
	}
	
	private QueenAnt()
	{
		load();
		
		addSpawnId(MOBS);
		addAttackId(QUEEN, LARVA);
		addKillId(MOBS);
		addAggroRangeEnterId(MOBS);
		addFactionCallId(NURSE);
		
		_zone = GrandBossManager.getInstance().getZone(QUEEN_X, QUEEN_Y, QUEEN_Z);
		final StatSet info = GrandBossManager.getInstance().getStatSet(QUEEN);
		if (GrandBossManager.getInstance().getStatus(QUEEN) == DEAD)
		{
			// load the unlock date and time for queen ant from DB
			final long temp = info.getLong("respawn_time") - System.currentTimeMillis();
			// if queen ant is locked until a certain time, mark it so and start the unlock timer
			// the unlock time has not yet expired.
			if (temp > 0)
			{
				startQuestTimer("queen_unlock", temp, null, null);
			}
			else
			{
				// the time has already expired while the server was offline. Immediately spawn queen ant.
				final GrandBoss queen = (GrandBoss) addSpawn(QUEEN, QUEEN_X, QUEEN_Y, QUEEN_Z, 0, false, 0);
				GrandBossManager.getInstance().setStatus(QUEEN, ALIVE);
				spawnBoss(queen);
			}
		}
		else
		{
			final int locX = QUEEN_X;
			final int locY = QUEEN_Y;
			final int locZ = QUEEN_Z;
			final int heading = info.getInt("heading");
			final double hp = info.getDouble("currentHP");
			final double mp = info.getDouble("currentMP");
			final GrandBoss queen = (GrandBoss) addSpawn(QUEEN, locX, locY, locZ, heading, false, 0);
			queen.setCurrentHpMp(hp, mp);
			spawnBoss(queen);
		}
	}
	
	@Override
	public void load()
	{
		MINION_SPAWNS.clear();
		parseDatapackFile("data/scripts/ai/bosses/QueenAnt/QueenAnt.xml");
		LOGGER.info(() -> getClass().getSimpleName() + ": Loaded " + MINION_SPAWNS.size() + " minion spawns.");
	}
	
	@Override
	public void parseDocument(Document doc, File f)
	{
		for (Node node = doc.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if ("list".equalsIgnoreCase(node.getNodeName()))
			{
				for (Node minionNode = node.getFirstChild(); minionNode != null; minionNode = minionNode.getNextSibling())
				{
					if ("minion".equalsIgnoreCase(minionNode.getNodeName()))
					{
						final NamedNodeMap attrs = minionNode.getAttributes();
						final int npcId = parseInteger(attrs, "npcId");
						final int x = parseInteger(attrs, "x");
						final int y = parseInteger(attrs, "y");
						final int z = parseInteger(attrs, "z");
						final int respawnTime = parseInteger(attrs, "respawnTime", 60);
						
						MINION_SPAWNS.add(new MinionSpawn(npcId, x, y, z, respawnTime));
					}
				}
			}
		}
	}
	
	private void spawnBoss(GrandBoss npc)
	{
		GrandBossManager.getInstance().addBoss(npc);
		if (getRandom(100) < 33)
		{
			_zone.movePlayersTo(OUST_LOC_1);
		}
		else if (getRandom(100) < 50)
		{
			_zone.movePlayersTo(OUST_LOC_2);
		}
		else
		{
			_zone.movePlayersTo(OUST_LOC_3);
		}
		GrandBossManager.getInstance().addBoss(npc);
		startQuestTimer("action", 10000, npc, null, true);
		startQuestTimer("heal", 1000, null, null, true);
		npc.broadcastPacket(new PlaySound(1, "BS01_A", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		_queen = npc;
		// Spawn minions from XML data
		for (MinionSpawn spawn : MINION_SPAWNS)
		{
			final Location loc = spawn.getLocation();
			final Monster mob = addSpawn(spawn.getNpcId(), loc.getX(), loc.getY(), loc.getZ(), getRandom(61794), false, 0).asMonster();
			if (spawn.getNpcId() == LARVA)
			{
				_larva = mob;
			}
			else
			{
				_minions.add(mob);
			}
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "heal":
			{
				boolean notCasting;
				final boolean larvaNeedHeal = (_larva != null) && (_larva.getCurrentHp() < _larva.getMaxHp());
				final boolean queenNeedHeal = (_queen != null) && (_queen.getCurrentHp() < _queen.getMaxHp());
				for (Monster nurse : _nurses)
				{
					if ((nurse == null) || nurse.isDead() || nurse.isCastingNow())
					{
						continue;
					}
					
					notCasting = nurse.getAI().getIntention() != Intention.CAST;
					if (larvaNeedHeal)
					{
						if ((nurse.getTarget() != _larva) || notCasting)
						{
							nurse.setTarget(_larva);
							nurse.useMagic(getRandomBoolean() ? HEAL1.getSkill() : HEAL2.getSkill());
						}
						continue;
					}
					if (queenNeedHeal)
					{
						if (nurse.getLeader() == _larva)
						{
							continue;
						}
						
						if ((nurse.getTarget() != _queen) || notCasting)
						{
							nurse.setTarget(_queen);
							nurse.useMagic(HEAL1.getSkill());
						}
						continue;
					}
					// if nurse not casting - remove target
					if (notCasting && (nurse.getTarget() != null))
					{
						nurse.setTarget(null);
					}
				}
				break;
			}
			case "action":
			{
				if ((npc != null) && (getRandom(3) == 0))
				{
					if (getRandom(2) == 0)
					{
						npc.broadcastSocialAction(3);
					}
					else
					{
						npc.broadcastSocialAction(4);
					}
				}
				break;
			}
			case "queen_unlock":
			{
				final GrandBoss queen = (GrandBoss) addSpawn(QUEEN, QUEEN_X, QUEEN_Y, QUEEN_Z, 0, false, 0);
				GrandBossManager.getInstance().setStatus(QUEEN, ALIVE);
				spawnBoss(queen);
				break;
			}
			case "spawn_minion":
			{
				final Monster mob = addSpawn(npc.getId(), npc.getX(), npc.getY(), npc.getZ(), getRandom(61794), false, 0).asMonster();
				_minions.add(mob);
				break;
			}
			case "DISTANCE_CHECK":
			{
				if ((_queen == null) || _queen.isDead())
				{
					cancelQuestTimers("DISTANCE_CHECK");
					break;
				}
				else if (_queen.calculateDistance2D(QUEEN_X, QUEEN_Y, QUEEN_Z) > 2000)
				{
					_queen.clearAggroList();
					_queen.getAI().setIntention(Intention.MOVE_TO, new Location(QUEEN_X, QUEEN_Y, QUEEN_Z, 0));
				}
				
				// No one currently fighting the Queen means the challenge ended, so re-arm the first-hit announcement.
				if (_queen.getMostHated() == null)
				{
					_firstAttacked = false;
				}
				
				// Keep HP-stage announcements in sync with the actual HP, regardless of combat state.
				syncHpAnnouncements();
				
				// Same for the Larva, which can be challenged independently of the Queen.
				if ((_larva != null) && (_larva.getMostHated() == null))
				{
					_larvaFirstAttacked = false;
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
	
	/**
	 * Keeps _hp80Announced/_hp20Announced consistent with the Queen's actual HP, so a stage already
	 * passed doesn't re-fire and a stage not yet reached doesn't get skipped when combat resumes.
	 */
	private void syncHpAnnouncements()
	{
		if ((_queen == null) || _queen.isDead())
		{
			return;
		}
		
		final double currentHpPercent = (_queen.getCurrentHp() * 100.0) / _queen.getMaxHp();
		if (currentHpPercent > 80.0)
		{
			_hp80Announced = false;
			_hp20Announced = false;
		}
		else if (currentHpPercent > 20.0)
		{
			_hp80Announced = true;
			_hp20Announced = false;
		}
		else
		{
			_hp80Announced = true;
			_hp20Announced = true;
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		final Monster mob = npc.asMonster();
		switch (npc.getId())
		{
			case LARVA:
			{
				mob.setImmobilized(true);
				mob.setMortal(false);
				mob.setIsRaidMinion(true);
				break;
			}
			case NURSE:
			{
				mob.disableCoreAI(true);
				mob.setIsRaidMinion(true);
				_nurses.add(mob);
				break;
			}
			case ROYAL, GUARD:
			{
				mob.setIsRaidMinion(true);
				break;
			}
			case QUEEN:
			{
				cancelQuestTimer("DISTANCE_CHECK", npc, null);
				startQuestTimer("DISTANCE_CHECK", 5000, npc, null, true);
				break;
			}
			default:
			{
				break;
			}
		}
	}
	
	@Override
	public void onFactionCall(Npc npc, Npc caller, Player attacker, boolean isSummon)
	{
		if ((caller == null) || (npc == null))
		{
			return;
		}
		
		if (!npc.isCastingNow() && (npc.getAI().getIntention() != Intention.CAST) && (caller.getCurrentHp() < caller.getMaxHp()))
		{
			npc.setTarget(caller);
			npc.asAttackable().useMagic(HEAL1.getSkill());
		}
	}
	
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if ((npc == null) || (player.isGM() && player.isInvisible()))
		{
			return;
		}
		
		final boolean isMage;
		final Playable character;
		if (isSummon)
		{
			isMage = false;
			character = player.getSummon();
		}
		else
		{
			isMage = player.isMageClass();
			character = player;
		}
		
		if (character == null)
		{
			return;
		}
		
		if (!Config.RAID_DISABLE_CURSE && ((character.getLevel() - npc.getLevel()) > 8))
		{
			Skill curse = null;
			if (isMage)
			{
				if (!character.isMuted() && (getRandom(4) == 0))
				{
					curse = CommonSkill.RAID_CURSE.getSkill();
				}
			}
			else if (!character.isParalyzed() && (getRandom(4) == 0))
			{
				curse = CommonSkill.RAID_CURSE2.getSkill();
			}
			
			if (curse != null)
			{
				npc.broadcastPacket(new MagicSkillUse(npc, character, curse.getId(), curse.getLevel(), 300, 0));
				curse.applyEffects(npc, character);
			}
			
			npc.asAttackable().stopHating(character); // for calling again
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.getId() == LARVA)
		{
			if (!_larvaFirstAttacked)
			{
				_larvaFirstAttacked = true;
				// The Queen calls out to protect the Larva, not the Larva itself.
				if (_queen != null)
				{
					_queen.broadcastPacket(new NpcSay(_queen.getObjectId(), ChatType.NPC_GENERAL, _queen.getId(), LARVA_FIRST_HIT_TEXT));
				}
			}
			return;
		}
		
		if (npc.getId() != QUEEN)
		{
			return;
		}
		
		if (!_firstAttacked)
		{
			_firstAttacked = true;
			npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), FIRST_HIT_TEXTS[getRandom(FIRST_HIT_TEXTS.length)]));
		}
		
		final double hpPercent = ((npc.getCurrentHp() - damage) * 100) / npc.getMaxHp();
		// Chained stages: a hit that skips straight past 20% must not fire both texts at once.
		if (!_hp20Announced && (hpPercent <= 20))
		{
			_hp80Announced = true;
			_hp20Announced = true;
			npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), HP_20_TEXT));
		}
		else if (!_hp80Announced && (hpPercent <= 80))
		{
			_hp80Announced = true;
			npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), HP_80_TEXT));
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final int npcId = npc.getId();
		if (npcId == QUEEN)
		{
			npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
			npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), DEATH_TEXTS[getRandom(DEATH_TEXTS.length)]));
			GrandBossManager.getInstance().setStatus(QUEEN, DEAD);
			_firstAttacked = false;
			_hp80Announced = false;
			_hp20Announced = false;
			_larvaFirstAttacked = false;
			
			final long baseIntervalMillis = Config.QUEEN_ANT_SPAWN_INTERVAL * 3600000L;
			final long randomRangeMillis = Config.QUEEN_ANT_SPAWN_RANDOM * 3600000L;
			final long respawnTime = baseIntervalMillis + getRandom(-randomRangeMillis, randomRangeMillis);
			
			// Next respawn time.
			final long nextRespawnTime = System.currentTimeMillis() + respawnTime;
			LOGGER.info(() -> "Queen Ant will respawn at: " + TimeUtil.getDateTimeString(nextRespawnTime));
			
			startQuestTimer("queen_unlock", respawnTime, null, null);
			cancelQuestTimer("action", npc, null);
			cancelQuestTimer("heal", null, null);
			// also save the respawn time so that the info is maintained past reboots
			final StatSet info = GrandBossManager.getInstance().getStatSet(QUEEN);
			info.set("respawn_time", System.currentTimeMillis() + respawnTime);
			GrandBossManager.getInstance().setStatSet(QUEEN, info);
			_nurses.clear();
			if (_larva != null)
			{
				_larva.deleteMe();
			}
			_larva = null;
			_queen = null;
			cancelQuestTimers("DISTANCE_CHECK");
			cancelQuestTimers("spawn_minion");
			for (Monster minion : _minions)
			{
				if (minion != null)
				{
					minion.deleteMe();
				}
			}
			_minions.clear();
		}
		else if (((_queen != null) && !_queen.isAlikeDead()) && ((npcId == ROYAL) || (npcId == NURSE)))
		{
			final Monster mob = npc.asMonster();
			_minions.remove(mob);
			if (npcId == NURSE)
			{
				_nurses.remove(mob);
			}

			// Same respawn timing as before: base time from XML, Royal keeps its 0-40s random jitter.
			int baseRespawnSeconds = 60;
			for (MinionSpawn spawn : MINION_SPAWNS)
			{
				if (spawn.getNpcId() == npcId)
				{
					baseRespawnSeconds = spawn.getRespawnTime();
					break;
				}
			}
			final long minionRespawnTime = (npcId == ROYAL) ? (baseRespawnSeconds + getRandom(40)) * 1000L : baseRespawnSeconds * 1000L;
			startQuestTimer("spawn_minion", minionRespawnTime, npc, null);
		}
	}
	
	public static void main(String[] args)
	{
		new QueenAnt();
	}
}
