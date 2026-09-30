package com.l2journey.gameserver.model.phantom;

import java.util.List;

import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

public class PhantomMenu
{
	private static final int PAGE_SIZE = 10;
	private static final String BUTTON_SKIN = " back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">";
	
	public static void showMenu(Player player)
	{
		showMenu(player, 0);
	}
	
	public static void showMenu(Player player, int page)
	{
		if ((player == null) || !player.isGM())
		{
			return;
		}
		
		List<Player> phantoms = PhantomEngine.activePhantoms;
		int totalPhantoms = phantoms.size();
		int maxPages = (totalPhantoms > 0) ? (totalPhantoms - 1) / PAGE_SIZE : 0;
		if (page < 0)
		{
			page = 0;
		}
		if (page > maxPages)
		{
			page = maxPages;
		}
		
		// Calcular Warriors e Magos (Trocamos 'p' por 'temp' para evitar conflito de nomes)
		int magesCount = 0;
		for (Player temp : phantoms)
		{
			if (temp.isMageClass())
			{
				magesCount++;
			}
		}
		int warriorsCount = totalPhantoms - magesCount;
		
		StringBuilder html = new StringBuilder();
		html.append("<html><title>Phantom Manager</title><body><center>");
		
		// --- CABECALHO ---
		html.append("<font color=\"LEVEL\">PHANTOM CONTROL PANEL</font><br1>");
		html.append("<img src=\"L2UI.SquareWhite\" width=260 height=1><br>");
		
		// --- ESTATISTICAS ---
		html.append("Online: <font color=\"00FF00\">").append(totalPhantoms).append("</font> | ");
		html.append("Page: <font color=\"LEVEL\">").append(page + 1).append("</font>/").append(maxPages + 1).append("</font><br>");
		
		// --- LOGS E CLASSES ---
		html.append("<table width=260><tr>");
		html.append("<td width=80>Logs: ").append(PhantomManager.isDebugMode() ? "<font color=\"00FF00\">ON</font>" : "<font color=\"FF5555\">OFF</font>").append("</td>");
		html.append("<td width=90>Warriors: <font color=\"LEVEL\">").append(warriorsCount).append("</font></td>");
		html.append("<td width=90>Wizards: <font color=\"LEVEL\">").append(magesCount).append("</font></td>");
		html.append("</tr></table>");
		html.append("<img src=\"L2UI.SquareGray\" width=260 height=1><br>");
		
		// --- ACOES DE SPAWN ---
		html.append("<table width=260 cellpadding=0 cellspacing=0><tr>");
		html.append("<td>").append(button("Start 10", "admin_phantom_start_10", 125, 22)).append("</td>");
		html.append("<td>").append(button("Start 60", "admin_phantom_start_60", 125, 22)).append("</td>");
		html.append("</tr><tr>");
		html.append("<td>").append(button("Create 10", "admin_phantom_create_10", 125, 22)).append("</td>");
		html.append("<td>").append(button("Create 60", "admin_phantom_create_60", 125, 22)).append("</td>");
		html.append("</tr></table>");
		
		// --- SISTEMA E CONTROLE (Botoes grandes) ---
		html.append("<table width=260 cellpadding=0 cellspacing=0><tr>");
		html.append("<td>").append(button("Stop 10", "admin_phantom_stop_10", 125, 22)).append("</td>");
		html.append("<td>").append(button("Stop All", "admin_phantom_stop_all", 125, 22)).append("</td>");
		html.append("</tr></table>");
		
		// --- OS 3 BOTOES COMPACTOS ---
		html.append("<table width=260 cellpadding=0 cellspacing=0><tr>");
		html.append("<td width=86 align=center>").append(button("Reload XML", "admin_phantom_reload_xml", 82, 22)).append("</td>");
		html.append("<td width=86 align=center>").append(button("Reload", "admin_phantom_pload", 82, 22)).append("</td>");
		html.append("<td width=88 align=center>").append(button("Logs ON/Off", "admin_phantom_debug", 82, 22)).append("</td>");
		html.append("</tr></table><br>");
		
		// --- LISTA DE BOTS ---
		html.append("<table width=260>");
		if (totalPhantoms == 0)
		{
			html.append("<tr><td><center>No active bots.</center></td></tr>");
		}
		else
		{
			int startIndex = page * PAGE_SIZE;
			int endIndex = Math.min(startIndex + PAGE_SIZE, totalPhantoms);
			
			for (int i = startIndex; i < endIndex; i++)
			{
				Player p = phantoms.get(i);
				
				// Formato de classe: primeira letra Maiuscula, resto minuscula
				String rawClass = p.getPlayerClass().name().toLowerCase().replace("_", " ");
				String className = rawClass.substring(0, 1).toUpperCase() + rawClass.substring(1);
				
				html.append("<tr>");
				html.append("<td width=130>").append(p.getName()).append(" Lv").append(p.getLevel()).append(" - ").append(className).append("</td>");
				html.append("<td width=30>").append(button("Go", "admin_phantom_go " + p.getName(), 25, 20)).append("</td>");
				html.append("<td width=45>").append(button("Bring", "admin_phantom_bring " + p.getName(), 40, 20)).append("</td>");
				html.append("<td width=35>").append(button("Kill", "admin_phantom_kill " + p.getName(), 30, 20)).append("</td>");
				html.append("</tr>");
			}
		}
		html.append("</table><br>");
		
		// --- NAVEGACAO ---
		html.append("<table width=200><tr>");
		html.append("<td>").append(button("<Back", "admin_phantom_menu " + (page - 1), 60, 22)).append("</td>");
		html.append("<td>").append(button("Refresh", "admin_phantom_menu " + page, 60, 22)).append("</td>");
		html.append("<td>").append(button("Next>", "admin_phantom_menu " + (page + 1), 60, 22)).append("</td>");
		html.append("</tr></table>");
		
		html.append("</center></body></html>");
		
		NpcHtmlMessage msg = new NpcHtmlMessage();
		msg.setHtml(html.toString());
		player.sendPacket(msg);
	}
	
	private static String button(String value, String action, int width, int height)
	{
		return "<button value=\"" + value + "\" action=\"bypass -h " + action + "\" width=" + width + " height=" + height + BUTTON_SKIN;
	}
}
