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
package ai.areas.DenOfEvil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.util.IXmlReader;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.managers.ZoneManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.zone.type.EffectZone;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;
import com.l2journey.gameserver.util.ArrayUtil;

import ai.AbstractNpcAI;

/**
 * Den of Evil area AI.<br>
 * Manages Kasha's Eye spawns and the zone destruction mechanic.<br>
 * When 4 Eyes of the same type accumulate in a zone, it triggers a Kasha Destruction after 10 seconds.
 * @author Gnacik, KingHanker
 */
public class DenOfEvil extends AbstractNpcAI implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(DenOfEvil.class.getName());
	
	protected static final int[] EYE_IDS =
	{
		18812,
		18813,
		18814
	};
	private static final int SKILL_ID = 6150; // others +2
	private static final int KASHA_DESTRUCTION_SKILL_ID = 6149;
	private static final int KASHA_DESTRUCTION_SKILL_LEVEL = 1;
	private static final int DESTRUCTION_DELAY = 5000; // 5 seconds
	private static final int RESPAWN_DELAY = 15000; // 15 seconds
	private static final int MAX_SKILL_LEVEL = 4;
	
	private final List<Location> _eyeSpawns = new ArrayList<>();
	
	private DenOfEvil()
	{
		addKillId(EYE_IDS);
		addSpawnId(EYE_IDS);
		load();
	}
	
	@Override
	public void load()
	{
		_eyeSpawns.clear();
		parseDatapackFile("data/scripts/ai/areas/DenOfEvil/DenOfEvil.xml");
		LOGGER.info(() -> "[Den of Evil] Loaded " + _eyeSpawns.size() + " eye spawn locations.");
		for (Location loc : _eyeSpawns)
		{
			addSpawn(getRandomEntry(EYE_IDS), loc, false, 0);
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node node = document.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if ("list".equals(node.getNodeName()))
			{
				for (Node minion = node.getFirstChild(); minion != null; minion = minion.getNextSibling())
				{
					if ("minion".equals(minion.getNodeName()))
					{
						final NamedNodeMap attrs = minion.getAttributes();
						final int x = parseInteger(attrs, "x");
						final int y = parseInteger(attrs, "y");
						final int z = parseInteger(attrs, "z");
						final int heading = parseInteger(attrs, "heading", 0);
						_eyeSpawns.add(new Location(x, y, z, heading));
					}
				}
			}
		}
	}
	
	private int getSkillIdByNpcId(int npcId)
	{
		int diff = npcId - EYE_IDS[0];
		diff *= 2;
		return SKILL_ID + diff;
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		npc.disableCoreAI(true);
		npc.setImmobilized(true);
		final EffectZone zone = ZoneManager.getInstance().getZone(npc, EffectZone.class);
		if (zone == null)
		{
			LOGGER.warning(() -> "NPC " + npc + " spawned outside of EffectZone, check your zone coords! X:" + npc.getX() + " Y:" + npc.getY() + " Z:" + npc.getZ());
			return;
		}
		
		final int skillId = getSkillIdByNpcId(npc.getId());
		final int newLevel = zone.incrementSkillLevel(skillId);
		if (newLevel >= MAX_SKILL_LEVEL)
		{
			zone.broadcastPacket(new SystemMessage(SystemMessageId.KASHA_S_EYE_PITCHES_AND_TOSSES_LIKE_IT_S_ABOUT_TO_EXPLODE));
			ThreadPool.schedule(new KashaDestruction(zone), DESTRUCTION_DELAY);
		}
		else if (newLevel == 3)
		{
			zone.broadcastPacket(new SystemMessage(SystemMessageId.I_CAN_FEEL_THAT_THE_ENERGY_BEING_FLOWN_IN_THE_KASHA_S_EYE_IS_GETTING_STRONGER_RAPIDLY));
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		ThreadPool.schedule(new RespawnNewEye(npc.getLocation()), RESPAWN_DELAY);
		final EffectZone zone = ZoneManager.getInstance().getZone(npc, EffectZone.class);
		if (zone == null)
		{
			LOGGER.warning(() -> "NPC " + npc + " killed outside of EffectZone, check your zone coords! X:" + npc.getX() + " Y:" + npc.getY() + " Z:" + npc.getZ());
			return;
		}
		
		final int skillId = getSkillIdByNpcId(npc.getId());
		zone.decrementSkillLevel(skillId);
	}
	
	private class RespawnNewEye implements Runnable
	{
		private final Location _loc;
		
		public RespawnNewEye(Location loc)
		{
			_loc = loc;
		}
		
		@Override
		public void run()
		{
			addSpawn(getRandomEntry(EYE_IDS), _loc, false, 0);
		}
	}
	
	private class KashaDestruction implements Runnable
	{
		private final EffectZone _zone;
		
		public KashaDestruction(EffectZone zone)
		{
			_zone = zone;
		}
		
		@Override
		public void run()
		{
			for (int i = SKILL_ID; i <= (SKILL_ID + 4); i += 2)
			{
				if (_zone.getSkillLevel(i) >= MAX_SKILL_LEVEL)
				{
					destroyZone();
					return;
				}
			}
		}
		
		private void destroyZone()
		{
			// Cache the skill lookup outside the loop — single lookup instead of one per player.
			final Skill destructionSkill = SkillData.getInstance().getSkill(KASHA_DESTRUCTION_SKILL_ID, KASHA_DESTRUCTION_SKILL_LEVEL);
			if (destructionSkill == null)
			{
				LOGGER.severe("[Den of Evil] Skill " + KASHA_DESTRUCTION_SKILL_ID + " level " + KASHA_DESTRUCTION_SKILL_LEVEL + " not found! Kasha Destruction cannot apply debuff to players.");
				return;
			}
			
			for (Creature creature : _zone.getCharactersInside())
			{
				if (creature == null)
				{
					continue;
				}
				if (creature.isPlayable())
				{
					destructionSkill.applyEffects(creature, creature);
				}
				else if (creature.isNpc())
				{
					final Npc npc = creature.asNpc();
					if (npc.doDie(null) && ArrayUtil.contains(EYE_IDS, npc.getId()))
					{
						ThreadPool.schedule(new RespawnNewEye(npc.getLocation()), RESPAWN_DELAY);
					}
				}
			}
			
			// Clear all zone skills.
			for (int i = SKILL_ID; i <= (SKILL_ID + 4); i += 2)
			{
				_zone.removeSkill(i);
			}
		}
	}
	
	public static void main(String[] args)
	{
		new DenOfEvil();
	}
}