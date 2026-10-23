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
package com.l2journey.gameserver.network.clientpackets;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.Config;
import com.l2journey.EventsConfig;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.LoginServerThread;
import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.data.sql.AnnouncementsTable;
import com.l2journey.gameserver.data.sql.ClanHallTable;
import com.l2journey.gameserver.data.sql.OfflineTraderTable;
import com.l2journey.gameserver.data.xml.AdminData;
import com.l2journey.gameserver.data.xml.EnchantItemGroupsData;
import com.l2journey.gameserver.data.xml.SkillTreeData;
import com.l2journey.gameserver.managers.AntiFeedManager;
import com.l2journey.gameserver.managers.CHSiegeManager;
import com.l2journey.gameserver.managers.CastleManager;
import com.l2journey.gameserver.managers.CoupleManager;
import com.l2journey.gameserver.managers.CursedWeaponsManager;
import com.l2journey.gameserver.managers.DimensionalRiftManager;
import com.l2journey.gameserver.managers.FortManager;
import com.l2journey.gameserver.managers.FortSiegeManager;
import com.l2journey.gameserver.managers.HitmanManager;
import com.l2journey.gameserver.managers.InstanceManager;
import com.l2journey.gameserver.managers.MailManager;
import com.l2journey.gameserver.managers.PcCafePointsManager;
import com.l2journey.gameserver.managers.PetitionManager;
import com.l2journey.gameserver.managers.PunishmentManager;
import com.l2journey.gameserver.managers.ServerRestartManager;
import com.l2journey.gameserver.managers.SiegeManager;
import com.l2journey.gameserver.managers.TerritoryWarManager;
import com.l2journey.gameserver.model.Couple;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.appearance.PlayerAppearance;
import com.l2journey.gameserver.model.actor.enums.creature.Race;
import com.l2journey.gameserver.model.actor.enums.player.IllegalActionPunishmentType;
import com.l2journey.gameserver.model.actor.enums.player.TeleportWhereType;
import com.l2journey.gameserver.model.actor.instance.ClassMaster;
import com.l2journey.gameserver.model.clan.Clan;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.punishment.PunishmentAffect;
import com.l2journey.gameserver.model.punishment.PunishmentType;
import com.l2journey.gameserver.model.quest.Quest;
import com.l2journey.gameserver.model.residences.AuctionableHall;
import com.l2journey.gameserver.model.sevensigns.SevenSigns;
import com.l2journey.gameserver.model.siege.Castle;
import com.l2journey.gameserver.model.siege.Fort;
import com.l2journey.gameserver.model.siege.FortSiege;
import com.l2journey.gameserver.model.siege.Siege;
import com.l2journey.gameserver.model.siege.clanhalls.SiegableHall;
import com.l2journey.gameserver.model.skill.CommonSkill;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.variables.AccountVariables;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.network.ConnectionState;
import com.l2journey.gameserver.network.Disconnection;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.PacketLogger;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.enums.ChatType;
import com.l2journey.gameserver.network.holders.ClientHardwareInfoHolder;
import com.l2journey.gameserver.network.serverpackets.ActionFailed;
import com.l2journey.gameserver.network.serverpackets.CreatureSay;
import com.l2journey.gameserver.network.serverpackets.Die;
import com.l2journey.gameserver.network.serverpackets.EtcStatusUpdate;
import com.l2journey.gameserver.network.serverpackets.ExBasicActionList;
import com.l2journey.gameserver.network.serverpackets.ExBrPremiumState;
import com.l2journey.gameserver.network.serverpackets.ExGetBookMarkInfoPacket;
import com.l2journey.gameserver.network.serverpackets.ExNoticePostArrived;
import com.l2journey.gameserver.network.serverpackets.ExNotifyPremiumItem;
import com.l2journey.gameserver.network.serverpackets.ExRotation;
import com.l2journey.gameserver.network.serverpackets.ExShowContactList;
import com.l2journey.gameserver.network.serverpackets.ExShowScreenMessage;
import com.l2journey.gameserver.network.serverpackets.ExStorageMaxCount;
import com.l2journey.gameserver.network.serverpackets.ExVoteSystemInfo;
import com.l2journey.gameserver.network.serverpackets.FriendList;
import com.l2journey.gameserver.network.serverpackets.HennaInfo;
import com.l2journey.gameserver.network.serverpackets.ItemList;
import com.l2journey.gameserver.network.serverpackets.LeaveWorld;
import com.l2journey.gameserver.network.serverpackets.NpcHtmlMessage;
import com.l2journey.gameserver.network.serverpackets.PledgeShowMemberListAll;
import com.l2journey.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import com.l2journey.gameserver.network.serverpackets.PledgeSkillList;
import com.l2journey.gameserver.network.serverpackets.PledgeStatusChanged;
import com.l2journey.gameserver.network.serverpackets.PremiumState;
import com.l2journey.gameserver.network.serverpackets.QuestList;
import com.l2journey.gameserver.network.serverpackets.ShortcutInit;
import com.l2journey.gameserver.network.serverpackets.SkillCoolTime;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;
import com.l2journey.gameserver.network.serverpackets.UserInfo;
import com.l2journey.gameserver.network.serverpackets.ValidateLocation;
import com.l2journey.gameserver.taskmanagers.GameTimeTaskManager;
import com.l2journey.gameserver.util.Broadcast;

/**
 * Enter World Packet Handler
 * <p>
 * <p>
 * 0000: 03
 * <p>
 * packet format rev87 bddddbdcccccccccccccccccccc
 * <p>
 */
public class EnterWorld extends ClientPacket
{
	private static final Map<String, ClientHardwareInfoHolder> TRACE_HWINFO = new ConcurrentHashMap<>();
	
	private final int[][] _tracert = new int[5][4];
	
	@Override
	protected void readImpl()
	{
		readBytes(32); // Unknown Byte Array
		readInt(); // Unknown Value
		readInt(); // Unknown Value
		readInt(); // Unknown Value
		readInt(); // Unknown Value
		readBytes(32); // Unknown Byte Array
		readInt(); // Unknown Value
		for (int i = 0; i < 5; i++)
		{
			for (int o = 0; o < 4; o++)
			{
				_tracert[i][o] = readUnsignedByte();
			}
		}
	}
	
	@Override
	protected void runImpl()
	{
		final GameClient client = getClient();
		final Player player = client.getPlayer();
		if (player == null)
		{
			PacketLogger.warning("EnterWorld failed! player returned 'null'.");
			Disconnection.of(client).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
			
			return;
		}
		
		client.setConnectionState(ConnectionState.IN_GAME);
		
		final String[] adress = new String[5];
		for (int i = 0; i < 5; i++)
		{
			adress[i] = _tracert[i][0] + "." + _tracert[i][1] + "." + _tracert[i][2] + "." + _tracert[i][3];
		}
		
		LoginServerThread.getInstance().sendClientTracert(player.getAccountName(), adress);
		client.setClientTracert(_tracert);
		
		player.sendPacket(new UserInfo(player));
		
		// Restore to instanced area if enabled
		if (Config.RESTORE_PLAYER_INSTANCE)
		{
			player.setInstanceId(InstanceManager.getInstance().getPlayer(player.getObjectId()));
		}
		else
		{
			final int instanceId = InstanceManager.getInstance().getPlayer(player.getObjectId());
			if (instanceId > 0)
			{
				InstanceManager.getInstance().getInstance(instanceId).removePlayer(player.getObjectId());
			}
		}
		
		if (!player.isGM())
		{
			player.updatePvpTitleAndColor(false);
		}
		
		// Apply special GM properties to the GM when entering
		if (player.isGM())
		{
			gmStartupProcess:
			{
				if (Config.GM_STARTUP_BUILDER_HIDE && AdminData.getInstance().hasAccess("admin_hide", player.getAccessLevel()))
				{
					player.setHiding(true);
					player.sendSysMessage("hide is default for builder.");
					player.sendSysMessage("FriendAddOff is default for builder.");
					player.sendSysMessage("whisperoff is default for builder.");
				}
				
				// It isn't recommend to use the below custom L2J GMStartup functions together with retail-like GMStartupBuilderHide, so breaking the process at that stage.
				break gmStartupProcess;
			}
			
			if (Config.GM_STARTUP_INVULNERABLE && AdminData.getInstance().hasAccess("admin_invul", player.getAccessLevel()))
			{
				player.setInvul(true);
			}
			
			if (Config.GM_STARTUP_INVISIBLE && AdminData.getInstance().hasAccess("admin_invisible", player.getAccessLevel()))
			{
				player.setInvisible(true);
			}
			
			if (Config.GM_STARTUP_SILENCE && AdminData.getInstance().hasAccess("admin_silence", player.getAccessLevel()))
			{
				player.setSilenceMode(true);
			}
			
			if (Config.GM_STARTUP_DIET_MODE && AdminData.getInstance().hasAccess("admin_diet", player.getAccessLevel()))
			{
				player.setDietMode(true);
				player.refreshOverloaded();
			}
			
			AdminData.getInstance().addGm(player, !(Config.GM_STARTUP_AUTO_LIST && AdminData.getInstance().hasAccess("admin_gmliston", player.getAccessLevel())));
			
			if (Config.GM_GIVE_SPECIAL_SKILLS)
			{
				SkillTreeData.getInstance().addSkills(player, false);
			}
			
			if (Config.GM_GIVE_SPECIAL_AURA_SKILLS)
			{
				SkillTreeData.getInstance().addSkills(player, true);
			}
		}
		
		// Set dead status if applies
		if (player.getCurrentHp() < 0.5)
		{
			player.setDead(true);
		}
		
		boolean showClanNotice = false;
		
		// Clan related checks are here
		final Clan clan = player.getClan();
		if (clan != null)
		{
			player.sendPacket(new PledgeSkillList(clan));
			notifyClanMembers(player);
			
			notifySponsorOrApprentice(player);
			
			if (player.getClan().getLeaderName().equals(player.getName()) && (player.getClan().getCastleId() > 0) && Config.ANNOUNCE_CASTLE_LORD && !player.isGM())
			{
				Map<Integer, String> castleNames = new HashMap<>();
				castleNames.put(1, "Gludio");
				castleNames.put(2, "Dion");
				castleNames.put(3, "Giran");
				castleNames.put(4, "Oren");
				castleNames.put(5, "Aden");
				castleNames.put(6, "Innadril");
				castleNames.put(7, "Goddard");
				castleNames.put(8, "Rune");
				castleNames.put(9, "Schuttgart");
				
				int castleId = player.getClan().getCastleId();
				String castlename = castleNames.get(castleId);
				
				if (castlename != null)
				{
					Broadcast.toAllOnlinePlayers(player.getName() + ", the lord of " + castlename + " castle, has logged into the game.");
				}
			}
			
			final AuctionableHall clanHall = ClanHallTable.getInstance().getClanHallByOwner(clan);
			if ((clanHall != null) && !clanHall.getPaid())
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.PAYMENT_FOR_YOUR_CLAN_HALL_HAS_NOT_BEEN_MADE_PLEASE_MAKE_PAYMENT_TO_YOUR_CLAN_WAREHOUSE_BY_S1_TOMORROW);
				sm.addInt(clanHall.getLease());
				player.sendPacket(sm);
			}
			
			for (Siege siege : SiegeManager.getInstance().getSieges())
			{
				if (!siege.isInProgress())
				{
					continue;
				}
				
				if (siege.checkIsAttacker(clan))
				{
					player.setSiegeState((byte) 1);
					player.setSiegeSide(siege.getCastle().getResidenceId());
				}
				else if (siege.checkIsDefender(clan))
				{
					player.setSiegeState((byte) 2);
					player.setSiegeSide(siege.getCastle().getResidenceId());
				}
			}
			
			for (FortSiege siege : FortSiegeManager.getInstance().getSieges())
			{
				if (!siege.isInProgress())
				{
					continue;
				}
				
				if (siege.checkIsAttacker(clan))
				{
					player.setSiegeState((byte) 1);
					player.setSiegeSide(siege.getFort().getResidenceId());
				}
				else if (siege.checkIsDefender(clan))
				{
					player.setSiegeState((byte) 2);
					player.setSiegeSide(siege.getFort().getResidenceId());
				}
			}
			
			for (SiegableHall hall : CHSiegeManager.getInstance().getConquerableHalls().values())
			{
				if (!hall.isInSiege())
				{
					continue;
				}
				
				if (hall.isRegistered(clan))
				{
					player.setSiegeState((byte) 1);
					player.setSiegeSide(hall.getId());
					player.setInHideoutSiege(true);
				}
			}
			
			player.sendPacket(new PledgeShowMemberListAll(clan, player));
			player.sendPacket(new PledgeStatusChanged(clan));
			
			// Residential skills support
			if (clan.getCastleId() > 0)
			{
				final Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
				if (castle != null)
				{
					castle.giveResidentialSkills(player);
				}
			}
			
			if (clan.getFortId() > 0)
			{
				final Fort fort = FortManager.getInstance().getFortByOwner(clan);
				if (fort != null)
				{
					fort.giveResidentialSkills(player);
				}
			}
			
			showClanNotice = clan.isNoticeEnabled();
		}
		
		if (TerritoryWarManager.getInstance().getRegisteredTerritoryId(player) > 0)
		{
			if (TerritoryWarManager.getInstance().isTWInProgress())
			{
				player.setSiegeState((byte) 1);
			}
			
			player.setSiegeSide(TerritoryWarManager.getInstance().getRegisteredTerritoryId(player));
		}
		
		// Updating Seal of Strife Buff/Debuff
		if (SevenSigns.getInstance().isSealValidationPeriod() && (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) != SevenSigns.CABAL_NULL))
		{
			final int cabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
			if (cabal != SevenSigns.CABAL_NULL)
			{
				if (cabal == SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE))
				{
					player.addSkill(CommonSkill.THE_VICTOR_OF_WAR.getSkill());
				}
				else
				{
					player.addSkill(CommonSkill.THE_VANQUISHED_OF_WAR.getSkill());
				}
			}
		}
		else
		{
			player.removeSkill(CommonSkill.THE_VICTOR_OF_WAR.getSkill());
			player.removeSkill(CommonSkill.THE_VANQUISHED_OF_WAR.getSkill());
		}
		
		if (Config.ENABLE_VITALITY && Config.RECOVER_VITALITY_ON_RECONNECT)
		{
			final float points = (Config.RATE_RECOVERY_ON_RECONNECT * (System.currentTimeMillis() - player.getLastAccess())) / 60000;
			if (points > 0)
			{
				player.updateVitalityPoints(points, false, true);
			}
		}
		
		if (Config.NEVIT_ENABLED)
		{
			player.checkRecoBonusTask();
		}
		
		// Send Macro List
		player.getMacros().sendUpdate();
		
		// Send Item List
		player.sendPacket(new ItemList(player, false));
		
		// Send Teleport Bookmark List
		player.sendPacket(new ExGetBookMarkInfoPacket(player));
		
		// Send Shortcuts
		player.sendPacket(new ShortcutInit(player));
		
		// Send Action list - disable only the player's pet actions if no summon
		if (player.hasSummon())
		{
			player.sendPacket(ExBasicActionList.STATIC_PACKET);
		}
		else
		{
			player.sendPacket(ExBasicActionList.getPacketWithoutPlayerPetActions());
		}
		
		// Send Dye Information
		player.sendPacket(new HennaInfo(player));
		Quest.playerEnter(player);
		
		// Faction System
		if (EventsConfig.FACTION_SYSTEM_ENABLED)
		{
			final PlayerAppearance appearance = player.getAppearance();
			if (player.isGood())
			{
				appearance.setNameColor(EventsConfig.FACTION_GOOD_NAME_COLOR);
				appearance.setTitleColor(EventsConfig.FACTION_GOOD_NAME_COLOR);
				player.sendMessage("Welcome " + player.getName() + ", you are fighting for the " + EventsConfig.FACTION_GOOD_TEAM_NAME + " faction.");
				player.sendPacket(new ExShowScreenMessage("Welcome " + player.getName() + ", you are fighting for the " + EventsConfig.FACTION_GOOD_TEAM_NAME + " faction.", 10000));
				player.updateUserInfo(); // for seeing self name color
			}
			else if (player.isEvil())
			{
				appearance.setNameColor(EventsConfig.FACTION_EVIL_NAME_COLOR);
				appearance.setTitleColor(EventsConfig.FACTION_EVIL_NAME_COLOR);
				player.sendMessage("Welcome " + player.getName() + ", you are fighting for the " + EventsConfig.FACTION_EVIL_TEAM_NAME + " faction.");
				player.sendPacket(new ExShowScreenMessage("Welcome " + player.getName() + ", you are fighting for the " + EventsConfig.FACTION_EVIL_TEAM_NAME + " faction.", 10000));
				player.updateUserInfo(); // for seeing self name color
			}
		}
		
		// Login systems above can replace appearance colors; purchased choices win on login.
		player.restoreCommunityColors();

		// Hitman Event - notify player about bounties
		if (EventsConfig.HITMAN_ENABLED)
		{
			HitmanManager.getInstance().onEnterWorld(player);
		}
		
		player.sendPacket(new QuestList(player));
		if (Config.PLAYER_SPAWN_PROTECTION > 0)
		{
			player.setSpawnProtection(true);
		}
		
		player.spawnMe(player.getX(), player.getY(), player.getZ());
		player.sendPacket(new ExRotation(player.getObjectId(), player.getHeading()));
		player.sendPacket(new PremiumState(player.getObjectId(), player.hasPremiumStatus() ? 1 : 0));
		player.sendPacket(new ExBrPremiumState(player.getObjectId(), player.hasPremiumStatus() ? 1 : 0));
		
		// Wedding Checks
		if (EventsConfig.ALLOW_WEDDING)
		{
			engage(player);
			notifyPartner(player);
		}
		
		if (player.isCursedWeaponEquipped())
		{
			CursedWeaponsManager.getInstance().getCursedWeapon(player.getCursedWeaponEquippedId()).cursedOnLogin();
		}
		
		player.updateEffectIcons();
		
		player.sendPacket(new EtcStatusUpdate(player));
		
		// Expand Skill
		player.sendPacket(new ExStorageMaxCount(player));
		player.sendPacket(new FriendList(player));
		SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_FRIEND_S1_JUST_LOGGED_IN);
		sm.addString(player.getName());
		for (int id : player.getFriendList())
		{
			final WorldObject obj = World.getInstance().findObject(id);
			if (obj != null)
			{
				obj.sendPacket(sm);
			}
		}
		
		player.sendPacket(SystemMessageId.WELCOME_TO_THE_WORLD_OF_LINEAGE_II);
		
		SevenSigns.getInstance().sendCurrentPeriodMsg(player);
		
		AnnouncementsTable.getInstance().showAnnouncements(player);
		
		if ((Config.SERVER_RESTART_SCHEDULE_ENABLED) && (Config.SERVER_RESTART_SCHEDULE_MESSAGE))
		{
			player.sendPacket(new CreatureSay(null, ChatType.BATTLEFIELD, "[SERVER]", "Next restart is scheduled at " + ServerRestartManager.getInstance().getNextRestartTime() + "."));
		}
		
		if (showClanNotice)
		{
			final NpcHtmlMessage notice = new NpcHtmlMessage();
			notice.setFile(player, "data/html/clanNotice.htm");
			notice.replace("%clan_name%", player.getClan().getName());
			notice.replace("%notice_text%", player.getClan().getNotice().replaceAll("(\r\n|\n)", "<br>"));
			notice.disableValidation();
			player.sendPacket(notice);
		}
		else if (Config.SERVER_NEWS)
		{
			final String serverNews = HtmCache.getInstance().getHtm(player, "data/html/servnews.htm");
			if (serverNews != null)
			{
				player.sendPacket(new NpcHtmlMessage(serverNews));
			}
		}
		
		if (Config.PETITIONING_ALLOWED)
		{
			PetitionManager.getInstance().checkPetitionMessages(player);
		}
		
		if (player.isAlikeDead()) // dead or fake dead
		{
			// no broadcast needed since the player will already spawn dead to others
			player.sendPacket(new Die(player));
		}
		
		player.onPlayerEnter();
		
		// Apply item skills.
		player.getInventory().applyItemSkills();
		
		// Send Skill list
		player.sendSkillList();
		
		player.sendPacket(new SkillCoolTime(player));
		if (Config.NEVIT_ENABLED)
		{
			player.sendPacket(new ExVoteSystemInfo(player));
		}
		player.sendPacket(new ExShowContactList(player));
		for (Item i : player.getInventory().getItems())
		{
			if (i.isTimeLimitedItem())
			{
				i.scheduleLifeTimeTask();
			}
			if (i.isShadowItem() && i.isEquipped())
			{
				i.decreaseMana(false);
			}
		}
		
		for (Item i : player.getWarehouse().getItems())
		{
			if (i.isTimeLimitedItem())
			{
				i.scheduleLifeTimeTask();
			}
		}
		
		if (DimensionalRiftManager.getInstance().checkIfInRiftZone(player.getX(), player.getY(), player.getZ(), false))
		{
			DimensionalRiftManager.getInstance().teleportToWaitingRoom(player);
		}
		
		if (player.getClanJoinExpiryTime() > System.currentTimeMillis())
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_RECENTLY_BEEN_DISMISSED_FROM_A_CLAN_YOU_ARE_NOT_ALLOWED_TO_JOIN_ANOTHER_CLAN_FOR_24_HOURS);
		}
		
		// remove combat flag before teleporting
		if (player.getInventory().getItemByItemId(9819) != null)
		{
			final Fort fort = FortManager.getInstance().getFort(player);
			if (fort != null)
			{
				FortSiegeManager.getInstance().dropCombatFlag(player, fort.getResidenceId());
			}
			else
			{
				final int slot = player.getInventory().getSlotFromItem(player.getInventory().getItemByItemId(9819));
				player.getInventory().unEquipItemInBodySlot(slot);
				player.destroyItem(ItemProcessType.DESTROY, player.getInventory().getItemByItemId(9819), null, true);
			}
		}
		
		// Attacker or spectator logging in to a siege zone.
		// Actually should be checked for inside castle only?
		if (!player.isGM() && player.isInsideZone(ZoneId.SIEGE) && (!player.isInSiege() || (player.getSiegeState() < 2)))
		{
			player.teleToLocation(TeleportWhereType.TOWN);
		}
		
		// Over-enchant protection.
		if (Config.OVER_ENCHANT_PROTECTION && !player.isGM())
		{
			boolean punish = false;
			for (Item item : player.getInventory().getItems())
			{
				if (item.isEquipable() //
					&& ((item.isWeapon() && (item.getEnchantLevel() > EnchantItemGroupsData.getInstance().getMaxWeaponEnchant())) //
						|| ((item.getTemplate().getType2() == ItemTemplate.TYPE2_ACCESSORY) && (item.getEnchantLevel() > EnchantItemGroupsData.getInstance().getMaxAccessoryEnchant())) //
						|| (item.isArmor() && (item.getTemplate().getType2() != ItemTemplate.TYPE2_ACCESSORY) && (item.getEnchantLevel() > EnchantItemGroupsData.getInstance().getMaxArmorEnchant()))))
				{
					PacketLogger.info("Over-enchanted (+" + item.getEnchantLevel() + ") " + item + " has been removed from " + player);
					player.getInventory().destroyItem(ItemProcessType.DESTROY, item, player, null);
					punish = true;
				}
			}
			if (punish && (Config.OVER_ENCHANT_PUNISHMENT != IllegalActionPunishmentType.NONE))
			{
				player.sendMessage("[Server]: You have over-enchanted items!");
				player.sendMessage("[Server]: Respect our server rules.");
				player.sendPacket(new ExShowScreenMessage("You have over-enchanted items!", 6000));
				PunishmentManager.handleIllegalPlayerAction(player, player.getName() + " has over-enchanted items.", Config.OVER_ENCHANT_PUNISHMENT);
			}
		}
		
		// Remove demonic weapon if character is not cursed weapon equipped.
		if ((player.getInventory().getItemByItemId(8190) != null) && !player.isCursedWeaponEquipped())
		{
			player.destroyItem(ItemProcessType.DESTROY, player.getInventory().getItemByItemId(8190), null, true);
		}
		if ((player.getInventory().getItemByItemId(8689) != null) && !player.isCursedWeaponEquipped())
		{
			player.destroyItem(ItemProcessType.DESTROY, player.getInventory().getItemByItemId(8689), null, true);
		}
		
		if (Config.ALLOW_MAIL && MailManager.getInstance().hasUnreadPost(player))
		{
			player.sendPacket(ExNoticePostArrived.valueOf(false));
		}
		
		if (Config.WELCOME_MESSAGE_ENABLED)
		{
			player.sendPacket(new ExShowScreenMessage(Config.WELCOME_MESSAGE_TEXT, Config.WELCOME_MESSAGE_TIME));
		}
		
		ClassMaster.showQuestionMark(player);
		
		final int birthday = player.checkBirthDay();
		if (birthday == 0)
		{
			player.sendPacket(SystemMessageId.HAPPY_BIRTHDAY_ALEGRIA_HAS_SENT_YOU_A_BIRTHDAY_GIFT);
			// player.sendPacket(new ExBirthdayPopup()); Removed in H5?
		}
		else if (birthday != -1)
		{
			sm = new SystemMessage(SystemMessageId.THERE_ARE_S1_DAYS_REMAINING_UNTIL_YOUR_BIRTHDAY_ON_YOUR_BIRTHDAY_YOU_WILL_RECEIVE_A_GIFT_THAT_ALEGRIA_HAS_CAREFULLY_PREPARED);
			sm.addString(Integer.toString(birthday));
			player.sendPacket(sm);
		}
		
		if (!player.getPremiumItemList().isEmpty())
		{
			player.sendPacket(ExNotifyPremiumItem.STATIC_PACKET);
		}
		
		if ((Config.OFFLINE_TRADE_ENABLE || Config.OFFLINE_CRAFT_ENABLE) && Config.STORE_OFFLINE_TRADE_IN_REALTIME)
		{
			OfflineTraderTable.getInstance().onTransaction(player, true, false);
		}
		
		// Check if expoff is enabled.
		if (player.getVariables().getBoolean("EXPOFF", false))
		{
			player.disableExpGain();
			player.sendMessage("Experience gain is disabled.");
		}
		
		player.broadcastUserInfo();
		
		// Prevent relogin in game gfx.
		player.sendPacket(new ValidateLocation(player));
		
		// Unstuck players that had client open when server crashed.
		player.sendPacket(ActionFailed.STATIC_PACKET);
		
		// Delayed HWID checks.
		if (Config.HARDWARE_INFO_ENABLED)
		{
			ThreadPool.schedule(() ->
			{
				// Generate trace string.
				final StringBuilder sb = new StringBuilder();
				for (int[] i : _tracert)
				{
					for (int j : i)
					{
						sb.append(j);
						sb.append(".");
					}
				}
				final String trace = sb.toString();
				
				// Get hardware info from client.
				ClientHardwareInfoHolder hwInfo = client.getHardwareInfo();
				if (hwInfo != null)
				{
					hwInfo.store(player);
					TRACE_HWINFO.put(trace, hwInfo);
				}
				else
				{
					// Get hardware info from stored tracert map.
					hwInfo = TRACE_HWINFO.get(trace);
					if (hwInfo != null)
					{
						hwInfo.store(player);
						client.setHardwareInfo(hwInfo);
					}
					// Get hardware info from account variables.
					else
					{
						final String storedInfo = player.getAccountVariables().getString(AccountVariables.HWID, "");
						if (!storedInfo.isEmpty())
						{
							hwInfo = new ClientHardwareInfoHolder(storedInfo);
							TRACE_HWINFO.put(trace, hwInfo);
							client.setHardwareInfo(hwInfo);
						}
					}
				}
				
				// Banned?
				if ((hwInfo != null) && PunishmentManager.getInstance().hasPunishment(hwInfo.getMacAddress(), PunishmentAffect.HWID, PunishmentType.BAN))
				{
					Disconnection.of(client).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
					return;
				}
				
				// Check max players.
				if (Config.KICK_MISSING_HWID && (hwInfo == null))
				{
					Disconnection.of(client).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
				}
				else if (Config.MAX_PLAYERS_PER_HWID > 0)
				{
					int count = 0;
					for (Player plr : World.getInstance().getPlayers())
					{
						if (plr.isOnlineInt() == 1)
						{
							final ClientHardwareInfoHolder hwi = plr.getClient().getHardwareInfo();
							if ((hwi != null) && hwi.equals(hwInfo))
							{
								count++;
							}
						}
					}
					if (count > Config.MAX_PLAYERS_PER_HWID)
					{
						Disconnection.of(client).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
					}
				}
			}, 5000);
		}
		
		// Check if the player is a Dark Elf and has the Shadow Sense skill.
		if ((player.getRace() == Race.DARK_ELF))
		{
			final Skill shadowSense = player.getKnownSkill(CommonSkill.SHADOW_SENSE.getId());
			if (shadowSense != null)
			{
				boolean isNight = GameTimeTaskManager.getInstance().isNight();
				if (isNight)
				{
					player.sendPacket(new SystemMessage(SystemMessageId.IT_IS_NOW_MIDNIGHT_AND_THE_EFFECT_OF_S1_CAN_BE_FELT).addSkillName(shadowSense));
				}
				else
				{
					player.sendPacket(new SystemMessage(SystemMessageId.IT_IS_DAWN_AND_THE_EFFECT_OF_S1_WILL_NOW_DISAPPEAR).addSkillName(shadowSense));
				}
			}
		}
		
		AntiFeedManager.getInstance().removePlayer(AntiFeedManager.OFFLINE_PLAY, player);
		
		// EnterWorld has finished.
		player.setEnteredWorld();
		
		if ((player.hasPremiumStatus() || !EventsConfig.PC_CAFE_ONLY_PREMIUM) && EventsConfig.PC_CAFE_RETAIL_LIKE)
		{
			PcCafePointsManager.getInstance().run(player);
		}
	}
	
	private void engage(Player player)
	{
		final int chaId = player.getObjectId();
		for (Couple cl : CoupleManager.getInstance().getCouples())
		{
			if ((cl.getPlayer1Id() == chaId) || (cl.getPlayer2Id() == chaId))
			{
				if (cl.getMaried())
				{
					player.setMarried(true);
				}
				
				player.setCoupleId(cl.getId());
				
				if (cl.getPlayer1Id() == chaId)
				{
					player.setPartnerId(cl.getPlayer2Id());
				}
				else
				{
					player.setPartnerId(cl.getPlayer1Id());
				}
			}
		}
	}
	
	private void notifyPartner(Player player)
	{
		final int objId = player.getPartnerId();
		if (objId != 0)
		{
			final Player partner = World.getInstance().getPlayer(objId);
			if (partner != null)
			{
				partner.sendMessage("Your Partner has logged in.");
			}
		}
	}
	
	private void notifyClanMembers(Player player)
	{
		final Clan clan = player.getClan();
		if (clan != null)
		{
			clan.getClanMember(player.getObjectId()).setPlayer(player);
			
			final SystemMessage msg = new SystemMessage(SystemMessageId.CLAN_MEMBER_S1_HAS_LOGGED_INTO_GAME);
			msg.addString(player.getName());
			clan.broadcastToOtherOnlineMembers(msg, player);
			clan.broadcastToOtherOnlineMembers(new PledgeShowMemberListUpdate(player), player);
		}
	}
	
	private void notifySponsorOrApprentice(Player player)
	{
		if (player.getSponsor() != 0)
		{
			final Player sponsor = World.getInstance().getPlayer(player.getSponsor());
			if (sponsor != null)
			{
				final SystemMessage msg = new SystemMessage(SystemMessageId.YOUR_APPRENTICE_S1_HAS_LOGGED_IN);
				msg.addString(player.getName());
				sponsor.sendPacket(msg);
			}
		}
		else if (player.getApprentice() != 0)
		{
			final Player apprentice = World.getInstance().getPlayer(player.getApprentice());
			if (apprentice != null)
			{
				final SystemMessage msg = new SystemMessage(SystemMessageId.YOUR_SPONSOR_C1_HAS_LOGGED_IN);
				msg.addString(player.getName());
				apprentice.sendPacket(msg);
			}
		}
	}
}
