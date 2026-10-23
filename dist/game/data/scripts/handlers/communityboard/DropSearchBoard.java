/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.communityboard;

import static com.l2journey.gameserver.model.dropcalculator.DropInfoFunctions.escape;
import static com.l2journey.gameserver.model.dropcalculator.DropInfoFunctions.page;
import static com.l2journey.gameserver.model.dropcalculator.DropInfoFunctions.percent;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.l2journey.Config;
import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.data.SpawnTable;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.data.xml.NpcData;
import com.l2journey.gameserver.handler.CommunityBoardHandler;
import com.l2journey.gameserver.handler.IParseBoardHandler;
import com.l2journey.gameserver.model.Spawn;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.dropcalculator.DropInfoFunctions;
import com.l2journey.gameserver.model.dropcalculator.DropInfoFunctions.Page;
import com.l2journey.gameserver.model.dropcalculator.DropInfoHolder;
import com.l2journey.gameserver.model.dropcalculator.DropInfoHandler;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.stats.MoveType;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.RadarControl;
import com.l2journey.gameserver.network.serverpackets.ShowMiniMap;

/** Community Board UI for the shared drop index. @author yksdtc, vGodFather */
public class DropSearchBoard implements IParseBoardHandler
{
	private static final String[] COMMANDS = { "_bbsdrop", "_bbs_search_item", "_bbs_search_drop", "_bbs_npc_trace", "_bbssearchNpcDropList_", "_bbssearchShowSkills_", "_bbssearchdropMonstersByItem_" };
	private record Search(String kind, String query) { }
	// Session-only search context: never write calculator queries into character variables.
	private final Map<Player, Search> _searches = Collections.synchronizedMap(new WeakHashMap<>());
	private final DropInfoHandler _index;

	public DropSearchBoard() { this(DropInfoHandler.getInstance()); }
	public DropSearchBoard(DropInfoHandler index) { _index = index; }

	@Override
	public String[] getCommunityBoardCommands() { return COMMANDS; }

	private static String text(Player player, String en, String br) { return "br".equalsIgnoreCase(player.getLang()) ? br : en; }

	@Override
	public boolean parseCommunityBoardCommand(String command, Player player)
	{
		if (!Config.ENABLE_COMMUNITY_BOARD || !Config.COMMUNITYBOARD_ENABLED || !Config.COMMUNITYBOARD_DROP_SEARCH)
		{
			player.sendPacket(SystemMessageId.THE_COMMUNITY_SERVER_IS_CURRENTLY_OFFLINE);
			return false;
		}
		try
		{
			final String[] parts = normalize(command).split(";", 5);
			if (!parts[0].equals("_bbsdrop")) { return false; }
			final String body;
			if (parts.length == 1)
			{
				body = "<br>" + text(player, "Search by item or NPC name, partial name, or exact ID.", "Pesquise por nome de item ou NPC, parte do nome ou ID exato.");
			}
			else
			{
				body = switch (parts[1])
				{
					case "items", "npcs" -> search(player, parts);
					case "item" -> sources(player, parts);
					case "npc" -> npcDrops(player, parts);
					case "stats" -> stats(player, npc(integer(parts, 2, 3)));
					case "skills" -> skills(player, parts);
					case "map" -> { locate(player, npc(integer(parts, 2, 3))); yield null; }
					default -> throw new IllegalArgumentException("Unknown calculator action");
				};
			}
			if (body != null)
			{
				String html = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/dropsearch/main.html");
				if (html == null) { throw new IllegalStateException("Missing drop calculator template"); }
				html = html.replace("%calculator_body%", body);
				CommunityBoardHandler.getInstance().addBypass(player, text(player, "Drop Calculator", "Calculadora de Drops"), "_bbsdrop");
				CommunityBoardHandler.separateAndSend(html, player);
			}
			return true;
		}
		catch (IllegalArgumentException | IndexOutOfBoundsException | ArithmeticException e)
		{
			player.sendMessage(text(player, "Invalid calculator request or unknown item/NPC.", "Pedido inválido ou item/NPC desconhecido."));
			return false;
		}
	}

	private static int integer(String[] parts, int index, int length)
	{
		if (parts.length != length) { throw new IllegalArgumentException("Wrong parameter count"); }
		return Integer.parseInt(parts[index]);
	}

	private static String normalize(String command)
	{
		if (command.startsWith("_bbs_search_item ")) { return "_bbsdrop;items;1;0;" + command.substring(17); }
		if (command.equals("_bbs_search_item")) { return "_bbsdrop;items;1;0;"; }
		if (command.startsWith("_bbs_search_drop "))
		{
			final String[] p = command.split("\\s+");
			if (p.length < 3) { throw new IllegalArgumentException("Missing item/page"); }
			return "_bbsdrop;item;" + Integer.parseInt(p[1]) + ";" + Integer.parseInt(p[2]) + ";0";
		}
		if (command.startsWith("_bbs_npc_trace ")) { return "_bbsdrop;map;" + Integer.parseInt(command.substring(15)); }
		if (command.startsWith("_bbssearchdropMonstersByItem_"))
		{
			final String[] p = command.substring("_bbssearchdropMonstersByItem_".length()).split("_");
			if (p.length != 2) { throw new IllegalArgumentException(); }
			return "_bbsdrop;item;" + Integer.parseInt(p[0]) + ";" + Integer.parseInt(p[1]) + ";0";
		}
		if (command.startsWith("_bbssearchShowSkills_"))
		{
			final String[] p = command.substring("_bbssearchShowSkills_".length()).split("_");
			if (p.length != 2) { throw new IllegalArgumentException(); }
			return "_bbsdrop;skills;" + Integer.parseInt(p[0]) + ";" + Math.addExact(Integer.parseInt(p[1]), 1);
		}
		if (command.startsWith("_bbssearchNpcDropList_"))
		{
			final String[] p = command.substring("_bbssearchNpcDropList_".length()).split("_");
			if (p.length != 3) { throw new IllegalArgumentException(); }
			return "_bbsdrop;npc;" + Integer.parseInt(p[1]) + ";" + p[0] + ";" + Integer.parseInt(p[2]);
		}
		return command;
	}

	private String search(Player player, String[] parts)
	{
		if ((parts.length != 4) && (parts.length != 5)) { throw new IllegalArgumentException(); }
		final int requested = Integer.parseInt(parts[2]);
		final int sort = Integer.parseInt(parts[3]);
		if ((sort < 0) || (sort > 2)) { throw new IllegalArgumentException(); }
		final Search last = _searches.get(player);
		final String query = parts.length == 5 ? parts[4].trim() : (last != null && last.kind().equals(parts[1]) ? last.query() : "");
		if (query.length() > 64) { throw new IllegalArgumentException("Query too long"); }
		_searches.put(player, new Search(parts[1], query));
		final StringBuilder html = new StringBuilder("<br><font color=F2C75C>").append(escape(query)).append("</font><br>");
		for (int order = 0; order <= 2; order++)
		{
			final String name = order == 0 ? text(player, "Name", "Nome") : order == 1 ? (parts[1].equals("items") ? "Drop" : text(player, "Level", "Nível")) : (parts[1].equals("items") ? "Spoil" : "Drops");
			html.append(link(name, "_bbsdrop;" + parts[1] + ";1;" + order)).append(" &nbsp; ");
		}
		if (parts[1].equals("items"))
		{
			final Page<ItemTemplate> found = page(_index.searchItems(query, sort), requested, 10);
			html.append("<table width=700 cellpadding=3>");
			for (ItemTemplate item : found.entries())
			{
				final long drops = _index.getDrop(item.getId()).stream().filter(d -> !d.isSpoil()).count();
				final long spoils = _index.getDrop(item.getId()).size() - drops;
				html.append("<tr><td width=40><img src=\"").append(escape(item.getIcon() == null ? "icon.etc_question_mark_i00" : item.getIcon())).append("\" width=32 height=32></td><td width=480>");
				html.append(link(shortName(item.getName()) + " [" + item.getId() + "]", "_bbsdrop;item;" + item.getId() + ";1;0"));
				html.append("</td><td>Drop: ").append(drops).append(" / Spoil: ").append(spoils).append("</td></tr>");
			}
			html.append("</table>").append(navigation(found, "_bbsdrop;items;%d;" + sort, player));
		}
		else
		{
			final Page<NpcTemplate> found = page(_index.searchNpcs(query, sort), requested, 10);
			html.append("<table width=700 cellpadding=4>");
			for (NpcTemplate npc : found.entries())
			{
				html.append("<tr><td>").append(link(shortName(npc.getName()) + " [" + npc.getId() + "]", "_bbsdrop;npc;" + npc.getId() + ";ALL;1"));
				html.append("</td><td>Lv. ").append(npc.getLevel()).append("</td><td>").append(_index.getNpcDrops(npc.getId()).size()).append(" drops</td></tr>");
			}
			html.append("</table>").append(navigation(found, "_bbsdrop;npcs;%d;" + sort, player));
		}
		return html.toString();
	}

	private String sources(Player player, String[] parts)
	{
		final int id = integer(parts, 2, 5);
		final int requested = Integer.parseInt(parts[3]);
		final int sort = Integer.parseInt(parts[4]);
		final ItemTemplate item = ItemData.getInstance().getTemplate(id);
		if ((item == null) || (sort < 0) || (sort > 4)) { throw new IllegalArgumentException(); }
		final Comparator<DropInfoHolder> order = switch (sort)
		{
			case 1 -> Comparator.comparingInt((DropInfoHolder d) -> d.npc().getLevel()).reversed();
			case 2 -> Comparator.comparingDouble((DropInfoHolder d) -> DropInfoFunctions.estimate(d, player).chance()).reversed();
			case 3 -> Comparator.comparing(DropInfoHolder::isSpoil).reversed();
			case 4 -> Comparator.comparingLong((DropInfoHolder d) -> DropInfoFunctions.estimate(d, player).max()).reversed();
			default -> Comparator.comparing(d -> d.npc().getName(), String.CASE_INSENSITIVE_ORDER);
		};
		final Page<DropInfoHolder> found = page(_index.getDrop(id).stream().sorted(order.thenComparingInt(d -> d.npc().getId())).toList(), requested, 10);
		final StringBuilder html = new StringBuilder("<br><font color=F2C75C>").append(escape(item.getName())).append(" [").append(id).append("]</font><br>");
		final String[] labels = { text(player, "Name", "Nome"), text(player, "Level", "Nível"), text(player, "Chance", "Chance"), text(player, "Type", "Tipo"), text(player, "Amount", "Quantidade") };
		for (int i = 0; i < labels.length; i++) { html.append(link(labels[i], "_bbsdrop;item;" + id + ";1;" + i)).append(" &nbsp; "); }
		html.append(dropHeader(player));
		for (DropInfoHolder drop : found.entries())
		{
			html.append(dropRow(player, drop, link(shortName(drop.npc().getName()) + " (Lv. " + drop.npc().getLevel() + ")", "_bbsdrop;npc;" + drop.npc().getId() + ";ALL;1")));
		}
		return html.append("</table>").append(navigation(found, "_bbsdrop;item;" + id + ";%d;" + sort, player)).append(note(player)).toString();
	}

	private String npcDrops(Player player, String[] parts)
	{
		if (parts.length != 5) { throw new IllegalArgumentException(); }
		final NpcTemplate npc = npc(Integer.parseInt(parts[2]));
		final String type = parts[3];
		if (!List.of("ALL", "DEATH", "DROP", "CORPSE", "SPOIL").contains(type)) { throw new IllegalArgumentException(); }
		final Page<DropInfoHolder> found = page(_index.getNpcDrops(npc.getId()).stream().filter(d -> type.equals("ALL") || (d.isSpoil() == type.equals("SPOIL"))).toList(), Integer.parseInt(parts[4]), 10);
		final StringBuilder html = new StringBuilder(npcMenu(npc, player)).append(dropHeader(player));
		for (DropInfoHolder drop : found.entries())
		{
			final ItemTemplate item = ItemData.getInstance().getTemplate(drop.itemId());
			if (item != null) { html.append(dropRow(player, drop, link(shortName(item.getName()), "_bbsdrop;item;" + item.getId() + ";1;0"))); }
		}
		return html.append("</table>").append(navigation(found, "_bbsdrop;npc;" + npc.getId() + ";" + type + ";%d", player)).append(note(player)).toString();
	}

	private static String dropHeader(Player player)
	{
		return "<br><table width=700 cellpadding=3 cellspacing=1><tr bgcolor=2A2418><td width=270>" + text(player, "Item / NPC", "Item / NPC") + "</td><td width=80>Drop/Spoil</td><td width=90>" + text(player, "Amount", "Quantidade") + "</td><td width=100>" + text(player, "Base", "Base") + "</td><td width=110>" + text(player, "Your rates", "Suas taxas") + "</td></tr>";
	}

	private static String dropRow(Player player, DropInfoHolder drop, String name)
	{
		final var adjusted = DropInfoFunctions.estimate(drop, player);
		return "<tr bgcolor=161616><td>" + name + "</td><td>" + (drop.isSpoil() ? "Spoil" : "Drop") + (drop.group() > 0 ? " G" + drop.group() : "") + "</td><td>" + adjusted.min() + "–" + adjusted.max() + "</td><td>" + percent(drop.chance()) + "</td><td>" + percent(adjusted.chance()) + "</td></tr>";
	}

	private static String note(Player player)
	{
		return "<br><font color=AE9977>" + text(player, "Nominal chance per roll: server, premium and player bonuses. Excludes level penalties, champions and roll limits. Each group/row is separate; herbs are omitted.", "Chance nominal por sorteio: taxas do servidor, premium e bônus do jogador. Não inclui penalidades de nível, champions e limites de sorteios. Cada grupo/linha é separado; ervas são omitidas.") + "</font>";
	}

	private static NpcTemplate npc(int id)
	{
		final NpcTemplate npc = NpcData.getInstance().getTemplate(id);
		if (npc == null) { throw new IllegalArgumentException("Unknown NPC"); }
		return npc;
	}

	private static String npcMenu(NpcTemplate npc, Player player)
	{
		final String base = "_bbsdrop;npc;" + npc.getId();
		return "<br><font color=F2C75C>" + escape(npc.getName()) + " [" + npc.getId() + "] Lv. " + npc.getLevel() + "</font><br>"
			+ link("Drop", base + ";DEATH;1") + " &nbsp; " + link("Spoil", base + ";SPOIL;1") + " &nbsp; "
			+ link(text(player, "Stats", "Atributos"), "_bbsdrop;stats;" + npc.getId()) + " &nbsp; " + link("Skills", "_bbsdrop;skills;" + npc.getId() + ";1") + " &nbsp; " + link(text(player, "Map", "Mapa"), "_bbsdrop;map;" + npc.getId());
	}

	private static String stats(Player player, NpcTemplate npc)
	{
		final StringBuilder html = new StringBuilder(npcMenu(npc, player)).append("<br><table width=650 cellpadding=4>");
		final String[][] values = {
			{ "HP / MP", npc.getBaseHpMax() + " / " + npc.getBaseMpMax() },
			{ "P.Atk / M.Atk", npc.getBasePAtk() + " / " + npc.getBaseMAtk() },
			{ "P.Def / M.Def", npc.getBasePDef() + " / " + npc.getBaseMDef() },
			{ "Atk.Spd / Cast.Spd", npc.getBasePAtkSpd() + " / " + npc.getBaseMAtkSpd() },
			{ "Critical / Run speed", npc.getBaseCritRate() + " / " + npc.getBaseMoveSpeed(MoveType.RUN) },
			{ "STR / DEX / CON", npc.getBaseSTR() + " / " + npc.getBaseDEX() + " / " + npc.getBaseCON() },
			{ "INT / WIT / MEN", npc.getBaseINT() + " / " + npc.getBaseWIT() + " / " + npc.getBaseMEN() },
			{ "Fire / Water / Wind", npc.getBaseFireRes() + " / " + npc.getBaseWaterRes() + " / " + npc.getBaseWindRes() },
			{ "Earth / Holy / Dark", npc.getBaseEarthRes() + " / " + npc.getBaseHolyRes() + " / " + npc.getBaseDarkRes() }
		};
		for (String[] value : values) { html.append("<tr><td>").append(value[0]).append("</td><td>").append(value[1]).append("</td></tr>"); }
		return html.append("</table>").toString();
	}

	private static String skills(Player player, String[] parts)
	{
		final NpcTemplate npc = npc(integer(parts, 2, 4));
		final Page<Skill> found = page(npc.getSkills().values().stream().sorted(Comparator.comparingInt(Skill::getId).thenComparingInt(Skill::getLevel)).toList(), Integer.parseInt(parts[3]), 10);
		final StringBuilder html = new StringBuilder(npcMenu(npc, player)).append("<br><table width=650 cellpadding=4>");
		for (Skill skill : found.entries()) { html.append("<tr><td>").append(escape(shortName(skill.getName()))).append("</td><td>").append(skill.getId()).append(" / Lv. ").append(skill.getLevel()).append("</td></tr>"); }
		return html.append("</table>").append(navigation(found, "_bbsdrop;skills;" + npc.getId() + ";%d", player)).toString();
	}

	private static void locate(Player player, NpcTemplate npc)
	{
		final Spawn spawn = SpawnTable.getInstance().getAnySpawn(npc.getId());
		if (spawn == null)
		{
			player.sendMessage(text(player, "No public spawn is available (event, boss or instance NPC).", "Não há spawn público disponível (NPC de evento, boss ou instância)."));
			return;
		}
		player.sendPacket(new ShowMiniMap(-1));
		player.getRadar().addMarker(spawn.getX(), spawn.getY(), spawn.getZ());
		player.sendPacket(new RadarControl(0, 2, spawn.getX(), spawn.getY(), spawn.getZ()));
	}

	private static String shortName(String name) { return name.length() <= 60 ? name : name.substring(0, 57) + "..."; }
	private static String link(String label, String command) { return "<a action=\"bypass " + command + "\">" + escape(label) + "</a>"; }
	private static String navigation(Page<?> page, String command, Player player)
	{
		final StringBuilder html = new StringBuilder("<br>");
		if (page.count() == 0) { html.append(text(player, "No matches.", "Nenhum resultado.")).append("<br>"); }
		if (page.number() > 1) { html.append(link("«", String.format(java.util.Locale.ROOT, command, page.number() - 1))).append(" &nbsp; "); }
		html.append(text(player, "Page ", "Página ")).append(page.number()).append('/').append(page.total()).append(" (").append(page.count()).append(')');
		if (page.number() < page.total()) { html.append(" &nbsp; ").append(link("»", String.format(java.util.Locale.ROOT, command, page.number() + 1))); }
		return html.toString();
	}
}
