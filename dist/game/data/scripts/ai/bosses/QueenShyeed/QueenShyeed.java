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
package ai.bosses.QueenShyeed;

import com.l2journey.gameserver.managers.GlobalVariablesManager;
import com.l2journey.gameserver.managers.ZoneManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.zone.type.EffectZone;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;

import ai.AbstractNpcAI;

/**
 * Queen Shyeed AI
 * @author malyelfik, KingHanker
 */
public class QueenShyeed extends AbstractNpcAI
{
	private static final String RESPAWN_KEY = "respawn";

	// NPC
	private static final int SHYEED = 25671;
	private static final Location SHYEED_LOC = new Location(79634, -55428, -6104, 0);
	// Respawn
	private static final int RESPAWN = 86400000; // 24 h
	private static final int RANDOM_RESPAWN = 43200000; // 12 h
	// Zones
	private static final EffectZone MOB_BUFF_ZONE = ZoneManager.getInstance().getZoneById(200103, EffectZone.class);
	private static final EffectZone MOB_BUFF_DISPLAY_ZONE = ZoneManager.getInstance().getZoneById(200104, EffectZone.class);
	private static final EffectZone FULL_AUTHORITY_ZONE = ZoneManager.getInstance().getZoneById(200105, EffectZone.class);
	
	private QueenShyeed()
	{
		addKillId(SHYEED);
		spawnShyeed();
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case RESPAWN_KEY:
			{
				spawnShyeed();
				break;
			}
			case "despawn":
			{
				if (!npc.isDead())
				{
					startRespawn();
					npc.deleteMe();
				}
				break;
			}
			default:
			{
				break;
			}
		}
		return null;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.SHYEED_S_CRY_IS_STEADILY_DYING_DOWN);
		startRespawn();
		FULL_AUTHORITY_ZONE.setEnabled(true);
	}
	
	private void spawnShyeed()
	{
		final long respawn = GlobalVariablesManager.getInstance().getLong("QueenShyeedRespawn", 0);
		final long remain = respawn != 0 ? respawn - System.currentTimeMillis() : 0;
		if (remain > 0)
		{
			// Boss is dead and waiting to respawn. Zone "_enabled" flags only live in
			// memory and reset to the XML default (false) on every server restart, so
			// they must be restored here to match the persisted respawn state,
			// otherwise a restart during this window leaves the whole area unbuffed.
			MOB_BUFF_ZONE.setEnabled(false);
			MOB_BUFF_DISPLAY_ZONE.setEnabled(false);
			FULL_AUTHORITY_ZONE.setEnabled(true);
			startQuestTimer(RESPAWN_KEY, remain, null, null);
			return;
		}
		// Zones must be enabled before spawning, otherwise the boss and her minions
		// (spawned synchronously by addSpawn) won't be registered in the zone's
		// character list, since it only registers creatures while the zone is enabled.
		FULL_AUTHORITY_ZONE.setEnabled(false);
		MOB_BUFF_ZONE.setEnabled(true);
		MOB_BUFF_DISPLAY_ZONE.setEnabled(true);
		final Npc npc = addSpawn(SHYEED, SHYEED_LOC, false, 0);
		startQuestTimer("despawn", 10800000, npc, null);
	}
	
	private void startRespawn()
	{
		final int respawnTime = RESPAWN - getRandom(RANDOM_RESPAWN);
		GlobalVariablesManager.getInstance().set("QueenShyeedRespawn", Long.toString(System.currentTimeMillis() + respawnTime));
		startQuestTimer(RESPAWN_KEY, respawnTime, null, null);
		MOB_BUFF_ZONE.setEnabled(false);
		MOB_BUFF_DISPLAY_ZONE.setEnabled(false);
	}
	
	public static void main(String[] args)
	{
		new QueenShyeed();
	}
}