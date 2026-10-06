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
package conquerablehalls.FortressOfTheDead;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.data.sql.ClanTable;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.siege.clanhalls.ClanHallSiegeEngine;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * Fortress of the Dead clan hall siege script.
 * @author BiggBoss
 */
public class FortressOfTheDead extends ClanHallSiegeEngine
{
	private static final int LIDIA = 35629;
	private static final int ALFRED = 35630;
	private static final int GISELLE = 35631;
	
	private static Map<Integer, Integer> _damageToLidia = new HashMap<>();
	
	public FortressOfTheDead()
	{
		super(FORTRESS_OF_DEAD);
		addKillId(LIDIA);
		addKillId(ALFRED);
		addKillId(GISELLE);
		
		addSpawnId(LIDIA);
		addSpawnId(ALFRED);
		addSpawnId(GISELLE);
		
		addAttackId(LIDIA);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == LIDIA)
		{
			npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.HMM_THOSE_WHO_ARE_NOT_OF_THE_BLOODLINE_ARE_COMING_THIS_WAY_TO_TAKE_OVER_THE_CASTLE_HUMPH_THE_BITTER_GRUDGES_OF_THE_DEAD_YOU_MUST_NOT_MAKE_LIGHT_OF_THEIR_POWER);
		}
		else if (npc.getId() == ALFRED)
		{
			npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.HEH_HEH_I_SEE_THAT_THE_FEAST_HAS_BEGUN_BE_WARY_THE_CURSE_OF_THE_HELLMANN_FAMILY_HAS_POISONED_THIS_LAND);
		}
		else if (npc.getId() == GISELLE)
		{
			npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.ARISE_MY_FAITHFUL_SERVANTS_YOU_MY_PEOPLE_WHO_HAVE_INHERITED_THE_BLOOD_IT_IS_THE_CALLING_OF_MY_DAUGHTER_THE_FEAST_OF_BLOOD_WILL_NOW_BEGIN);
		}
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (!_hall.isInSiege())
		{
			return;
		}
		
		synchronized (this)
		{
			final Clan clan = attacker.getClan();
			if ((clan != null) && checkIsAttacker(clan))
			{
				final int id = clan.getId();
				if ((id > 0) && _damageToLidia.containsKey(id))
				{
					int newDamage = _damageToLidia.get(id);
					newDamage += damage;
					_damageToLidia.put(id, newDamage);
				}
				else
				{
					_damageToLidia.put(id, damage);
				}
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (!_hall.isInSiege())
		{
			return;
		}
		
		final int npcId = npc.getId();
		if ((npcId == ALFRED) || (npcId == GISELLE))
		{
			npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.AARGH_IF_I_DIE_THEN_THE_MAGIC_FORCE_FIELD_OF_BLOOD_WILL);
		}
		if (npcId == LIDIA)
		{
			npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.GRARR_FOR_THE_NEXT_2_MINUTES_OR_SO_THE_GAME_ARENA_ARE_WILL_BE_CLEANED_THROW_ANY_ITEMS_YOU_DON_T_NEED_TO_THE_FLOOR_NOW);
			_missionAccomplished = true;
			synchronized (this)
			{
				cancelSiegeTask();
				endSiege();
			}
		}
	}
	
	@Override
	public Clan getWinner()
	{
		int counter = 0;
		int damagest = 0;
		for (Entry<Integer, Integer> e : _damageToLidia.entrySet())
		{
			final int damage = e.getValue();
			if (damage > counter)
			{
				counter = damage;
				damagest = e.getKey();
			}
		}
		return ClanTable.getInstance().getClan(damagest);
	}
	
	@Override
	public void startSiege()
	{
		// Siege must start at night
		final int hoursLeft = (GameTimeTaskManager.getInstance().getGameTime() / 60) % 24;
		if ((hoursLeft < 0) || (hoursLeft > 6))
		{
			cancelSiegeTask();
			final long scheduleTime = ((24 - hoursLeft) * 10L) * 60000;
			_siegeTask = ThreadPool.schedule(new SiegeStarts(), scheduleTime);
		}
		else
		{
			super.startSiege();
		}
	}
	
	public static void main(String[] args)
	{
		new FortressOfTheDead();
	}
}
