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
package village_master.FirstClassTransferTalk;

import java.util.HashMap;
import java.util.Map;

import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.actor.instance.VillageMasterFighter;
import com.l2journey.gameserver.model.actor.instance.VillageMasterPriest;

import ai.AbstractNpcAI;

/**
 * This script manages the dialogs of the headmasters of all newbie villages.<br>
 * None of them provide actual class transfers, they only talk about it.
 * @author jurchiks, xban1x
 */
public class FirstClassTransferTalk extends AbstractNpcAI
{
	private static final Map<Integer, Race> MASTERS = new HashMap<>();
	static
	{
		MASTERS.put(30026, Race.HUMAN); // Blitz, TI Fighter Guild Head Master
		MASTERS.put(30031, Race.HUMAN); // Biotin, TI Einhasad Temple High Priest
		MASTERS.put(30154, Race.ELF); // Asterios, Elven Village Tetrarch
		MASTERS.put(30358, Race.DARK_ELF); // Thifiell, Dark Elf Village Tetrarch
		MASTERS.put(30565, Race.ORC); // Kakai, Orc Village Flame Lord
		MASTERS.put(30520, Race.DWARF); // Reed, Dwarven Village Warehouse Chief
		MASTERS.put(30525, Race.DWARF); // Bronk, Dwarven Village Head Blacksmith
		// Kamael Village NPCs
		MASTERS.put(32171, Race.DWARF); // Hoffa, Warehouse Chief
		MASTERS.put(32158, Race.DWARF); // Fisler, Dwarf Guild Warehouse Chief
		MASTERS.put(32157, Race.DWARF); // Moka, Dwarf Guild Head Blacksmith
		MASTERS.put(32160, Race.DARK_ELF); // Devon, Dark Elf Guild Grand Magister
		MASTERS.put(32147, Race.ELF); // Rivian, Elf Guild Grand Master
		MASTERS.put(32150, Race.ORC); // Took, Orc Guild High Prefect
		MASTERS.put(32153, Race.HUMAN); // Prana, Human Guild High Priest
		MASTERS.put(32154, Race.HUMAN); // Aldenia, Human Guild Grand Master
	}
	
	private FirstClassTransferTalk()
	{
		addStartNpc(MASTERS.keySet());
		addTalkId(MASTERS.keySet());
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		return event;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = npc.getId() + "_";
		if (MASTERS.get(npc.getId()) != player.getRace())
		{
			return htmltext += "no.html";
		}
		
		final Race race = MASTERS.get(npc.getId());
		switch (race)
		{
			case HUMAN:
			{
				if (player.getPlayerClass().level() == 0)
				{
					if (player.isMageClass())
					{
						if (npc instanceof VillageMasterPriest)
						{
							htmltext += "mystic.html";
						}
						else
						{
							htmltext += "no.html";
						}
					}
					else if (npc instanceof VillageMasterFighter)
					{
						htmltext += "fighter.html";
					}
					else
					{
						htmltext += "no.html";
					}
				}
				else if (player.getPlayerClass().level() == 1)
				{
					htmltext += "transfer_1.html";
				}
				else
				{
					htmltext += "transfer_2.html";
				}
				break;
			}
			case ELF, DARK_ELF, ORC:
			{
				if (player.getPlayerClass().level() == 0)
				{
					if (player.isMageClass())
					{
						htmltext += "mystic.html";
					}
					else
					{
						htmltext += "fighter.html";
					}
				}
				else if (player.getPlayerClass().level() == 1)
				{
					htmltext += "transfer_1.html";
				}
				else
				{
					htmltext += "transfer_2.html";
				}
				break;
			}
			case DWARF:
			{
				if (player.getPlayerClass().level() == 0)
				{
					htmltext += "fighter.html";
				}
				else if (player.getPlayerClass().level() == 1)
				{
					htmltext += "transfer_1.html";
				}
				else
				{
					htmltext += "transfer_2.html";
				}
				break;
			}
			default:
			{
				htmltext += "no.html";
				break;
			}
		}
		return htmltext;
	}
	
	public static void main(String[] args)
	{
		new FirstClassTransferTalk();
	}
}
