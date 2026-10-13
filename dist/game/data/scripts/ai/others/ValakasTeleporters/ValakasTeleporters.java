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
package ai.others.ValakasTeleporters;

import com.l2journey.Config;
import com.l2journey.gameserver.data.xml.DoorData;
import com.l2journey.gameserver.managers.GrandBossManager;
import com.l2journey.gameserver.managers.QuestManager;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.GrandBoss;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.quest.QuestState;
import com.l2journey.gameserver.model.zone.type.BossZone;

import ai.AbstractNpcAI;
import ai.bosses.Valakas.Valakas;

/**
 * Grand Bosses teleport AI.<br>
 * Original python script by Emperorc.
 * @author Plim, KingHanker
 */
public class ValakasTeleporters extends AbstractNpcAI
{
	private static final String ALLOWENTER = "allowEnter";

	// NPCs
	private static final int[] NPCs =
	{
		31384, // Gatekeeper of Fire Dragon : Opening some doors
		31385, // Heart of Volcano : Teleport into Lair of Valakas
		31540, // Watcher of Valakas Klein : Teleport into Hall of Flames
		31686, // Gatekeeper of Fire Dragon : Opens doors to Heart of Volcano
		31687, // Gatekeeper of Fire Dragon : Opens doors to Heart of Volcano
		31759, // Teleportation Cubic : Teleport out of Lair of Valakas
	};
	// Items
	private static final int VACUALITE_FLOATING_STONE = 7267;
	private static final Location ENTER_HALL_OF_FLAMES = new Location(183813, -115157, -3303);
	private static final Location TELEPORT_INTO_VALAKAS_LAIR = new Location(204328, -111874, 70);
	private static final Location TELEPORT_OUT_OF_VALAKAS_LAIR = new Location(150037, -57720, -2976);
	
	private static int playerCount = 0;
	
	private ValakasTeleporters()
	{
		addStartNpc(NPCs);
		addTalkId(NPCs);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = "";
		final QuestState qs = getQuestState(player, false);
		if (hasQuestItems(player, VACUALITE_FLOATING_STONE))
		{
			takeItems(player, VACUALITE_FLOATING_STONE, 1);
			player.teleToLocation(ENTER_HALL_OF_FLAMES);
			qs.set(ALLOWENTER, "1");
		}
		else
		{
			htmltext = "31540-06.htm";
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = "";
		final QuestState qs = getQuestState(player, true);
		
		switch (npc.getId())
		{
			case 31385:
			{
				if (valakasAI() != null)
				{
					final int status = GrandBossManager.getInstance().getStatus(29028);
					if ((status == 0) || (status == 1))
					{
						if (playerCount >= 200)
						{
							htmltext = "31385-03.htm";
						}
						else if (qs.getInt(ALLOWENTER) == 1)
						{
							qs.unset(ALLOWENTER);
							final BossZone zone = GrandBossManager.getInstance().getZone(212852, -114842, -1632);
							if (zone != null)
							{
								zone.allowPlayerEntry(player, 30);
							}
							
							player.teleToLocation(TELEPORT_INTO_VALAKAS_LAIR.getX() + getRandom(600), TELEPORT_INTO_VALAKAS_LAIR.getY() + getRandom(600), TELEPORT_INTO_VALAKAS_LAIR.getZ());
							playerCount++;
							
							if (status == 0)
							{
								final GrandBoss valakas = GrandBossManager.getInstance().getBoss(29028);
								valakasAI().startQuestTimer("beginning", Config.VALAKAS_WAIT_TIME * 60000L, valakas, null);
								GrandBossManager.getInstance().setStatus(29028, 1);
							}
						}
						else
						{
							htmltext = "31385-04.htm";
						}
					}
					else if (status == 2)
					{
						htmltext = "31385-02.htm";
					}
					else
					{
						htmltext = "31385-01.htm";
					}
				}
				else
				{
					htmltext = "31385-01.htm";
				}
				break;
			}
			case 31384:
			{
				DoorData.getInstance().getDoor(24210004).openMe();
				break;
			}
			case 31686:
			{
				DoorData.getInstance().getDoor(24210005).openMe();
				break;
			}
			case 31687:
			{
				DoorData.getInstance().getDoor(24210006).openMe();
				break;
			}
			case 31540:
			{
				if (playerCount < 50)
				{
					htmltext = "31540-01.htm";
				}
				else if (playerCount < 100)
				{
					htmltext = "31540-02.htm";
				}
				else if (playerCount < 150)
				{
					htmltext = "31540-03.htm";
				}
				else if (playerCount < 200)
				{
					htmltext = "31540-04.htm";
				}
				else
				{
					htmltext = "31540-05.htm";
				}
				break;
			}
			case 31759:
			{
				player.teleToLocation(TELEPORT_OUT_OF_VALAKAS_LAIR.getX() + getRandom(500), TELEPORT_OUT_OF_VALAKAS_LAIR.getY() + getRandom(500), TELEPORT_OUT_OF_VALAKAS_LAIR.getZ());
				break;
			}
			default:
			{
				break;
			}
		}
		return htmltext;
	}
	
	private Quest valakasAI()
	{
		return QuestManager.getInstance().getQuest(Valakas.class.getSimpleName());
	}
	
	public static void main(String[] args)
	{
		new ValakasTeleporters();
	}
}
