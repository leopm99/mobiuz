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
package handlers.voicedcommandhandlers;

import com.l2journey.gameserver.cache.PanelHtmlBuilder;
import com.l2journey.gameserver.handler.IVoicedCommandHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author KingHanker
 */
public class Panel implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"panel",
		"cp",
		"user",
		"changeexp",
		"enchantanime",
		"tradeprot"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		switch (command)
		{
			case "panel", "cp", "user":
				break;
			case "tradeprot":
				toggleSetting(activeChar, "noTrade", "Block Trade");
				break;
			case "changeexp":
				toggleSetting(activeChar, "noExp", "Block Experience");
				break;
			case "enchantanime":
				toggleSetting(activeChar, "showEnchantAnime", "Enchant Animation");
				break;
		}
		sendHtml(activeChar);
		return true;
	}
	
	private void toggleSetting(Player player, String varName, String settingName)
	{
		boolean newValue = !player.getVarB(varName);
		player.setVar(varName, String.valueOf(newValue));
		player.sendMessage(settingName + ": " + (newValue ? "Enabled." : "Disabled."));
	}
	
	public static void sendHtml(Player player)
	{
		NpcHtmlMessage html = new NpcHtmlMessage();
		html.setHtml(new PanelHtmlBuilder(player).build());
		player.sendPacket(html);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
