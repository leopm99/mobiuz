package com.l2journey.gameserver.model.phantom;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.serverpackets.MagicSkillUse;

public class PhantomWalk
{
	// === CONFIGURACAO DE CAMINHADA ===
	public static boolean ENABLE_TOWN_WALKING = false;
	
	private static final Map<Integer, Long> _lastTownVisit = new ConcurrentHashMap<>();
	private static final Map<Integer, Boolean> _isResting = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> _restingStartTime = new ConcurrentHashMap<>();
	
	// NOVOS MAPAS: Para controlar as caminhadas e descansos dentro da cidade
	private static final Map<Integer, Integer> _walkCount = new ConcurrentHashMap<>();
	private static final Map<Integer, Integer> _targetWalks = new ConcurrentHashMap<>();
	private static final Map<Integer, Boolean> _isWaitingInTown = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> _waitingStartTime = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> _lastWalkActionTime = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> _soeCastStartTime = new ConcurrentHashMap<>();
	
	// Tempos configuraveis
	private static final long FARM_DURATION = (45 * 60L) * 1000; // 45 minutos farmando
	private static final long TOWN_DURATION = (5 * 60L) * 1000; // 5 minutos na cidade
	private static final long TIME_QUIETO_TOWN = (2 * 60L) * 1000; // 2 minutos parado entre caminhadas
	private static final int SOE_SKILL_ID = 2036;
	private static final long SOE_CAST_MS = 2000L;
	private PhantomWalk()
	{
	}

	public static void update(Player bot, boolean skipTownVisit)
	{
		update(bot, skipTownVisit, System.currentTimeMillis());
	}

	/**
	 * Town visit / farm cycle. Package-private clock for tests.
	 * @param bot the phantom
	 * @param skipTownVisit {@code true} when cursed activity must cancel town rest
	 * @param currentTime clock ms
	 */
	static void update(Player bot, boolean skipTownVisit, long currentTime)
	{
		if ((bot == null) || bot.isDead())
		{
			return;
		}
		
		int objectId = bot.getObjectId();
		
		if (skipTownVisit)
		{
			cancelTownRest(objectId, currentTime);
			return;
		}
		
		long lastVisit = _lastTownVisit.getOrDefault(objectId, 0L);
		if (lastVisit == 0L)
		{
			lastVisit = currentTime;
			_lastTownVisit.put(objectId, lastVisit);
		}
		
		boolean resting = _isResting.getOrDefault(objectId, false);
		long startTime = _restingStartTime.getOrDefault(objectId, 0L);
		
		if (resting)
		{
			updateTownRest(bot, objectId, currentTime, startTime);
		}
		else
		{
			updateFarming(bot, objectId, currentTime, lastVisit);
		}
	}

	private static void updateTownRest(Player bot, int objectId, long currentTime, long startTime)
	{
		if ((currentTime - startTime) > TOWN_DURATION)
		{
			_isResting.put(objectId, false);
			_lastTownVisit.put(objectId, currentTime);
			
			Location farmSpot = getFarmingLocation(bot.getLevel(), 1000);
			forceTeleport(bot, farmSpot);
			return;
		}
		
		if (bot.isSitting())
		{
			bot.standUp();
		}
		
		if (!bot.isRunning())
		{
			bot.setRunning();
		}
		
		if (bot.getTarget() != null)
		{
			bot.setTarget(null);
		}
		
		if ((bot.getAI().getIntention() != Intention.ACTIVE) && (bot.getAI().getIntention() != Intention.MOVE_TO))
		{
			bot.getAI().setIntention(Intention.ACTIVE);
		}
		
		if (!ENABLE_TOWN_WALKING)
		{
			return;
		}
		
		boolean isWaiting = _isWaitingInTown.getOrDefault(objectId, false);
		
		if (isWaiting)
		{
			long waitStart = _waitingStartTime.getOrDefault(objectId, 0L);
			if ((currentTime - waitStart) > TIME_QUIETO_TOWN)
			{
				_isWaitingInTown.put(objectId, false);
				_walkCount.put(objectId, 0);
				_targetWalks.put(objectId, Rnd.get(100) < 50 ? 2 : 3);
			}
			else
			{
				return;
			}
		}
		
		if (!bot.isMoving())
		{
			long lastWalk = _lastWalkActionTime.getOrDefault(objectId, 0L);
			
			if ((currentTime - lastWalk) > 12000)
			{
				int currentWalks = _walkCount.getOrDefault(objectId, 0);
				int target = _targetWalks.getOrDefault(objectId, 2);
				
				if (currentWalks < target)
				{
					Location dest = getCityLocationWithSpread(bot.getLevel(), 400);
					bot.getAI().setIntention(Intention.MOVE_TO, dest);
					
					_walkCount.put(objectId, currentWalks + 1);
					_lastWalkActionTime.put(objectId, currentTime);
				}
				else
				{
					_isWaitingInTown.put(objectId, true);
					_waitingStartTime.put(objectId, currentTime);
				}
			}
		}
	}

	private static void updateFarming(Player bot, int objectId, long currentTime, long lastVisit)
	{
		if ((currentTime - lastVisit) <= FARM_DURATION)
		{
			return;
		}

		long soeStart = _soeCastStartTime.getOrDefault(objectId, 0L);
		if (soeStart == 0L)
		{
			bot.abortAttack();
			bot.abortCast();
			bot.setTarget(null);
			bot.getAI().setIntention(Intention.ACTIVE);
			bot.broadcastPacket(new MagicSkillUse(bot, bot, SOE_SKILL_ID, 1, (int) SOE_CAST_MS, 0));
			_soeCastStartTime.put(objectId, currentTime);
			return;
		}

		if ((currentTime - soeStart) < SOE_CAST_MS)
		{
			return;
		}

		_soeCastStartTime.remove(objectId);
		_isResting.put(objectId, true);
		_restingStartTime.put(objectId, currentTime);
		_walkCount.put(objectId, 0);
		_targetWalks.put(objectId, Rnd.get(100) < 50 ? 2 : 3);
		_isWaitingInTown.put(objectId, false);
		_lastWalkActionTime.put(objectId, 0L);
		
		Location town = getCityLocationWithSpread(bot.getLevel(), 400);
		forceTeleport(bot, town);
	}
	
	private static void cancelTownRest(int objectId, long currentTime)
	{
		_isResting.put(objectId, false);
		_restingStartTime.remove(objectId);
		_walkCount.remove(objectId);
		_targetWalks.remove(objectId);
		_isWaitingInTown.remove(objectId);
		_waitingStartTime.remove(objectId);
		_lastWalkActionTime.remove(objectId);
		_soeCastStartTime.remove(objectId);
		_lastTownVisit.put(objectId, currentTime);
	}
	
	private static void forceTeleport(Player bot, Location loc)
	{
		bot.abortAttack();
		bot.abortCast();
		bot.setTarget(null);
		bot.getAI().setIntention(Intention.ACTIVE);
		
		bot.decayMe();
		bot.setXYZ(loc.getX(), loc.getY(), loc.getZ());
		bot.spawnMe(loc.getX(), loc.getY(), loc.getZ());
		
		bot.broadcastUserInfo();
	}

	private static Location clampLanding(int x, int y, int z)
	{
		return PhantomGeo.getValidLocation(x, y, z);
	}
	
	private static Location getCityLocationWithSpread(int lvl, int spreadRadius)
	{
		int spreadX = Rnd.get(-spreadRadius, spreadRadius);
		int spreadY = Rnd.get(-spreadRadius, spreadRadius);
		
		int baseX;
		int baseY;
		int baseZ;
		if (lvl >= 76)
		{
			baseX = 147702;
			baseY = -56030;
			baseZ = -2775; // Goddard (Lvl 76+)
		}
		else if (lvl >= 61)
		{
			baseX = 146855;
			baseY = 27011;
			baseZ = -2205; // Aden (Lvl 61 a 75)
		}
		else if (lvl >= 52)
		{
			baseX = 116589;
			baseY = 76268;
			baseZ = -2723; // Hunter (Lvl 52 a 60)
		}
		else if (lvl >= 40)
		{
			baseX = 82611;
			baseY = 148719;
			baseZ = -3469; // Giran (Lvl 40 a 60)
		}
		else if (lvl >= 20)
		{
			baseX = 18810;
			baseY = 145240;
			baseZ = -3130; // Dion (Lvl 20 a 39)
		}
		else
		{
			baseX = -14372;
			baseY = 123051;
			baseZ = -3117; // Gludio (Lvl 19 ou menos)
		}
		return clampLanding(baseX + spreadX, baseY + spreadY, baseZ);
	}
	
	private static Location getFarmingLocation(int lvl, int spreadRadius)
	{
		int spreadX = Rnd.get(-spreadRadius, spreadRadius);
		int spreadY = Rnd.get(-spreadRadius, spreadRadius);
		if (lvl >= 40)
		{
			return clampLanding(83143 + spreadX, 147943 + spreadY, -3402); // Giran
		}
		return clampLanding(15415 + spreadX, 142767 + spreadY, -2703); // Dion
	}
	
	public static boolean isResting(Player bot)
	{
		if (bot == null)
		{
			return false;
		}
		return _isResting.getOrDefault(bot.getObjectId(), false);
	}
	
	public static void removeBot(Player bot)
	{
		if (bot != null)
		{
			int objectId = bot.getObjectId();
			_lastTownVisit.remove(objectId);
			_isResting.remove(objectId);
			_restingStartTime.remove(objectId);
			_walkCount.remove(objectId);
			_targetWalks.remove(objectId);
			_isWaitingInTown.remove(objectId);
			_waitingStartTime.remove(objectId);
			_lastWalkActionTime.remove(objectId);
			_soeCastStartTime.remove(objectId);
		}
	}
	
	public static void clearAll()
	{
		_lastTownVisit.clear();
		_isResting.clear();
		_restingStartTime.clear();
		_walkCount.clear();
		_targetWalks.clear();
		_isWaitingInTown.clear();
		_waitingStartTime.clear();
		_lastWalkActionTime.clear();
		_soeCastStartTime.clear();
	}
}
