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

import java.util.Map;
import java.util.logging.Logger;

import com.l2journey.gameserver.data.xml.NpcData;
import com.l2journey.gameserver.handler.IVoicedCommandHandler;
import com.l2journey.gameserver.managers.GrandBossManager;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author KingHanker
 */
public class Epic implements IVoicedCommandHandler
{
	static final Logger LOGGER = Logger.getLogger(Epic.class.getName());
	
	private static final String[] VOICED_COMMANDS =
	{
		"epic"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		NpcHtmlMessage msg = new NpcHtmlMessage(5);
		final StringBuilder tb = new StringBuilder("<html noscrollbar><title>Grand Boss Info</title><body>");
		tb.append("<br>");
		tb.append("<center>");
		tb.append("<table border=0 width=360>");
		tb.append("<tr><td width=5></td>");
		tb.append("<td width=40 align=center><font name=hs12 name=CreditTextNormal color=B59A75>Jewel</font></td>");
		tb.append("<td width=70 align=center><font name=hs12 name=CreditTextNormal color=B59A75>Boss</font></td>");
		tb.append("<td width=100 align=center><font name=hs12 name=CreditTextNormal color=B59A75>Status</font></td>");
		tb.append("</tr>");
		
		tb.append("<tr><td width=5></td>");
		tb.append("<td background=\"l2ui_ct1.ComboBox_DF_Dropmenu_Bg\" width=40></td>");
		tb.append("<td background=\"l2ui_ct1.ComboBox_DF_Dropmenu_Bg\" width=70 align=center></td>");
		tb.append("<td background=\"l2ui_ct1.ComboBox_DF_Dropmenu_Bg\" width=100 align=center></td>");
		tb.append("</tr>");
		
		int[] BOSSES =
		{
			29001, // Queen Ant
			29006, // Core
			29014, // Orfen
			29020, // Baium
			29028, // Valakas
			29068, // Antharas
			29118 // Beleth
		
		};
		
		for (int boss : BOSSES)
		{
			final String bossName = NpcData.getInstance().getTemplate(boss).getName();
			long delay = GrandBossManager.getInstance().getStatSet(boss).getLong("respawn_time");
			if (delay <= System.currentTimeMillis())
			{
				tb.append("<tr><td width=5></td>");
				tb.append("<td width=40>" + getBossIcon(bossName) + "</td>");
				tb.append("<td width=70 align=center>" + bossName + "</td>");
				tb.append("<td width=100 align=center><font color=\"32C332\">Is Alive</font></td>");
				tb.append("<tr><td width=50></td>");
				tb.append("</tr>");
			}
			else
			{
				int hours = (int) ((delay - System.currentTimeMillis()) / 1000 / 60 / 60);
				int mins = (int) (((delay - (hours * 60 * 60 * 1000)) - System.currentTimeMillis()) / 1000 / 60);
				tb.append("<tr><td width=5></td>");
				tb.append("<td width=40>" + getBossIcon(bossName) + "</td>");
				tb.append("<td width=70 align=center>" + bossName + "</td>");
				tb.append("<td width=100 align=center>respawn in: " + " " + "<font color=\"32C332\"> " + hours + " : " + mins + "</font></td>");
				tb.append("<tr><td width=50></td>");
				tb.append("</tr>");
			}
		}
		
		tb.append("</tr>");
		tb.append("</table>");
		tb.append("</center></body></html>");
		msg.setHtml(tb.toString());
		activeChar.sendPacket(msg);
		return true;
	}
	
	public static final String getBossIcon(String bossName)
	{
		Map<String, String> icons = Map.of("Queen Ant", "<img src=\"icon.accessory_ring_of_queen_ant_i00\" width=\"32\" height=\"32\">", "Core", "<img src=\"icon.accessory_ring_of_core_i00\" width=\"32\" height=\"32\">", "Orfen", "<img src=\"icon.accessory_earring_of_orfen_i00\" width=\"32\" height=\"32\">", "Baium", "<img src=\"icon.accessory_ring_of_baium_i00\" width=\"32\" height=\"32\">", "Valakas", "<img src=\"icon.accessory_necklace_of_valakas_i00\" width=\"32\" height=\"32\">", "Antharas", "<img src=\"icon.accessory_earring_of_antaras_i00\" width=\"32\" height=\"32\">", "Beleth", "<img src=\"icon.accessary_dynasty_ring_i00\" width=\"32\" height=\"32\">");
		
		return icons.getOrDefault(bossName, "Unknown Boss");
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
