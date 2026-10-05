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
package custom.events.Wedding;

import com.l2journey.EventsConfig;
import com.l2journey.gameserver.managers.CoupleManager;
import com.l2journey.gameserver.model.Couple;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.skill.CommonSkill;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2journey.gameserver.util.Broadcast;

import ai.AbstractNpcAI;

/**
 * Wedding AI.
 * @author Zoey76
 */
public class Wedding extends AbstractNpcAI
{
	// NPC
	private static final int MANAGER_ID = 50007;
	// Item
	private static final int FORMAL_WEAR = 6408;
	
	public Wedding()
	{
		addFirstTalkId(MANAGER_ID);
		addTalkId(MANAGER_ID);
		addStartNpc(MANAGER_ID);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (player.getPartnerId() == 0)
		{
			return "NoPartner.html";
		}
		
		final Player partner = World.getInstance().getPlayer(player.getPartnerId());
		if ((partner == null) || !partner.isOnline())
		{
			return "NotFound.html";
		}
		
		if (player.isMarried())
		{
			return "Already.html";
		}
		
		if (player.isMarryAccepted())
		{
			return "WaitForPartner.html";
		}
		
		String htmltext = null;
		if (player.isMarryRequest())
		{
			if (!isWearingFormalWear(player) || !isWearingFormalWear(partner))
			{
				htmltext = sendHtml(partner, "NoFormal.html", null, null);
			}
			else
			{
				player.setMarryRequest(false);
				partner.setMarryRequest(false);
				htmltext = getHtm(player, "Ask.html");
				htmltext = htmltext.replace("%player%", partner.getName());
			}
			return htmltext;
		}
		
		switch (event)
		{
			case "ask":
			{
				if (!isWearingFormalWear(player) || !isWearingFormalWear(partner))
				{
					htmltext = sendHtml(partner, "NoFormal.html", null, null);
				}
				else
				{
					player.setMarryAccepted(true);
					partner.setMarryRequest(true);
					
					sendHtml(partner, "Ask.html", "%player%", player.getName());
					htmltext = getHtm(player, "Requested.html");
					htmltext = htmltext.replace("%player%", partner.getName());
				}
				break;
			}
			case "accept":
			{
				if (!isWearingFormalWear(player) || !isWearingFormalWear(partner))
				{
					htmltext = sendHtml(partner, "NoFormal.html", null, null);
				}
				else if ((player.getAdena() < EventsConfig.WEDDING_PRICE) || (partner.getAdena() < EventsConfig.WEDDING_PRICE))
				{
					htmltext = sendHtml(partner, "Adena.html", "%fee%", String.valueOf(EventsConfig.WEDDING_PRICE));
				}
				else
				{
					player.reduceAdena(ItemProcessType.FEE, EventsConfig.WEDDING_PRICE, player.getLastFolkNPC(), true);
					partner.reduceAdena(ItemProcessType.FEE, EventsConfig.WEDDING_PRICE, player.getLastFolkNPC(), true);
					
					// Accept the wedding request
					player.setMarryAccepted(true);
					final Couple couple = CoupleManager.getInstance().getCouple(player.getCoupleId());
					couple.marry();
					
					// Messages to the couple
					player.sendMessage("Congratulations you are married!");
					player.setMarried(true);
					player.setMarryRequest(false);
					partner.sendMessage("Congratulations you are married!");
					partner.setMarried(true);
					partner.setMarryRequest(false);
					
					// Wedding march
					player.broadcastPacket(new MagicSkillUse(player, player, 2230, 1, 1, 0));
					partner.broadcastPacket(new MagicSkillUse(partner, partner, 2230, 1, 1, 0));
					
					// Fireworks
					final Skill skill = CommonSkill.LARGE_FIREWORK.getSkill();
					if (skill != null)
					{
						player.doCast(skill);
						partner.doCast(skill);
					}
					
					Broadcast.toAllOnlinePlayers("Congratulations to " + player.getName() + " and " + partner.getName() + "! They have been married.");
					htmltext = sendHtml(partner, "Accepted.html", null, null);
				}
				break;
			}
			case "decline":
			{
				player.setMarryRequest(false);
				partner.setMarryRequest(false);
				player.setMarryAccepted(false);
				partner.setMarryAccepted(false);
				
				player.sendMessage("You declined your partner's marriage request.");
				partner.sendMessage("Your partner declined your marriage request.");
				htmltext = sendHtml(partner, "Declined.html", null, null);
				break;
			}
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final String htmltext = getHtm(player, "Start.html");
		return htmltext.replace("%fee%", String.valueOf(EventsConfig.WEDDING_PRICE));
	}
	
	private String sendHtml(Player player, String fileName, String regex, String replacement)
	{
		String html = getHtm(player, fileName);
		if ((regex != null) && (replacement != null))
		{
			html = html.replaceAll(regex, replacement);
		}
		player.sendPacket(new NpcHtmlMessage(html));
		return html;
	}
	
	private static boolean isWearingFormalWear(Player player)
	{
		if (EventsConfig.WEDDING_FORMALWEAR)
		{
			final Item formalWear = player.getChestArmorInstance();
			return (formalWear != null) && (formalWear.getId() == FORMAL_WEAR);
		}
		return true;
	}
	
	public static void main(String[] args)
	{
		new Wedding();
	}
}
