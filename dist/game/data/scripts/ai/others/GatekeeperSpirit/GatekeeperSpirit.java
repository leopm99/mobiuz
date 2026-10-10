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
package ai.others.GatekeeperSpirit;

import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.sevensigns.SevenSigns;

import ai.AbstractNpcAI;

/**
 * Gatekeeper Spirit AI.
 * @author Zoey76
 */
public class GatekeeperSpirit extends AbstractNpcAI
{
	// NPCs
	private static final int GATEKEEPER_SPIRIT_ENTER = 31111;
	private static final int GATEKEEPER_SPIRIT_EXIT = 31112;
	private static final int LILITH = 25283;
	private static final int ANAKIM = 25286;
	// Exit gatekeeper spawn locations
	private static final Location SPAWN_LILITH_GATEKEEPER = new Location(184410, -10111, -5488);
	private static final Location SPAWN_ANAKIM_GATEKEEPER = new Location(184410, -13102, -5488);
	// Teleport
	private static final Location TELEPORT_DUSK = new Location(184464, -13104, -5504);
	private static final Location TELEPORT_DAWN = new Location(184448, -10112, -5504);
	private static final Location EXIT = new Location(182960, -11904, -4897);
	
	private GatekeeperSpirit()
	{
		addStartNpc(GATEKEEPER_SPIRIT_ENTER, GATEKEEPER_SPIRIT_EXIT);
		addFirstTalkId(GATEKEEPER_SPIRIT_ENTER, GATEKEEPER_SPIRIT_EXIT);
		addTalkId(GATEKEEPER_SPIRIT_ENTER, GATEKEEPER_SPIRIT_EXIT);
		addKillId(LILITH, ANAKIM);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "ANAKIM":
			{
				addSpawn(GATEKEEPER_SPIRIT_EXIT, SPAWN_ANAKIM_GATEKEEPER, false, 900000);
				break;
			}
			case "LILITH":
			{
				addSpawn(GATEKEEPER_SPIRIT_EXIT, SPAWN_LILITH_GATEKEEPER, false, 900000);
				break;
			}
			case "TeleportIn":
			{
				final int playerCabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
				final int sealOfAvariceOwner = SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_AVARICE);
				final int compWinner = SevenSigns.getInstance().getCabalHighestScore();
				if (!SevenSigns.getInstance().isSealValidationPeriod())
				{
					htmltext = "31111-no.html";
				}
				else if ((compWinner == SevenSigns.CABAL_DUSK) && (playerCabal == SevenSigns.CABAL_DUSK) && (sealOfAvariceOwner == SevenSigns.CABAL_DUSK))
				{
					player.teleToLocation(TELEPORT_DUSK, false);
				}
				else if ((compWinner == SevenSigns.CABAL_DAWN) && (playerCabal == SevenSigns.CABAL_DAWN) && (sealOfAvariceOwner == SevenSigns.CABAL_DAWN))
				{
					player.teleToLocation(TELEPORT_DAWN, false);
				}
				else
				{
					htmltext = "31111-no.html";
				}
				break;
			}
			case "TeleportOut":
			{
				player.teleToLocation(EXIT, true);
				break;
			}
			default:
			{
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		switch (npc.getId())
		{
			case ANAKIM:
			{
				startQuestTimer("ANAKIM", 10000, npc, killer);
				break;
			}
			case LILITH:
			{
				startQuestTimer("LILITH", 10000, npc, killer);
				break;
			}
			default:
			{
				break;
			}
		}
	}
	
	public static void main(String[] args)
	{
		new GatekeeperSpirit();
	}
}