package com.l2journey.gameserver.model.phantom;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.model.actor.Player;

/**
 * Lightweight per-phantom mood used to add human-like delays, mistakes, and chat rate.
 */
public class PhantomMood
{
	public enum Mood
	{
		NEUTRAL(1.0, 1.0, 1.0),
		ANNOYED(0.8, 1.6, 0.7),
		ANGRY(0.7, 2.0, 1.4),
		CHEERFUL(1.0, 0.8, 1.8),
		TIRED(1.7, 1.4, 0.5);

		private final double _reactionFactor;
		private final double _mistakeFactor;
		private final double _talkFactor;

		Mood(double reactionFactor, double mistakeFactor, double talkFactor)
		{
			_reactionFactor = reactionFactor;
			_mistakeFactor = mistakeFactor;
			_talkFactor = talkFactor;
		}

		public double getReactionFactor()
		{
			return _reactionFactor;
		}

		public double getMistakeFactor()
		{
			return _mistakeFactor;
		}

		public double getTalkFactor()
		{
			return _talkFactor;
		}
	}

	private static final class State
	{
		private Mood _currentMood = Mood.NEUTRAL;
		private long _moodUntil;
		private int _recentDeaths;
		private long _lastDeathAt;
		private long _sessionStartedAt = System.currentTimeMillis();
		private long _afkUntil;
		private long _distractedUntil;
		private long _nextReactionAt;
		private long _nextAfkRollAt;
		private long _nextDistractRollAt;
	}

	private static final long MOOD_DURATION_MS = 480000L;
	private static final long DEATH_WINDOW_MS = 600000L;
	private static final long TIRED_AFTER_MS = 9000000L;
	private static final long BEHAVIOR_ROLL_INTERVAL_MS = 10000L;
	private static final Map<Integer, State> MOOD_DATA = new ConcurrentHashMap<>();

	private PhantomMood()
	{
	}

	private static State getState(int objectId)
	{
		return MOOD_DATA.computeIfAbsent(objectId, _ -> new State());
	}

	/**
	 * @param bot the phantom
	 * @return current mood, falling back to tired after a long neutral session
	 */
	public static Mood getMood(Player bot)
	{
		if (bot == null)
		{
			return Mood.NEUTRAL;
		}

		State state = getState(bot.getObjectId());
		long now = System.currentTimeMillis();
		if ((state._moodUntil > 0) && (now > state._moodUntil))
		{
			state._currentMood = Mood.NEUTRAL;
			state._moodUntil = 0;
		}

		if ((state._currentMood == Mood.NEUTRAL) && ((now - state._sessionStartedAt) > TIRED_AFTER_MS))
		{
			return Mood.TIRED;
		}
		return state._currentMood;
	}

	/**
	 * @param bot the phantom
	 * @param mood mood to apply for {@link #MOOD_DURATION_MS}
	 */
	public static void setMood(Player bot, Mood mood)
	{
		if ((bot == null) || (mood == null))
		{
			return;
		}
		State state = getState(bot.getObjectId());
		state._currentMood = mood;
		state._moodUntil = System.currentTimeMillis() + MOOD_DURATION_MS;
	}

	/**
	 * Marks a successful event (e.g. level-up) as cheerful.
	 * @param bot the phantom
	 */
	public static void onGoodThing(Player bot)
	{
		setMood(bot, Mood.CHEERFUL);
	}

	/**
	 * Records a death. One death in the window sets annoyed; three set angry.
	 * @param bot the phantom
	 */
	public static void onDeath(Player bot)
	{
		if (bot == null)
		{
			return;
		}

		State state = getState(bot.getObjectId());
		long now = System.currentTimeMillis();
		if ((now - state._lastDeathAt) > DEATH_WINDOW_MS)
		{
			state._recentDeaths = 0;
		}
		state._recentDeaths++;
		state._lastDeathAt = now;
		setMood(bot, state._recentDeaths >= 3 ? Mood.ANGRY : Mood.ANNOYED);
	}

	/**
	 * @param bot the phantom
	 * @return {@code true} when the think tick should pause for AFK, reaction delay, or distraction
	 */
	public static boolean shouldPauseThink(Player bot)
	{
		if (bot == null)
		{
			return false;
		}
		return isAfk(bot) || !canReactNow(bot) || isDistracted(bot);
	}

	/**
	 * Schedules the next reaction window based on the current mood reaction factor.
	 * @param bot the phantom
	 * @return {@code true} when the bot may act on this tick
	 */
	public static boolean canReactNow(Player bot)
	{
		if (bot == null)
		{
			return true;
		}

		State state = getState(bot.getObjectId());
		long now = System.currentTimeMillis();
		if (state._nextReactionAt > now)
		{
			return false;
		}

		Mood mood = getMood(bot);
		int base = 250 + ((bot.getObjectId() % 7) * 90);
		int jitter = Rnd.get(0, 400);
		state._nextReactionAt = now + (long) ((base + jitter) * mood.getReactionFactor());
		return true;
	}

	/**
	 * Rolls AFK at most once every {@link #BEHAVIOR_ROLL_INTERVAL_MS} when not in combat and HP is above 60%.
	 * @param bot the phantom
	 * @return {@code true} while an AFK window is active
	 */
	public static boolean isAfk(Player bot)
	{
		if (bot == null)
		{
			return false;
		}

		State state = getState(bot.getObjectId());
		long now = System.currentTimeMillis();
		if (state._afkUntil > now)
		{
			return true;
		}
		if (bot.isInCombat() || (bot.getCurrentHp() < (bot.getMaxHp() * 0.6)))
		{
			return false;
		}
		if (state._nextAfkRollAt > now)
		{
			return false;
		}
		state._nextAfkRollAt = now + BEHAVIOR_ROLL_INTERVAL_MS;
		if (Rnd.get(100) < 2)
		{
			state._afkUntil = now + Rnd.get(30000, 300000);
			return true;
		}
		return false;
	}

	/**
	 * Rolls distraction at most once every {@link #BEHAVIOR_ROLL_INTERVAL_MS}.
	 * @param bot the phantom
	 * @return {@code true} while a short distraction window is active
	 */
	public static boolean isDistracted(Player bot)
	{
		if (bot == null)
		{
			return false;
		}

		State state = getState(bot.getObjectId());
		long now = System.currentTimeMillis();
		if (state._distractedUntil > now)
		{
			return true;
		}
		if (state._nextDistractRollAt > now)
		{
			return false;
		}
		state._nextDistractRollAt = now + BEHAVIOR_ROLL_INTERVAL_MS;
		if (Rnd.get(1000) < 3)
		{
			state._distractedUntil = now + Rnd.get(1500, 6000);
			return true;
		}
		return false;
	}

	/**
	 * @param bot the phantom
	 * @param baseChancePercent base miss chance before mood mistake factor
	 * @return {@code true} when the bot should skip the action
	 */
	public static boolean makesMistake(Player bot, int baseChancePercent)
	{
		if (bot == null)
		{
			return false;
		}
		double chance = baseChancePercent * getMood(bot).getMistakeFactor();
		return Rnd.get(100) < chance;
	}

	/**
	 * @param bot the phantom
	 * @return talk-rate multiplier for the current mood
	 */
	public static double getTalkFactor(Player bot)
	{
		return getMood(bot).getTalkFactor();
	}

	public static void unregister(int objectId)
	{
		MOOD_DATA.remove(objectId);
	}

	public static void clear()
	{
		MOOD_DATA.clear();
	}

	/** Test helper: force an active AFK window. */
	static void forceAfkUntil(int objectId, long until)
	{
		getState(objectId)._afkUntil = until;
	}

	/** Test helper: force an active distraction window. */
	static void forceDistractedUntil(int objectId, long until)
	{
		getState(objectId)._distractedUntil = until;
	}

	/** Test helper: backdate session start for tired mood. */
	static void forceSessionStartedAt(int objectId, long startedAt)
	{
		getState(objectId)._sessionStartedAt = startedAt;
	}

	/** Test helper: expire the current mood window. */
	static void forceMoodUntil(int objectId, long until)
	{
		getState(objectId)._moodUntil = until;
	}

	/** Test helper: set last death timestamp for death-window tests. */
	static void forceLastDeathAt(int objectId, long lastDeathAt)
	{
		getState(objectId)._lastDeathAt = lastDeathAt;
		getState(objectId)._recentDeaths = 1;
	}
}
