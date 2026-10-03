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
package ai.areas.Gracia.AI.NPC.Nottingale;

import java.util.HashMap;
import java.util.Map;

import com.l2journey.gameserver.managers.AirShipManager;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.clan.ClanAccess;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.network.serverpackets.RadarControl;

import ai.AbstractNpcAI;
import quests.Q10273_GoodDayToFly.Q10273_GoodDayToFly;

/**
 * Nottingale AI
 * @author xban1x
 */
public class Nottingale extends AbstractNpcAI
{
	// NPC
	private static final int NOTTINGALE = 32627;
	// Misc
	private static final Map<Integer, RadarControl> RADARS = new HashMap<>();
	static
	{
		RADARS.put(2, new RadarControl(0, -184545, 243120, 1581, 2));
		RADARS.put(5, new RadarControl(0, -192361, 254528, 3598, 1));
		RADARS.put(6, new RadarControl(0, -174600, 219711, 4424, 1));
		RADARS.put(7, new RadarControl(0, -181989, 208968, 4424, 1));
		RADARS.put(8, new RadarControl(0, -252898, 235845, 5343, 1));
		RADARS.put(9, new RadarControl(0, -212819, 209813, 4288, 1));
		RADARS.put(10, new RadarControl(0, -246899, 251918, 4352, 1));
	}
	
	public Nottingale()
	{
		addStartNpc(NOTTINGALE);
		addTalkId(NOTTINGALE);
		addFirstTalkId(NOTTINGALE);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "32627-02.html", "32627-03.html", "32627-04.html":
			{
				if (player.getClan() != null)
				{
					if (player.hasAccess(ClanAccess.ACCESS_AIRSHIP) && AirShipManager.getInstance().hasAirShipLicense(player.getClanId()) && !AirShipManager.getInstance().hasAirShip(player.getClanId()))
					{
						htmltext = event;
					}
					else
					{
						final QuestState qs = player.getQuestState(Q10273_GoodDayToFly.class.getSimpleName());
						if ((qs != null) && qs.isCompleted())
						{
							htmltext = event;
						}
						else
						{
							player.sendPacket(RADARS.get(2));
							htmltext = "32627-01.html";
						}
					}
				}
				else
				{
					final QuestState qs = player.getQuestState(Q10273_GoodDayToFly.class.getSimpleName());
					if ((qs != null) && qs.isCompleted())
					{
						htmltext = event;
					}
					else
					{
						player.sendPacket(RADARS.get(2));
						htmltext = "32627-01.html";
					}
				}
				break;
			}
			case "32627-05.html", "32627-06.html", "32627-07.html", "32627-08.html", "32627-09.html", "32627-10.html":
			{
				player.sendPacket(RADARS.get(Integer.parseInt(event.substring(6, 8))));
				htmltext = event;
				break;
			}
		}
		return htmltext;
	}
}
