package com.l2journey;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.l2journey.commons.util.ConfigReader;

/** Rebirth configuration, validated before the service can mutate a character. @author vGodFather */
public final class RebirthEngineConfigs
{
	private static final Logger LOGGER = Logger.getLogger(RebirthEngineConfigs.class.getName());
	private static final String FILE = "./config/player/rebirth.ini";
	public static volatile boolean REBIRTH_ENABLED;
	public static int REBIRTH_MIN_LEVEL;
	public static int REBIRTH_MAX;
	public static int REBIRTH_RETURN_TO_LEVEL;
	public static List<Integer> REBIRTH_SKILL_IDS = List.of();
	public static int REBIRTH_SKILL_SELLER_ID;
	public static long REBIRTH_SKILL_RESET_PRICE;
	public record Reward(int id, long count) { }
	public static List<Reward> REBIRTH_REWARDS = List.of();

	public static void load() { loadRebirthEngineConfigs(); }
	public static synchronized void loadRebirthEngineConfigs()
	{
		REBIRTH_ENABLED = false;
		try
		{
			final ConfigReader config = new ConfigReader(FILE);
			REBIRTH_SKILL_SELLER_ID = config.getInt("RebirthSkillSellerId", 70003);
			REBIRTH_SKILL_RESET_PRICE = config.getLong("RebirthSkillResetPrice", 10_000_000L);
			if (REBIRTH_SKILL_RESET_PRICE <= 0) { throw new IllegalArgumentException("RebirthSkillResetPrice must be positive"); }
			REBIRTH_MIN_LEVEL = config.getInt("RebirthMinLevel", 78);
			REBIRTH_MAX = config.getInt("RebirthMaxAllowed", 3);
			REBIRTH_RETURN_TO_LEVEL = config.getInt("RebirthReturnToLevel", 1);
			if (REBIRTH_SKILL_SELLER_ID <= 0 || REBIRTH_MAX <= 0 || REBIRTH_RETURN_TO_LEVEL < 1 || REBIRTH_MIN_LEVEL <= REBIRTH_RETURN_TO_LEVEL)
			{ throw new IllegalArgumentException("Invalid level, limit or trainer ID"); }
			final var ids = new LinkedHashSet<Integer>();
			for (String value : config.getString("RebirthSkillIds", "35200,35201,35202,35203,35204,35205,35206,35207,35208,35209,35210,35211").split(","))
			{
				final int id = Integer.parseInt(value.trim());
				if (id <= 0) { throw new IllegalArgumentException("Invalid protected skill ID"); }
				ids.add(id);
			}
			final List<Reward> rewards = new ArrayList<>();
			for (String value : config.getString("RebirthItems", "").split(";"))
			{
				if (value.isBlank()) { continue; }
				final String[] pair = value.trim().split(",");
				if (pair.length != 2) { throw new IllegalArgumentException("Expected itemId,count"); }
				final Reward reward = new Reward(Integer.parseInt(pair[0].trim()), Long.parseLong(pair[1].trim()));
				if (reward.id() <= 0 || reward.count() <= 0) { throw new IllegalArgumentException("Invalid rebirth reward"); }
				rewards.add(reward);
			}
			if (rewards.size() != REBIRTH_MAX) { throw new IllegalArgumentException("RebirthItems must contain one reward for each allowed rebirth"); }
			REBIRTH_SKILL_IDS = List.copyOf(ids);
			REBIRTH_REWARDS = List.copyOf(rewards);
			REBIRTH_ENABLED = config.getBoolean("RebirthEnabled", true);
		}
		catch (RuntimeException e) { LOGGER.log(Level.SEVERE, "Rebirth disabled: invalid configuration.", e); }
	}
	public static RebirthEngineConfigs getInstance() { return Holder.INSTANCE; }
	private static class Holder { private static final RebirthEngineConfigs INSTANCE = new RebirthEngineConfigs(); }
}
