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
package com.l2journey.gameserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.l2journey.Config;
import com.l2journey.EventsConfig;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.commons.enums.ServerMode;
import com.l2journey.commons.network.ConnectionManager;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.commons.util.DeadlockWatcher;
import com.l2journey.gameserver.cache.HtmCache;
import com.l2journey.gameserver.data.AugmentationData;
import com.l2journey.gameserver.data.BotReportTable;
import com.l2journey.gameserver.data.MerchantPriceConfigTable;
import com.l2journey.gameserver.data.SchemeBufferTable;
import com.l2journey.gameserver.data.sql.AnnouncementsTable;
import com.l2journey.gameserver.data.sql.CharInfoTable;
import com.l2journey.gameserver.data.sql.CharSummonTable;
import com.l2journey.gameserver.data.sql.ClanHallTable;
import com.l2journey.gameserver.data.sql.ClanTable;
import com.l2journey.gameserver.data.sql.CrestTable;
import com.l2journey.gameserver.data.sql.OfflineTraderTable;
import com.l2journey.gameserver.data.sql.TeleportLocationTable;
import com.l2journey.gameserver.data.xml.AdminData;
import com.l2journey.gameserver.data.xml.ArmorSetData;
import com.l2journey.gameserver.data.xml.BuyListData;
import com.l2journey.gameserver.data.xml.CategoryData;
import com.l2journey.gameserver.data.xml.ClassListData;
import com.l2journey.gameserver.data.xml.DoorData;
import com.l2journey.gameserver.data.xml.DynamicExpRateData;
import com.l2journey.gameserver.data.xml.ElementalAttributeData;
import com.l2journey.gameserver.data.xml.EnchantItemData;
import com.l2journey.gameserver.data.xml.EnchantItemGroupsData;
import com.l2journey.gameserver.data.xml.EnchantItemHPBonusData;
import com.l2journey.gameserver.data.xml.EnchantItemOptionsData;
import com.l2journey.gameserver.data.xml.EnchantSkillGroupsData;
import com.l2journey.gameserver.data.xml.ExperienceData;
import com.l2journey.gameserver.data.xml.FenceData;
import com.l2journey.gameserver.data.xml.FishData;
import com.l2journey.gameserver.data.xml.FishingMonstersData;
import com.l2journey.gameserver.data.xml.FishingRodsData;
import com.l2journey.gameserver.data.xml.HennaData;
import com.l2journey.gameserver.data.xml.HitConditionBonusData;
import com.l2journey.gameserver.data.xml.InitialEquipmentData;
import com.l2journey.gameserver.data.xml.InitialShortcutData;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.data.xml.KarmaData;
import com.l2journey.gameserver.data.xml.LevelUpCrystalData;
import com.l2journey.gameserver.data.xml.MultisellData;
import com.l2journey.gameserver.data.xml.NpcData;
import com.l2journey.gameserver.data.xml.NpcNameLocalisationData;
import com.l2journey.gameserver.data.xml.OptionData;
import com.l2journey.gameserver.data.xml.PetDataTable;
import com.l2journey.gameserver.data.xml.PetSkillData;
import com.l2journey.gameserver.data.xml.PlayerTemplateData;
import com.l2journey.gameserver.data.xml.PlayerXpPercentLostData;
import com.l2journey.gameserver.data.xml.PrimeShopData;
import com.l2journey.gameserver.data.xml.RecipeData;
import com.l2journey.gameserver.data.xml.SecondaryAuthData;
import com.l2journey.gameserver.data.xml.SendMessageLocalisationData;
import com.l2journey.gameserver.data.xml.SiegeScheduleData;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.data.xml.SkillLearnData;
import com.l2journey.gameserver.data.xml.SkillTreeData;
import com.l2journey.gameserver.data.xml.SpawnData;
import com.l2journey.gameserver.data.xml.StaticObjectData;
import com.l2journey.gameserver.data.xml.TransformData;
import com.l2journey.gameserver.data.xml.UIData;
import com.l2journey.gameserver.handler.EffectHandler;
import com.l2journey.gameserver.managers.AirShipManager;
import com.l2journey.gameserver.managers.AntiFeedManager;
import com.l2journey.gameserver.managers.BoatManager;
import com.l2journey.gameserver.managers.CHSiegeManager;
import com.l2journey.gameserver.managers.CaptchaManager;
import com.l2journey.gameserver.managers.CastleManager;
import com.l2journey.gameserver.managers.CastleManorManager;
import com.l2journey.gameserver.managers.ClanHallAuctionManager;
import com.l2journey.gameserver.managers.CoupleManager;
import com.l2journey.gameserver.managers.CursedWeaponsManager;
import com.l2journey.gameserver.managers.CustomMailManager;
import com.l2journey.gameserver.managers.DailyRewardManager;
import com.l2journey.gameserver.managers.DayNightSpawnManager;
import com.l2journey.gameserver.managers.DimensionalRiftManager;
import com.l2journey.gameserver.managers.EventDropManager;
import com.l2journey.gameserver.managers.FishingChampionshipManager;
import com.l2journey.gameserver.managers.FortManager;
import com.l2journey.gameserver.managers.FortSiegeManager;
import com.l2journey.gameserver.managers.GlobalVariablesManager;
import com.l2journey.gameserver.managers.GrandBossManager;
import com.l2journey.gameserver.managers.HitmanManager;
import com.l2journey.gameserver.managers.IdManager;
import com.l2journey.gameserver.managers.InstanceManager;
import com.l2journey.gameserver.managers.ItemAuctionManager;
import com.l2journey.gameserver.managers.ItemsOnGroundManager;
import com.l2journey.gameserver.managers.MailManager;
import com.l2journey.gameserver.managers.MapRegionManager;
import com.l2journey.gameserver.managers.MercTicketManager;
import com.l2journey.gameserver.managers.PcCafePointsManager;
import com.l2journey.gameserver.managers.PetitionManager;
import com.l2journey.gameserver.managers.PrecautionaryRestartManager;
import com.l2journey.gameserver.managers.PremiumManager;
import com.l2journey.gameserver.managers.PunishmentManager;
import com.l2journey.gameserver.managers.QuestManager;
import com.l2journey.gameserver.managers.RaidBossPointsManager;
import com.l2journey.gameserver.managers.RaidBossSpawnManager;
import com.l2journey.gameserver.managers.SellBuffsManager;
import com.l2journey.gameserver.managers.ServerRestartManager;
import com.l2journey.gameserver.managers.SiegeManager;
import com.l2journey.gameserver.managers.SoDManager;
import com.l2journey.gameserver.managers.SoIManager;
import com.l2journey.gameserver.managers.TerritoryWarManager;
import com.l2journey.gameserver.managers.TownWarManager;
import com.l2journey.gameserver.managers.WalkingManager;
import com.l2journey.gameserver.managers.ZoneManager;
import com.l2journey.gameserver.managers.games.KrateisCubeManager;
import com.l2journey.gameserver.managers.games.LotteryManager;
import com.l2journey.gameserver.managers.games.MonsterRaceManager;
import com.l2journey.gameserver.managers.games.UndergroundColiseumManager;
import com.l2journey.gameserver.model.AutoSpawnHandler;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.events.EventDispatcher;
import com.l2journey.gameserver.model.events.EventType;
import com.l2journey.gameserver.model.events.holders.OnServerStart;
import com.l2journey.gameserver.model.groups.matching.PartyMatchRoomList;
import com.l2journey.gameserver.model.groups.matching.PartyMatchWaitingList;
import com.l2journey.gameserver.model.olympiad.Hero;
import com.l2journey.gameserver.model.olympiad.Olympiad;
import com.l2journey.gameserver.model.sevensigns.SevenSigns;
import com.l2journey.gameserver.model.sevensigns.SevenSignsFestival;
import com.l2journey.gameserver.model.visualSystem.DressMeLoader;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.GamePacketHandler;
import com.l2journey.gameserver.network.NpcStringId;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.scripting.ScriptEngineManager;
import com.l2journey.gameserver.taskmanagers.GameTimeTaskManager;
import com.l2journey.gameserver.taskmanagers.ItemLifeTimeTaskManager;
import com.l2journey.gameserver.taskmanagers.ItemsAutoDestroyTaskManager;
import com.l2journey.gameserver.taskmanagers.PersistentTaskManager;
import com.l2journey.gameserver.util.Broadcast;
import com.l2journey.tools.dashboard.DashboardControlServer;

public class GameServer
{
	private static final Logger LOGGER = Logger.getLogger(GameServer.class.getName());
	
	private static final long START_TIME = System.currentTimeMillis();
	
	public GameServer() throws Exception
	{
		// Create log folder
		final File logFolder = new File(".", "log");
		logFolder.mkdir();
		
		// Create input stream for log file -- or store file data into memory
		try (InputStream is = new FileInputStream(new File("./log.cfg")))
		{
			LogManager.getLogManager().readConfiguration(is);
		}
		
		// Initialize config
		Config.load(ServerMode.GAME);
		EventsConfig.load();
		
		printSection("Database");
		DatabaseFactory.init();
		
		printSection("ThreadPool");
		ThreadPool.init();
		
		// Start game time task manager early
		GameTimeTaskManager.getInstance();
		
		printSection("IdManager");
		IdManager.getInstance();
		
		printSection("Scripting Engine");
		EventDispatcher.getInstance();
		ScriptEngineManager.getInstance();
		
		printSection("World");
		InstanceManager.getInstance();
		World.getInstance();
		DashboardControlServer.start();
		MapRegionManager.getInstance();
		AnnouncementsTable.getInstance();
		GlobalVariablesManager.getInstance();
		
		printSection("Data");
		CategoryData.getInstance();
		DynamicExpRateData.getInstance();
		SecondaryAuthData.getInstance();
		
		printSection("Skills");
		EffectHandler.getInstance().executeScript();
		EnchantSkillGroupsData.getInstance();
		SkillTreeData.getInstance();
		SkillData.getInstance();
		PetSkillData.getInstance();
		
		printSection("Items");
		ItemData.getInstance();
		EnchantItemGroupsData.getInstance();
		EnchantItemData.getInstance();
		EnchantItemOptionsData.getInstance();
		ElementalAttributeData.getInstance();
		OptionData.getInstance();
		EnchantItemHPBonusData.getInstance();
		MerchantPriceConfigTable.getInstance().loadInstances();
		BuyListData.getInstance();
		MultisellData.getInstance();
		RecipeData.getInstance();
		ArmorSetData.getInstance();
		FishData.getInstance();
		FishingMonstersData.getInstance();
		FishingRodsData.getInstance();
		HennaData.getInstance();
		PrimeShopData.getInstance();
		PcCafePointsManager.getInstance();
		ItemLifeTimeTaskManager.getInstance();
		
		printSection("Characters");
		ClassListData.getInstance();
		InitialEquipmentData.getInstance();
		InitialShortcutData.getInstance();
		ExperienceData.getInstance();
		PlayerXpPercentLostData.getInstance();
		KarmaData.getInstance();
		HitConditionBonusData.getInstance();
		PlayerTemplateData.getInstance();
		CharInfoTable.getInstance();
		AdminData.getInstance();
		RaidBossPointsManager.getInstance();
		PetDataTable.getInstance();
		CharSummonTable.getInstance().init();
		CaptchaManager.getInstance();
		
		if (Config.PREMIUM_SYSTEM_ENABLED)
		{
			LOGGER.info("PremiumManager: Premium system is enabled.");
			PremiumManager.getInstance();
		}
		
		printSection("Clans");
		ClanTable.getInstance();
		CHSiegeManager.getInstance();
		ClanHallTable.getInstance();
		ClanHallAuctionManager.getInstance();
		
		printSection("Geodata");
		GeoData.getInstance();
		
		printSection("NPCs");
		DoorData.getInstance();
		FenceData.getInstance();
		SkillLearnData.getInstance();
		NpcData.getInstance();
		LevelUpCrystalData.getInstance();
		WalkingManager.getInstance();
		StaticObjectData.getInstance();
		ItemAuctionManager.getInstance();
		CastleManager.getInstance().loadInstances();
		SchemeBufferTable.getInstance();
		ZoneManager.getInstance();
		GrandBossManager.getInstance().initZones();
		EventDropManager.getInstance();
		
		printSection("Olympiad");
		Olympiad.getInstance();
		Hero.getInstance();
		
		printSection("Seven Signs");
		SevenSigns.getInstance();
		
		// Call to load caches
		printSection("Cache");
		HtmCache.getInstance();
		CrestTable.getInstance();
		TeleportLocationTable.getInstance();
		UIData.getInstance();
		PartyMatchWaitingList.getInstance();
		PartyMatchRoomList.getInstance();
		PetitionManager.getInstance();
		AugmentationData.getInstance();
		CursedWeaponsManager.getInstance();
		TransformData.getInstance();
		BotReportTable.getInstance();
		if (Config.SELLBUFF_ENABLED)
		{
			SellBuffsManager.getInstance();
		}
		if (Config.DAILY_REWARD_ENABLED)
		{
			DailyRewardManager.getInstance();
		}
		if (EventsConfig.HITMAN_ENABLED)
		{
			HitmanManager.getInstance();
		}
		if (Config.MULTILANG_ENABLE)
		{
			SystemMessageId.loadLocalisations();
			NpcStringId.loadLocalisations();
			SendMessageLocalisationData.getInstance();
			NpcNameLocalisationData.getInstance();
		}
		
		printSection("Scripts");
		QuestManager.getInstance();
		BoatManager.getInstance();
		AirShipManager.getInstance();
		SoDManager.getInstance();
		SoIManager.getInstance();
		
		try
		{
			LOGGER.info("Loading server scripts...");
			ScriptEngineManager.getInstance().executeScript(ScriptEngineManager.MASTER_HANDLER_FILE);
			ScriptEngineManager.getInstance().executeScriptList();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Failed to execute script list!", e);
		}
		
		SpawnData.getInstance();
		DayNightSpawnManager.getInstance().trim().notifyChangeMode();
		DimensionalRiftManager.getInstance();
		RaidBossSpawnManager.getInstance();
		
		printSection("Siege");
		SiegeManager.getInstance().getSieges();
		CastleManager.getInstance().activateInstances();
		FortManager.getInstance().loadInstances();
		FortManager.getInstance().activateInstances();
		FortSiegeManager.getInstance();
		SiegeScheduleData.getInstance();
		MerchantPriceConfigTable.getInstance().updateReferences();
		TerritoryWarManager.getInstance();
		CastleManorManager.getInstance();
		MercTicketManager.getInstance();
		QuestManager.getInstance().report();
		if (Config.SAVE_DROPPED_ITEM)
		{
			ItemsOnGroundManager.getInstance();
		}
		if ((Config.AUTODESTROY_ITEM_AFTER > 0) || (Config.HERB_AUTO_DESTROY_TIME > 0))
		{
			ItemsAutoDestroyTaskManager.getInstance();
		}
		MonsterRaceManager.getInstance();
		LotteryManager.getInstance();
		SevenSigns.getInstance().spawnSevenSignsNPC();
		SevenSignsFestival.getInstance();
		AutoSpawnHandler.getInstance();
		LOGGER.info("AutoSpawnHandler: Loaded " + AutoSpawnHandler.getInstance().size() + " handlers in total.");
		if (EventsConfig.ALLOW_WEDDING)
		{
			CoupleManager.getInstance();
		}
		if (Config.ALT_FISH_CHAMPIONSHIP_ENABLED)
		{
			FishingChampionshipManager.getInstance();
		}
		KrateisCubeManager.getInstance();
		UndergroundColiseumManager.getInstance();
		PersistentTaskManager.getInstance();
		
		AntiFeedManager.getInstance().registerEvent(AntiFeedManager.GAME_ID);
		if (Config.ENABLE_OFFLINE_PLAY_COMMAND)
		{
			AntiFeedManager.getInstance().registerEvent(AntiFeedManager.OFFLINE_PLAY);
		}
		if (Config.ALLOW_MAIL)
		{
			MailManager.getInstance();
		}
		if (Config.CUSTOM_MAIL_MANAGER_ENABLED)
		{
			CustomMailManager.getInstance();
		}
		if (EventDispatcher.getInstance().hasListener(EventType.ON_SERVER_START))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnServerStart());
		}
		
		PunishmentManager.getInstance();
		
		if (Config.DRESSME_ENABLE)
		{
			DressMeLoader.load();
		}
		
		Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());
		
		LOGGER.info("-----------------------------------------------=[ IdManager ]");
		LOGGER.info("IdManager: Free ObjectID's remaining: " + IdManager.getInstance().getAvailableIdCount());
		
		LOGGER.info("-------------------------------------------=[ Events Engine ]");
		TownWarManager.getInstance();
		LOGGER.info("-------------------------------------------------------------");
		
		if ((Config.OFFLINE_TRADE_ENABLE || Config.OFFLINE_CRAFT_ENABLE) && Config.RESTORE_OFFLINERS)
		{
			OfflineTraderTable.getInstance().restoreOfflineTraders();
		}
		if (Config.SERVER_RESTART_SCHEDULE_ENABLED)
		{
			ServerRestartManager.getInstance();
		}
		if (Config.PRECAUTIONARY_RESTART_ENABLED)
		{
			PrecautionaryRestartManager.getInstance();
		}
		if (Config.DEADLOCK_WATCHER)
		{
			final DeadlockWatcher deadlockWatcher = new DeadlockWatcher(Duration.ofSeconds(Config.DEADLOCK_CHECK_INTERVAL), () ->
			{
				if (Config.RESTART_ON_DEADLOCK)
				{
					Broadcast.toAllOnlinePlayers("Server has stability issues - restarting now.");
					Shutdown.getInstance().startShutdown(null, 60, true);
				}
			});
			deadlockWatcher.setDaemon(true);
			deadlockWatcher.start();
		}
		
		System.gc();
		final long totalMem = Runtime.getRuntime().maxMemory() / 1048576;
		LOGGER.info(getClass().getSimpleName() + ": Started, using " + getUsedMemoryMB() + " of " + totalMem + " MB total memory.");
		LOGGER.info(getClass().getSimpleName() + ": Maximum number of connected players is " + Config.MAXIMUM_ONLINE_USERS + ".");
		LOGGER.info(getClass().getSimpleName() + ": Server loaded in " + ((System.currentTimeMillis() - START_TIME) / 1000) + " seconds.");
		
		new ConnectionManager<>(new InetSocketAddress(Config.PORT_GAME), GameClient::new, new GamePacketHandler());
		
		LoginServerThread.getInstance().start();
	}
	
	private void printSection(String section)
	{
		String s = "=[ " + section + " ]";
		while (s.length() < 61)
		{
			s = "-" + s;
		}
		LOGGER.info(s);
	}
	
	public long getUsedMemoryMB()
	{
		return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;
	}
	
	public static long getStartTime()
	{
		return START_TIME;
	}
	
	public static void main(String[] args) throws Exception
	{
		new GameServer();
	}
}
