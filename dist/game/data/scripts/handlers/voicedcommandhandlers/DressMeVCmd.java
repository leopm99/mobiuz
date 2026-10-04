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

import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.data.xml.ArmorSetData;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.handler.IVoicedCommandHandler;
import com.l2journey.gameserver.model.ArmorSet;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.item.type.ItemType;
import com.l2journey.gameserver.model.itemcontainer.Inventory;
import com.l2journey.gameserver.model.visualSystem.DressMeHandler;
import com.l2journey.gameserver.model.visualSystem.data.DressMeArmorData;
import com.l2journey.gameserver.model.visualSystem.data.DressMeCloakData;
import com.l2journey.gameserver.model.visualSystem.data.DressMeHatData;
import com.l2journey.gameserver.model.visualSystem.data.DressMeShieldData;
import com.l2journey.gameserver.model.visualSystem.data.DressMeWeaponData;
import com.l2journey.gameserver.model.visualSystem.dataHolder.DressMeArmorHolder;
import com.l2journey.gameserver.model.visualSystem.dataHolder.DressMeCloakHolder;
import com.l2journey.gameserver.model.visualSystem.dataHolder.DressMeHatHolder;
import com.l2journey.gameserver.model.visualSystem.dataHolder.DressMeShieldHolder;
import com.l2journey.gameserver.model.visualSystem.dataHolder.DressMeWeaponHolder;
import com.l2journey.gameserver.model.visualSystem.dataParser.Util;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author KingHanker, Zoinha
 */
public class DressMeVCmd implements IVoicedCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(DressMeVCmd.class.getName());
	
	private static final String[] VOICED_COMMANDS =
	{
		"dressme",
		"undressme",
		
		"dressinfo",
		
		"showdress",
		"hidedress",
		
		"dressme-armor",
		"dress-armor",
		"dress-armorpage",
		"undressme-armor",
		
		"dressme-cloak",
		"dress-cloak",
		"dress-cloakpage",
		"undressme-cloak",
		
		"dressme-shield",
		"dress-shield",
		"dress-shieldpage",
		"undressme-shield",
		
		"dressme-weapon",
		"dress-weapon",
		"dress-weaponpage",
		"undressme-weapon",
		
		"dressme-hat",
		"dress-hat",
		"dress-hatpage",
		"undressme-hat"
	};
	
	private final int ITEMS_PER_PAGE = 6;
	
	String index_path = "data/html/dressme/index.htm";
	String info_path = "data/html/dressme/info.htm";
	String undressme_path = "data/html/dressme/undressme.htm";
	
	String index_armor_path = "data/html/dressme/index-armor.htm";
	String template_armor_path = "data/html/dressme/template-armor.htm";
	
	String index_cloak = "data/html/dressme/index-cloak.htm";
	String template_cloak_path = "data/html/dressme/template-cloak.htm";
	
	String index_shield_path = "data/html/dressme/index-shield.htm";
	String template_shield_path = "data/html/dressme/template-shield.htm";
	
	String index_weapon_path = "data/html/dressme/index-weapon.htm";
	String template_weapon_path = "data/html/dressme/template-weapon.htm";
	
	String index_hat_path = "data/html/dressme/index-hat.htm";
	String template_hat_path = "data/html/dressme/template-hat.htm";
	
	String dress_cloak_path = "data/html/dressme/dress-cloak.htm";
	String dress_shield_path = "data/html/dressme/dress-shield.htm";
	String dress_armor_path = "data/html/dressme/dress-armor.htm";
	String dress_weapon_path = "data/html/dressme/dress-weapon.htm";
	String dress_hat_path = "data/html/dressme/dress-hat.htm";
	
	/**
	 * Implementation of the "dressme" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_Dressme(Player player, String params)
	{
		String html = HtmCache.getInstance().getHtm(player, index_path);
		html = html.replace("<?show_hide?>", !player.getVarB("showVisualChange") ? "Show visual equip on other player!" : "Hide visual equip on other player!");
		html = html.replace("<?show_hide_b?>", !player.getVarB("showVisualChange") ? "showdress" : "hidedress");
		
		sendHtml(player, html);
		return true;
	}
	
	/**
	 * Implementation of the "dressme-armor" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_dressme_armor(Player player, String params)
	{
		final String args = params == null ? "1" : params;
		Item slot = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		if ((slot == null) || !slot.isArmor())
		{
			player.sendMessage("Error: Armor chest must be equiped!");
			return false;
		}
		
		String html = HtmCache.getInstance().getHtm(player, index_armor_path);
		String template = HtmCache.getInstance().getHtm(player, template_armor_path);
		String block = "";
		String list = "";
		
		String[] param = args.split(" ");
		
		final int page = !param[0].isEmpty() ? Integer.parseInt(param[0]) : 1;
		final int perpage = ITEMS_PER_PAGE;
		int counter = 0;
		
		String type = slot.getArmorItem().getItemType().getDescription();
		Map<Integer, DressMeArmorData> map = DressMeHandler.initArmorMap(type);
		if (map == null)
		{
			LOGGER.info("DressMe system: Armor Map is null.");
			return false;
		}
		
		for (int i = (page - 1) * perpage; i < map.size(); i++)
		{
			DressMeArmorData dress = map.get(i + 1);
			if (dress != null)
			{
				block = template;
				
				String dress_name = dress.getName();
				
				if (dress_name.length() > 29)
				{
					dress_name = dress_name.substring(0, 29) + "...";
				}
				
				block = block.replace("{bypass}", "bypass -h voice .dress-armorpage " + dress.getId());
				block = block.replace("{name}", dress_name);
				block = block.replace("{price}", Util.formatPay(player, dress.getPriceCount(), dress.getPriceId()));
				block = block.replace("{icon}", Util.getItemIcon(dress.getChest()));
				list += block;
			}
			
			counter++;
			
			if (counter >= perpage)
			{
				break;
			}
		}
		
		double count = Math.ceil((double) map.size() / perpage);
		int inline = 1;
		String navigation = "";
		
		for (int i = 1; i <= count; i++)
		{
			if (i == page)
			{
				navigation += "<td width=25 align=center valign=top><button value=\"[" + i + "]\" action=\"bypass -h voice .dressme-armor " + i + "\" width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
			}
			else
			{
				navigation += "<td width=25 align=center valign=top><button value=\"" + i + "\" action=\"bypass -h voice .dressme-armor " + i + "\" width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
			}
			
			if (inline == 7)
			{
				navigation += "</tr><tr>";
				inline = 0;
			}
			inline++;
		}
		
		if (navigation.equals(""))
		{
			navigation = "<td width=30 align=center valign=top>...</td>";
		}
		
		html = html.replace("{list}", list);
		html = html.replace("{navigation}", navigation);
		
		NpcHtmlMessage msg = new NpcHtmlMessage();
		msg.setHtml(html);
		player.sendPacket(msg);
		return true;
	}
	
	/**
	 * Implementation of the "dressme-cloak" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_dressme_cloak(Player player, String params)
	{
		final String args = params == null ? "1" : params;
		Item slot = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CLOAK);
		if ((slot == null) || !slot.isArmor())
		{
			player.sendMessage("Error: Cloak must be equiped!");
			return false;
		}
		String html = HtmCache.getInstance().getHtm(player, index_cloak);
		String template = HtmCache.getInstance().getHtm(player, template_cloak_path);
		String block = "";
		String list = "";
		String[] param = args.split(" ");
		
		final int page = !param[0].isEmpty() ? Integer.parseInt(param[0]) : 1;
		final int perpage = ITEMS_PER_PAGE;
		int counter = 0;
		
		for (int i = (page - 1) * perpage; i < DressMeCloakHolder.getInstance().size(); i++)
		{
			DressMeCloakData cloak = DressMeCloakHolder.getInstance().getCloak(i + 1);
			if (cloak != null)
			{
				block = template;
				
				String cloak_name = cloak.getName();
				
				if (cloak_name.length() > 29)
				{
					cloak_name = cloak_name.substring(0, 29) + "...";
				}
				
				block = block.replace("{bypass}", "bypass -h voice .dress-cloakpage " + (i + 1));
				block = block.replace("{name}", cloak_name);
				block = block.replace("{price}", Util.formatPay(player, cloak.getPriceCount(), cloak.getPriceId()));
				block = block.replace("{icon}", Util.getItemIcon(cloak.getCloakId()));
				list += block;
			}
			
			counter++;
			
			if (counter >= perpage)
			{
				break;
			}
		}
		
		double count = Math.ceil((double) DressMeCloakHolder.getInstance().size() / perpage);
		int inline = 1;
		String navigation = "";
		
		for (int i = 1; i <= count; i++)
		{
			if (i == page)
			{
				navigation += "<td width=25 align=center valign=top><button value=\"[" + i + "]\" action=\"bypass -h voice .dressme-cloak " + i + "\" width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
			}
			else
			{
				navigation += "<td width=25 align=center valign=top><button value=\"" + i + "\" action=\"bypass -h voice .dressme-cloak " + i + "\" width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
			}
			
			if (inline == 7)
			{
				navigation += "</tr><tr>";
				inline = 0;
			}
			inline++;
		}
		
		if (navigation.equals(""))
		{
			navigation = "<td width=30 align=center valign=top>...</td>";
		}
		
		html = html.replace("{list}", list);
		html = html.replace("{navigation}", navigation);
		
		sendHtml(player, html);
		return true;
	}
	
	/**
	 * Implementation of the "dressme-shield" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_dressme_shield(Player player, String params)
	{
		final String args = params == null ? "1" : params;
		Item slot = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if ((slot == null) || !slot.isArmor())
		{
			player.sendMessage("Error: Shield must be equiped!");
			return false;
		}
		
		String html = HtmCache.getInstance().getHtm(player, index_shield_path);
		String template = HtmCache.getInstance().getHtm(player, template_shield_path);
		String block = "";
		String list = "";
		String[] param = args.split(" ");
		
		final int page = !param[0].isEmpty() ? Integer.parseInt(param[0]) : 1;
		final int perpage = ITEMS_PER_PAGE;
		int counter = 0;
		
		for (int i = (page - 1) * perpage; i < DressMeShieldHolder.getInstance().size(); i++)
		{
			DressMeShieldData shield = DressMeShieldHolder.getInstance().getShield(i + 1);
			if (shield != null)
			{
				block = template;
				
				String shield_name = shield.getName();
				
				if (shield_name.length() > 29)
				{
					shield_name = shield_name.substring(0, 29) + "...";
				}
				
				block = block.replace("{bypass}", "bypass -h voice .dress-shieldpage " + (i + 1));
				block = block.replace("{name}", shield_name);
				block = block.replace("{price}", Util.formatPay(player, shield.getPriceCount(), shield.getPriceId()));
				block = block.replace("{icon}", Util.getItemIcon(shield.getShieldId()));
				list += block;
			}
			
			counter++;
			
			if (counter >= perpage)
			{
				break;
			}
		}
		
		double count = Math.ceil((double) DressMeShieldHolder.getInstance().size() / perpage);
		int inline = 1;
		String navigation = "";
		
		for (int i = 1; i <= count; i++)
		{
			if (i == page)
			{
				navigation += "<td width=25 align=center valign=top><button value=\"[" + i + "]\" action=\"bypass -h voice .dressme-shield " + i + "\" width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
			}
			else
			{
				navigation += "<td width=25 align=center valign=top><button value=\"" + i + "\" action=\"bypass -h voice .dressme-shield " + i + "\" width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
			}
			
			if (inline == 7)
			{
				navigation += "</tr><tr>";
				inline = 0;
			}
			inline++;
		}
		
		if (navigation.equals(""))
		{
			navigation = "<td width=30 align=center valign=top>...</td>";
		}
		
		html = html.replace("{list}", list);
		html = html.replace("{navigation}", navigation);
		
		sendHtml(player, html);
		return true;
	}
	
	/**
	 * Implementation of the "dressme-weapon" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_dressme_weapon(Player player, String params)
	{
		final String args = params == null ? "1" : params;
		Item slot = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (slot == null)
		{
			player.sendMessage("Error: Weapon must be equiped!");
			return false;
		}
		
		String html = HtmCache.getInstance().getHtm(player, index_weapon_path);
		String template = HtmCache.getInstance().getHtm(player, template_weapon_path);
		String block = "";
		String list = "";
		String[] param = args.split(" ");
		
		final int page = !param[0].isEmpty() ? Integer.parseInt(param[0]) : 1;
		final int perpage = ITEMS_PER_PAGE;
		int counter = 0;
		
		ItemType type = slot.getItemType();
		Map<Integer, DressMeWeaponData> map = DressMeHandler.initWeaponMap(type.toString(), slot);
		if (map == null)
		{
			LOGGER.info("DressMe system: Weapon Map is null.");
			return false;
		}
		
		for (int i = (page - 1) * perpage; i < map.size(); i++)
		{
			DressMeWeaponData weapon = map.get(i + 1);
			if (weapon != null)
			{
				block = template;
				
				String cloak_name = weapon.getName();
				
				if (cloak_name.length() > 29)
				{
					cloak_name = cloak_name.substring(0, 29) + "...";
				}
				
				block = block.replace("{bypass}", "bypass -h voice .dress-weaponpage " + weapon.getId());
				block = block.replace("{name}", cloak_name);
				block = block.replace("{price}", Util.formatPay(player, weapon.getPriceCount(), weapon.getPriceId()));
				block = block.replace("{icon}", Util.getItemIcon(weapon.getId()));
				list += block;
			}
			
			counter++;
			
			if (counter >= perpage)
			{
				break;
			}
		}
		
		double count = Math.ceil((double) map.size() / perpage);
		int inline = 1;
		String navigation = "";
		
		for (int i = 1; i <= count; i++)
		{
			if (i == page)
			{
				navigation += "<td width=25 align=center valign=top><button value=\"[" + i + "]\" action=\"bypass -h voice .dressme-weapon " + i + "\" width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
			}
			else
			{
				navigation += "<td width=25 align=center valign=top><button value=\"" + i + "\" action=\"bypass -h voice .dressme-weapon " + i + "\" width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
			}
			
			if (inline == 7)
			{
				navigation += "</tr><tr>";
				inline = 0;
			}
			inline++;
		}
		
		if (navigation.equals(""))
		{
			navigation = "<td width=30 align=center valign=top>...</td>";
		}
		
		html = html.replace("{list}", list);
		html = html.replace("{navigation}", navigation);
		
		sendHtml(player, html);
		return true;
	}
	
	/**
	 * Implementation of the "dressme-hat" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_dressme_hat(Player player, String params)
	{
		final String args = params == null ? "1" : params;
		Item slot = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR);
		if (slot == null)
		{
			slot = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR2);
		}
		
		if (slot == null)
		{
			player.sendMessage("Error: Hat must be equiped!");
			return false;
		}
		
		String html = HtmCache.getInstance().getHtm(player, index_hat_path);
		String template = HtmCache.getInstance().getHtm(player, template_hat_path);
		String block = "";
		String list = "";
		String[] param = args.split(" ");
		
		final int page = !param[0].isEmpty() ? Integer.parseInt(param[0]) : 1;
		final int perpage = ITEMS_PER_PAGE;
		int counter = 0;
		
		Map<Integer, DressMeHatData> map = DressMeHandler.initHatMap(slot);
		if (map == null)
		{
			LOGGER.info("DressMe system: Hat Map is null.");
			return false;
		}
		
		for (int i = (page - 1) * perpage; i < map.size(); i++)
		{
			DressMeHatData hat = map.get(i + 1);
			if (hat != null)
			{
				block = template;
				
				String hat_name = hat.getName();
				
				if (hat_name.length() > 29)
				{
					hat_name = hat_name.substring(0, 29) + "...";
				}
				
				block = block.replace("{bypass}", "bypass -h voice .dress-hatpage " + hat.getId());
				block = block.replace("{name}", hat_name);
				block = block.replace("{price}", Util.formatPay(player, hat.getPriceCount(), hat.getPriceId()));
				block = block.replace("{icon}", Util.getItemIcon(hat.getHatId()));
				list += block;
			}
			
			counter++;
			
			if (counter >= perpage)
			{
				break;
			}
		}
		
		double count = Math.ceil((double) map.size() / perpage);
		int inline = 1;
		String navigation = "";
		
		for (int i = 1; i <= count; i++)
		{
			if (i == page)
			{
				navigation += "<td width=25 align=center valign=top><button value=\"[" + i + "]\" action=\"bypass -h voice .dressme-hat " + i + "\" width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
			}
			else
			{
				navigation += "<td width=25 align=center valign=top><button value=\"" + i + "\" action=\"bypass -h voice .dressme-hat " + i + "\" width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>";
			}
			
			if (inline == 7)
			{
				navigation += "</tr><tr>";
				inline = 0;
			}
			inline++;
		}
		
		if (navigation.equals(""))
		{
			navigation = "<td width=30 align=center valign=top>...</td>";
		}
		
		html = html.replace("{list}", list);
		html = html.replace("{navigation}", navigation);
		
		sendHtml(player, html);
		return true;
	}
	
	/**
	 * Implementation of the "dress-armorpage" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_dress_armorpage(Player player, String params)
	{
		final String args = params == null ? "1" : params;
		final int armorSetId = Integer.parseInt(args.split(" ")[0]);
		final DressMeArmorData dress = DressMeArmorHolder.getInstance().getArmor(armorSetId);
		if (dress == null)
		{
			player.sendMessage("Error: The selected armor is out of bonds!");
			return false;
		}
		
		final ItemData itemData = ItemData.getInstance();
		final ItemTemplate chestTemplate = itemData.getTemplate(dress.getChest());
		if (chestTemplate == null)
		{
			LOGGER.warning("The DressMe Armor Set id '" + armorSetId + "' is mapped to an invalid chest piece id: " + dress.getChest() + ".");
			player.sendMessage("Error: The selected armor is invalid!");
			return false;
		}
		
		String html = HtmCache.getInstance().getHtm(player, dress_armor_path);
		final Inventory inv = player.getInventory();
		
		final Item my_chest = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		html = html.replace("{my_chest_icon}", my_chest == null ? "icon.NOIMAGE" : my_chest.getTemplate().getIcon());
		final Item my_legs = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		html = html.replace("{my_legs_icon}", my_legs == null ? "icon.NOIMAGE" : my_legs.getTemplate().getIcon());
		final Item my_gloves = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		html = html.replace("{my_gloves_icon}", my_gloves == null ? "icon.NOIMAGE" : my_gloves.getTemplate().getIcon());
		final Item my_feet = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		html = html.replace("{my_feet_icon}", my_feet == null ? "icon.NOIMAGE" : my_feet.getTemplate().getIcon());
		
		html = html.replace("{bypass}", "bypass -h voice .dress-armor " + armorSetId);
		html = html.replace("{name}", dress.getName());
		html = html.replace("{price}", Util.formatPay(player, dress.getPriceCount(), dress.getPriceId()));
		
		html = html.replace("{chest_icon}", chestTemplate.getIcon());
		html = html.replace("{chest_name}", chestTemplate.getName());
		html = html.replace("{chest_grade}", chestTemplate.getItemGrade().name());
		
		final ItemTemplate legsTemplate = itemData.getTemplate(dress.getLegs());
		if (legsTemplate != null)
		{
			html = html.replace("{legs_icon}", legsTemplate.getIcon());
			html = html.replace("{legs_name}", legsTemplate.getName());
			html = html.replace("{legs_grade}", legsTemplate.getItemGrade().name());
		}
		else
		{
			html = html.replace("{legs_icon}", "icon.NOIMAGE");
			html = html.replace("{legs_name}", "<font color=FF0000>...</font>");
			html = html.replace("{legs_grade}", "NO");
		}
		
		final ItemTemplate glovesTemplate = itemData.getTemplate(dress.getGloves());
		if (glovesTemplate != null)
		{
			html = html.replace("{gloves_icon}", glovesTemplate.getIcon());
			html = html.replace("{gloves_name}", glovesTemplate.getName());
			html = html.replace("{gloves_grade}", glovesTemplate.getItemGrade().name());
		}
		else
		{
			html = html.replace("{gloves_icon}", "icon.NOIMAGE");
			html = html.replace("{gloves_name}", "<font color=FF0000>...</font>");
			html = html.replace("{gloves_grade}", "NO");
		}
		
		final ItemTemplate feetTemplate = itemData.getTemplate(dress.getFeet());
		if (feetTemplate != null)
		{
			html = html.replace("{feet_icon}", feetTemplate.getIcon());
			html = html.replace("{feet_name}", feetTemplate.getName());
			html = html.replace("{feet_grade}", feetTemplate.getItemGrade().name());
		}
		else
		{
			html = html.replace("{feet_icon}", "icon.NOIMAGE");
			html = html.replace("{feet_name}", "<font color=FF0000>...</font>");
			html = html.replace("{feet_grade}", "NO");
		}
		
		sendHtml(player, html);
		return true;
	}
	
	/**
	 * Implementation of the "dress-cloakpage" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_dress_cloakpage(Player player, String params)
	{
		final String args = params == null ? "1" : params;
		final int set = Integer.parseInt(args.split(" ")[0]);
		DressMeCloakData cloak = DressMeCloakHolder.getInstance().getCloak(set);
		if (cloak != null)
		{
			String html = HtmCache.getInstance().getHtm(player, dress_cloak_path);
			
			Inventory inv = player.getInventory();
			Item my_cloak = inv.getPaperdollItem(Inventory.PAPERDOLL_CLOAK);
			html = html.replace("{my_cloak_icon}", my_cloak == null ? "icon.NOIMAGE" : my_cloak.getTemplate().getIcon());
			
			html = html.replace("{bypass}", "bypass -h voice .dress-cloak " + cloak.getId());
			html = html.replace("{name}", cloak.getName());
			html = html.replace("{price}", Util.formatPay(player, cloak.getPriceCount(), cloak.getPriceId()));
			
			ItemTemplate item = ItemData.getInstance().getTemplate(cloak.getCloakId());
			html = html.replace("{item_icon}", item.getIcon());
			html = html.replace("{item_name}", item.getName());
			html = html.replace("{item_grade}", item.getItemGrade().name());
			
			sendHtml(player, html);
			return true;
		}
		return false;
	}
	
	/**
	 * Implementation of the "dress-shieldpage" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_dress_shieldpage(Player player, String params)
	{
		final String args = params == null ? "1" : params;
		final int set = Integer.parseInt(args.split(" ")[0]);
		DressMeShieldData shield = DressMeShieldHolder.getInstance().getShield(set);
		if (shield != null)
		{
			String html = HtmCache.getInstance().getHtm(player, dress_shield_path);
			
			Inventory inv = player.getInventory();
			Item my_shield = inv.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
			html = html.replace("{my_shield_icon}", my_shield == null ? "icon.NOIMAGE" : my_shield.getTemplate().getIcon());
			
			html = html.replace("{bypass}", "bypass -h voice .dress-shield " + shield.getId());
			html = html.replace("{name}", shield.getName());
			html = html.replace("{price}", Util.formatPay(player, shield.getPriceCount(), shield.getPriceId()));
			
			ItemTemplate item = ItemData.getInstance().getTemplate(shield.getShieldId());
			html = html.replace("{item_icon}", item.getIcon());
			html = html.replace("{item_name}", item.getName());
			html = html.replace("{item_grade}", item.getItemGrade().name());
			
			sendHtml(player, html);
			return true;
		}
		return false;
	}
	
	/**
	 * Implementation of the "dress-weaponpage" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_dress_weaponpage(Player player, String params)
	{
		final String args = params == null ? "1" : params;
		final int set = Integer.parseInt(args.split(" ")[0]);
		DressMeWeaponData weapon = DressMeWeaponHolder.getInstance().getWeapon(set);
		if (weapon != null)
		{
			String html = HtmCache.getInstance().getHtm(player, dress_weapon_path);
			
			Inventory inv = player.getInventory();
			Item my_weapon = inv.getPaperdollItem(Inventory.PAPERDOLL_RHAND);
			html = html.replace("{my_weapon_icon}", my_weapon == null ? "icon.NOIMAGE" : my_weapon.getTemplate().getIcon());
			
			html = html.replace("{bypass}", "bypass -h voice .dress-weapon " + weapon.getId());
			html = html.replace("{name}", weapon.getName());
			html = html.replace("{price}", Util.formatPay(player, weapon.getPriceCount(), weapon.getPriceId()));
			
			ItemTemplate item = ItemData.getInstance().getTemplate(weapon.getId());
			html = html.replace("{item_icon}", item.getIcon());
			html = html.replace("{item_name}", item.getName());
			html = html.replace("{item_grade}", item.getItemGrade().name());
			
			sendHtml(player, html);
			return true;
		}
		return false;
	}
	
	/**
	 * Implementation of the "dress-hatpage" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_dress_hatpage(Player player, String params)
	{
		final String args = params == null ? "1" : params;
		final int set = Integer.parseInt(args.split(" ")[0]);
		DressMeHatData hat = DressMeHatHolder.getInstance().getHat(set);
		if (hat != null)
		{
			String html = HtmCache.getInstance().getHtm(player, dress_hat_path);
			
			Inventory inv = player.getInventory();
			
			Item my_hat = null;
			switch (hat.getSlot())
			{
				case 1, 3: // HAIR; FULL HAIR
					my_hat = inv.getPaperdollItem(Inventory.PAPERDOLL_HAIR);
					break;
				case 2: // HAIR2
					my_hat = inv.getPaperdollItem(Inventory.PAPERDOLL_HAIR2);
					break;
			}
			
			html = html.replace("{my_hat_icon}", my_hat == null ? "icon.NOIMAGE" : my_hat.getTemplate().getIcon());
			
			html = html.replace("{bypass}", "bypass -h voice .dress-hat " + hat.getId());
			html = html.replace("{name}", hat.getName());
			html = html.replace("{price}", Util.formatPay(player, hat.getPriceCount(), hat.getPriceId()));
			
			ItemTemplate item = ItemData.getInstance().getTemplate(hat.getHatId());
			html = html.replace("{item_icon}", item.getIcon());
			html = html.replace("{item_name}", item.getName());
			html = html.replace("{item_grade}", item.getItemGrade().name());
			
			sendHtml(player, html);
			return true;
		}
		return false;
	}
	
	/**
	 * Implementation of the "dressinfo" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_dressinfo(Player player, String params)
	{
		String html = HtmCache.getInstance().getHtm(player, info_path);
		sendHtml(player, html);
		return true;
	}
	
	/**
	 * Implementation of the "dress-armor" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_dress_armor(Player player, String params)
	{
		final String args = params == null ? "1" : params;
		final int set = Integer.parseInt(args.split(" ")[0]);
		
		DressMeArmorData dress = DressMeArmorHolder.getInstance().getArmor(set);
		Inventory inv = player.getInventory();
		
		Item chest = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		Item legs = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		Item gloves = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		Item feet = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		
		if (chest == null)
		{
			player.sendMessage("Error: Chest must be equiped.");
			useVoicedCommand("dress-armorpage", player, args);
			return false;
		}
		
		if (chest.getTemplate().getBodyPart() == ItemTemplate.SLOT_FULL_ARMOR)
		{
			ItemTemplate visual = ItemData.getInstance().getTemplate(dress.getChest());
			if ((chest.getTemplate().getBodyPart() != visual.getBodyPart()) && (visual.getBodyPart() != ItemTemplate.SLOT_ALLDRESS))
			{
				player.sendMessage("Error: You can't change visual in full armor type not full armors.");
				useVoicedCommand("dress-armorpage", player, args);
				return false;
			}
		}
		
		// Checks for armor set for the equipped chest.
		if (!ArmorSetData.getInstance().isArmorSet(chest.getId()))
		{
			player.sendMessage("Error: You can't visualize current set.");
			useVoicedCommand("dress-armorpage", player, args);
			return false;
		}
		
		ArmorSet armoSet = ArmorSetData.getInstance().getSet(chest.getId());
		if ((armoSet == null) || !armoSet.containAll(player))
		{
			player.sendMessage("Error: You can't visualize, set is not complete.");
			useVoicedCommand("dress-armorpage", player, args);
			return false;
		}
		
		if (!chest.getArmorItem().getItemType().getDescription().equals(dress.getType()))
		{
			player.sendMessage("Error: You can't visualize current set.");
			useVoicedCommand("dress-armorpage", player, args);
			return false;
		}
		
		if (checkPlayerItemCount(player, dress.getPriceId(), (int) dress.getPriceCount()))
		{
			player.destroyItemByItemId(ItemProcessType.DESTROY, dress.getPriceId(), dress.getPriceCount(), player, true);
			DressMeHandler.visuality(player, chest, dress.getChest());
			
			if (dress.getLegs() != -1)
			{
				DressMeHandler.visuality(player, legs, dress.getLegs());
			}
			else if ((dress.getLegs() == -1) && (chest.getTemplate().getBodyPart() != ItemTemplate.SLOT_FULL_ARMOR))
			{
				DressMeHandler.visuality(player, legs, dress.getChest());
			}
			
			if (dress.getGloves() == -1)
			{
				DressMeHandler.visuality(player, gloves, dress.getChest());
			}
			else
			{
				DressMeHandler.visuality(player, gloves, dress.getGloves());
			}
			
			if (dress.getFeet() == -1)
			{
				DressMeHandler.visuality(player, feet, dress.getChest());
			}
			else
			{
				DressMeHandler.visuality(player, feet, dress.getFeet());
			}
			player.broadcastUserInfo();
		}
		useVoicedCommand("dressme", player, null);
		return true;
	}
	
	/**
	 * Implementation of the "dress-cloak" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_dress_cloak(Player player, String params)
	{
		final String args = params == null ? "1" : params;
		final int set = Integer.parseInt(args.split(" ")[0]);
		
		DressMeCloakData cloak_data = DressMeCloakHolder.getInstance().getCloak(set);
		Inventory inv = player.getInventory();
		
		Item cloak = inv.getPaperdollItem(Inventory.PAPERDOLL_CLOAK);
		
		if (cloak == null)
		{
			player.sendMessage("Error: Cloak must be equiped.");
			useVoicedCommand("dress-cloakpage", player, args);
			return false;
		}
		
		if (checkPlayerItemCount(player, cloak_data.getPriceId(), (int) cloak_data.getPriceCount()))
		{
			player.destroyItemByItemId(ItemProcessType.DESTROY, cloak_data.getPriceId(), cloak_data.getPriceCount(), player, true);
			DressMeHandler.visuality(player, cloak, cloak_data.getCloakId());
		}
		player.broadcastUserInfo();
		useVoicedCommand("dressme", player, null);
		return true;
	}
	
	/**
	 * Implementation of the "dress-shield" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_dress_shield(Player player, String params)
	{
		final String args = params == null ? "1" : params;
		final int shield_id = Integer.parseInt(args.split(" ")[0]);
		
		DressMeShieldData shield_data = DressMeShieldHolder.getInstance().getShield(shield_id);
		Inventory inv = player.getInventory();
		
		Item shield = inv.getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		
		if (shield == null)
		{
			player.sendMessage("Error: Shield must be equiped.");
			useVoicedCommand("dress-shieldpage", player, args);
			return false;
		}
		
		if (checkPlayerItemCount(player, shield_data.getPriceId(), (int) shield_data.getPriceCount()))
		{
			player.destroyItemByItemId(ItemProcessType.DESTROY, shield_data.getPriceId(), shield_data.getPriceCount(), player, true);
			DressMeHandler.visuality(player, shield, shield_data.getShieldId());
		}
		player.broadcastUserInfo();
		useVoicedCommand("dressme", player, null);
		return true;
	}
	
	/**
	 * Implementation of the "dress-weapon" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_dress_weapon(Player player, String params)
	{
		final String args = params == null ? "1" : params;
		final int set = Integer.parseInt(args.split(" ")[0]);
		
		DressMeWeaponData weapon_data = DressMeWeaponHolder.getInstance().getWeapon(set);
		Inventory inv = player.getInventory();
		
		Item weapon = inv.getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		
		if (weapon == null)
		{
			player.sendMessage("Error: Weapon must be equiped.");
			useVoicedCommand("dress-weaponpage", player, args);
			return false;
		}
		
		if (!weapon.getItemType().toString().equals(weapon_data.getType()))
		{
			player.sendMessage("Error: Weapon must be equals type.");
			useVoicedCommand("dressme-weapon", player, null);
			return false;
		}
		
		if (checkPlayerItemCount(player, weapon_data.getPriceId(), (int) weapon_data.getPriceCount()))
		{
			player.destroyItemByItemId(ItemProcessType.DESTROY, weapon_data.getPriceId(), weapon_data.getPriceCount(), player, true);
			DressMeHandler.visuality(player, weapon, weapon_data.getId());
		}
		player.broadcastUserInfo();
		useVoicedCommand("dressme", player, null);
		return true;
	}
	
	/**
	 * Implementation of the "dress-hat" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_dress_hat(Player player, String params)
	{
		final String args = params == null ? "1" : params;
		final int set = Integer.parseInt(args.split(" ")[0]);
		
		DressMeHatData hat_data = DressMeHatHolder.getInstance().getHat(set);
		Inventory inv = player.getInventory();
		
		Item hat = null;
		switch (hat_data.getSlot())
		{
			case 1, 3: // HAIR; FULL HAIR
				hat = inv.getPaperdollItem(Inventory.PAPERDOLL_HAIR);
				break;
			case 2: // HAIR2
				hat = inv.getPaperdollItem(Inventory.PAPERDOLL_HAIR2);
				break;
		}
		
		if (hat == null)
		{
			player.sendMessage("Error: Hat must be equiped.");
			useVoicedCommand("dress-hatpage", player, args);
			return false;
		}
		
		ItemTemplate visual = ItemData.getInstance().getTemplate(hat_data.getHatId());
		if (hat.getTemplate().getBodyPart() != visual.getBodyPart())
		{
			player.sendMessage("Error: You can't change visual on different hat types!");
			useVoicedCommand("dress-hatpage", player, args);
			return false;
		}
		
		if (checkPlayerItemCount(player, hat_data.getPriceId(), (int) hat_data.getPriceCount()))
		{
			player.destroyItemByItemId(ItemProcessType.DESTROY, hat_data.getPriceId(), hat_data.getPriceCount(), player, true);
			DressMeHandler.visuality(player, hat, hat_data.getHatId());
		}
		player.broadcastUserInfo();
		useVoicedCommand("dressme", player, null);
		return true;
	}
	
	/**
	 * Implementation of the "undressme" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_undressme(Player player, String params)
	{
		String html = HtmCache.getInstance().getHtm(player, undressme_path);
		html = html.replace("<?show_hide?>", !player.getVarB("showVisualChange") ? "Show visual equip on other player!" : "Hide visual equip on other player!");
		html = html.replace("<?show_hide_b?>", !player.getVarB("showVisualChange") ? "showdress" : "hidedress");
		
		sendHtml(player, html);
		return true;
	}
	
	/**
	 * Implementation of the "undressme-armor" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_undressme_armor(Player player, String params)
	{
		Inventory inv = player.getInventory();
		Item chest = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		Item legs = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		Item gloves = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		Item feet = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);
		
		if (chest != null)
		{
			DressMeHandler.visuality(player, chest, 0);
		}
		if (legs != null)
		{
			DressMeHandler.visuality(player, legs, 0);
		}
		if (gloves != null)
		{
			DressMeHandler.visuality(player, gloves, 0);
		}
		if (feet != null)
		{
			DressMeHandler.visuality(player, feet, 0);
		}
		
		player.broadcastUserInfo();
		useVoicedCommand("undressme", player, null);
		return true;
	}
	
	/**
	 * Implementation of the "undressme-cloak" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_undressme_cloak(Player player, String params)
	{
		Item cloak = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CLOAK);
		if (cloak != null)
		{
			DressMeHandler.visuality(player, cloak, 0);
		}
		player.broadcastUserInfo();
		useVoicedCommand("undressme", player, null);
		return true;
	}
	
	/**
	 * Implementation of the "undressme-shield" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_undressme_shield(Player player, String params)
	{
		Item shield = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (shield != null)
		{
			DressMeHandler.visuality(player, shield, 0);
		}
		player.broadcastUserInfo();
		useVoicedCommand("undressme", player, null);
		return true;
	}
	
	/**
	 * Implementation of the "undressme-weapon" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_undressme_weapon(Player player, String params)
	{
		Item weapon = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (weapon != null)
		{
			DressMeHandler.visuality(player, weapon, 0);
		}
		player.broadcastUserInfo();
		useVoicedCommand("undressme", player, null);
		return true;
	}
	
	/**
	 * Implementation of the "undressme-hat" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_undressme_hat(Player player, String params)
	{
		Item slot = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR);
		if (slot == null)
		{
			slot = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HAIR2);
		}
		
		if (slot != null)
		{
			DressMeHandler.visuality(player, slot, 0);
		}
		player.broadcastUserInfo();
		useVoicedCommand("undressme", player, null);
		return true;
	}
	
	/**
	 * Implementation of the "showdress" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_showdress(Player player, String params)
	{
		player.setVar("showVisualChange", "true");
		player.broadcastUserInfo();
		useVoicedCommand("dressme", player, null);
		return true;
	}
	
	/**
	 * Implementation of the "hidedress" command.
	 * @param player The instance of the player that used the command.
	 * @param params The params required by the command.
	 * @return TRUE if the command was successful, FALSE otherwise.
	 */
	private boolean command_hidedress(Player player, String params)
	{
		player.setVar("showVisualChange", "false");
		player.broadcastUserInfo();
		useVoicedCommand("dressme", player, null);
		return true;
	}
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String params)
	{
		switch (command)
		{
			case "dressme":
				return command_Dressme(player, params);
			case "dressme-armor":
				return command_dressme_armor(player, params);
			case "dressme-cloak":
				return command_dressme_cloak(player, params);
			case "dressme-shield":
				return command_dressme_shield(player, params);
			case "dressme-weapon":
				return command_dressme_weapon(player, params);
			case "dressme-hat":
				return command_dressme_hat(player, params);
			case "dress-armorpage":
				return command_dress_armorpage(player, params);
			case "dress-cloakpage":
				return command_dress_cloakpage(player, params);
			case "dress-shieldpage":
				return command_dress_shieldpage(player, params);
			case "dress-weaponpage":
				return command_dress_weaponpage(player, params);
			case "dress-hatpage":
				return command_dress_hatpage(player, params);
			case "dressinfo":
				return command_dressinfo(player, params);
			case "dress-armor":
				return command_dress_armor(player, params);
			case "dress-cloak":
				return command_dress_cloak(player, params);
			case "dress-shield":
				return command_dress_shield(player, params);
			case "dress-weapon":
				return command_dress_weapon(player, params);
			case "dress-hat":
				return command_dress_hat(player, params);
			case "undressme":
				return command_undressme(player, params);
			case "undressme-armor":
				return command_undressme_armor(player, params);
			case "undressme-cloak":
				return command_undressme_cloak(player, params);
			case "undressme-shield":
				return command_undressme_shield(player, params);
			case "undressme-weapon":
				return command_undressme_weapon(player, params);
			case "undressme-hat":
				return command_undressme_hat(player, params);
			case "showdress":
				return command_showdress(player, params);
			case "hidedress":
				return command_hidedress(player, params);
			default:
				return false;
		}
	}
	
	/**
	 * Check the item count in inventory
	 * @param player [L2PcInstance]
	 * @param itemId
	 * @param count
	 * @return boolean
	 */
	public static boolean checkPlayerItemCount(Player player, int itemId, int count)
	{
		if ((player.getInventory().getItemByItemId(itemId) == null) || (player.getInventory().getItemByItemId(itemId).getCount() < count))
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_ENOUGH_REQUIRED_ITEMS);
			return false;
		}
		return true;
	}
	
	private void sendHtml(Player player, String html)
	{
		NpcHtmlMessage msg = new NpcHtmlMessage();
		msg.setHtml(html);
		player.sendPacket(msg);
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
