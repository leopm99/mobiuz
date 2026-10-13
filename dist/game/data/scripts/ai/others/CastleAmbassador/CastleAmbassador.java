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
package ai.others.CastleAmbassador;

import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.siege.Castle;
import com.l2journey.gameserver.model.siege.Fort;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

import ai.AbstractNpcAI;

/**
 * Castle Ambassador AI.
 * @author St3eT
 */
public class CastleAmbassador extends AbstractNpcAI
{
	private static final String DESPAWN = "DESPAWN";

	// NPCs
	// @formatter:off
	private static final int[] CASTLE_AMBASSADOR =
	{
		36393, 36394, 36437, 36435, // Gludio
		36395, 36436, 36439, 36441, // Dion
		36396, 36440, 36444, 36449, 36451, // Giran
		36397, 36438, 36442, 36443, 36446, // Oren
		36398, 36399, 36445, 36448, // Aden
		36400, 36450, // Innadril
		36401, 36447, 36453, // Goddard
		36433, 36452, 36454, // Rune
		36434, 36455, // Schuttgart
	};
	// @formatter:on
	
	private CastleAmbassador()
	{
		addStartNpc(CASTLE_AMBASSADOR);
		addTalkId(CASTLE_AMBASSADOR);
		addFirstTalkId(CASTLE_AMBASSADOR);
		addEventReceivedId(CASTLE_AMBASSADOR);
		addSpawnId(CASTLE_AMBASSADOR);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (npc != null)
		{
			final Fort fortresss = npc.getFort();
			String htmltext = null;
			
			switch (event)
			{
				case "signed":
				{
					if (fortresss.getFortState() == 0)
					{
						fortresss.setFortState(2, fortresss.getCastleIdByAmbassador(npc.getId()));
						cancelQuestTimer(DESPAWN, npc, null);
						startQuestTimer(DESPAWN, 3000, npc, null);
						htmltext = "ambassador-05.html";
					}
					else if (fortresss.getFortState() == 1)
					{
						htmltext = "ambassador-04.html";
					}
					break;
				}
				case "rejected":
				{
					if (fortresss.getFortState() == 0)
					{
						fortresss.setFortState(1, fortresss.getCastleIdByAmbassador(npc.getId()));
						cancelQuestTimer(DESPAWN, npc, null);
						startQuestTimer(DESPAWN, 3000, npc, null);
						htmltext = "ambassador-02.html";
					}
					else if (fortresss.getFortState() == 2)
					{
						htmltext = "ambassador-02.html";
					}
					break;
				}
				case DESPAWN:
				{
					if (fortresss.getFortState() == 0)
					{
						fortresss.setFortState(1, fortresss.getCastleIdByAmbassador(npc.getId()));
					}
					cancelQuestTimer(DESPAWN, npc, null);
					npc.broadcastEvent(DESPAWN, 1000, null);
					npc.deleteMe();
					break;
				}
				default:
				{
					break;
				}
			}
			
			if (htmltext != null)
			{
				final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
				packet.setHtml(getHtm(player, htmltext));
				packet.replace("%castleName%", fortresss.getCastleByAmbassador(npc.getId()).getName());
				player.sendPacket(packet);
			}
		}
		return super.onEvent(event, npc, player);
	}
	
	@Override
	public String onEventReceived(String eventName, Npc sender, Npc receiver, WorldObject reference)
	{
		if (receiver != null)
		{
			receiver.deleteMe();
		}
		return super.onEventReceived(eventName, sender, receiver, reference);
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Fort fortresss = npc.getFort();
		final int fortOwner = fortresss.getOwnerClan() == null ? 0 : fortresss.getOwnerClan().getId();
		String htmltext = null;
		
		if (player.isClanLeader() && (player.getClanId() == fortOwner))
		{
			htmltext = fortresss.isBorderFortress() ? "ambassador-01.html" : "ambassador.html";
		}
		else
		{
			htmltext = "ambassador-03.html";
		}
		
		final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
		packet.setHtml(getHtm(player, htmltext));
		packet.replace("%castleName%", fortresss.getCastleByAmbassador(npc.getId()).getName());
		player.sendPacket(packet);
		return null;
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		final Fort fort = npc.getFort();
		final Castle castle = fort == null ? null : fort.getCastleByAmbassador(npc.getId());
		if ((castle == null) || (castle.getOwnerId() == 0))
		{
			npc.deleteMe();
		}
		else
		{
			startQuestTimer(DESPAWN, 3600000, npc, null);
		}
	}
	
	public static void main(String[] args)
	{
		new CastleAmbassador();
	}
}