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
package com.l2journey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.l2journey.commons.util.ConfigReader;
import com.l2journey.gameserver.model.Location;

// Assigned from ini files in load(), so these fields stay public static and non-final.
@SuppressWarnings({ "java:S3008", "java:S1444", "java:S1104" })
public class EventsConfig
{
	private static final Logger LOGGER = Logger.getLogger(EventsConfig.class.getName());
	
	private static final String ELPIES_EVENT_CONFIG_FILE = "./config/events/elpies.ini";
	private static final String FACTION_SYSTEM_CONFIG_FILE = "./config/events/factionsystem.ini";
	private static final String HITMAN_CONFIG_FILE = "./config/events/hitman.ini";
	private static final String LUCKY_PIG_EVENT_CONFIG_FILE = "./config/events/luckypig.ini";
	private static final String OLYMPIAD_CONFIG_FILE = "./config/events/olympiad.ini";
	private static final String PC_BANG_EVENT_CONFIG_FILE = "./config/events/pcbangpoints.ini";
	private static final String TOWN_WAR_EVENT_CONFIG_FILE = "./config/events/townwar.ini";
	private static final String UNDERGROUND_COLISEUM_CONFIG_FILE = "./config/events/undergroundcoliseum.ini";
	private static final String WEDDING_CONFIG_FILE = "./config/events/wedding.ini";
	
	// --------------------------------------------------
	// Elpies Event
	// --------------------------------------------------
	public static int ELPY_ID;
	public static int ELPY_AMOUNT;
	public static int ELPY_DURATION_MINUTES;
	
	// --------------------------------------------------
	// Hitman Event
	// --------------------------------------------------
	public static boolean HITMAN_ENABLED;
	public static boolean HITMAN_TAKE_KARMA;
	public static int HITMAN_TARGETS_LIMIT;
	public static boolean HITMAN_ANNOUNCE;
	public static int HITMAN_MAX_PER_PAGE;
	public static List<Integer> HITMAN_CURRENCY;
	public static long HITMAN_MIN_BOUNTY;
	public static boolean HITMAN_SAME_TEAM;
	public static int HITMAN_SAVE_INTERVAL;
	
	// --------------------------------------------------
	// Custom - Faction System
	// --------------------------------------------------
	public static boolean FACTION_SYSTEM_ENABLED;
	public static Location FACTION_STARTING_LOCATION;
	public static Location FACTION_MANAGER_LOCATION;
	public static Location FACTION_GOOD_BASE_LOCATION;
	public static Location FACTION_EVIL_BASE_LOCATION;
	public static String FACTION_GOOD_TEAM_NAME;
	public static String FACTION_EVIL_TEAM_NAME;
	public static int FACTION_GOOD_NAME_COLOR;
	public static int FACTION_EVIL_NAME_COLOR;
	public static boolean FACTION_GUARDS_ENABLED;
	public static boolean FACTION_RESPAWN_AT_BASE;
	public static boolean FACTION_AUTO_NOBLESS;
	public static boolean FACTION_SPECIFIC_CHAT;
	public static boolean FACTION_BALANCE_ONLINE_PLAYERS;
	public static int FACTION_BALANCE_PLAYER_EXCEED_LIMIT;
	
	// --------------------------------------------------
	// Lucky Pig [Event]
	// --------------------------------------------------
	public static boolean LUCKY_PID_SPAWN_ENABLED;
	public static int LUCKY_PID_LOW_ADENA;
	public static int LUCKY_PID_MEDIUM_ADENA;
	public static int LUCKY_PID_TOP_ADENA;
	public static int LUCKY_PID_CHANCE;
	
	// --------------------------------------------------
	// Olympiad
	// --------------------------------------------------
	public static boolean OLYMPIAD_ENABLED;
	public static int OLYMPIAD_START_TIME;
	public static int OLYMPIAD_MIN;
	public static int OLYMPIAD_MAX_BUFFS;
	public static long OLYMPIAD_CPERIOD;
	public static long OLYMPIAD_BATTLE;
	public static long OLYMPIAD_WPERIOD;
	public static long OLYMPIAD_VPERIOD;
	public static int OLYMPIAD_START_POINTS;
	public static int OLYMPIAD_WEEKLY_POINTS;
	public static int OLYMPIAD_CLASSED;
	public static int OLYMPIAD_NONCLASSED;
	public static int OLYMPIAD_TEAMS;
	public static int OLYMPIAD_REG_DISPLAY;
	public static int[][] OLYMPIAD_CLASSED_REWARD;
	public static int[][] OLYMPIAD_NONCLASSED_REWARD;
	public static int[][] OLYMPIAD_TEAM_REWARD;
	public static int OLYMPIAD_COMP_RITEM;
	public static int OLYMPIAD_MIN_MATCHES;
	public static int OLYMPIAD_GP_PER_POINT;
	public static int OLYMPIAD_HERO_POINTS;
	public static int OLYMPIAD_RANK1_POINTS;
	public static int OLYMPIAD_RANK2_POINTS;
	public static int OLYMPIAD_RANK3_POINTS;
	public static int OLYMPIAD_RANK4_POINTS;
	public static int OLYMPIAD_RANK5_POINTS;
	public static int OLYMPIAD_MAX_POINTS;
	public static int OLYMPIAD_DIVIDER_CLASSED;
	public static int OLYMPIAD_DIVIDER_NON_CLASSED;
	public static int OLYMPIAD_MAX_WEEKLY_MATCHES;
	public static int OLYMPIAD_MAX_WEEKLY_MATCHES_NON_CLASSED;
	public static int OLYMPIAD_MAX_WEEKLY_MATCHES_CLASSED;
	public static int OLYMPIAD_MAX_WEEKLY_MATCHES_TEAM;
	public static boolean OLYMPIAD_LOG_FIGHTS;
	public static boolean OLYMPIAD_SHOW_MONTHLY_WINNERS;
	public static boolean OLYMPIAD_ANNOUNCE_GAMES;
	public static Set<Integer> LIST_OLY_RESTRICTED_ITEMS = new HashSet<>();
	public static boolean OLYMPIAD_DISABLE_BLESSED_SPIRITSHOTS;
	public static int OLYMPIAD_ENCHANT_LIMIT;
	public static int OLYMPIAD_WAIT_TIME;
	public static boolean OLYMPIAD_USE_CUSTOM_PERIOD_SETTINGS;
	public static String OLYMPIAD_PERIOD;
	public static int OLYMPIAD_PERIOD_MULTIPLIER;
	public static List<Integer> OLYMPIAD_COMPETITION_DAYS;
	
	// --------------------------------------------------
	// PC Bang Points [Event]
	// --------------------------------------------------
	public static boolean PC_CAFE_ENABLED;
	public static boolean PC_CAFE_ONLY_PREMIUM;
	public static boolean PC_CAFE_RETAIL_LIKE;
	public static int PC_CAFE_REWARD_TIME;
	public static int PC_CAFE_MAX_POINTS;
	public static boolean PC_CAFE_ENABLE_DOUBLE_POINTS;
	public static int PC_CAFE_DOUBLE_POINTS_CHANCE;
	public static int ACQUISITION_PC_CAFE_RETAIL_LIKE_POINTS;
	public static double PC_CAFE_POINT_RATE;
	public static boolean PC_CAFE_RANDOM_POINT;
	public static boolean PC_CAFE_REWARD_LOW_EXP_KILLS;
	public static int PC_CAFE_LOW_EXP_KILLS_CHANCE;
	
	// --------------------------------------------------
	// TownWar [Event]
	// --------------------------------------------------
	public static boolean TW_AUTO_EVENT;
	public static String[] TW_INTERVAL;
	public static int TW_TIME_BEFORE_START;
	public static boolean TW_ALL_TOWNS;
	public static int TW_TOWN_ID;
	public static String TW_TOWN_NAME;
	public static int TW_RUNNING_TIME;
	public static int TW_ITEM_ID;
	public static int TW_ITEM_AMOUNT;
	public static boolean TW_DISABLE_GK;
	
	// --------------------------------------------------
	// Underground Coliseum
	// --------------------------------------------------
	public static String UC_START_TIME;
	public static int UC_TIME_PERIOD;
	public static boolean UC_ALLOW_ANNOUNCE;
	public static int UC_PARTY_SIZE;
	public static int UC_RESS_TIME;
	
	// --------------------------------------------------
	// Wedding
	// --------------------------------------------------
	public static boolean ALLOW_WEDDING;
	public static int WEDDING_PRICE;
	public static boolean WEDDING_PUNISH_INFIDELITY;
	public static boolean WEDDING_TELEPORT;
	public static int WEDDING_TELEPORT_PRICE;
	public static int WEDDING_TELEPORT_DURATION;
	public static boolean WEDDING_SAMESEX;
	public static boolean WEDDING_FORMALWEAR;
	public static int WEDDING_DIVORCE_COSTS;
	private EventsConfig()
	{
	}

	/**
	 * Esta classe inicia a maioria das configuracoes custom adicionadas no servidor. Obs: Algumas Configs ainda podem estar no arquivo Config.java.
	 */
	public static void load()
	{
		loadElpiesEvent();
		loadFactionSystem();
		loadHitmanEvent();
		loadLuckyPig();
		loadOlympiadConfig();
		loadPcBangPoints();
		loadTownWarEvent();
		loadUndergroundColiseum();
		loadWeddingConfig();
	}
	
	/**
	 * Load loadElpiesEvent file (if exists).
	 */
	private static void loadElpiesEvent()
	{
		final ConfigReader elpiesEventConfig = new ConfigReader(ELPIES_EVENT_CONFIG_FILE);
		ELPY_ID = elpiesEventConfig.getInt("ElpyId", 900100);
		ELPY_AMOUNT = elpiesEventConfig.getInt("ElpyAmount", 100);
		ELPY_DURATION_MINUTES = elpiesEventConfig.getInt("ElpyEventDuration", 2);
	}
	
	/**
	 * Load Hitman Event file (if exists).
	 */
	private static void loadHitmanEvent()
	{
		final ConfigReader hitmanConfig = new ConfigReader(HITMAN_CONFIG_FILE);
		HITMAN_ENABLED = hitmanConfig.getBoolean("HitmanEnabled", false);
		HITMAN_TAKE_KARMA = hitmanConfig.getBoolean("HitmanTakeKarma", true);
		HITMAN_TARGETS_LIMIT = hitmanConfig.getInt("HitmanTargetsLimit", 5);
		HITMAN_ANNOUNCE = hitmanConfig.getBoolean("HitmanAnnounce", false);
		HITMAN_MAX_PER_PAGE = hitmanConfig.getInt("HitmanMaxPerPage", 20);
		HITMAN_MIN_BOUNTY = hitmanConfig.getLong("HitmanMinBounty", 100000);
		HITMAN_SAME_TEAM = hitmanConfig.getBoolean("HitmanSameTeam", false);
		HITMAN_SAVE_INTERVAL = hitmanConfig.getInt("HitmanSaveInterval", 15);
		
		// Parse currency list
		HITMAN_CURRENCY = new ArrayList<>();
		final String currencyList = hitmanConfig.getString("HitmanCurrency", "57");
		for (String id : currencyList.split(","))
		{
			try
			{
				HITMAN_CURRENCY.add(Integer.parseInt(id.trim()));
			}
			catch (NumberFormatException _)
			{
				LOGGER.warning("Invalid hitman currency ID: " + id);
			}
		}
	}
	
	/**
	 * Load FactionSystem file (if exists).
	 */
	private static void loadFactionSystem()
	{
		final ConfigReader factionSystemConfig = new ConfigReader(FACTION_SYSTEM_CONFIG_FILE);
		String[] tempString;
		FACTION_SYSTEM_ENABLED = factionSystemConfig.getBoolean("EnableFactionSystem", false);
		tempString = factionSystemConfig.getString("StartingLocation", "85332,16199,-1252").split(",");
		FACTION_STARTING_LOCATION = new Location(Integer.parseInt(tempString[0]), Integer.parseInt(tempString[1]), Integer.parseInt(tempString[2]));
		tempString = factionSystemConfig.getString("ManagerSpawnLocation", "85712,15974,-1260,26808").split(",");
		FACTION_MANAGER_LOCATION = new Location(Integer.parseInt(tempString[0]), Integer.parseInt(tempString[1]), Integer.parseInt(tempString[2]), tempString[3] != null ? Integer.parseInt(tempString[3]) : 0);
		tempString = factionSystemConfig.getString("GoodBaseLocation", "45306,48878,-3058").split(",");
		FACTION_GOOD_BASE_LOCATION = new Location(Integer.parseInt(tempString[0]), Integer.parseInt(tempString[1]), Integer.parseInt(tempString[2]));
		tempString = factionSystemConfig.getString("EvilBaseLocation", "-44037,-113283,-237").split(",");
		FACTION_EVIL_BASE_LOCATION = new Location(Integer.parseInt(tempString[0]), Integer.parseInt(tempString[1]), Integer.parseInt(tempString[2]));
		FACTION_GOOD_TEAM_NAME = factionSystemConfig.getString("GoodTeamName", "Good");
		FACTION_EVIL_TEAM_NAME = factionSystemConfig.getString("EvilTeamName", "Evil");
		FACTION_GOOD_NAME_COLOR = Integer.decode("0x" + factionSystemConfig.getString("GoodNameColor", "00FF00"));
		FACTION_EVIL_NAME_COLOR = Integer.decode("0x" + factionSystemConfig.getString("EvilNameColor", "0000FF"));
		FACTION_GUARDS_ENABLED = factionSystemConfig.getBoolean("EnableFactionGuards", true);
		FACTION_RESPAWN_AT_BASE = factionSystemConfig.getBoolean("RespawnAtFactionBase", true);
		FACTION_AUTO_NOBLESS = factionSystemConfig.getBoolean("FactionAutoNobless", false);
		FACTION_SPECIFIC_CHAT = factionSystemConfig.getBoolean("EnableFactionChat", true);
		FACTION_BALANCE_ONLINE_PLAYERS = factionSystemConfig.getBoolean("BalanceOnlinePlayers", true);
		FACTION_BALANCE_PLAYER_EXCEED_LIMIT = factionSystemConfig.getInt("BalancePlayerExceedLimit", 20);
	}
	
	/**
	 * Load luckyPig file (if exists).
	 */
	private static void loadLuckyPig()
	{
		final ConfigReader luckyPig = new ConfigReader(LUCKY_PIG_EVENT_CONFIG_FILE);
		LUCKY_PID_SPAWN_ENABLED = luckyPig.getBoolean("EventLuckyPigEnabled", false);
		LUCKY_PID_LOW_ADENA = luckyPig.getInt("LuckyPigLowAdena", 500);
		LUCKY_PID_MEDIUM_ADENA = luckyPig.getInt("LuckyPigMediumAdena", 1000);
		LUCKY_PID_TOP_ADENA = luckyPig.getInt("LuckyPigTopAdena", 2000);
		LUCKY_PID_CHANCE = luckyPig.getInt("LuckyPigChance", 3);
	}
	
	/**
	 * Load olympiadConfig file (if exists).
	 */
	private static void loadOlympiadConfig()
	{
		final ConfigReader olympiadConfig = new ConfigReader(OLYMPIAD_CONFIG_FILE);
		OLYMPIAD_ENABLED = olympiadConfig.getBoolean("OlympiadEnabled", true);
		OLYMPIAD_START_TIME = olympiadConfig.getInt("OlympiadStartTime", 18);
		OLYMPIAD_MIN = olympiadConfig.getInt("OlympiadMin", 0);
		OLYMPIAD_MAX_BUFFS = olympiadConfig.getInt("OlympiadMaxBuffs", 5);
		OLYMPIAD_CPERIOD = olympiadConfig.getLong("OlympiadCPeriod", 21600000);
		OLYMPIAD_BATTLE = olympiadConfig.getLong("OlympiadBattle", 300000);
		OLYMPIAD_WPERIOD = olympiadConfig.getLong("OlympiadWPeriod", 604800000);
		OLYMPIAD_VPERIOD = olympiadConfig.getLong("OlympiadVPeriod", 86400000);
		OLYMPIAD_START_POINTS = olympiadConfig.getInt("OlympiadStartPoints", 10);
		OLYMPIAD_WEEKLY_POINTS = olympiadConfig.getInt("OlympiadWeeklyPoints", 10);
		OLYMPIAD_CLASSED = olympiadConfig.getInt("OlympiadClassedParticipants", 11);
		OLYMPIAD_NONCLASSED = olympiadConfig.getInt("OlympiadNonClassedParticipants", 11);
		OLYMPIAD_TEAMS = olympiadConfig.getInt("OlympiadTeamsParticipants", 6);
		OLYMPIAD_REG_DISPLAY = olympiadConfig.getInt("OlympiadRegistrationDisplayNumber", 100);
		OLYMPIAD_CLASSED_REWARD = parseItemsList(olympiadConfig.getString("OlympiadClassedReward", "13722,50"));
		OLYMPIAD_NONCLASSED_REWARD = parseItemsList(olympiadConfig.getString("OlympiadNonClassedReward", "13722,40"));
		OLYMPIAD_TEAM_REWARD = parseItemsList(olympiadConfig.getString("OlympiadTeamReward", "13722,85"));
		OLYMPIAD_COMP_RITEM = olympiadConfig.getInt("OlympiadCompRewItem", 13722);
		OLYMPIAD_MIN_MATCHES = olympiadConfig.getInt("OlympiadMinMatchesForPoints", 15);
		OLYMPIAD_GP_PER_POINT = olympiadConfig.getInt("OlympiadGPPerPoint", 1000);
		OLYMPIAD_HERO_POINTS = olympiadConfig.getInt("OlympiadHeroPoints", 200);
		OLYMPIAD_RANK1_POINTS = olympiadConfig.getInt("OlympiadRank1Points", 100);
		OLYMPIAD_RANK2_POINTS = olympiadConfig.getInt("OlympiadRank2Points", 75);
		OLYMPIAD_RANK3_POINTS = olympiadConfig.getInt("OlympiadRank3Points", 55);
		OLYMPIAD_RANK4_POINTS = olympiadConfig.getInt("OlympiadRank4Points", 40);
		OLYMPIAD_RANK5_POINTS = olympiadConfig.getInt("OlympiadRank5Points", 30);
		OLYMPIAD_MAX_POINTS = olympiadConfig.getInt("OlympiadMaxPoints", 10);
		OLYMPIAD_DIVIDER_CLASSED = olympiadConfig.getInt("OlympiadDividerClassed", 5);
		OLYMPIAD_DIVIDER_NON_CLASSED = olympiadConfig.getInt("OlympiadDividerNonClassed", 5);
		OLYMPIAD_MAX_WEEKLY_MATCHES = olympiadConfig.getInt("OlympiadMaxWeeklyMatches", 70);
		OLYMPIAD_MAX_WEEKLY_MATCHES_NON_CLASSED = olympiadConfig.getInt("OlympiadMaxWeeklyMatchesNonClassed", 60);
		OLYMPIAD_MAX_WEEKLY_MATCHES_CLASSED = olympiadConfig.getInt("OlympiadMaxWeeklyMatchesClassed", 30);
		OLYMPIAD_MAX_WEEKLY_MATCHES_TEAM = olympiadConfig.getInt("OlympiadMaxWeeklyMatchesTeam", 10);
		OLYMPIAD_LOG_FIGHTS = olympiadConfig.getBoolean("OlympiadLogFights", false);
		OLYMPIAD_SHOW_MONTHLY_WINNERS = olympiadConfig.getBoolean("OlympiadShowMonthlyWinners", true);
		OLYMPIAD_ANNOUNCE_GAMES = olympiadConfig.getBoolean("OlympiadAnnounceGames", true);
		final String olyRestrictedItems = olympiadConfig.getString("OlympiadRestrictedItems", "").trim();
		if (!olyRestrictedItems.isEmpty())
		{
			final String[] olyRestrictedItemsSplit = olyRestrictedItems.split(",");
			LIST_OLY_RESTRICTED_ITEMS = HashSet.newHashSet(olyRestrictedItemsSplit.length);
			for (String id : olyRestrictedItemsSplit)
			{
				LIST_OLY_RESTRICTED_ITEMS.add(Integer.parseInt(id));
			}
		}
		else // In case of reload with removal of all items ids.
		{
			LIST_OLY_RESTRICTED_ITEMS.clear();
		}
		OLYMPIAD_DISABLE_BLESSED_SPIRITSHOTS = olympiadConfig.getBoolean("OlympiadDisableBlessedSpiritShots", false);
		OLYMPIAD_ENCHANT_LIMIT = olympiadConfig.getInt("OlympiadEnchantLimit", -1);
		OLYMPIAD_WAIT_TIME = olympiadConfig.getInt("OlympiadWaitTime", 120);
		OLYMPIAD_USE_CUSTOM_PERIOD_SETTINGS = olympiadConfig.getBoolean("OlympiadUseCustomPeriodSettings", false);
		OLYMPIAD_PERIOD = olympiadConfig.getString("OlympiadPeriod", "MONTH");
		OLYMPIAD_PERIOD_MULTIPLIER = olympiadConfig.getInt("OlympiadPeriodMultiplier", 1);
		OLYMPIAD_COMPETITION_DAYS = new ArrayList<>();
		for (String s : olympiadConfig.getString("OlympiadCompetitionDays", "1,2,3,4,5,6,7").split(","))
		{
			OLYMPIAD_COMPETITION_DAYS.add(Integer.parseInt(s));
		}
	}
	
	/**
	 * Load pcBangPoints file (if exists).
	 */
	private static void loadPcBangPoints()
	{
		final ConfigReader pcBangPoints = new ConfigReader(PC_BANG_EVENT_CONFIG_FILE);
		PC_CAFE_ENABLED = pcBangPoints.getBoolean("PcCafeEnabled", false);
		PC_CAFE_ONLY_PREMIUM = pcBangPoints.getBoolean("PcCafeOnlyPremium", false);
		PC_CAFE_RETAIL_LIKE = pcBangPoints.getBoolean("PcCafeRetailLike", true);
		PC_CAFE_REWARD_TIME = pcBangPoints.getInt("PcCafeRewardTime", 300000);
		PC_CAFE_MAX_POINTS = Math.max(pcBangPoints.getInt("MaxPcCafePoints", 200000), 0);
		PC_CAFE_ENABLE_DOUBLE_POINTS = pcBangPoints.getBoolean("DoublingAcquisitionPoints", false);
		PC_CAFE_DOUBLE_POINTS_CHANCE = pcBangPoints.getInt("DoublingAcquisitionPointsChance", 1);
		if ((PC_CAFE_DOUBLE_POINTS_CHANCE < 0) || (PC_CAFE_DOUBLE_POINTS_CHANCE > 100))
		{
			PC_CAFE_DOUBLE_POINTS_CHANCE = 1;
		}
		ACQUISITION_PC_CAFE_RETAIL_LIKE_POINTS = pcBangPoints.getInt("AcquisitionPointsRetailLikePoints", 10);
		PC_CAFE_POINT_RATE = pcBangPoints.getDouble("AcquisitionPointsRate", 1.0);
		PC_CAFE_RANDOM_POINT = pcBangPoints.getBoolean("AcquisitionPointsRandom", false);
		if (PC_CAFE_POINT_RATE < 0)
		{
			PC_CAFE_POINT_RATE = 1;
		}
		PC_CAFE_REWARD_LOW_EXP_KILLS = pcBangPoints.getBoolean("RewardLowExpKills", true);
		PC_CAFE_LOW_EXP_KILLS_CHANCE = Math.clamp(pcBangPoints.getInt("RewardLowExpKillsChance", 50), 0, 100);
	}
	
	/**
	 * Load TownWarEvent file (if exists).
	 */
	private static void loadTownWarEvent()
	{
		final ConfigReader TownWarEvent = new ConfigReader(TOWN_WAR_EVENT_CONFIG_FILE);
		TW_AUTO_EVENT = TownWarEvent.getBoolean("TownWarAutoEvent", false);
		TW_INTERVAL = TownWarEvent.getString("TownWarInterval", "20:00").split(",");
		TW_TIME_BEFORE_START = TownWarEvent.getInt("TownWarTimeBeforeStart", 5);
		TW_ALL_TOWNS = TownWarEvent.getBoolean("TownWarAllTowns", false);
		TW_TOWN_ID = TownWarEvent.getInt("TownWarTownId", 9);
		TW_TOWN_NAME = TownWarEvent.getString("TownWarTownName", "Giran Town");
		TW_RUNNING_TIME = TownWarEvent.getInt("TownWarRunningTime", 10);
		TW_ITEM_ID = TownWarEvent.getInt("TownWarItemId", 57);
		TW_ITEM_AMOUNT = TownWarEvent.getInt("TownWarItemAmount", 5000);
		TW_DISABLE_GK = TownWarEvent.getBoolean("TownWarDisableGK", false);
	}
	
	/**
	 * Load UndergroundColiseum file (if exists).
	 */
	private static void loadUndergroundColiseum()
	{
		final ConfigReader undergroundColiseumConfig = new ConfigReader(UNDERGROUND_COLISEUM_CONFIG_FILE);
		UC_START_TIME = undergroundColiseumConfig.getString("BattleStartTime", "0 17 * * *");
		UC_TIME_PERIOD = undergroundColiseumConfig.getInt("BattlePeriod", 2);
		UC_ALLOW_ANNOUNCE = undergroundColiseumConfig.getBoolean("AllowAnnouncements", false);
		UC_PARTY_SIZE = undergroundColiseumConfig.getInt("PartySize", 7);
		UC_RESS_TIME = undergroundColiseumConfig.getInt("ResurrectionTime", 10);
	}
	
	/**
	 * Load weddingConfig file (if exists).
	 */
	private static void loadWeddingConfig()
	{
		final ConfigReader weddingConfig = new ConfigReader(WEDDING_CONFIG_FILE);
		ALLOW_WEDDING = weddingConfig.getBoolean("AllowWedding", false);
		WEDDING_PRICE = weddingConfig.getInt("WeddingPrice", 250000000);
		WEDDING_PUNISH_INFIDELITY = weddingConfig.getBoolean("WeddingPunishInfidelity", true);
		WEDDING_TELEPORT = weddingConfig.getBoolean("WeddingTeleport", true);
		WEDDING_TELEPORT_PRICE = weddingConfig.getInt("WeddingTeleportPrice", 50000);
		WEDDING_TELEPORT_DURATION = weddingConfig.getInt("WeddingTeleportDuration", 60);
		WEDDING_SAMESEX = weddingConfig.getBoolean("WeddingAllowSameSex", false);
		WEDDING_FORMALWEAR = weddingConfig.getBoolean("WeddingFormalWear", true);
		WEDDING_DIVORCE_COSTS = weddingConfig.getInt("WeddingDivorceCosts", 20);
	}
	
	/**
	 * Parse a config value from its string representation to a two-dimensional int array.<br>
	 * The format of the value to be parsed should be as follows: "item1Id,item1Amount;item2Id,item2Amount;...itemNId,itemNAmount".
	 * @param line the value of the parameter to parse
	 * @return the parsed list or {@code null} if nothing was parsed
	 */
	private static int[][] parseItemsList(String line)
	{
		final String[] propertySplit = line.split(";");
		if (propertySplit.length == 0)
		{
			return null;
		}
		
		int i = 0;
		String[] valueSplit;
		final int[][] result = new int[propertySplit.length][];
		int[] tmp;
		for (String value : propertySplit)
		{
			valueSplit = value.split(",");
			if (valueSplit.length != 2)
			{
				LOGGER.warning("parseItemsList[Config.load()]: invalid entry -> \"" + valueSplit[0] + "\", should be itemId,itemNumber. Skipping to the next entry in the list.");
				continue;
			}
			
			tmp = new int[2];
			try
			{
				tmp[0] = Integer.parseInt(valueSplit[0]);
			}
			catch (NumberFormatException _)
			{
				LOGGER.warning("parseItemsList[Config.load()]: invalid itemId -> \"" + valueSplit[0] + "\", value must be an integer. Skipping to the next entry in the list.");
				continue;
			}
			try
			{
				tmp[1] = Integer.parseInt(valueSplit[1]);
			}
			catch (NumberFormatException _)
			{
				LOGGER.warning("parseItemsList[Config.load()]: invalid item number -> \"" + valueSplit[1] + "\", value must be an integer. Skipping to the next entry in the list.");
				continue;
			}
			result[i++] = tmp;
		}
		return result;
	}
}
