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
 * Portions of this software are derived from the L2JMobius Project,
 * shared under the MIT License. The original license terms are preserved where
 * applicable.
 */
package ai.npc.Achievements;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.l2journey.Config;
import com.l2journey.gameserver.data.sql.ClanHallTable;
import com.l2journey.gameserver.managers.CHSiegeManager;
import com.l2journey.gameserver.model.achievements.PlayerAchievements;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.residences.AuctionableHall;
import com.l2journey.gameserver.model.variables.PlayerVariables;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2journey.gameserver.util.FormatUtil;

import ai.AbstractNpcAI;

/**
 * Achievement NPC - Shows player achievements based on adena possession
 * @author KingHanker
 */
public class AchievementNpc extends AbstractNpcAI
{
	private static final Logger LOGGER = Logger.getLogger(AchievementNpc.class.getName());
	
	private static final int ACHIEVEMENT_NPC_ID = 70000;
	private static final String ACHIEVEMENTS_FILE = "data/Achievements.xml";
	private static final boolean DEBUG_MINIMAL_HTML = false;
	private static final String DEFAULT_ICON = "icon.skill0000";
	private static final boolean SHOW_ICONS = true;
	private static final boolean SHOW_PROGRESS_BAR = false;
	private static final boolean MULTI_MODE = true;
	private static final int LEVEL_ID_FACTOR = 1000;
	
	private static final List<Achievement> ACHIEVEMENTS = new ArrayList<>();
	
	private static final Map<Integer, Category> CATEGORIES = new HashMap<>();
	private static final Map<Integer, BaseAchievement> BASE_ACHIEVEMENTS = new HashMap<>();
	private static final Map<Integer, LevelEntry> LEVELS_BY_PHYSICAL_ID = new HashMap<>();
	
	public static class Category
	{
		public final int id;
		public final String name;
		public final String desc;
		public final String icon;
		public final List<BaseAchievement> achievements = new ArrayList<>();
		
		public Category(int id, String name, String desc, String icon)
		{
			this.id = id;
			this.name = name;
			this.desc = desc == null ? "" : desc;
			this.icon = ((icon == null) || icon.isEmpty()) ? DEFAULT_ICON : icon;
		}
	}
	
	public static class BaseAchievement
	{
		public final int baseId;
		public final int categoryId;
		public final String type;
		public final String descTemplate;
		public final List<LevelEntry> levels = new ArrayList<>();
		public final List<RewardItem> globalRewards = new ArrayList<>();
		
		public BaseAchievement(int baseId, int categoryId, String type, String descTemplate)
		{
			this.baseId = baseId;
			this.categoryId = categoryId;
			this.type = type.toLowerCase();
			this.descTemplate = (descTemplate == null) ? "" : descTemplate;
		}
	}
	
	public static class LevelEntry
	{
		public final int levelId;
		public final int physicalId;
		public final long need;
		public final String name;
		public final String icon;
		public final List<RewardItem> rewards = new ArrayList<>();
		
		public LevelEntry(int levelId, int physicalId, long need, String name, String icon)
		{
			this.levelId = levelId;
			this.physicalId = physicalId;
			this.need = need;
			this.name = name == null ? ("Lv." + levelId) : name;
			this.icon = ((icon == null) || icon.isEmpty()) ? DEFAULT_ICON : icon;
		}
	}
	
	public static class RewardItem
	{
		public final int itemId;
		public final long count;
		
		public RewardItem(int itemId, long count)
		{
			this.itemId = itemId;
			this.count = count;
		}
	}
	
	public static class Achievement
	{
		public final int id;
		public final String name;
		public final String description;
		public final long target;
		public final String type;
		public String icon;
		public final List<RewardItem> rewards = new ArrayList<>();
		
		public Achievement(int id, String name, String description, long target, String type, String icon)
		{
			this.id = id;
			this.name = name;
			this.description = description;
			this.target = target;
			this.type = (type == null) ? "adena" : type.toLowerCase();
			this.icon = ((icon == null) || icon.isEmpty()) ? DEFAULT_ICON : icon;
		}
	}
	
	public AchievementNpc()
	{
		addStartNpc(ACHIEVEMENT_NPC_ID);
		addFirstTalkId(ACHIEVEMENT_NPC_ID);
		addTalkId(ACHIEVEMENT_NPC_ID);
		loadAchievements();
	}
	
	/**
	 * Reload achievements in runtime. Only for Admin (ex: GM)
	 */
	public synchronized void reloadAchievements()
	{
		loadAchievements();
	}
	
	private void loadAchievements()
	{
		ACHIEVEMENTS.clear();
		CATEGORIES.clear();
		BASE_ACHIEVEMENTS.clear();
		LEVELS_BY_PHYSICAL_ID.clear();
		try
		{
			final File f = new File(Config.DATAPACK_ROOT, ACHIEVEMENTS_FILE);
			LOGGER.log(Level.INFO, "Achievements: Loaded");
			if (!f.exists())
			{
				LOGGER.log(Level.WARNING, () -> getClass().getSimpleName() + ": " + ACHIEVEMENTS_FILE + " not found: " + f.getAbsolutePath());
				return;
			}
			
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setIgnoringComments(true);
			final DocumentBuilder builder = factory.newDocumentBuilder();
			final Document doc = builder.parse(f);
			final Node root = doc.getDocumentElement();
			if ((root == null) || !"list".equalsIgnoreCase(root.getNodeName()))
			{
				LOGGER.log(Level.WARNING, "AchievementNpc: unexpected root value.");
				return;
			}
			
			final Set<Integer> parsedBases = new HashSet<>();
			traverseAndParse(root, parsedBases);
			LOGGER.log(Level.INFO, () -> getClass().getSimpleName() + ": Categories: " + CATEGORIES.size() + ", Achievements: " + BASE_ACHIEVEMENTS.size());
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error loading achievements.", e);
		}
	}
	
	/**
	 * Recursively traverses the DOM tree looking for <cat> and <achievement> elements at any depth.
	 * @param node Root node to start scanning from.
	 * @param parsedBases set of already processed base achievement IDs to avoid duplication.
	 */
	private void traverseAndParse(Node node, java.util.Set<Integer> parsedBases)
	{
		for (Node n = node.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (n.getNodeType() != Node.ELEMENT_NODE)
			{
				continue;
			}
			final String nn = n.getNodeName();
			if ("cat".equalsIgnoreCase(nn))
			{
				int id = parseInt(n, "id", -1);
				if ((id >= 0) && !CATEGORIES.containsKey(id))
				{
					String cname = parseString(n, "name", "Cat " + id);
					String desc = parseString(n, "desc", "");
					String icon = parseString(n, "icon", DEFAULT_ICON);
					CATEGORIES.put(id, new Category(id, cname, desc, icon));
				}
			}
			else if ("achievement".equalsIgnoreCase(nn))
			{
				if (!MULTI_MODE)
				{
					parseLegacyAchievement(n);
				}
				else
				{
					int baseId = parseInt(n, "id", -1);
					if ((baseId >= 0) && parsedBases.contains(baseId))
					{
						LOGGER.log(Level.WARNING, () -> "Duplicate achievement: id=" + baseId + " ignored.");
					}
					else
					{
						parseMultiAchievement(n);
						parsedBases.add(baseId);
					}
				}
			}
			
			traverseAndParse(n, parsedBases);
		}
	}
	
	private void parseLegacyAchievement(Node n)
	{
		int id = parseInt(n, "id", -1);
		String name = parseString(n, "name", "Unnamed");
		String desc = parseString(n, "desc", parseString(n, "description", ""));
		long target = parseLong(n, "target", 0L);
		String type = parseString(n, "type", "adena");
		String icon = parseString(n, "icon", DEFAULT_ICON);
		Achievement a = new Achievement(id, name, desc, target, type, icon);
		for (Node r = n.getFirstChild(); r != null; r = r.getNextSibling())
		{
			if (r.getNodeType() != Node.ELEMENT_NODE)
			{
				continue;
			}
			final String nodeName = r.getNodeName();
			if ("reward".equalsIgnoreCase(nodeName))
			{
				int itemId = parseInt(r, "id", 0);
				long count = parseLong(r, "count", 0L);
				if ((itemId > 0) && (count > 0))
				{
					a.rewards.add(new RewardItem(itemId, count));
				}
				for (Node c = r.getFirstChild(); c != null; c = c.getNextSibling())
				{
					if ((c.getNodeType() == Node.ELEMENT_NODE) && "item".equalsIgnoreCase(c.getNodeName()))
					{
						int ci = parseInt(c, "id", 0);
						long cc = parseLong(c, "count", 0L);
						if ((ci > 0) && (cc > 0))
						{
							a.rewards.add(new RewardItem(ci, cc));
						}
					}
				}
			}
			else if ("item".equalsIgnoreCase(nodeName))
			{
				int itemId = parseInt(r, "id", 0);
				long count = parseLong(r, "count", 0L);
				if ((itemId > 0) && (count > 0))
				{
					a.rewards.add(new RewardItem(itemId, count));
				}
			}
		}
		
		ACHIEVEMENTS.add(a);
	}
	
	private void parseMultiAchievement(Node n)
	{
		int baseId = parseInt(n, "id", -1);
		int catId = parseInt(n, "cat", -1);
		String type = parseString(n, "type", "adena");
		String desc = parseString(n, "desc", "");
		if ((baseId < 0) || (catId < 0))
		{
			LOGGER.log(Level.WARNING, () -> "Base achievement ignored (invalid id or category): id=" + baseId + ", cat=" + catId);
			return;
		}
		
		Category cat = CATEGORIES.get(catId);
		if (cat == null)
		{
			cat = new Category(catId, "Categoria " + catId, "", DEFAULT_ICON);
			CATEGORIES.put(catId, cat);
		}
		BaseAchievement base = new BaseAchievement(baseId, catId, type, desc);
		
		for (Node c = n.getFirstChild(); c != null; c = c.getNextSibling())
		{
			if (c.getNodeType() != Node.ELEMENT_NODE)
			{
				continue;
			}
			if ("levels".equalsIgnoreCase(c.getNodeName()))
			{
				for (Node l = c.getFirstChild(); l != null; l = l.getNextSibling())
				{
					if ((l.getNodeType() == Node.ELEMENT_NODE) && "lvl".equalsIgnoreCase(l.getNodeName()))
					{
						int levelId = parseInt(l, "id", -1);
						long need = parseLong(l, "need", -1L);
						String lname = parseString(l, "name", null);
						String licon = parseString(l, "icon", null);
						if ((levelId < 0) || (need < 0))
						{
							LOGGER.log(Level.WARNING, () -> "Invalid level in base achievement=" + baseId + " level=" + levelId);
							continue;
						}
						int physicalId = (baseId * LEVEL_ID_FACTOR) + levelId;
						if (LEVELS_BY_PHYSICAL_ID.containsKey(physicalId))
						{
							LOGGER.log(Level.SEVERE, () -> "physicalId collision=" + physicalId + " (base achievement " + baseId + ")");
							continue;
						}
						LevelEntry le = new LevelEntry(levelId, physicalId, need, lname, licon);
						// Rewards dentro de <lvl><rewards>
						for (Node lr = l.getFirstChild(); lr != null; lr = lr.getNextSibling())
						{
							if (lr.getNodeType() != Node.ELEMENT_NODE)
							{
								continue;
							}
							if ("rewards".equalsIgnoreCase(lr.getNodeName()))
							{
								for (Node it = lr.getFirstChild(); it != null; it = it.getNextSibling())
								{
									if ((it.getNodeType() == Node.ELEMENT_NODE) && "item".equalsIgnoreCase(it.getNodeName()))
									{
										int iid = parseInt(it, "id", 0);
										long cnt = parseLong(it, "count", 0L);
										if ((iid > 0) && (cnt > 0))
										{
											le.rewards.add(new RewardItem(iid, cnt));
										}
									}
								}
							}
						}
						
						base.levels.add(le);
						LEVELS_BY_PHYSICAL_ID.put(physicalId, le);
					}
				}
			}
			else if ("rewards".equalsIgnoreCase(c.getNodeName()))
			{
				for (Node it = c.getFirstChild(); it != null; it = it.getNextSibling())
				{
					if ((it.getNodeType() == Node.ELEMENT_NODE) && "item".equalsIgnoreCase(it.getNodeName()))
					{
						int iid = parseInt(it, "id", 0);
						long cnt = parseLong(it, "count", 0L);
						if ((iid > 0) && (cnt > 0))
						{
							base.globalRewards.add(new RewardItem(iid, cnt));
						}
					}
				}
			}
		}
		
		if (base.levels.isEmpty())
		{
			LOGGER.log(Level.WARNING, () -> "Base achievement without levels: " + baseId);
			return;
		}
		
		Collections.sort(base.levels, Comparator.comparingInt(l -> l.levelId));
		BASE_ACHIEVEMENTS.put(baseId, base);
		cat.achievements.add(base);
	}
	
	private int parseInt(Node n, String att, int def)
	{
		Node a = n.getAttributes().getNamedItem(att);
		if (a == null)
		{
			return def;
		}
		try
		{
			return Integer.parseInt(a.getNodeValue());
		}
		catch (Exception _)
		{
			return def;
		}
	}
	
	private long parseLong(Node n, String att, long def)
	{
		Node a = n.getAttributes().getNamedItem(att);
		if (a == null)
		{
			return def;
		}
		try
		{
			return Long.parseLong(a.getNodeValue());
		}
		catch (Exception _)
		{
			return def;
		}
	}
	
	private String parseString(Node n, String att, String def)
	{
		Node a = n.getAttributes().getNamedItem(att);
		return a == null ? def : a.getNodeValue();
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (player == null)
		{
			return null;
		}
		
		if (MULTI_MODE)
		{
			if ("back_categories".equalsIgnoreCase(event))
			{
				return showCategoriesUI(player);
			}
			if (event.startsWith("cat_"))
			{
				try
				{
					int catId = Integer.parseInt(event.substring(4));
					return showCategoryAchievements(player, catId);
				}
				catch (Exception _)
				{
					player.sendMessage("Invalid category.");
				}
			}
			if (event.startsWith("ach_"))
			{
				try
				{
					int baseId = Integer.parseInt(event.substring(4));
					return showAchievementDetail(player, baseId);
				}
				catch (Exception _)
				{
					player.sendMessage("Invalid achievement.");
				}
			}
			if (event.startsWith("claim_"))
			{
				try
				{
					int physicalId = Integer.parseInt(event.substring(6));
					return claimLevel(player, physicalId);
				}
				catch (Exception _)
				{
					player.sendMessage("Invalid ID.");
				}
			}
			if (event.startsWith("back_cat_"))
			{
				try
				{
					int catId = Integer.parseInt(event.substring(9));
					return showCategoryAchievements(player, catId);
				}
				catch (Exception _)
				{
					return showCategoriesUI(player);
				}
			}
			// Reload (GM only): "reload" ou "reload_all"
			if ("reload".equalsIgnoreCase(event) || "reload_all".equalsIgnoreCase(event))
			{
				if (player.isGM())
				{
					reloadAchievements();
					player.sendMessage("[Achievements] Reloaded.");
					return showCategoriesUI(player);
				}
				player.sendMessage("You don't have permission to reload achievements.");
				return null;
			}
			
			return null;
		}
		
		if (event.startsWith("claim_"))
		{
			try
			{
				int id = Integer.parseInt(event.substring(6));
				return claimAchievement(player, id);
			}
			catch (NumberFormatException _)
			{
				player.sendMessage("Invalid achievement id.");
				return showAchievementList(player);
			}
		}
		
		return null;
	}
	
	@Override
	public String onTalk(Npc npc, Player player)
	{
		if (MULTI_MODE && !CATEGORIES.isEmpty())
		{
			return showCategoriesUI(player);
		}
		
		return showAchievementList(player); // fallback
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (MULTI_MODE && !CATEGORIES.isEmpty())
		{
			return showCategoriesUI(player);
		}
		
		return showAchievementList(player); // fallback
	}
	
	public String showCategoriesUI(Player player)
	{
		String template = loadTemplate("data/html/achievements/categories.htm");
		List<Category> cats = new ArrayList<>(CATEGORIES.values());
		Collections.sort(cats, Comparator.comparingInt(c -> c.id));
		StringBuilder catRows = new StringBuilder();
		for (Category c : cats)
		{
			double sum = 0d;
			for (BaseAchievement ba : c.achievements)
			{
				sum += computeOverallPercent(player, ba);
			}
			double catPercent = c.achievements.isEmpty() ? 0d : (sum / c.achievements.size());
			catRows.append("<table width=205 height=60 cellspacing=0 cellpadding=7><tr>");
			catRows.append("<td valign=top><table width=32 cellspacing=0 cellpadding=0>");
			catRows.append("<tr><td><img src=\"" + c.icon + "\" width=32 height=32 align=top></td></tr>");
			catRows.append("<tr><td height=14></td></tr>");
			catRows.append("<tr><td>" + renderGaugeBarCompact24(catPercent) + "</td></tr>");
			catRows.append("</table></td>");
			catRows.append("<td valign=top><table width=164 cellspacing=0 cellpadding=0>");
			catRows.append("<tr><td height=24 valign=top><font color=af9f47>" + escape(c.name) + "</font></td></tr>");
			catRows.append("<tr><td><font color=999999>" + escape(c.desc) + "</font></td></tr>");
			catRows.append("</table></td>");
			catRows.append("<td valign=top><table width=32 cellspacing=0 cellpadding=0><tr>");
			catRows.append("<td valign=top><button value=\"\" action=\"bypass -h Quest AchievementNpc cat_" + c.id + "\" width=32 height=32 back=\"L2UI_CT1.MiniMap_DF_PlusBtn_Blue_Over\" fore=\"L2UI_CT1.MiniMap_DF_PlusBtn_Blue\"></td>");
			catRows.append("</tr></table></td>");
			catRows.append("</tr></table>");
		}
		template = template.replace("%categories%", catRows.toString());
		String gmBtn = "";
		if (player.isGM())
		{
			gmBtn = "<br><center><button value=\"Reload Achievements\" action=\"bypass -h Quest AchievementNpc reload\" width=140 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></center>";
		}
		template = template.replace("%gm_reload_button%", gmBtn);
		NpcHtmlMessage msg = new NpcHtmlMessage(player.getObjectId());
		msg.setHtml(template);
		player.sendPacket(msg);
		return null;
	}
	
	private String showCategoryAchievements(Player player, int categoryId)
	{
		Category cat = CATEGORIES.get(categoryId);
		if (cat == null)
		{
			player.sendMessage("Category not found.");
			return showCategoriesUI(player);
		}
		String template = loadTemplate("data/html/achievements/category_achievements.htm");
		
		double sum = 0d;
		for (BaseAchievement ba : cat.achievements)
		{
			sum += computeOverallPercent(player, ba);
		}
		double catPercent = cat.achievements.isEmpty() ? 0d : (sum / cat.achievements.size());
		int inner = 256;
		int filled = (int) Math.round((catPercent / 100d) * inner);
		if (filled < 0)
		{
			filled = 0;
		}
		if (filled > inner)
		{
			filled = inner;
		}
		
		int remaining = inner - filled;
		String caps1Short = (filled > 0) ? "Gauge_DF_Large_MP_Left" : "Gauge_DF_Large_Exp_bg_Left";
		String caps2Short = (filled >= inner) ? "Gauge_DF_Large_MP_Right" : "Gauge_DF_Large_Exp_bg_Right";
		template = template.replace("%caps1%", caps1Short).replace("%caps2%", caps2Short).replace("%bar1up%", String.valueOf(filled)).replace("%bar2up%", String.valueOf(remaining)).replace("%catname%", escape(cat.name)).replace("%catDesc%", escape(cat.desc)).replace("%catIcon%", cat.icon);
		
		StringBuilder activeRows = new StringBuilder();
		// Preserve the XML-defined order: do not sort here.
		List<BaseAchievement> list = new ArrayList<>(cat.achievements);
		for (BaseAchievement ba : list)
		{
			LevelEntry next = getNextPendingLevel(player, ba);
			if (next == null)
			{
				continue;
			}
			long currentValue = getCurrentValue(player, ba.type);
			long prevNeed = 0L;
			for (LevelEntry prev : ba.levels)
			{
				if (prev.levelId == next.levelId)
				{
					break;
				}
				
				prevNeed = prev.need;
			}
			
			double segmentPercent;
			if (next.need <= prevNeed)
			{
				segmentPercent = (currentValue >= next.need) ? 100d : 0d;
			}
			else if (currentValue >= next.need)
			{
				segmentPercent = 100d;
			}
			else if (currentValue <= prevNeed)
			{
				segmentPercent = 0d;
			}
			else
			{
				final long span = next.need - prevNeed;
				final long doneSeg = currentValue - prevNeed;
				segmentPercent = (span <= 0) ? 0d : ((doneSeg * 100) / span);
			}
			
			if (!Double.isFinite(segmentPercent) || (segmentPercent < 0d))
			{
				segmentPercent = 0d;
			}
			else if (segmentPercent > 100d)
			{
				segmentPercent = 100d;
			}
			String desc = applyPlaceholders(player, ba, currentValue);
			StringBuilder row = new StringBuilder();
			row.append("<table width=205 height=60 cellspacing=0 cellpadding=7><tr>");
			row.append("<td valign=top><table width=32 cellspacing=0 cellpadding=0>");
			row.append("<tr><td><img src=\"" + next.icon + "\" width=32 height=32 align=top></td></tr>");
			row.append("<tr><td height=14></td></tr>");
			row.append("<tr><td>" + renderGaugeBarCompact24(segmentPercent) + "</td></tr>");
			row.append("</table></td>");
			row.append("<td valign=top><table width=164 cellspacing=0 cellpadding=0>");
			// If this achievement has only one level (level 1), don't show the level suffix in the title
			{
				int totalLevels = ba.levels.size();
				boolean showLevelSuffix = !((totalLevels == 1) && (next.levelId == 1));
				String title = escape(next.name) + (showLevelSuffix ? (" (Lv." + next.levelId + ")") : "");
				row.append("<tr><td height=24 valign=top><font color=af9f47>" + title + "</font></td></tr>");
			}
			row.append("<tr><td><font color=999999>" + escape(desc) + "</font></td></tr>");
			row.append("</table></td>");
			
			row.append("<td width=32 align=center valign=middle>");
			boolean completedSegment = (segmentPercent >= 100d) && !player.getAchievements().isClaimed(next.physicalId);
			if (completedSegment)
			{
				row.append("<table border=0 cellspacing=0 cellpadding=0 width=32 height=32 background=\"branchSys.br_lucky_bag_box_i00\"><tr><td width=32 height=32 align=center>" + "<button action=\"bypass -h Quest AchievementNpc claim_" + next.physicalId + "\" width=34 height=34 back=\"L2UI_CT1.ItemWindow_DF_Frame_Down\" fore=\"L2UI_CT1.ItemWindow_DF_Frame\"/>" + "</td></tr></table>");
			}
			else
			{
				row.append("<img src=\"branchSys.PremiumItemBtn_disable\" width=32 height=32>");
			}
			
			row.append("</td>");
			row.append("</tr></table>");
			activeRows.append(row);
		}
		if (activeRows.isEmpty())
		{
			activeRows.append("<center><font color=55FF55>All achievements in this category completed.</font></center>");
		}
		
		template = template.replace("%achivmentsNotDone%", activeRows.toString()).replace("%achivmentsDone%", "");
		NpcHtmlMessage msg = new NpcHtmlMessage(player.getObjectId());
		msg.setHtml(template);
		player.sendPacket(msg);
		return null;
	}
	
	private String showAchievementDetail(Player player, int baseId)
	{
		BaseAchievement ba = BASE_ACHIEVEMENTS.get(baseId);
		if (ba == null)
		{
			player.sendMessage("Non-existent achievement.");
			return showCategoriesUI(player);
		}
		
		Category cat = CATEGORIES.get(ba.categoryId);
		if (cat == null)
		{
			// Category was removed or not loaded properly; avoid NPE and return to main screen
			LOGGER.warning("[Achievements] Non-existent category for baseId=" + baseId + " categoryId=" + ba.categoryId);
			player.sendMessage("Missing category for this achievement.");
			return showCategoriesUI(player);
		}
		
		long currentValue = getCurrentValue(player, ba.type);
		String template = loadTemplate("data/html/achievements/achievement_detail.htm");
		String desc = applyPlaceholders(player, ba, currentValue);
		StringBuilder levelRows = new StringBuilder();
		for (LevelEntry le : ba.levels)
		{
			boolean completed = currentValue >= le.need;
			boolean claimed = player.getAchievements().isClaimed(le.physicalId);
			long prevNeed = 0L;
			for (LevelEntry prev : ba.levels)
			{
				if (prev.levelId == le.levelId)
				{
					break;
				}
				prevNeed = prev.need;
			}
			
			double segmentPercent;
			if (le.need <= prevNeed)
			{
				segmentPercent = (currentValue >= le.need) ? 100d : 0d;
			}
			else if (currentValue >= le.need)
			{
				segmentPercent = 100d;
			}
			else if (currentValue <= prevNeed)
			{
				segmentPercent = 0d;
			}
			else
			{
				final long span = le.need - prevNeed;
				final long done = currentValue - prevNeed;
				segmentPercent = (span <= 0) ? 0d : ((done * 100) / span);
			}
			
			if (!Double.isFinite(segmentPercent) || (segmentPercent < 0d))
			{
				segmentPercent = 0d;
			}
			else if (segmentPercent > 100d)
			{
				segmentPercent = 100d;
			}
			
			StringBuilder r = new StringBuilder();
			r.append("<table width=205 height=60 cellspacing=0 cellpadding=7><tr>");
			r.append("<td valign=top><table width=32 cellspacing=0 cellpadding=0>");
			r.append("<tr><td><img src=\"" + le.icon + "\" width=32 height=32 align=top></td></tr>");
			r.append("<tr><td height=14></td></tr>");
			r.append("<tr><td>" + renderGaugeBarCompact24(segmentPercent) + "</td></tr>");
			r.append("</table></td>");
			r.append("<td valign=top><table width=140 cellspacing=0 cellpadding=0>");
			// If the base achievement has only one level (level 1), don't show the level suffix here either
			{
				int totalLevels = ba.levels.size();
				boolean showLevelSuffix = !((totalLevels == 1) && (le.levelId == 1));
				String title = escape(le.name) + (showLevelSuffix ? (" (Lv." + le.levelId + ")") : "");
				r.append("<tr><td height=24 valign=top><font color=af9f47>" + title + "</font></td></tr>");
			}
			if (completed && !claimed)
			{
				r.append("<tr><td><font color=55FF55>Ready to claim</font></td></tr>");
			}
			else if (claimed)
			{
				r.append("<tr><td><font color=00FF00>Claimed</font></td></tr>");
			}
			else
			{
				r.append("<tr><td><font color=999999>Need: " + le.need + "</font></td></tr>");
			}
			r.append("</table></td>");
			r.append("<td width=32 align=center valign=middle>");
			
			if (claimed)
			{
				r.append("<font color=00FF00>OK</font>");
			}
			else if (completed && !claimed)
			{
				r.append("<font color=55FF55>Ready</font>");
			}
			else
			{
				r.append("<font color=FFCC00>...</font>");
			}
			r.append("</td>");
			r.append("</tr></table>");
			levelRows.append(r);
		}
		
		template = template.replace("%cat_name%", escape(cat.name)).replace("%type%", escape(ba.type)).replace("%desc%", escape(desc)).replace("%level_rows%", levelRows.toString()).replace("%cat_id%", String.valueOf(cat.id));
		NpcHtmlMessage msg = new NpcHtmlMessage(player.getObjectId());
		msg.setHtml(template);
		player.sendPacket(msg);
		return null;
	}
	
	private LevelEntry getNextPendingLevel(Player player, BaseAchievement ba)
	{
		for (LevelEntry le : ba.levels)
		{
			if (!player.getAchievements().isClaimed(le.physicalId))
			{
				return le;
			}
		}
		
		return null;
	}
	
	private double computeOverallPercent(Player player, BaseAchievement ba)
	{
		long currentValue = getCurrentValue(player, ba.type);
		List<LevelEntry> lvls = ba.levels;
		if (lvls.isEmpty())
		{
			return 0d;
		}
		
		Collections.sort(lvls, Comparator.comparingInt(l -> l.levelId));
		int n = lvls.size();
		long prevNeed = 0L;
		int completedCount = 0;
		double partial = 0d;
		for (int i = 0; i < n; i++)
		{
			LevelEntry le = lvls.get(i);
			if (currentValue >= le.need)
			{
				completedCount++;
				prevNeed = le.need;
				continue;
			}
			
			if (currentValue <= prevNeed)
			{
				partial = 0d;
			}
			else
			{
				partial = (double) (currentValue - prevNeed) / (double) (le.need - prevNeed);
			}
			break;
		}
		double total = ((completedCount + partial) / n) * 100d;
		if (total > 100d)
		{
			total = 100d;
		}
		
		return total;
	}
	
	private String applyPlaceholders(Player player, BaseAchievement ba, long currentValue)
	{
		LevelEntry next = getNextPendingLevelForPlaceholder(player, ba);
		int claimedLevel = getHighestClaimedLevel(player, ba);
		long nextNeed = (next == null) ? 0L : next.need;
		long remaining = (next == null) ? 0L : Math.max(0L, nextNeed - currentValue);
		String out = ba.descTemplate;
		out = out.replace("%current%", String.valueOf(currentValue));
		out = out.replace("%maxlevel%", String.valueOf(ba.levels.size()));
		out = out.replace("%level%", String.valueOf(claimedLevel));
		out = out.replace("%nextneed%", String.valueOf(nextNeed));
		out = out.replace("%need%", String.valueOf(remaining)); // user-defined as remaining.
		out = out.replace("%remaining%", String.valueOf(remaining));
		return out;
	}
	
	private LevelEntry getNextPendingLevelForPlaceholder(Player player, BaseAchievement ba)
	{
		for (LevelEntry le : ba.levels)
		{
			if (!player.getAchievements().isClaimed(le.physicalId))
			{
				return le;
			}
		}
		
		return null;
	}
	
	private int getHighestClaimedLevel(Player player, BaseAchievement ba)
	{
		int highest = 0;
		for (LevelEntry le : ba.levels)
		{
			if (player.getAchievements().isClaimed(le.physicalId))
			{
				highest = Math.max(highest, le.levelId);
			}
		}
		
		return highest;
	}
	
	private String claimLevel(Player player, int physicalId)
	{
		LevelEntry le = LEVELS_BY_PHYSICAL_ID.get(physicalId);
		if (le == null)
		{
			player.sendMessage("Invalid level.");
			return showCategoriesUI(player);
		}
		
		BaseAchievement base = null;
		for (BaseAchievement b : BASE_ACHIEVEMENTS.values())
		{
			for (LevelEntry x : b.levels)
			{
				if (x.physicalId == physicalId)
				{
					base = b;
					break;
				}
			}
			if (base != null)
			{
				break;
			}
		}
		
		if (base == null)
		{
			return showCategoriesUI(player);
		}
		long currentValue = getCurrentValue(player, base.type);
		if (currentValue < le.need)
		{
			player.sendMessage("Requirements not met.");
			return showAchievementDetail(player, base.baseId);
		}
		
		for (LevelEntry prev : base.levels)
		{
			if (prev.levelId >= le.levelId)
			{
				break;
			}
			if (!player.getAchievements().isClaimed(prev.physicalId))
			{
				player.sendMessage("Previous claims pending.");
				return showAchievementDetail(player, base.baseId);
			}
		}
		
		if (!player.getAchievements().isCompleted(physicalId))
		{
			player.getAchievements().setCompleted(physicalId);
		}
		if (player.getAchievements().isClaimed(physicalId))
		{
			player.sendMessage("Already claimed.");
			return showAchievementDetail(player, base.baseId);
		}
		
		if (le.rewards.isEmpty())
		{
			for (RewardItem gri : base.globalRewards)
			{
				player.addItem(ItemProcessType.REWARD, gri.itemId, gri.count, player, true);
			}
		}
		else
		{
			for (RewardItem ri : le.rewards)
			{
				player.addItem(ItemProcessType.REWARD, ri.itemId, ri.count, player, true);
			}
		}
		player.getAchievements().setClaimed(physicalId);
		player.sendMessage("Reward received: " + le.name);
		
		return showCategoryAchievements(player, base.categoryId);
	}
	
	private long getCurrentValue(Player player, String type)
	{
		switch (type)
		{
			case "adena":
				return player.getAdena();
			case "pvpkill":
				return player.getPvpKills();
			case "level":
				return player.getLevel();
			case "hero":
			{
				try
				{
					if (player.isHero())
					{
						return 1L;
					}
				}
				catch (Exception _)
				{
				}
				return 0L;
			}
			case "noble":
			{
				try
				{
					if (player.isNoble())
					{
						return 1L;
					}
				}
				catch (Exception _)
				{
				}
				return 0L;
			}
			case "classlevel":
			{
				// Use the base class to avoid counting subclass progress.
				try
				{
					int baseClassId = player.getBaseClass();
					return PlayerClass.getPlayerClass(baseClassId).level();
				}
				catch (Exception _)
				{
					return 0L;
				}
			}
			case "hassubclass":
			{
				try
				{
					return (player.getTotalSubClasses() > 0) ? 1L : 0L;
				}
				catch (Exception _)
				{
					return 0L;
				}
			}
			case "siegewon":
			{
				try
				{
					if ((player.getClan() != null) && (player.getClan().getCastleId() > 0))
					{
						return 1L;
					}
				}
				catch (Exception _)
				{
				}
				return 0L;
			}
			case "fortressewon":
			{
				try
				{
					if ((player.getClan() != null) && (player.getClan().getFortId() > 0))
					{
						return 1L;
					}
				}
				catch (Exception _)
				{
				}
				return 0L;
			}
			case "contestableclanhall":
			{
				try
				{
					if (player.getClan() != null)
					{
						final int chId = player.getClan().getHideoutId();
						if (chId > 0)
						{
							// Use CHSiegeManager to determine if this clan hall is conquerable/contestable
							final Object hall = CHSiegeManager.getInstance().getSiegableHall(chId);
							if (hall != null)
							{
								return 1L;
							}
						}
					}
				}
				catch (Exception _)
				{
				}
				return 0L;
			}
			case "buyableclanhall":
			{
				try
				{
					if (player.getClan() != null)
					{
						final int chId = player.getClan().getHideoutId();
						if (chId > 0)
						{
							// Must be in clanhall table (auctionable) and NOT in siegable table
							final AuctionableHall ah = ClanHallTable.getInstance().getAuctionableHallById(chId);
							final Object siegable = CHSiegeManager.getInstance().getSiegableHall(chId);
							if ((ah != null) && (siegable == null))
							{
								return 1L;
							}
						}
					}
				}
				catch (Exception _)
				{
				}
				return 0L;
			}
			case "usedzariche":
			{
				try
				{
					// 8190 = Zariche
					if (player.isCursedWeaponEquipped() && (player.getCursedWeaponEquippedId() == 8190))
					{
						return 1L;
					}
					final PlayerVariables vars = player.getVariables();
					if (vars.getInt("HAS_EVER_USED_ZARICHE", 0) == 1)
					{
						return 1L;
					}
				}
				catch (Exception _)
				{
				}
				return 0L;
			}
			case "usedakamanah":
			{
				try
				{
					// 8689 = Akamanah
					if (player.isCursedWeaponEquipped() && (player.getCursedWeaponEquippedId() == 8689))
					{
						return 1L;
					}
					final PlayerVariables vars = player.getVariables();
					if (vars.getInt("HAS_EVER_USED_AKAMANAH", 0) == 1)
					{
						return 1L;
					}
				}
				catch (Exception _)
				{
				}
				return 0L;
			}
			default:
				return player.getCounters().getCounter(type);
		}
	}
	
	/**
	 * Generic gauge bar (not used after partial migration, kept for potential reuse in other screens).
	 * @param percent percentage value 0-100
	 * @param innerWidth usable width (without caps) in pixels
	 * @return HTML for the complete bar with textual percent
	 */
	@SuppressWarnings("unused")
	private String renderGaugeBar(double percent, int innerWidth)
	{
		int p = (int) Math.clamp(Math.round(percent), 0, 100);
		int filled = (int) Math.round((p / 100.0) * innerWidth);
		if (filled < 0)
		{
			filled = 0;
		}
		if (filled > innerWidth)
		{
			filled = innerWidth;
		}
		int remaining = innerWidth - filled;
		
		String capLeft = (filled > 0) ? "Gauge_DF_MP_Left" : "Gauge_DF_Exp_bg_Left";
		String capRight;
		if (filled >= innerWidth)
		{
			capRight = "Gauge_DF_MP_Right";
		}
		else
		{
			capRight = "Gauge_DF_Exp_bg_Right";
		}
		StringBuilder sb = new StringBuilder();
		
		int total = innerWidth + 8;
		sb.append("<table width=" + total + " cellpadding=0 cellspacing=0><tr>");
		sb.append("<td width=4><img src=\"L2UI_CT1." + capLeft + "\" width=4 height=8></td>");
		if (filled > 0)
		{
			sb.append("<td width=" + filled + "><img src=\"L2UI_CT1.Gauge_DF_MP_Center\" width=" + filled + " height=8></td>");
		}
		if (remaining > 0)
		{
			sb.append("<td width=" + remaining + "><img src=\"L2UI_CT1.Gauge_DF_Exp_bg_Center\" width=" + remaining + " height=8></td>");
		}
		sb.append("<td width=4><img src=\"L2UI_CT1." + capRight + "\" width=4 height=8></td>");
		sb.append("</tr></table>");
		
		sb.append("<font color=999999>" + p + "%</font><br>");
		return sb.toString();
	}
	
	/**
	 * Compact version for the categories gallery (total 32px: 2 + inner(28) + 2). No percent label. Caps reduced to 2px as requested.
	 * @param percent percentage value 0-100
	 * @return HTML for the compact bar
	 */
	private String renderGaugeBarCompact24(double percent)
	{
		final int innerWidth = 28;
		int p = (int) Math.clamp(Math.round(percent), 0, 100);
		int filled = (int) Math.round((p / 100.0) * innerWidth);
		if (filled < 0)
		{
			filled = 0;
		}
		if (filled > innerWidth)
		{
			filled = innerWidth;
		}
		int remaining = innerWidth - filled;
		String capLeft = (filled > 0) ? "Gauge_DF_MP_Left" : "Gauge_DF_Exp_bg_Left";
		String capRight = (filled >= innerWidth) ? "Gauge_DF_MP_Right" : "Gauge_DF_Exp_bg_Right";
		StringBuilder sb = new StringBuilder();
		sb.append("<table width=32 cellpadding=0 cellspacing=0><tr>");
		sb.append("<td width=2><img src=\"L2UI_CT1." + capLeft + "\" width=2 height=8></td>");
		if (filled > 0)
		{
			sb.append("<td width=" + filled + "><img src=\"L2UI_CT1.Gauge_DF_MP_Center\" width=" + filled + " height=8></td>");
		}
		if (remaining > 0)
		{
			sb.append("<td width=" + remaining + "><img src=\"L2UI_CT1.Gauge_DF_Exp_bg_Center\" width=" + remaining + " height=8></td>");
		}
		sb.append("<td width=2><img src=\"L2UI_CT1." + capRight + "\" width=2 height=8></td>");
		sb.append("</tr></table>");
		return sb.toString();
	}
	
	private String escape(String s)
	{
		if (s == null)
		{
			return "";
		}
		
		return s.replace("<", "&lt;").replace(">", "&gt;");
	}
	
	/**
	 * Loads a simple template file.
	 * @param path relative to the datapack root
	 * @return content or a fallback string in case of error
	 */
	private String loadTemplate(String path)
	{
		try
		{
			File f = new File(Config.DATAPACK_ROOT, path);
			if (!f.exists())
			{
				return "<html><body><font color=FF0000>Template missing: " + path + "</font></body></html>";
			}
			return new String(Files.readAllBytes(f.toPath()));
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e, () -> "Failed to load template: " + path);
			return "<html><body><font color=FF0000>Template error: " + path + "</font></body></html>";
		}
	}
	
	private String showAchievementList(Player player)
	{
		if (DEBUG_MINIMAL_HTML)
		{
			final String minimal = "<html><body><center><font color=LEVEL>Achievements</font><br><button value=Refresh action=\"bypass -h Quest AchievementNpc refresh\" width=80 height=21 back=L2UI_CT1.Button_DF_Down fore=L2UI_CT1.Button_DF></center></body></html>";
			final NpcHtmlMessage msg = new NpcHtmlMessage(player.getObjectId());
			msg.setHtml(minimal);
			player.sendPacket(msg);
			return null;
		}
		
		final PlayerAchievements playerAchievements = player.getAchievements();
		
		final long playerAdena = player.getAdena();
		
		final StringBuilder html = new StringBuilder();
		html.append("<html>");
		html.append("<title>Achievement System</title>");
		html.append("<body>");
		html.append("<br>");
		
		html.append("<font color=\"AAAAAA\">Adena Achievements</font><br>");
		for (Achievement a : ACHIEVEMENTS)
		{
			buildAchievementBlock(html, a, playerAchievements.isClaimed(a.id), playerAchievements.isCompleted(a.id), playerAdena, player, false);
		}
		
		html.append("</body></html>");
		
		String out = html.toString();
		
		if (out.length() > 12000)
		{
			out = "<html><body><font color=\"FF0000\">The achievement list is too long to display.</font></body></html>";
		}
		final NpcHtmlMessage msg = new NpcHtmlMessage(player.getObjectId());
		msg.setHtml(out);
		player.sendPacket(msg);
		return null;
	}
	
	private void buildAchievementBlock(StringBuilder html, Achievement a, boolean claimed, boolean completed, long currentValue, Player player, boolean isKarma)
	{
		final boolean meets = currentValue >= a.target;
		if (meets && !completed)
		{
			player.getAchievements().setCompleted(a.id);
			completed = true;
		}
		final boolean canClaim = meets && !claimed;
		
		html.append("<table width=\"270\" cellpadding=\"2\" cellspacing=\"0\" bgcolor=\"000000\"><tr>");
		if (SHOW_ICONS)
		{
			html.append("<td width=\"38\" align=\"center\" valign=\"top\"><img src=\"" + a.icon + "\" width=\"32\" height=\"32\"></td>");
		}
		html.append("<td valign=\"top\" width=\"" + (SHOW_ICONS ? "150" : "188") + "\">");
		html.append("<font color=LEVEL>" + a.name + "</font><br>");
		if ((a.description != null) && !a.description.isEmpty())
		{
			html.append("<font color=999999>" + a.description + "</font><br>");
		}
		if (isKarma)
		{
			html.append("Target: " + a.target + " karma<br>");
			html.append("Progress: " + currentValue + " / " + a.target + "<br>");
		}
		else
		{
			html.append("Target: " + FormatUtil.formatAdena(a.target) + " adena<br>");
			html.append("Progress: " + FormatUtil.formatAdena(currentValue) + " / " + FormatUtil.formatAdena(a.target) + "<br>");
		}
		if (SHOW_PROGRESS_BAR)
		{
			long safeTarget = Math.max(1L, a.target);
			int percent = (int) Math.min(100, ((currentValue * 100) / safeTarget));
			int filled = percent / 10; // 10 blocos
			html.append("[");
			for (int i = 0; i < 10; i++)
			{
				html.append(i < filled ? "#" : ".");
			}
			html.append("] " + percent + "%<br>");
		}
		
		html.append("</td><td valign=\"top\" align=\"right\" width=\"70\">");
		if (claimed)
		{
			html.append("<font color=00FF00>COMPLETED</font>");
		}
		else if (canClaim)
		{
			html.append("<button value=\"Reward\" action=\"bypass -h Quest AchievementNpc claim_" + a.id + "\" width=\"60\" height=\"18\" back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		}
		else
		{
			if (completed)
			{
				html.append("<font color=FFCC00>READY</font>");
			}
			else
			{
				html.append("<font color=FF9900>...</font>");
			}
		}
		html.append("</td></tr></table><br>");
	}
	
	private String claimAchievement(Player player, int achievementId)
	{
		Achievement achievement = null;
		for (Achievement ach : ACHIEVEMENTS)
		{
			if (ach.id == achievementId)
			{
				achievement = ach;
				break;
			}
		}
		if (achievement == null)
		{
			player.sendMessage("Achievement not found!");
			return showAchievementList(player);
		}
		final PlayerAchievements playerAchievements = player.getAchievements();
		if (playerAchievements.isClaimed(achievementId))
		{
			player.sendMessage("You have already claimed this achievement!");
			return showAchievementList(player);
		}
		long currentValue = player.getAdena();
		if (currentValue < achievement.target)
		{
			player.sendMessage("Requirements not met.");
			return showAchievementList(player);
		}
		if (!playerAchievements.isCompleted(achievementId))
		{
			playerAchievements.setCompleted(achievementId);
		}
		for (RewardItem reward : achievement.rewards)
		{
			player.addItem(ItemProcessType.REWARD, reward.itemId, reward.count, player, true);
		}
		playerAchievements.setClaimed(achievementId);
		player.sendMessage("Achievement claimed: " + achievement.name);
		return showAchievementList(player);
	}
	
	public static void main(String[] args)
	{
		new AchievementNpc();
	}
}
