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
package ai.others.TeleportWithCharm;

import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;

import ai.AbstractNpcAI;

/**
 * Charm teleport AI.<br>
 * Original Jython script by DraX.
 * @author Plim
 */
public class TeleportWithCharm extends AbstractNpcAI
{
	// NPCs
	private static final int WHIRPY = 30540;
	private static final int TAMIL = 30576;
	// Items
	private static final int ORC_GATEKEEPER_CHARM = 1658;
	private static final int DWARF_GATEKEEPER_TOKEN = 1659;
	// Locations
	private static final Location ORC_TELEPORT = new Location(-80826, 149775, -3043);
	private static final Location DWARF_TELEPORT = new Location(-80826, 149775, -3043);
	
	private TeleportWithCharm()
	{
		addStartNpc(WHIRPY, TAMIL);
		addTalkId(WHIRPY, TAMIL);
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		switch (npc.getId())
		{
			case WHIRPY:
			{
				if (hasQuestItems(player, DWARF_GATEKEEPER_TOKEN))
				{
					takeItems(player, DWARF_GATEKEEPER_TOKEN, 1);
					player.teleToLocation(DWARF_TELEPORT);
				}
				else
				{
					return "30540-01.htm";
				}
				break;
			}
			case TAMIL:
			{
				if (hasQuestItems(player, ORC_GATEKEEPER_CHARM))
				{
					takeItems(player, ORC_GATEKEEPER_CHARM, 1);
					player.teleToLocation(ORC_TELEPORT);
				}
				else
				{
					return "30576-01.htm";
				}
				break;
			}
			default:
			{
				break;
			}
		}
		return super.onTalk(npc, player);
	}
	
	public static void main(String[] args)
	{
		new TeleportWithCharm();
	}
}
