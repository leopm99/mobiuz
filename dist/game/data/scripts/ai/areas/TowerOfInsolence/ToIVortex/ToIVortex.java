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
package ai.areas.TowerOfInsolence.ToIVortex;

import java.util.HashMap;
import java.util.Map;

import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.itemcontainer.Inventory;

import ai.AbstractNpcAI;

/**
 * Tower of Insolence Vortex teleport AI.<br>
 * Updated to High Five by pmq.<br>
 * Reworked by xban1x.
 * @author Plim
 */
public class ToIVortex extends AbstractNpcAI
{
	// NPCs
	private static final int KEPLON = 30949;
	private static final int EUCLIE = 30950;
	private static final int PITHGON = 30951;
	private static final int DIMENSION_VORTEX_1 = 30952;
	private static final int DIMENSION_VORTEX_2 = 30953;
	private static final int DIMENSION_VORTEX_3 = 30954;
	// Items
	private static final int GREEN_DIMENSION_STONE = 4401;
	private static final int BLUE_DIMENSION_STONE = 4402;
	private static final int RED_DIMENSION_STONE = 4403;
	private static final Map<String, Integer> TOI_FLOOR_ITEMS = new HashMap<>();
	// Locations
	private static final Map<String, Location> TOI_FLOORS = new HashMap<>();
	// Misc
	private static final Map<String, Integer> DIMENSION_TRADE = new HashMap<>();
	static
	{
		TOI_FLOORS.put("1", new Location(114356, 13423, -5096));
		TOI_FLOORS.put("2", new Location(114666, 13380, -3608));
		TOI_FLOORS.put("3", new Location(111982, 16028, -2120));
		TOI_FLOORS.put("4", new Location(114636, 13413, -640));
		TOI_FLOORS.put("5", new Location(114152, 19902, 928));
		TOI_FLOORS.put("6", new Location(117131, 16044, 1944));
		TOI_FLOORS.put("7", new Location(113026, 17687, 2952));
		TOI_FLOORS.put("8", new Location(115571, 13723, 3960));
		TOI_FLOORS.put("9", new Location(114649, 14144, 4976));
		TOI_FLOORS.put("10", new Location(118507, 16605, 5984));
		
		TOI_FLOOR_ITEMS.put("1", GREEN_DIMENSION_STONE);
		TOI_FLOOR_ITEMS.put("2", GREEN_DIMENSION_STONE);
		TOI_FLOOR_ITEMS.put("3", GREEN_DIMENSION_STONE);
		TOI_FLOOR_ITEMS.put("4", BLUE_DIMENSION_STONE);
		TOI_FLOOR_ITEMS.put("5", BLUE_DIMENSION_STONE);
		TOI_FLOOR_ITEMS.put("6", BLUE_DIMENSION_STONE);
		TOI_FLOOR_ITEMS.put("7", RED_DIMENSION_STONE);
		TOI_FLOOR_ITEMS.put("8", RED_DIMENSION_STONE);
		TOI_FLOOR_ITEMS.put("9", RED_DIMENSION_STONE);
		TOI_FLOOR_ITEMS.put("10", RED_DIMENSION_STONE);
		
		DIMENSION_TRADE.put("GREEN", GREEN_DIMENSION_STONE);
		DIMENSION_TRADE.put("BLUE", BLUE_DIMENSION_STONE);
		DIMENSION_TRADE.put("RED", RED_DIMENSION_STONE);
	}
	
	private ToIVortex()
	{
		addStartNpc(KEPLON, EUCLIE, PITHGON, DIMENSION_VORTEX_1, DIMENSION_VORTEX_2, DIMENSION_VORTEX_3);
		addTalkId(KEPLON, EUCLIE, PITHGON, DIMENSION_VORTEX_1, DIMENSION_VORTEX_2, DIMENSION_VORTEX_3);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final int npcId = npc.getId();
		
		switch (event)
		{
			case "1", "2", "3", "4", "5", "6", "7", "8", "9", "10":
			{
				final Location loc = TOI_FLOORS.get(event);
				final int itemId = TOI_FLOOR_ITEMS.get(event);
				if (hasQuestItems(player, itemId))
				{
					takeItems(player, itemId, 1);
					player.teleToLocation(loc, true);
				}
				else
				{
					return "no-stones.htm";
				}
				break;
			}
			case "GREEN", "BLUE", "RED":
			{
				if (player.getAdena() >= 10000)
				{
					takeItems(player, Inventory.ADENA_ID, 10000);
					giveItems(player, DIMENSION_TRADE.get(event), 1);
				}
				else
				{
					return npcId + "no-adena.htm";
				}
				break;
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	public static void main(String[] args)
	{
		new ToIVortex();
	}
}
