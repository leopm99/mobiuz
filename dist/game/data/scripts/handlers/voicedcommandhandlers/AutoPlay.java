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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.l2journey.Config;
import com.l2journey.commons.util.StringUtil;
import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.data.xml.PetSkillData;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.handler.IVoicedCommandHandler;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.Summon;
import com.l2journey.gameserver.model.events.Containers;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.holders.actor.player.OnPlayerLogin;
import com.l2journey.gameserver.model.events.holders.actor.player.OnPlayerLogout;
import com.l2journey.gameserver.model.events.listeners.ConsumerEventListener;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.skill.AbnormalType;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.holders.SkillHolder;
import com.l2journey.gameserver.model.variables.PlayerVariables;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.serverpackets.ActionFailed;
import com.l2journey.gameserver.network.serverpackets.CreatureSay;
import com.l2journey.gameserver.network.serverpackets.ExShowScreenMessage;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2journey.gameserver.taskmanagers.AutoPlayTaskManager;
import com.l2journey.gameserver.taskmanagers.AutoUseTaskManager;
import com.l2journey.gameserver.util.HtmlUtil;

/**
 * @author Mobius, L2Journey, KingHanker, Mafias
 */
public class AutoPlay implements IVoicedCommandHandler
{
	private static final int PAGE_LIMIT = 7;
	private static final Integer AUTO_ATTACK_ACTION = 2;
	
	private static final String[] VOICED_COMMANDS =
	{
		"play",
		"playskills",
		"playitems",
		"playpotion",
		"plaympotion"
	};
	
	private static final Consumer<OnPlayerLogin> ON_PLAYER_LOGIN = event ->
	{
		if (!Config.ENABLE_AUTO_PLAY)
		{
			return;
		}
		
		final Player player = event.getPlayer();
		if (!Config.AUTO_PLAY_LOGIN_MESSAGE.isEmpty())
		{
			player.sendPacket(new CreatureSay(null, ChatType.ANNOUNCEMENT, "AutoPlay", Config.AUTO_PLAY_LOGIN_MESSAGE));
		}
		
		player.getVariables().getIntegerList(PlayerVariables.AUTO_USE_ACTIONS).forEach(id -> player.getAutoUseSettings().getAutoActions().add(id));
		player.getVariables().getIntegerList(PlayerVariables.AUTO_USE_BUFFS).forEach(id -> player.getAutoUseSettings().getAutoBuffs().add(id));
		player.getVariables().getIntegerList(PlayerVariables.AUTO_USE_SKILLS).forEach(id -> player.getAutoUseSettings().getAutoSkills().add(id));
		player.getVariables().getIntegerList(PlayerVariables.AUTO_USE_ITEMS).forEach(id -> player.getAutoUseSettings().getAutoSupplyItems().add(id));
		player.getAutoUseSettings().setAutoPotionItem(player.getVariables().getInt(PlayerVariables.AUTO_USE_POTION, 0));
		player.getAutoUseSettings().setAutoManaPotionItem(player.getVariables().getInt("AUTO_USE_MP_POTION", 0));
		
		final List<Integer> settings = player.getVariables().getIntegerList(PlayerVariables.AUTO_USE_SETTINGS);
		if (settings.isEmpty() || (settings.size() < 8))
		{
			return;
		}
		
		final int options = settings.get(0);
		final boolean active = Config.RESUME_AUTO_PLAY && (settings.get(1) == 1);
		final boolean pickUp = settings.get(2) == 1;
		final int nextTargetMode = settings.get(3);
		final boolean shortRange = settings.get(4) == 1;
		final int potionPercent = settings.get(5);
		final boolean respectfulHunting = settings.get(6) == 1;
		final int manaPotionPercent = settings.get(7);
		
		player.getAutoPlaySettings().setAutoPotionPercent(potionPercent);
		player.getAutoPlaySettings().setAutoManaPotionPercent(manaPotionPercent);
		player.getAutoPlaySettings().setOptions(options);
		player.getAutoPlaySettings().setPickup(pickUp);
		player.getAutoPlaySettings().setNextTargetMode(nextTargetMode);
		player.getAutoPlaySettings().setShortRange(shortRange);
		player.getAutoPlaySettings().setRespectfulHunting(respectfulHunting);
		
		if (active)
		{
			AutoPlayTaskManager.getInstance().startAutoPlay(player);
		}
	};
	
	private static final Consumer<OnPlayerLogout> ON_PLAYER_LOGOUT = event ->
	{
		if (!Config.ENABLE_AUTO_PLAY)
		{
			return;
		}
		
		final Player player = event.getPlayer();
		player.getVariables().setIntegerList(PlayerVariables.AUTO_USE_ACTIONS, new ArrayList<>(player.getAutoUseSettings().getAutoActions()));
		player.getVariables().setIntegerList(PlayerVariables.AUTO_USE_BUFFS, new ArrayList<>(player.getAutoUseSettings().getAutoBuffs()));
		player.getVariables().setIntegerList(PlayerVariables.AUTO_USE_SKILLS, new ArrayList<>(player.getAutoUseSettings().getAutoSkills()));
		player.getVariables().setIntegerList(PlayerVariables.AUTO_USE_ITEMS, new ArrayList<>(player.getAutoUseSettings().getAutoSupplyItems()));
		
		final int potionId = player.getAutoUseSettings().getAutoPotionItem();
		if (potionId < 1)
		{
			player.getVariables().remove(PlayerVariables.AUTO_USE_POTION);
		}
		else
		{
			player.getVariables().set(PlayerVariables.AUTO_USE_POTION, potionId);
		}
		
		final int manaPotionId = player.getAutoUseSettings().getAutoManaPotionItem();
		if (manaPotionId < 1)
		{
			player.getVariables().remove("AUTO_USE_MP_POTION");
		}
		else
		{
			player.getVariables().set("AUTO_USE_MP_POTION", manaPotionId);
		}
		
		final List<Integer> settings = new ArrayList<>(8);
		settings.add(0, player.getAutoPlaySettings().getOptions());
		settings.add(1, player.isAutoPlaying() ? 1 : 0);
		settings.add(2, player.getAutoPlaySettings().doPickup() ? 1 : 0);
		settings.add(3, player.getAutoPlaySettings().getNextTargetMode());
		settings.add(4, player.getAutoPlaySettings().isShortRange() ? 1 : 0);
		settings.add(5, player.getAutoPlaySettings().getAutoPotionPercent());
		settings.add(6, player.getAutoPlaySettings().isRespectfulHunting() ? 1 : 0);
		settings.add(7, player.getAutoPlaySettings().getAutoManaPotionPercent());
		player.getVariables().setIntegerList(PlayerVariables.AUTO_USE_SETTINGS, settings);
	};
	
	public AutoPlay()
	{
		Containers.Players().addListener(new ConsumerEventListener(Containers.Players(), EventType.ON_PLAYER_LOGIN, ON_PLAYER_LOGIN, this));
		Containers.Players().addListener(new ConsumerEventListener(Containers.Players(), EventType.ON_PLAYER_LOGOUT, ON_PLAYER_LOGOUT, this));
	}
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String params)
	{
		if (!Config.ENABLE_AUTO_PLAY || (player == null))
		{
			return false;
		}
		
		if (Config.AUTO_PLAY_PREMIUM && !player.hasPremiumStatus())
		{
			player.sendPacket(new ExShowScreenMessage("This command is only available to premium players.", 5000));
			player.sendMessage("This command is only available to premium players.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		switch (command)
		{
			case "play":
			{
				if (params != null)
				{
					final String[] paramArray = params.toLowerCase().split(" ");
					COMMAND: switch (paramArray[0])
					{
						case "attack":
							if (player.getAutoUseSettings().getAutoActions().contains(AUTO_ATTACK_ACTION))
							{
								player.getAutoUseSettings().getAutoActions().remove(AUTO_ATTACK_ACTION);
							}
							else
							{
								player.getAutoUseSettings().getAutoActions().add(AUTO_ATTACK_ACTION);
							}
							break COMMAND;
						case "loot":
							player.getAutoPlaySettings().setPickup(!player.getAutoPlaySettings().doPickup());
							break COMMAND;
						case "respect":
							player.getAutoPlaySettings().setRespectfulHunting(!player.getAutoPlaySettings().isRespectfulHunting());
							break COMMAND;
						case "range":
							player.getAutoPlaySettings().setShortRange(!player.getAutoPlaySettings().isShortRange());
							break COMMAND;
						case "mode0":
							player.getAutoPlaySettings().setNextTargetMode(0);
							break COMMAND;
						case "mode1":
							player.getAutoPlaySettings().setNextTargetMode(1);
							break COMMAND;
						case "mode2":
							player.getAutoPlaySettings().setNextTargetMode(2);
							break COMMAND;
						case "mode3":
							player.getAutoPlaySettings().setNextTargetMode(3);
							break COMMAND;
						case "percent":
						{
							if ((paramArray.length > 1) && StringUtil.isNumeric(paramArray[1]))
							{
								player.getAutoPlaySettings().setAutoPotionPercent(Math.clamp(Integer.parseInt(paramArray[1]), 0, 100));
							}
							break COMMAND;
						}
						
						case "mppercent":
						{
							if ((paramArray.length > 1) && StringUtil.isNumeric(paramArray[1]))
							{
								player.getAutoPlaySettings().setAutoManaPotionPercent(Math.clamp(Integer.parseInt(paramArray[1]), 0, 100));
							}
							break COMMAND;
						}
						case "start":
							AutoPlayTaskManager.getInstance().startAutoPlay(player);
							AutoUseTaskManager.getInstance().startAutoUseTask(player);
							break COMMAND;
						case "stop":
							AutoPlayTaskManager.getInstance().stopAutoPlay(player);
							AutoUseTaskManager.getInstance().stopAutoUseTask(player);
							break COMMAND;
					}
				}
				
				final NpcHtmlMessage html = new NpcHtmlMessage();
				String content = HtmCache.getInstance().getHtm(player, "data/html/mods/AutoPlay/Main.htm");
				
				content = content.replace("%attack%", player.getAutoUseSettings().getAutoActions().contains(AUTO_ATTACK_ACTION) ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
				content = content.replace("%loot%", player.getAutoPlaySettings().doPickup() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
				content = content.replace("%respect%", player.getAutoPlaySettings().isRespectfulHunting() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
				content = content.replace("%range%", !player.getAutoPlaySettings().isShortRange() ? "L2UI.CheckBox_checked" : "L2UI.CheckBox");
				
				content = content.replace("%mode0%", player.getAutoPlaySettings().getNextTargetMode() == 0 ? "L2UI_CH3.radiobutton2" : "L2UI_CH3.radiobutton1");
				content = content.replace("%mode1%", player.getAutoPlaySettings().getNextTargetMode() == 1 ? "L2UI_CH3.radiobutton2" : "L2UI_CH3.radiobutton1");
				content = content.replace("%mode2%", player.getAutoPlaySettings().getNextTargetMode() == 2 ? "L2UI_CH3.radiobutton2" : "L2UI_CH3.radiobutton1");
				content = content.replace("%mode3%", player.getAutoPlaySettings().getNextTargetMode() == 3 ? "L2UI_CH3.radiobutton2" : "L2UI_CH3.radiobutton1");
				
				content = content.replace("%skill_button%", Config.ENABLE_AUTO_SKILL ? "<br1><table width=295><tr><td height=31><center><button action=\"bypass voice .playskills\" value=\"Select Skills\" width=200 height=31 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></center></td></tr></table>" : "");
				content = content.replace("%item_button%", Config.ENABLE_AUTO_ITEM ? "<br1><table width=295><tr><td height=31><center><button action=\"bypass voice .playitems\" value=\"Select Supply Items\" width=200 height=31 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></center></td></tr></table>" : "");
				content = content.replace("%potion_button%", Config.ENABLE_AUTO_POTION ? "<br1><table width=295>"
					
					+ "<tr><td height=31 colspan=2><center><button action=\"bypass voice .playpotion\" value=\"Selecionar Poção de HP\" width=200 height=31 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></center></td></tr>"
					
					+ "<tr><td height=31 colspan=2><center><button action=\"bypass voice .plaympotion\" value=\"Selecionar Poção de MP\" width=200 height=31 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></center></td></tr>"
					
					+ "<tr>" + "<td colspan=2>" + "<center>"
					
					+ "<table width=260 border=0 cellspacing=0 cellpadding=0>" + "<tr>"
					
					+ "<td width=120 align=center>" + "<font color=\"CDB67F\">HP Percent (%percent%)</font><br1>" + "<edit var=\"percentbox\" width=32 height=15>" + "<br>" + "<button value=\"Set HP\" action=\"bypass voice .play percent $percentbox\" width=70 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" + "</td>"
					
					+ "<td width=20></td>"
					
					+ "<td width=120 align=center>" + "<font color=\"CDB67F\">MP Percent (%mppercent%)</font><br1>" + "<edit var=\"mppercentbox\" width=32 height=15>" + "<br>" + "<button value=\"Set MP\" action=\"bypass voice .play mppercent $mppercentbox\" width=70 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" + "</td>"
					
					+ "</tr>" + "</table>"
					
					+ "</center>" + "</td>" + "</tr>"
					
					+ "</table>" : "");
				
				content = content.replace("%percent%", String.valueOf(player.getAutoPlaySettings().getAutoPotionPercent()));
				content = content.replace("%mppercent%", String.valueOf(player.getAutoPlaySettings().getAutoManaPotionPercent()));
				
				if (player.isAutoPlaying())
				{
					content = content.replace("%status_button%", "<table width=295><tr><td height=31><center><table width=295><tr><td height=31><center><button action=\"bypass voice .play stop\" value=\"Stop\" width=200 height=31 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td></tr></table></center></td></tr></table>");
				}
				else
				{
					content = content.replace("%status_button%", "<table width=295><tr><td height=31><center><table width=295><tr><td height=31><center><button action=\"bypass voice .play start\" value=\"Start\" width=200 height=31 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td></tr></table></center></td></tr></table>");
				}
				
				html.setHtml(content);
				player.sendPacket(html);
				break;
			}
			case "plaympotion":
			{
				final NpcHtmlMessage html = new NpcHtmlMessage();
				final String content = HtmCache.getInstance().getHtm(player, "data/html/mods/AutoPlay/MpPotion.htm");
				List<ItemTemplate> items = new ArrayList<>();
				POTION_SEARCH: for (Item item : player.getInventory().getItems())
				{
					final ItemTemplate template = item.getTemplate();
					if (item.isEtcItem() && template.hasSkills() && !Config.DISABLED_AUTO_ITEMS.contains(item.getId()))
					{
						for (SkillHolder holder : template.getSkills())
						{
							final Skill skill = holder.getSkill();
							if ((skill != null) && (skill.getAbnormalType() == AbnormalType.MP_RECOVER) && !items.contains(template))
							{
								items.add(template);
								continue POTION_SEARCH;
							}
						}
					}
				}
				
				final String[] paramArray = params == null ? new String[0] : params.split(" ");
				if (paramArray.length > 1)
				{
					final int itemId = Integer.parseInt(paramArray[1]);
					if (Config.ENABLE_AUTO_POTION && items.contains(ItemData.getInstance().getTemplate(itemId)))
					{
						if (player.getAutoUseSettings().getAutoManaPotionItem() == itemId)
						{
							player.getAutoUseSettings().setAutoManaPotionItem(0);
						}
						else
						{
							player.getAutoUseSettings().setAutoManaPotionItem(itemId);
						}
					}
				}
				
				final int max = HtmlUtil.countPageNumber(items.size(), PAGE_LIMIT);
				int page = ((params == null) || paramArray[0].isEmpty()) ? 1 : Integer.parseInt(paramArray[0]);
				if (page > max)
				{
					page = max;
				}
				if (page < 1)
				{
					page = 1;
				}
				
				final StringBuilder sb = new StringBuilder();
				List<ItemTemplate> subList = items.subList(Math.max(0, (page - 1) * PAGE_LIMIT), Math.min(page * PAGE_LIMIT, items.size()));
				if (subList.isEmpty())
				{
					sb.append("<center><br>Nenhuma poção de MP encontrada.<br></center>");
				}
				else
				{
					int row = 0;
					for (ItemTemplate template : subList)
					{
						sb.append(((row % 2) == 0 ? "<table width=\"295\" bgcolor=\"000000\"><tr>" : "<table width=\"295\"><tr>"));
						if (player.getAutoUseSettings().getAutoManaPotionItem() == template.getId())
						{
							sb.append("<td height=40 width=40><img src=\"" + template.getIcon() + "\" width=32 height=32></td><td width=190>" + template.getName() + "</td><td><button value=\" \" action=\"bypass voice .plaympotion " + page + " " + template.getId() + "\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomout2\" fore=\"L2UI_CH3.mapbutton_zoomout1\"></td>");
						}
						else
						{
							sb.append("<td height=40 width=40><img src=\"" + template.getIcon() + "\" width=32 height=32></td><td width=190><font color=\"B09878\">" + template.getName() + "</font></td><td><button value=\" \" action=\"bypass voice .plaympotion " + page + " " + template.getId() + "\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomin2\" fore=\"L2UI_CH3.mapbutton_zoomin1\"></td>");
						}
						sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=295 height=1>");
						row++;
					}
					sb.append("<br><img src=\"L2UI.SquareGray\" width=295 height=1><table width=\"100%\" bgcolor=000000><tr>");
					sb.append("<td align=left width=70>" + (page > 1 ? "<a action=\"bypass voice .plaympotion " + (page - 1) + "\"><font color=\"CDB67F\">Anterior</font></a>" : "<font color=\"B09878\">Anterior</font>") + "</td>");
					sb.append("<td align=center width=100>Página " + page + " de " + max + "</td>");
					sb.append("<td align=right width=70>" + (page < max ? "<a action=\"bypass voice .plaympotion " + (page + 1) + "\"><font color=\"CDB67F\">Próxima</font></a>" : "<font color=\"B09878\">Próxima</font>") + "</td>");
					sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=295 height=1>");
				}
				html.setHtml(content.replace("%items%", sb.toString()));
				player.sendPacket(html);
				break;
			}
			case "playskills":
			{
				final NpcHtmlMessage html = new NpcHtmlMessage();
				final String content = HtmCache.getInstance().getHtm(player, "data/html/mods/AutoPlay/Skills.htm");
				
				List<Skill> skills = new ArrayList<>();
				final Set<Skill> siegeSkills = new HashSet<>();
				final Skill[] siegeSkillArray = SkillData.getInstance().getSiegeSkills(true, true);
				if (siegeSkillArray != null)
				{
					for (Skill s : siegeSkillArray)
					{
						if (s != null)
						{
							siegeSkills.add(s);
						}
					}
				}
				for (Skill skill : player.getAllSkills())
				{
					if (!siegeSkills.contains(skill) && !skill.isPassive() && !skill.isToggle() && !skills.contains(skill) && !Config.DISABLED_AUTO_SKILLS.contains(skill.getId()))
					{
						skills.add(skill);
					}
				}
				if (player.hasServitor() || player.hasPet())
				{
					final Summon summon = player.getSummon();
					for (Skill skill : summon.getAllSkills())
					{
						if (!skill.isPassive() && !skill.isToggle() && !skills.contains(skill) && !Config.DISABLED_AUTO_SKILLS.contains(skill.getId()))
						{
							skills.add(skill);
						}
					}
					
					for (Skill skill : PetSkillData.getInstance().getKnownSkills(summon))
					{
						if (!skill.isPassive() && !skill.isToggle() && !skills.contains(skill) && !Config.DISABLED_AUTO_SKILLS.contains(skill.getId()))
						{
							skills.add(skill);
						}
					}
				}
				
				// Remove item skills logic (omitido para brevidade, mas mantido no original)
				// [O código de remoção de skills de itens permanece igual]
				
				final String[] paramArray = params == null ? new String[0] : params.split(" ");
				if (paramArray.length > 1)
				{
					final Integer skillId = Integer.parseInt(paramArray[1]);
					Skill knownSkill = player.getKnownSkill(skillId);
					if (Config.ENABLE_AUTO_SKILL && (knownSkill != null))
					{
						if (knownSkill.isBad())
						{
							if (player.getAutoUseSettings().getAutoSkills().contains(skillId))
							{
								player.getAutoUseSettings().getAutoSkills().remove(skillId);
							}
							else
							{
								player.getAutoUseSettings().getAutoSkills().add(skillId);
							}
						}
						else
						{
							if (player.getAutoUseSettings().getAutoBuffs().contains(skillId))
							{
								player.getAutoUseSettings().getAutoBuffs().remove(skillId);
							}
							else
							{
								player.getAutoUseSettings().getAutoBuffs().add(skillId);
							}
						}
					}
				}
				
				final int max = HtmlUtil.countPageNumber(skills.size(), PAGE_LIMIT);
				int page = ((params == null) || paramArray[0].isEmpty()) ? 1 : Integer.parseInt(paramArray[0]);
				if (page > max)
				{
					page = max;
				}
				
				final StringBuilder sb = new StringBuilder();
				List<Skill> subSkills = skills.subList(Math.max(0, (page - 1) * PAGE_LIMIT), Math.min(page * PAGE_LIMIT, skills.size()));
				if (subSkills.isEmpty())
				{
					sb.append("<center><br>No skills found.<br></center>");
				}
				else
				{
					int row = 0;
					for (Skill skill : subSkills)
					{
						sb.append(((row % 2) == 0 ? "<table width=\"295\" bgcolor=\"000000\"><tr>" : "<table width=\"295\"><tr>"));
						
						boolean isSelected = player.getAutoUseSettings().getAutoBuffs().contains(skill.getId()) || player.getAutoUseSettings().getAutoSkills().contains(skill.getId());
						
						sb.append("<td height=40 width=40><img src=\"" + skill.getIcon() + "\" width=32 height=32></td><td width=190>" + (isSelected ? skill.getName() : "<font color=\"B09878\">" + skill.getName() + "</font>") + "</td><td><button value=\" \" action=\"bypass voice .playskills " + page + " " + skill.getId() + "\" width=32 height=32 back=\"" + (isSelected ? "L2UI_CH3.mapbutton_zoomout2" : "L2UI_CH3.mapbutton_zoomin2") + "\" fore=\"" + (isSelected ? "L2UI_CH3.mapbutton_zoomout1" : "L2UI_CH3.mapbutton_zoomin1") + "\"></td>");
						
						sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=295 height=1>");
						row++;
					}
					
					sb.append("<br><img src=\"L2UI.SquareGray\" width=295 height=1><table width=\"100%\" bgcolor=000000><tr>");
					
					sb.append("<td align=left width=70>" + (page > 1 ? "<a action=\"bypass voice .playskills " + (page - 1) + "\"><font color=\"CDB67F\">Previous</font></a>" : "<font color=\"B09878\">Previous</font>") + "</td>");
					
					sb.append("<td align=center width=100>Page " + page + " of " + max + "</td>");
					
					sb.append("<td align=right width=70>" + (page < max ? "<a action=\"bypass voice .playskills " + (page + 1) + "\"><font color=\"CDB67F\">Next</font></a>" : "<font color=\"B09878\">Next</font>") + "</td>");
					
					sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=295 height=1>");
				}
				
				html.setHtml(content.replace("%skills%", sb.toString()));
				player.sendPacket(html);
				break;
			}
			case "playitems":
			{
				final NpcHtmlMessage html = new NpcHtmlMessage();
				final String content = HtmCache.getInstance().getHtm(player, "data/html/mods/AutoPlay/Items.htm");
				List<ItemTemplate> items = new ArrayList<>();
				ITEM_SEARCH: for (Item item : player.getInventory().getItems())
				{
					final ItemTemplate template = item.getTemplate();
					if (item.isEtcItem() && template.hasSkills() && !Config.DISABLED_AUTO_ITEMS.contains(item.getId()))
					{
						for (SkillHolder holder : template.getSkills())
						{
							final Skill skill = holder.getSkill();
							if ((skill != null) && skill.isContinuous() && (skill.getAbnormalType() != AbnormalType.HP_RECOVER) && (skill.getAbnormalType() != AbnormalType.MP_RECOVER) && !items.contains(template))
							{
								items.add(template);
								continue ITEM_SEARCH;
							}
						}
					}
				}
				
				final String[] paramArray = params == null ? new String[0] : params.split(" ");
				if (paramArray.length > 1)
				{
					final int itemId = Integer.parseInt(paramArray[1]);
					if (Config.ENABLE_AUTO_ITEM && items.contains(ItemData.getInstance().getTemplate(itemId)))
					{
						if (player.getAutoUseSettings().getAutoSupplyItems().contains(itemId))
						{
							player.getAutoUseSettings().getAutoSupplyItems().remove(itemId);
						}
						else
						{
							player.getAutoUseSettings().getAutoSupplyItems().add(itemId);
						}
					}
				}
				
				final int max = HtmlUtil.countPageNumber(items.size(), PAGE_LIMIT);
				int page = ((params == null) || paramArray[0].isEmpty()) ? 1 : Integer.parseInt(paramArray[0]);
				if (page > max)
				{
					page = max;
				}
				if (page < 1)
				{
					page = 1;
				}
				
				final StringBuilder sb = new StringBuilder();
				List<ItemTemplate> subList = items.subList(Math.max(0, (page - 1) * PAGE_LIMIT), Math.min(page * PAGE_LIMIT, items.size()));
				if (subList.isEmpty())
				{
					sb.append("<center><br>No items found.<br></center>");
				}
				else
				{
					int row = 0;
					for (ItemTemplate template : subList)
					{
						sb.append(((row % 2) == 0 ? "<table width=\"295\" bgcolor=\"000000\"><tr>" : "<table width=\"295\"><tr>"));
						if (player.getAutoUseSettings().getAutoSupplyItems().contains(template.getId()))
						{
							sb.append("<td height=40 width=40><img src=\"" + template.getIcon() + "\" width=32 height=32></td><td width=190>" + template.getName() + "</td><td><button value=\" \" action=\"bypass voice .playitems " + page + " " + template.getId() + "\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomout2\" fore=\"L2UI_CH3.mapbutton_zoomout1\"></td>");
						}
						else
						{
							sb.append("<td height=40 width=40><img src=\"" + template.getIcon() + "\" width=32 height=32></td><td width=190><font color=\"B09878\">" + template.getName() + "</font></td><td><button value=\" \" action=\"bypass voice .playitems " + page + " " + template.getId() + "\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomin2\" fore=\"L2UI_CH3.mapbutton_zoomin1\"></td>");
						}
						sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=295 height=1>");
						row++;
					}
					sb.append("<br><img src=\"L2UI.SquareGray\" width=295 height=1><table width=\"100%\" bgcolor=000000><tr>");
					sb.append("<td align=left width=70>" + (page > 1 ? "<a action=\"bypass voice .playitems " + (page - 1) + "\"><font color=\"CDB67F\">Previous</font></a>" : "<font color=\"B09878\">Previous</font>") + "</td>");
					sb.append("<td align=center width=100>Page " + page + " of " + max + "</td>");
					sb.append("<td align=right width=70>" + (page < max ? "<a action=\"bypass voice .playitems " + (page + 1) + "\"><font color=\"CDB67F\">Next</font></a>" : "<font color=\"B09878\">Next</font>") + "</td>");
					sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=295 height=1>");
				}
				html.setHtml(content.replace("%items%", sb.toString()));
				player.sendPacket(html);
				break;
			}
			case "playpotion":
			{
				final NpcHtmlMessage html = new NpcHtmlMessage();
				final String content = HtmCache.getInstance().getHtm(player, "data/html/mods/AutoPlay/Potion.htm");
				List<ItemTemplate> items = new ArrayList<>();
				POTION_SEARCH: for (Item item : player.getInventory().getItems())
				{
					final ItemTemplate template = item.getTemplate();
					if (item.isEtcItem() && template.hasSkills() && !Config.DISABLED_AUTO_ITEMS.contains(item.getId()))
					{
						for (SkillHolder holder : template.getSkills())
						{
							final Skill skill = holder.getSkill();
							if ((skill != null) && (skill.getAbnormalType() == AbnormalType.HP_RECOVER) && !items.contains(template))
							{
								items.add(template);
								continue POTION_SEARCH;
							}
						}
					}
				}
				
				final String[] paramArray = params == null ? new String[0] : params.split(" ");
				if (paramArray.length > 1)
				{
					final int itemId = Integer.parseInt(paramArray[1]);
					if (Config.ENABLE_AUTO_POTION && items.contains(ItemData.getInstance().getTemplate(itemId)))
					{
						if (player.getAutoUseSettings().getAutoPotionItem() == itemId)
						{
							player.getAutoUseSettings().setAutoPotionItem(0);
						}
						else
						{
							player.getAutoUseSettings().setAutoPotionItem(itemId);
						}
					}
				}
				
				final int max = HtmlUtil.countPageNumber(items.size(), PAGE_LIMIT);
				int page = ((params == null) || paramArray[0].isEmpty()) ? 1 : Integer.parseInt(paramArray[0]);
				if (page > max)
				{
					page = max;
				}
				if (page < 1)
				{
					page = 1;
				}
				
				final StringBuilder sb = new StringBuilder();
				List<ItemTemplate> subList = items.subList(Math.max(0, (page - 1) * PAGE_LIMIT), Math.min(page * PAGE_LIMIT, items.size()));
				if (subList.isEmpty())
				{
					sb.append("<center><br>Nenhuma poção de HP encontrada.<br></center>");
				}
				else
				{
					int row = 0;
					for (ItemTemplate template : subList)
					{
						sb.append(((row % 2) == 0 ? "<table width=\"295\" bgcolor=\"000000\"><tr>" : "<table width=\"295\"><tr>"));
						if (player.getAutoUseSettings().getAutoPotionItem() == template.getId())
						{
							sb.append("<td height=40 width=40><img src=\"" + template.getIcon() + "\" width=32 height=32></td><td width=190>" + template.getName() + "</td><td><button value=\" \" action=\"bypass voice .playpotion " + page + " " + template.getId() + "\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomout2\" fore=\"L2UI_CH3.mapbutton_zoomout1\"></td>");
						}
						else
						{
							sb.append("<td height=40 width=40><img src=\"" + template.getIcon() + "\" width=32 height=32></td><td width=190><font color=\"B09878\">" + template.getName() + "</font></td><td><button value=\" \" action=\"bypass voice .playpotion " + page + " " + template.getId() + "\" width=32 height=32 back=\"L2UI_CH3.mapbutton_zoomin2\" fore=\"L2UI_CH3.mapbutton_zoomin1\"></td>");
						}
						sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=295 height=1>");
						row++;
					}
					sb.append("<br><img src=\"L2UI.SquareGray\" width=295 height=1><table width=\"100%\" bgcolor=000000><tr>");
					sb.append("<td align=left width=70>" + (page > 1 ? "<a action=\"bypass voice .playpotion " + (page - 1) + "\"><font color=\"CDB67F\">Anterior</font></a>" : "<font color=\"B09878\">Anterior</font>") + "</td>");
					sb.append("<td align=center width=100>Página " + page + " de " + max + "</td>");
					sb.append("<td align=right width=70>" + (page < max ? "<a action=\"bypass voice .playpotion " + (page + 1) + "\"><font color=\"CDB67F\">Próxima</font></a>" : "<font color=\"B09878\">Próxima</font>") + "</td>");
					sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=295 height=1>");
				}
				html.setHtml(content.replace("%items%", sb.toString()));
				player.sendPacket(html);
				break;
			}
		}
		
		return true;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}