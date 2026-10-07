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
package com.l2journey.gameserver.model.actor.instance;

import java.io.File;
import java.util.Calendar;
import java.util.StringTokenizer;

import com.l2journey.Config;
import com.l2journey.EventsConfig;
import com.l2journey.gameserver.data.sql.TeleportLocationTable;
import com.l2journey.gameserver.managers.CastleManager;
import com.l2journey.gameserver.managers.SiegeManager;
import com.l2journey.gameserver.managers.TownManager;
import com.l2journey.gameserver.model.TeleportLocation;
import com.l2journey.gameserver.model.actor.Npc;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.enums.creature.InstanceType;
import com.l2journey.gameserver.model.actor.templates.NpcTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.ActionFailed;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author NightMarez
 */
public class Teleporter extends Npc
{
	private static final int COND_ALL_FALSE = 0;
	private static final int COND_BUSY_BECAUSE_OF_SIEGE = 1;
	private static final int COND_OWNER = 2;
	private static final int COND_REGULAR = 3;
	
	/**
	 * Creates a teleporter.
	 * @param template the teleporter NPC template
	 */
	public Teleporter(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Teleporter);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		final int condition = validateCondition(player);
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken(); // Get actual command
		if (player.isAffectedBySkill(6201) || player.isAffectedBySkill(6202) || player.isAffectedBySkill(6203))
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
			final String filename = "data/html/teleporter/epictransformed.htm";
			html.setFile(player, filename);
			html.replace("%objectId%", String.valueOf(getObjectId()));
			html.replace("%npcname%", getName());
			player.sendPacket(html);
			return;
		}
		else if (actualCommand.equalsIgnoreCase("goto"))
		{
			final int npcId = getId();
			
			switch (npcId)
			{
				case 32534, 32539: // Seed of Infinity
				{
					if (player.isFlyingMounted())
					{
						player.sendPacket(SystemMessageId.YOU_CANNOT_ENTER_A_SEED_WHILE_IN_A_FLYING_TRANSFORMATION_STATE);
						return;
					}
					break;
				}
			}
			
			if (st.countTokens() <= 0)
			{
				return;
			}
			
			final int whereTo = Integer.parseInt(st.nextToken());
			if (condition == COND_REGULAR)
			{
				doTeleport(player, whereTo);
				return;
			}
			else if (condition == COND_OWNER)
			{
				// TODO: Replace 0 with highest level when privilege level is implemented
				int minPrivilegeLevel = 0;
				if (st.countTokens() >= 1)
				{
					minPrivilegeLevel = Integer.parseInt(st.nextToken());
				}
				
				// TODO: Replace 10 with privilege level of player
				if (10 >= minPrivilegeLevel)
				{
					doTeleport(player, whereTo);
				}
				else
				{
					player.sendMessage("You don't have the sufficient access level to teleport there.");
				}
				return;
			}
		}
		else if (command.startsWith("Chat"))
		{
			final Calendar cal = Calendar.getInstance();
			int val = 0;
			try
			{
				val = Integer.parseInt(command.substring(5));
			}
			catch (IndexOutOfBoundsException _)
			{
			}
			catch (NumberFormatException _)
			{
			}
			
			if ((val == 1) && (player.getLevel() < 41))
			{
				showNewbieHtml(player);
				return;
			}
			else if ((val == 1) && (cal.get(Calendar.HOUR_OF_DAY) >= 20) && (cal.get(Calendar.HOUR_OF_DAY) <= 23) && ((cal.get(Calendar.DAY_OF_WEEK) == 1) || (cal.get(Calendar.DAY_OF_WEEK) == 7)))
			{
				showHalfPriceHtml(player);
				return;
			}
			showChatWindow(player, val);
		}
		
		super.onBypassFeedback(player, command);
	}
	
	@Override
	public String getHtmlPath(int npcId, int value)
	{
		String pom = "";
		if (value == 0)
		{
			pom = Integer.toString(npcId);
		}
		else
		{
			pom = npcId + "-" + value;
		}
		return "data/html/teleporter/" + pom + ".htm";
	}
	
	private void showNewbieHtml(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		String fileName = "data/html/teleporter/free/" + getTemplate().getId() + ".htm";
		final File file = new File(Config.DATAPACK_ROOT, fileName);
		if (!file.isFile())
		{
			fileName = "data/html/teleporter/" + getTemplate().getId() + "-1.htm";
		}
		
		html.setFile(player, fileName);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
	
	private void showHalfPriceHtml(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		String fileName = "data/html/teleporter/half/" + getId() + ".htm";
		final File file = new File(Config.DATAPACK_ROOT, fileName);
		if (!file.isFile())
		{
			fileName = "data/html/teleporter/" + getId() + "-1.htm";
		}
		
		html.setFile(player, fileName);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
	
	@Override
	public void showChatWindow(Player player)
	{
		String filename = "data/html/teleporter/castleteleporter-no.htm";
		
		final int condition = validateCondition(player);
		if (condition == COND_REGULAR)
		{
			super.showChatWindow(player);
			return;
		}
		else if (condition > COND_ALL_FALSE)
		{
			if (condition == COND_BUSY_BECAUSE_OF_SIEGE)
			{
				filename = "data/html/teleporter/castleteleporter-busy.htm"; // Busy because of siege
			}
			else if (condition == COND_OWNER) // Clan owns castle
			{
				filename = getHtmlPath(getId(), 0); // Owner message window
			}
		}
		
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
	
	private void doTeleport(Player player, int value)
	{
		final TeleportLocation list = TeleportLocationTable.getInstance().getTemplate(value);
		if (list != null)
		{
			if (EventsConfig.TW_DISABLE_GK && isInTownWarEvent())
			{
				player.sendMessage("You can't teleport during Town War Event.");
				return;
			}
			
			// you cannot teleport to village that is in siege
			if (SiegeManager.getInstance().getSiege(list.getLocX(), list.getLocY(), list.getLocZ()) != null)
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_TO_A_VILLAGE_THAT_IS_IN_A_SIEGE);
				return;
			}
			else if (TownManager.townHasCastleInSiege(list.getLocX(), list.getLocY()) && isInsideZone(ZoneId.TOWN))
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_TO_A_VILLAGE_THAT_IS_IN_A_SIEGE);
				return;
			}
			else if (!Config.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && (player.getKarma() > 0)) // karma
			{
				player.sendMessage("Go away, you're not welcome here.");
				return;
			}
			else if (Config.BLOCK_TELEPORT_IN_COMBAT_MODE && (player.getPvpFlag() > 0)) // Bloqueia o teleport quando estiver em modo de combat
			{
				player.sendMessage("Don't run from PvP!");
				return;
			}
			else if (player.isCombatFlagEquipped())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_WHILE_IN_POSSESSION_OF_A_WARD);
				return;
			}
			else if (list.isForNoble() && !player.isNoble())
			{
				final String filename = "data/html/teleporter/nobleteleporter-no.htm";
				final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
				html.setFile(player, filename);
				html.replace("%objectId%", String.valueOf(getObjectId()));
				html.replace("%npcname%", getName());
				player.sendPacket(html);
				return;
			}
			else if (player.isAlikeDead())
			{
				return;
			}
			
			int price = list.getPrice();
			if (player.getLevel() < 41)
			{
				price = 0;
			}
			else if (!list.isForNoble())
			{
				final Calendar cal = Calendar.getInstance();
				if ((cal.get(Calendar.HOUR_OF_DAY) >= 20) && (cal.get(Calendar.HOUR_OF_DAY) <= 23) && ((cal.get(Calendar.DAY_OF_WEEK) == 1) || (cal.get(Calendar.DAY_OF_WEEK) == 7)))
				{
					price /= 2;
				}
			}
			
			if (Config.FREE_TELEPORTING || player.destroyItemByItemId(ItemProcessType.FEE, list.getItemId(), price, this, true))
			{
				player.teleToLocation(list.getLocX(), list.getLocY(), list.getLocZ(), player.getHeading(), -1);
			}
			
			if (Config.TELEPORT_EFFECT)
			{
				player.broadcastPacket(new MagicSkillUse(player, player, 2036, 1, 0, 0));
			}
			
		}
		else
		{
			LOGGER.warning(() -> "No teleport destination with id:" + value);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	private int validateCondition(Player player)
	{
		// Teleporter isn't on castle ground
		if (CastleManager.getInstance().getCastleIndex(this) < 0)
		{
			return COND_REGULAR; // Regular access
		}
		// Teleporter is on castle ground and siege is in progress
		else if (getCastle().getSiege().isInProgress())
		{
			return COND_BUSY_BECAUSE_OF_SIEGE; // Busy because of siege
		}
		// Teleporter is on castle ground and player is in a clan
		else if ((player.getClan() != null) && (getCastle().getOwnerId() == player.getClanId()))
		{
			return COND_OWNER; // Owner
		}
		return COND_ALL_FALSE;
	}
}
