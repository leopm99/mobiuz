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
package conquerablehalls.FortressOfResistance;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.l2journey.commons.time.TimeUtil;
import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.data.sql.ClanTable;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.Spawn;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.siege.clanhalls.ClanHallSiegeEngine;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Fortress of Resistance clan hall siege Script.
 * @author BiggBoss
 */
public class FortressOfResistance extends ClanHallSiegeEngine
{
	private static final int MESSENGER = 35382;
	private static final int BLOODY_LORD_NURKA = 35375;
	
	private final Location[] NURKA_COORDS =
	{
		new Location(45109, 112124, -1900), // 30%
		new Location(47653, 110816, -2110), // 40%
		new Location(47247, 109396, -2000), // 30%
	};
	
	private Spawn _nurka;
	private final Map<Integer, Long> _damageToNurka = new HashMap<>();
	private NpcHtmlMessage _messengerMsg;
	
	private FortressOfResistance()
	{
		super(FORTRESS_RESSISTANCE);
		addFirstTalkId(MESSENGER);
		addKillId(BLOODY_LORD_NURKA);
		addAttackId(BLOODY_LORD_NURKA);
		buildMessengerMessage();
		
		try
		{
			_nurka = new Spawn(BLOODY_LORD_NURKA);
			_nurka.setAmount(1);
			_nurka.setRespawnDelay(10800);
//			@formatter:off
//			@formatter:on
			_nurka.setLocation(NURKA_COORDS[0]);
		}
		catch (Exception e)
		{
			LOGGER.warning(getName() + ": Could not set the Bloody Lord Nurka spawn " + e);
		}
	}
	
	private void buildMessengerMessage()
	{
		final String html = HtmCache.getInstance().getHtm(null, "data/scripts/conquerablehalls/FortressOfResistance/partisan_ordery_brakel001.htm");
		if (html != null)
		{
			// FIXME: We don't have an object id to put in here :(
			_messengerMsg = new NpcHtmlMessage();
			_messengerMsg.setHtml(html);
			_messengerMsg.replace("%nextSiege%", TimeUtil.formatDate(_hall.getSiegeDate().getTime(), "yyyy-MM-dd HH:mm:ss"));
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		player.sendPacket(_messengerMsg);
		return null;
	}
	
	@Override
	public void onAttack(Npc npc, Player player, int damage, boolean isSummon)
	{
		if (!_hall.isInSiege())
		{
			return;
		}
		
		final int clanId = player.getClanId();
		if (clanId > 0)
		{
			final long clanDmg = (_damageToNurka.containsKey(clanId)) ? _damageToNurka.get(clanId) + damage : damage;
			_damageToNurka.put(clanId, clanDmg);
			
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (!_hall.isInSiege())
		{
			return;
		}
		
		_missionAccomplished = true;
		
		synchronized (this)
		{
			npc.getSpawn().stopRespawn();
			npc.deleteMe();
			cancelSiegeTask();
			endSiege();
		}
	}
	
	@Override
	public Clan getWinner()
	{
		int winnerId = 0;
		long counter = 0;
		for (Entry<Integer, Long> e : _damageToNurka.entrySet())
		{
			final long dam = e.getValue();
			if (dam > counter)
			{
				winnerId = e.getKey();
				counter = dam;
			}
		}
		return ClanTable.getInstance().getClan(winnerId);
	}
	
	@Override
	public void onSiegeStarts()
	{
		_nurka.init();
	}
	
	@Override
	public void onSiegeEnds()
	{
		buildMessengerMessage();
	}
	
	public static void main(String[] args)
	{
		new FortressOfResistance();
	}
}