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
package instances.SSQHideoutOfTheDawn;

import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.instancezone.InstanceWorld;

import instances.AbstractInstance;

/**
 * Hideout of the Dawn instance zone.
 * @author Adry_85
 */
public class SSQHideoutOfTheDawn extends AbstractInstance
{
	// NPCs
	private static final int WOOD = 32593;
	private static final int JAINA = 32617;
	// Location
	private static final Location WOOD_LOC = new Location(-23758, -8959, -5384);
	private static final Location JAINA_LOC = new Location(147072, 23743, -1984);
	// Misc
	private static final int TEMPLATE_ID = 113;
	
	private SSQHideoutOfTheDawn()
	{
		addFirstTalkId(JAINA);
		addStartNpc(WOOD);
		addTalkId(WOOD, JAINA);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "32617-01.html", "32617-02a.html":
			{
				htmltext = event;
				break;
			}
			case "32617-02.html":
			{
				player.setInstanceId(0);
				player.teleToLocation(JAINA_LOC, true);
				htmltext = event;
				break;
			}
			case "32593-01.html":
			{
				enterInstance(player, TEMPLATE_ID);
				htmltext = event;
			}
			default:
			{
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onEnterInstance(Player player, InstanceWorld world, boolean firstEntrance)
	{
		if (firstEntrance)
		{
			world.addAllowed(player);
		}
		teleportPlayer(player, WOOD_LOC, world.getInstanceId(), false);
	}
	
	public static void main(String[] args)
	{
		new SSQHideoutOfTheDawn();
	}
}
