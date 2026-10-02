package com.l2journey.gameserver.model.phantom;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.actor.Player;

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
	
	// Tempos configuraveis
	private static final long FARM_DURATION = 45 * 60 * 1000; // 45 minutos farmando
	private static final long TOWN_DURATION = 5 * 60 * 1000; // 5 minutos na cidade
	private static final long TIME_QUIETO_TOWN = 2 * 60 * 1000; // 2 minutos parado entre caminhadas
	
	public static void update(Player bot, boolean skipTownVisit)
	{
		if ((bot == null) || bot.isDead())
		{
			return;
		}
		
		int objectId = bot.getObjectId();
		long currentTime = System.currentTimeMillis();
		
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
			// --- ESTADO: DESCANSANDO NA CIDADE ---
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
			
			// --- LOGICA DE SUB-CICLO: CAMINHAR VS FICAR PARADO ---
			boolean isWaiting = _isWaitingInTown.getOrDefault(objectId, false);
			
			if (isWaiting)
			{
				long waitStart = _waitingStartTime.getOrDefault(objectId, 0L);
				// Se ja passaram os 2 minutos parado, reiniciamos o contador de caminhadas
				if ((currentTime - waitStart) > TIME_QUIETO_TOWN)
				{
					_isWaitingInTown.put(objectId, false);
					_walkCount.put(objectId, 0);
					_targetWalks.put(objectId, Rnd.get(100) < 50 ? 2 : 3); // Escolhe aleatoriamente se caminhara 2 ou 3 vezes
				}
				else
				{
					// Esta em seu tempo de espera de 2 minutos, nao faz nada (fica parado)
					return;
				}
			}
			
			// Se nao esta esperando, processamos as caminhadas
			if (!bot.isMoving())
			{
				long lastWalk = _lastWalkActionTime.getOrDefault(objectId, 0L);
				
				// Cooldown de 12 segundos para dar tempo de terminarem de chegar ao destino antes de iniciar a proxima caminhada
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
						// Ja cumpriu as 2 ou 3 caminhadas, agora passamos para o estado de espera de 2 minutos
						_isWaitingInTown.put(objectId, true);
						_waitingStartTime.put(objectId, currentTime);
					}
				}
			}
		}
		else
		{
			// --- ESTADO: FARMANDO ---
			if ((currentTime - lastVisit) > FARM_DURATION)
			{
				
				// Inicializamos as variaveis do sub-ciclo da cidade
				_isResting.put(objectId, true);
				_restingStartTime.put(objectId, currentTime);
				_walkCount.put(objectId, 0);
				_targetWalks.put(objectId, Rnd.get(100) < 50 ? 2 : 3); // 2 ou 3 caminhadas aleatorias
				_isWaitingInTown.put(objectId, false);
				_lastWalkActionTime.put(objectId, 0L);
				
				Location town = getCityLocationWithSpread(bot.getLevel(), 400);
				forceTeleport(bot, town);
			}
		}
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
	
	private static Location getCityLocationWithSpread(int lvl, int spreadRadius)
	{
		int spreadX = Rnd.get(-spreadRadius, spreadRadius);
		int spreadY = Rnd.get(-spreadRadius, spreadRadius);
		
		if (lvl >= 76)
		{
			return new Location(147702 + spreadX, -56030 + spreadY, -2775); // Goddard (Lvl 76+)
		}
		if (lvl >= 61)
		{
			return new Location(146855 + spreadX, 27011 + spreadY, -2205); // Aden (Lvl 61 a 75)
		}
		if (lvl >= 52)
		{
			return new Location(116589 + spreadX, 76268 + spreadY, -2723); // Hunter (Lvl 52 a 60)
		}
		if (lvl >= 40)
		{
			return new Location(82611 + spreadX, 148719 + spreadY, -3469); // Giran (Lvl 40 a 60)
		}
		if (lvl >= 20)
		{
			return new Location(18810 + spreadX, 145240 + spreadY, -3130); // Dion (Lvl 20 a 39)
		}
		// Se nao cumpriu nenhuma das condicoes anteriores, e nivel 19 ou menor.
		return new Location(-14372 + spreadX, 123051 + spreadY, -3117); // Gludio (Lvl 19 ou menos)
	}
	
	private static Location getFarmingLocation(int lvl, int spreadRadius)
	{
		if (lvl >= 40)
		{
			return new Location(83143 + Rnd.get(-spreadRadius, spreadRadius), 147943 + Rnd.get(-spreadRadius, spreadRadius), -3402); // Giran
		}
		return new Location(15415 + Rnd.get(-spreadRadius, spreadRadius), 142767 + Rnd.get(-spreadRadius, spreadRadius), -2703); // Dion
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
	}
}
