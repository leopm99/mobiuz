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
package ai.bosses.Core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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
import com.l2journey.gameserver.managers.GlobalVariablesManager;
import com.l2journey.gameserver.managers.GrandBossManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.actor.Attackable;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.GrandBoss;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.NpcSay;
import com.l2journey.gameserver.network.serverpackets.PlaySound;

import ai.AbstractNpcAI;

/**
 * Core AI.
 * @author DrLecter, Emperorc, Mobius, KingHanker
 */
public class Core extends AbstractNpcAI implements IXmlReader
{
	private static final String SPAWN_MINION = "spawn_minion";
	private static final String CORE_UNLOCK = "core_unlock";

	private static final Logger LOGGER = Logger.getLogger(Core.class.getName());
	
	// NPCs
	private static final int CORE = 29006;
	// Minion data holder
	private static final List<MinionSpawn> MINION_SPAWNS = new ArrayList<>();
	private static final Set<Integer> MINION_IDS = new HashSet<>();
	// Misc
	private static final byte ALIVE = 0;
	private static final byte DEAD = 1;
	private static final Collection<Attackable> _minions = ConcurrentHashMap.newKeySet();
	private static boolean _firstAttacked;
	
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
	
	private Core()
	{
		// Load minions from XML first
		load();
		
		// Register Core and all minion IDs
		final int[] mobs = new int[MINION_IDS.size() + 1];
		mobs[0] = CORE;
		int i = 1;
		for (int minionId : MINION_IDS)
		{
			mobs[i++] = minionId;
		}
		registerMobs(mobs);
		
		_firstAttacked = false;
		final StatSet info = GrandBossManager.getInstance().getStatSet(CORE);
		if (GrandBossManager.getInstance().getStatus(CORE) == DEAD)
		{
			// Load the unlock date and time for Core from DB.
			final long temp = info.getLong("respawn_time") - System.currentTimeMillis();
			// If Core is locked until a certain time, mark it so and start the unlock timer the unlock time has not yet expired.
			if (temp > 0)
			{
				startQuestTimer(CORE_UNLOCK, temp, null, null);
			}
			else
			{
				// The time has already expired while the server was offline. Immediately spawn Core.
				final GrandBoss core = (GrandBoss) addSpawn(CORE, 17726, 108915, -6480, 0, false, 0);
				GrandBossManager.getInstance().setStatus(CORE, ALIVE);
				spawnBoss(core);
			}
		}
		else
		{
			final boolean test = GlobalVariablesManager.getInstance().getBoolean("Core_Attacked", false);
			if (test)
			{
				_firstAttacked = true;
			}
			final int loc_x = info.getInt("loc_x");
			final int loc_y = info.getInt("loc_y");
			final int loc_z = info.getInt("loc_z");
			final int heading = info.getInt("heading");
			final double hp = info.getDouble("currentHP");
			final double mp = info.getDouble("currentMP");
			final GrandBoss core = (GrandBoss) addSpawn(CORE, loc_x, loc_y, loc_z, heading, false, 0);
			core.setCurrentHpMp(hp, mp);
			spawnBoss(core);
		}
	}
	
	@Override
	public void onSave()
	{
		GlobalVariablesManager.getInstance().set("Core_Attacked", _firstAttacked);
	}
	
	@Override
	public void load()
	{
		MINION_SPAWNS.clear();
		MINION_IDS.clear();
		parseDatapackFile("data/scripts/ai/bosses/Core/Core.xml");
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
						MINION_IDS.add(npcId);
					}
				}
			}
		}
	}
	
	public void spawnBoss(GrandBoss npc)
	{
		GrandBossManager.getInstance().addBoss(npc);
		npc.broadcastPacket(new PlaySound(1, "BS01_A", 1, npc.getObjectId(), npc.getX(), npc.getY(), npc.getZ()));
		// Spawn minions from XML data
		for (MinionSpawn spawn : MINION_SPAWNS)
		{
			final Location loc = spawn.getLocation();
			final Attackable mob = addSpawn(spawn.getNpcId(), loc.getX(), loc.getY(), loc.getZ(), getRandom(61794), false, 0).asAttackable();
			mob.setIsRaidMinion(true);
			_minions.add(mob);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase(CORE_UNLOCK))
		{
			final GrandBoss core = (GrandBoss) addSpawn(CORE, 17726, 108915, -6480, 0, false, 0);
			GrandBossManager.getInstance().setStatus(CORE, ALIVE);
			spawnBoss(core);
		}
		else if (event.equalsIgnoreCase(SPAWN_MINION))
		{
			final Attackable mob = addSpawn(npc.getId(), npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 0).asAttackable();
			mob.setIsRaidMinion(true);
			_minions.add(mob);
		}
		else if (event.equalsIgnoreCase("despawn_minions"))
		{
			_minions.forEach(Attackable::decayMe);
			_minions.clear();
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.getId() == CORE)
		{
			if (_firstAttacked)
			{
				if (getRandom(100) == 0)
				{
					npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), NpcStringId.REMOVING_INTRUDERS));
				}
			}
			else
			{
				_firstAttacked = true;
				npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), NpcStringId.A_NON_PERMITTED_TARGET_HAS_BEEN_DISCOVERED));
				npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), NpcStringId.INTRUDER_REMOVAL_SYSTEM_INITIATED));
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() == CORE)
		{
			final int objId = npc.getObjectId();
			npc.broadcastPacket(new PlaySound(1, "BS02_D", 1, objId, npc.getX(), npc.getY(), npc.getZ()));
			npc.broadcastPacket(new NpcSay(objId, ChatType.NPC_GENERAL, npc.getId(), NpcStringId.A_FATAL_ERROR_HAS_OCCURRED));
			npc.broadcastPacket(new NpcSay(objId, ChatType.NPC_GENERAL, npc.getId(), NpcStringId.SYSTEM_IS_BEING_SHUT_DOWN));
			npc.broadcastPacket(new NpcSay(objId, ChatType.NPC_GENERAL, npc.getId(), NpcStringId.EMPTY));
			_firstAttacked = false;
			GrandBossManager.getInstance().setStatus(CORE, DEAD);
			
			final long baseIntervalMillis = Config.CORE_SPAWN_INTERVAL * 3600000L;
			final long randomRangeMillis = Config.CORE_SPAWN_RANDOM * 3600000L;
			final long respawnTime = baseIntervalMillis + getRandom(-randomRangeMillis, randomRangeMillis);
			
			// Next respawn time.
			final long nextRespawnTime = System.currentTimeMillis() + respawnTime;
			LOGGER.info(() -> "Core will respawn at: " + TimeUtil.getDateTimeString(nextRespawnTime));
			
			startQuestTimer(CORE_UNLOCK, respawnTime, null, null);
			// Also save the respawn time so that the info is maintained past reboots.
			final StatSet info = GrandBossManager.getInstance().getStatSet(CORE);
			info.set("respawn_time", System.currentTimeMillis() + respawnTime);
			GrandBossManager.getInstance().setStatSet(CORE, info);
			// Despawn minions after 10 seconds
			startQuestTimer("despawn_minions", 10000, null, null);
			cancelQuestTimers(SPAWN_MINION);
		}
		else if ((GrandBossManager.getInstance().getStatus(CORE) == ALIVE) && _minions.contains(npc))
		{
			_minions.remove(npc);
			// Find respawn time from XML data
			int respawnTime = 60000; // default 60 seconds
			for (MinionSpawn spawn : MINION_SPAWNS)
			{
				if (spawn.getNpcId() == npc.getId())
				{
					respawnTime = spawn.getRespawnTime() * 1000;
					break;
				}
			}
			startQuestTimer(SPAWN_MINION, respawnTime, npc, null);
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == CORE)
		{
			npc.setImmobilized(true);
		}
	}
	
	public static void main(String[] args)
	{
		new Core();
	}
}
