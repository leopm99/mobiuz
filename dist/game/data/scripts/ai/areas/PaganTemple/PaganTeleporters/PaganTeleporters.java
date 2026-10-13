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
package ai.areas.PaganTemple.PaganTeleporters;

import java.util.HashMap;
import java.util.Map;

import com.l2journey.gameserver.data.xml.DoorData;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;

import ai.AbstractNpcAI;

/**
 * Pagan Temple teleport AI.<br>
 * Original Jython script by BiTi.
 * @author Plim
 */
public class PaganTeleporters extends AbstractNpcAI
{
	private static final String HTML_FADEDMARK = "FadedMark.htm";
	private static final String CLOSE_DOOR1 = "Close_Door1";
	private static final String CLOSE_DOOR2 = "Close_Door2";

	// NPCs
	private static final int TRIOLS_MIRROR_1 = 32039;
	private static final int TRIOLS_MIRROR_2 = 32040;
	// Locations
	private static final Map<Integer, Location> TRIOLS_LOCS = new HashMap<>();
	static
	{
		TRIOLS_LOCS.put(TRIOLS_MIRROR_1, new Location(-12766, -35840, -10856));
		TRIOLS_LOCS.put(TRIOLS_MIRROR_2, new Location(36640, -51218, 718));
	}
	// @formatter:off
	private static final int[] NPCS =
	{
		32034, 32035, 32036, 32037, 32039, 32040
	};
	// @formatter:on
	// Items
	private static final int VISITORS_MARK = 8064;
	private static final int FADED_VISITORS_MARK = 8065;
	private static final int PAGANS_MARK = 8067;
	
	private PaganTeleporters()
	{
		addStartNpc(NPCS);
		addTalkId(NPCS);
		addFirstTalkId(TRIOLS_MIRROR_1, TRIOLS_MIRROR_2);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case CLOSE_DOOR1:
			{
				DoorData.getInstance().getDoor(19160001).closeMe();
				break;
			}
			case CLOSE_DOOR2:
			{
				DoorData.getInstance().getDoor(19160010).closeMe();
				DoorData.getInstance().getDoor(19160011).closeMe();
				break;
			}
			default:
			{
				break;
			}
		}
		return "";
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (TRIOLS_LOCS.containsKey(npc.getId()))
		{
			player.teleToLocation(TRIOLS_LOCS.get(npc.getId()));
		}
		return "";
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		switch (npc.getId())
		{
			case 32034:
			{
				if (!hasAtLeastOneQuestItem(player, VISITORS_MARK, FADED_VISITORS_MARK, PAGANS_MARK))
				{
					return "noItem.htm";
				}
				DoorData.getInstance().getDoor(19160001).openMe();
				startQuestTimer(CLOSE_DOOR1, 10000, null, null);
				return HTML_FADEDMARK;
			}
			case 32035:
			{
				DoorData.getInstance().getDoor(19160001).openMe();
				startQuestTimer(CLOSE_DOOR1, 10000, null, null);
				return HTML_FADEDMARK;
			}
			case 32036:
			{
				if (!hasQuestItems(player, PAGANS_MARK))
				{
					return "noMark.htm";
				}
				DoorData.getInstance().getDoor(19160010).openMe();
				DoorData.getInstance().getDoor(19160011).openMe();
				startQuestTimer(CLOSE_DOOR2, 10000, null, null);
				return "world.openDoor.htm";
			}
			case 32037:
			{
				DoorData.getInstance().getDoor(19160010).openMe();
				DoorData.getInstance().getDoor(19160011).openMe();
				startQuestTimer(CLOSE_DOOR2, 10000, null, null);
				return HTML_FADEDMARK;
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
		new PaganTeleporters();
	}
}
