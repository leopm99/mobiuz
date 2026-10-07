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
package handlers.bypasshandlers;

import java.util.HashSet;
import java.util.Set;

import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.handler.IBypassHandler;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Teleporter;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author KingHanker
 */
public class Link implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"Link"
	};
	
	private static final Set<String> SPECIFIC_VALID_LINKS = new HashSet<>();
	static
	{
		SPECIFIC_VALID_LINKS.add("adventurer_guildsman/AboutHighLevelGuilds.htm");
		SPECIFIC_VALID_LINKS.add("adventurer_guildsman/AboutNewLifeCrystals.htm");
		SPECIFIC_VALID_LINKS.add("clanHallDoorman/evolve.htm");
		SPECIFIC_VALID_LINKS.add("common/augmentation_01.htm");
		SPECIFIC_VALID_LINKS.add("common/augmentation_02.htm");
		SPECIFIC_VALID_LINKS.add("common/crafting_01.htm");
		SPECIFIC_VALID_LINKS.add("common/duals_01.htm");
		SPECIFIC_VALID_LINKS.add("common/duals_02.htm");
		SPECIFIC_VALID_LINKS.add("common/duals_03.htm");
		SPECIFIC_VALID_LINKS.add("common/g_cube_warehouse001.htm");
		SPECIFIC_VALID_LINKS.add("common/skill_enchant_help.htm");
		SPECIFIC_VALID_LINKS.add("common/skill_enchant_help_01.htm");
		SPECIFIC_VALID_LINKS.add("common/skill_enchant_help_02.htm");
		SPECIFIC_VALID_LINKS.add("common/skill_enchant_help_03.htm");
		SPECIFIC_VALID_LINKS.add("common/weapon_sa_01.htm");
		SPECIFIC_VALID_LINKS.add("common/welcomeback002.htm");
		SPECIFIC_VALID_LINKS.add("common/welcomeback003.htm");
		SPECIFIC_VALID_LINKS.add("default/BlessingOfProtection.htm");
		SPECIFIC_VALID_LINKS.add("default/SupportMagic.htm");
		SPECIFIC_VALID_LINKS.add("default/SupportMagicServitor.htm");
		SPECIFIC_VALID_LINKS.add("fisherman/fishing_championship.htm");
		SPECIFIC_VALID_LINKS.add("fortress/foreman.htm");
		SPECIFIC_VALID_LINKS.add("guard/kamaloka_help.htm");
		SPECIFIC_VALID_LINKS.add("guard/kamaloka_level.htm");
		SPECIFIC_VALID_LINKS.add("olympiad/hero_main2.htm");
		SPECIFIC_VALID_LINKS.add("petmanager/evolve.htm");
		SPECIFIC_VALID_LINKS.add("petmanager/exchange.htm");
		SPECIFIC_VALID_LINKS.add("petmanager/instructions.htm");
		SPECIFIC_VALID_LINKS.add("seven_signs/blkmrkt_1.htm");
		SPECIFIC_VALID_LINKS.add("seven_signs/blkmrkt_2.htm");
		SPECIFIC_VALID_LINKS.add("seven_signs/mammblack_1a.htm");
		SPECIFIC_VALID_LINKS.add("seven_signs/mammblack_1b.htm");
		SPECIFIC_VALID_LINKS.add("seven_signs/mammblack_1c.htm");
		SPECIFIC_VALID_LINKS.add("seven_signs/mammblack_2a.htm");
		SPECIFIC_VALID_LINKS.add("seven_signs/mammblack_2b.htm");
		SPECIFIC_VALID_LINKS.add("seven_signs/mammmerch_1.htm");
		SPECIFIC_VALID_LINKS.add("seven_signs/mammmerch_1a.htm");
		SPECIFIC_VALID_LINKS.add("seven_signs/mammmerch_1b.htm");
		SPECIFIC_VALID_LINKS.add("teleporter/separatedsoul.htm");
		SPECIFIC_VALID_LINKS.add("warehouse/clanwh.htm");
		SPECIFIC_VALID_LINKS.add("warehouse/privatewh.htm");
	}
	
	private static final Set<String> ALLOWED_DIRECTORIES = new HashSet<>();
	static
	{
		ALLOWED_DIRECTORIES.add("adventurer_guildsman/");
	}
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		final String htmlPath = command.substring(4).trim();
		if (htmlPath.isEmpty())
		{
			LOGGER.warning(() -> player + " sent empty link html!");
			return false;
		}
		
		if (htmlPath.contains(".."))
		{
			LOGGER.warning(() -> player + " sent invalid link html: " + htmlPath);
			return false;
		}
		
		boolean isValidLink = SPECIFIC_VALID_LINKS.contains(htmlPath) || isLinkInAllowedDirectory(htmlPath);
		String content = null;
		if (isValidLink)
		{
			content = HtmCache.getInstance().getHtm(player, "data/html/" + htmlPath);
			
			if (htmlPath.startsWith("teleporter/") && !(player.getTarget() instanceof Teleporter))
			{
				LOGGER.warning(() -> player + " tried to use teleporter link without targeting a teleporter: " + htmlPath);
				content = null;
			}
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(target != null ? target.getObjectId() : 0);
		
		if (content != null)
		{
			html.setHtml(content.replace("%objectId%", String.valueOf(target != null ? target.getObjectId() : 0)));
		}
		else
		{
			LOGGER.warning(() -> player + " tried to access a link in allowed directory but file not found or invalid: " + htmlPath);
			html.setHtml("<html><body>Link inválido ou arquivo não encontrado.</body></html>");
		}
		
		player.sendPacket(html);
		return true;
	}
	
	private boolean isLinkInAllowedDirectory(String htmlPath)
	{
		for (String directory : ALLOWED_DIRECTORIES)
		{
			if (htmlPath.startsWith(directory))
			{
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
