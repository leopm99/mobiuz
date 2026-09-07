package com.l2journey.gameserver.ai;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.PhantomConfig;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.handler.IItemHandler;
import com.l2journey.gameserver.handler.ItemHandler;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Monster;
import com.l2journey.gameserver.model.item.EtcItem;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.enums.ShotType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.phantom.PhantomChat;
import com.l2journey.gameserver.model.phantom.PhantomClan;
import com.l2journey.gameserver.model.phantom.PhantomEngine;
import com.l2journey.gameserver.model.phantom.PhantomEquipment;
import com.l2journey.gameserver.model.phantom.PhantomManager;
import com.l2journey.gameserver.model.phantom.PhantomSkills;
import com.l2journey.gameserver.model.phantom.PhantomState;
import com.l2journey.gameserver.model.phantom.PhantomWalk;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.util.LocationUtil;

import com.l2journey.gameserver.model.phantom.PhantomGeo;
import com.l2journey.gameserver.model.phantom.PhantomHuntingSpots;

public class PhantomAI
{
	private static final int EMPTY_TARGET_RELOCATE_LIMIT = 500;
	
	private static final int GOAL_LEVEL_STEP_MIN = 4;
	private static final int GOAL_LEVEL_STEP_MAX = 6;
	
	private static final int CITY_REST_MIN_TIME = 25000;
	private static final int CITY_REST_MAX_TIME = 45000;
	
	private static final int MAX_CHASE_DISTANCE = 10000;
	
	// MAPAS DE ESTADO
	private static final Map<Integer, Boolean> EQUIPPED_PHANTOMS = new ConcurrentHashMap<>();
	private static final Map<Integer, Integer> LAST_SKILL_LEVEL = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> LAST_KITE_TIME = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> LAST_FIGHTER_SKILL_TIME = new ConcurrentHashMap<>();
	
	// CORRECAO RAM: adicionamos relogios para nao saturar de requisicoes de pocoes
	private static final Map<Integer, Long> LAST_HP_POTION_TIME = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> LAST_MP_POTION_TIME = new ConcurrentHashMap<>();
	
	// CORRECAO: a sintaxe foi limpa e agora e usada para o Target Lock
	private static final Map<Integer, Monster> ULTIMO_TARGET = new ConcurrentHashMap<>();	

	public static void unregisterBot(int objectId)
	{
		EQUIPPED_PHANTOMS.remove(objectId);
		LAST_SKILL_LEVEL.remove(objectId);
		LAST_KITE_TIME.remove(objectId);
		LAST_FIGHTER_SKILL_TIME.remove(objectId);
		ULTIMO_TARGET.remove(objectId); // Limpeza do Target Lock
		
		// CORRECAO RAM: limpamos os relogios de pocoes
		LAST_HP_POTION_TIME.remove(objectId);
		LAST_MP_POTION_TIME.remove(objectId);
		
		PhantomSkills.unregisterBot(objectId);
		PhantomState.unregister(objectId);
	}
	
	public static void clearRuntimeState()
	{
		EQUIPPED_PHANTOMS.clear();
		LAST_SKILL_LEVEL.clear();
		LAST_KITE_TIME.clear();
		LAST_FIGHTER_SKILL_TIME.clear();
		ULTIMO_TARGET.clear(); // Limpeza do Target Lock
		
		// CORRECAO RAM: limpamos os relogios
		LAST_HP_POTION_TIME.clear();
		LAST_MP_POTION_TIME.clear();
	}
	
	// CICLO PRINCIPAL DE IA
	public static void thinkAndFarm(Player bot)
	{
		if ((bot == null) || bot.isDead())
		{
			return;
		}
		
		PhantomWalk.update(bot);
		
		if (PhantomWalk.isResting(bot))
		{
			return;
		}
		
		if (bot.getParty() != null)
		{
			Player leader = bot.getParty().getLeader();
			if ((leader != null) && (leader != bot))
			{
				if (LocationUtil.calculateDistance(bot, leader, true, false) > 10000)
				{
					bot.teleToLocation(leader.getX(), leader.getY(), leader.getZ());
					if (bot.getSummon() != null)
					{
						bot.getSummon().teleToLocation(leader.getX(), leader.getY(), leader.getZ());
					}
					return;
				}
			}
		}
		
		int botId = bot.getObjectId();
		long now = System.currentTimeMillis();
		Location currentPos = bot.getLocation();
		
		if (!checkStuckAndWatchdog(bot, botId, now, currentPos))
		{
			return;
		}
		
		int currentLevel = bot.getLevel();
		
		int ssId = 1463;
		int bssId = 3947;
		
		if (currentLevel >= 76) { ssId = 1462; bssId = 3952; }
		else if (currentLevel >= 61) { ssId = 1467; bssId = 3951; }
		else if (currentLevel >= 52) { ssId = 1466; bssId = 3950; }
		else if (currentLevel >= 40) { ssId = 1465; bssId = 3949; }
		else if (currentLevel >= 20) { ssId = 1464; bssId = 3948; }

		int lastLevel = LAST_SKILL_LEVEL.getOrDefault(botId, 0);
		if (currentLevel > lastLevel)
		{
			bot.giveAvailableSkills(true, true, true);
			LAST_SKILL_LEVEL.put(botId, currentLevel);
			
			// OTIMIZACAO: ativamos os soulshots somente ao nascer ou ao mudar de grau
			bot.addAutoSoulShot(ssId);
			bot.addAutoSoulShot(bssId);
		}
		
		handleItemRefill(bot, botId, ssId, bssId);
		
		if (bot.getCurrentHpPercent() <= 70)
		{
			useHpPotion(bot);
		}
		
		if (handleLifeGoal(bot) || handleMageMpRecovery(bot))
		{
			return;
		}
		
		if (bot.isSitting())
		{
			bot.standUp();
			bot.getAI().setIntention(Intention.IDLE);
		}
		
		if (PhantomState.STUCK_COUNTERS.getOrDefault(botId, 0) >= 2)
		{
			trace(bot, "atascado relocalizando");
			relocateStuckBot(bot);
			return;
		}
		
		if (bot.isCastingNow())
		{
			return;
		}
		
		if (PhantomSkills.handleSelfBuffs(bot))
		{
			return;
		}

		if (PhantomHuntingSpots.relocateForLevelIfNeeded(bot))
		{
			trace(bot, "teleport por nivel");
			return;
		}
		
		PhantomSkills.applyBasicBuffs(bot);
		PhantomEquipment.checkProgression(bot);
		PhantomEquipment.cleanInventory(bot);
		
		tryCasualChat(bot);
		PhantomClan.gestionarClanParaBot(bot);
		
		Player aggressor = findPvPAttacker(bot);
		if (aggressor != null)
		{
			ULTIMO_TARGET.remove(botId); // Quebrar fixacao de mob se houver PVP
			bot.setTarget(aggressor);
			executeAttackPlan(bot, aggressor);
			return;
		}
		
		Player victim = findAggressiveTarget(bot);
		if (victim != null)
		{
			makePkIfNeeded(bot, victim);
			ULTIMO_TARGET.remove(botId); // Quebrar fixacao de mob se houver PK
			bot.setTarget(victim);
			executeAttackPlan(bot, victim);
			return;
		}

		Monster target = findFarmTarget(bot);
		
		if (target != null)
		{

			if (bot.getParty() != null)
			{
				for (Player miembro : bot.getParty().getMembers())
				{
					if ((miembro != null) && (miembro.getClient() != null))
					{
						// addDamageHate(atacante, dano, aggro)
						target.addDamageHate(miembro, 1, 0);
					}
				}
			}

			if (bot.getAI().getIntention() == Intention.IDLE)
			{
				bot.getAI().setIntention(Intention.ACTIVE);
			}
			
			PhantomState.EMPTY_TARGET_COUNTERS.put(botId, 0);
			if (tryKite(bot, target))
			{
				return; 
			}
			
			if (bot.getTarget() != target)
			{
				// Limpeza extra ao trocar de target para evitar animacoes travadas
				bot.abortAttack();
				bot.abortCast();
				bot.setTarget(target);
				bot.getAI().setIntention(Intention.ACTIVE);
			}
			else
			{
				bot.setTarget(target);
			}
			
			executeAttackPlan(bot, target);
			return;
		}
		
		handleNoFarmTarget(bot);
	}
	
	public static void onEvtThink(Player bot)
	{
	}
	
	public static Player findRandomBotToAttack(Player bot)
	{
		java.util.List<Player> nearbyTargets = new java.util.ArrayList<>();
		
		for (Player obj : World.getInstance().getVisibleObjects(bot, Player.class))
		{
			if ((obj != null) && (obj != bot) && !obj.isAlikeDead() && !obj.isInvisible())
			{
				if (obj.getClient() == null)
				{
					nearbyTargets.add(obj);
				}
			}
		}
		
		if (!nearbyTargets.isEmpty())
		{
			return nearbyTargets.get(Rnd.get(nearbyTargets.size()));
		}
		
		return null;
	}
	
	private static boolean checkStuckAndWatchdog(Player bot, int botId, long now, Location currentPos)
	{
		if (bot.isAttackingNow() || bot.isCastingNow())
		{
			PhantomState.LAST_ATTACK_TIME.put(botId, now);
			PhantomState.STUCK_TIMER.put(botId, now);
			PhantomState.LAST_POS.put(botId, currentPos);
			return true;
		}
		
		Location lastPos = PhantomState.LAST_POS.getOrDefault(botId, currentPos);
		
		if (bot.getAI().getIntention() == Intention.MOVE_TO)
		{
			if (LocationUtil.calculateDistance(currentPos, lastPos, false, false) < 20)
			{
				long lastTime = PhantomState.STUCK_TIMER.getOrDefault(botId, now);
				if ((now - lastTime) > 55000)
				{
					bot.setTarget(null);
					bot.getAI().setIntention(Intention.IDLE);
					bot.stopMove(null);
					relocateStuckBot(bot);
					PhantomState.STUCK_TIMER.remove(botId);
					return false;
				}
			}
			else
			{
				PhantomState.LAST_POS.put(botId, currentPos);
				PhantomState.STUCK_TIMER.put(botId, now);
			}
		}
		else
		{
			long lastAttack = PhantomState.LAST_ATTACK_TIME.getOrDefault(botId, 0L);
			if (lastAttack == 0L)
			{
				lastAttack = now;
				PhantomState.LAST_ATTACK_TIME.put(botId, now);
			}
			
			boolean isResting = bot.isSitting() || PhantomState.MP_RECOVERY_STATE.getOrDefault(botId, false);
			
			if (((now - lastAttack) > 90000) && !bot.isDead())
			{
				if (isResting)
				{
					PhantomState.LAST_ATTACK_TIME.put(botId, now);
				}
				else
				{
					bot.setTarget(null);
					bot.getAI().setIntention(Intention.IDLE);
					relocateStuckBot(bot);
					PhantomState.LAST_ATTACK_TIME.put(botId, now);
					return false;
				}
			}
			
			PhantomState.LAST_POS.put(botId, currentPos);
			PhantomState.STUCK_TIMER.put(botId, now);
		}
		
		return true;
	}
	
	public static void handleItemRefill(Player bot, int botId, int ssId, int bssId)
	{
		if (bot.getInventory().getInventoryItemCount(3031, -1) < 1000)
		{
			bot.getInventory().addItem(ItemProcessType.NONE, 3031, 5000, bot, null);
		}
		if (bot.getInventory().getInventoryItemCount(1458, -1) < 500)
		{
			bot.getInventory().addItem(ItemProcessType.NONE, 1458, 2000, bot, null);
		}
		if (bot.getInventory().getInventoryItemCount(1459, -1) < 500)
		{
			bot.getInventory().addItem(ItemProcessType.NONE, 1459, 2000, bot, null);
		}
		if (bot.getInventory().getInventoryItemCount(1460, -1) < 500)
		{
			bot.getInventory().addItem(ItemProcessType.NONE, 1460, 2000, bot, null);
		}

		if (!EQUIPPED_PHANTOMS.containsKey(botId))
		{
			if (bot.getInventory().getInventoryItemCount(ssId, -1) < 500)
			{
				bot.getInventory().addItem(ItemProcessType.NONE, ssId, 1000, bot, null);
			}
			if (bot.getInventory().getInventoryItemCount(bssId, -1) < 500)
			{
				bot.getInventory().addItem(ItemProcessType.NONE, bssId, 1000, bot, null);
			}
			if (bot.getInventory().getInventoryItemCount(728, -1) < 100)
			{
				bot.getInventory().addItem(ItemProcessType.NONE, 728, 500, bot, null);
			}
			if (bot.getInventory().getInventoryItemCount(1539, -1) < 100)
			{
				bot.getInventory().addItem(ItemProcessType.NONE, 1539, 500, bot, null);
			}
			
			bot.setOverloaded(false);
			EQUIPPED_PHANTOMS.put(botId, true);
		}
		else
		{
			if (bot.getInventory().getInventoryItemCount(728, -1) < 100)
			{
				bot.getInventory().addItem(ItemProcessType.NONE, 728, 500, bot, null);
			}
			if (bot.getInventory().getInventoryItemCount(1539, -1) < 100)
			{
				bot.getInventory().addItem(ItemProcessType.NONE, 1539, 500, bot, null);
			}
			if (bot.getInventory().getInventoryItemCount(ssId, -1) < 300)
			{
				bot.getInventory().addItem(ItemProcessType.NONE, ssId, 1000, bot, null);
			}
			if (bot.getInventory().getInventoryItemCount(bssId, -1) < 300)
			{
				bot.getInventory().addItem(ItemProcessType.NONE, bssId, 1000, bot, null);
			}
		}
	}

	private static boolean handleLifeGoal(Player bot)
	{
		int objectId = bot.getObjectId();
		long now = System.currentTimeMillis();
		long restUntil = PhantomState.CITY_REST_UNTIL.getOrDefault(objectId, 0L);
		
		if (restUntil > now)
		{
			recoverInTown(bot);
			return true;
		}
		
		if (restUntil != 0L)
		{
			PhantomState.CITY_REST_UNTIL.put(objectId, 0L);
			assignNewGoal(bot);
			
			if (bot.isSitting())
			{
				bot.standUp();
			}
			
			PhantomHuntingSpots.travelToLevelSpot(bot, "Fin descanso");
			return true;
		}
		
		int goalLevel = PhantomState.GOAL_LEVEL.getOrDefault(objectId, 0);
		if ((goalLevel > 0) && (bot.getLevel() >= goalLevel))
		{
			travelToNearestTown(bot, "Meta cumplida");
			return true;
		}
		
		if (goalLevel <= 0)
		{
			assignNewGoal(bot);
		}
		
		return false;
	}
	
	private static void assignNewGoal(Player bot)
	{
		int goal = Math.min(99, bot.getLevel() + Rnd.get(GOAL_LEVEL_STEP_MIN, GOAL_LEVEL_STEP_MAX));
		PhantomState.GOAL_LEVEL.put(bot.getObjectId(), goal);
	}
	
	private static void travelToNearestTown(Player bot, String reason)
	{
		PhantomConfig.FarmZone nearest = null;
		long bestDistance = Long.MAX_VALUE;
		
		for (PhantomConfig.FarmZone zone : PhantomConfig.ROUTES)
		{
			long dx = bot.getX() - zone.townX;
			long dy = bot.getY() - zone.townY;
			long distance = (dx * dx) + (dy * dy);
			
			if (distance < bestDistance)
			{
				bestDistance = distance;
				nearest = zone;
			}
		}
		
		if (nearest == null)
		{
			return;
		}
		
		Location safeTown = new Location(nearest.townX, nearest.townY, nearest.townZ);
		PhantomEngine.movePhantomTo(bot, safeTown, reason);
		PhantomState.HUNT_LEVEL_BAND.put(bot.getObjectId(), -1);
		
		PhantomState.CITY_REST_UNTIL.put(bot.getObjectId(), System.currentTimeMillis() + Rnd.get(CITY_REST_MIN_TIME, CITY_REST_MAX_TIME));
	}
	
	private static void recoverInTown(Player bot)
	{
		if (bot.isDead())
		{
			return;
		}
		
		if (!bot.isSitting())
		{
			bot.getAI().setIntention(Intention.REST);
			bot.sitDown();
		}
		
		double mpGain = Math.max(1, bot.getMaxMp() * 0.08);
		double hpGain = Math.max(1, bot.getMaxHp() * 0.04);
		
		bot.setCurrentMp(Math.min(bot.getMaxMp(), bot.getCurrentMp() + mpGain));
		bot.setCurrentHp(Math.min(bot.getMaxHp(), bot.getCurrentHp() + hpGain));
		
		bot.broadcastUserInfo();
	}
	
	private static boolean handleMageMpRecovery(Player bot)
	{
		double mpPercent = bot.getCurrentMpPercent();
		
		if (mpPercent < 20)
		{
			useManaPotion(bot);
		}
		
		if (!bot.isMageClass())
		{
			return false;
		}
		
		boolean recovering = PhantomState.MP_RECOVERY_STATE.getOrDefault(bot.getObjectId(), false);
		
		if (!recovering && (mpPercent <= 15))
		{
			recovering = true;
			PhantomState.MP_RECOVERY_STATE.put(bot.getObjectId(), true);
			bot.getAI().setIntention(Intention.REST);
			bot.sitDown();
			trace(bot, "MP Critico (sin potas). El mago se sienta a descansar.");
		}
		
		if (!recovering)
		{
			return false;
		}
		
		if (mpPercent >= 85)
		{
			PhantomState.MP_RECOVERY_STATE.put(bot.getObjectId(), false);
			if (bot.isSitting())
			{
				bot.standUp();
			}
			trace(bot, "MP Recuperado al 85%. El mago se levanta.");
			return false;
		}
		
		Player danger = findPvPAttacker(bot);
		if (danger != null)
		{
			if (bot.isSitting())
			{
				bot.standUp();
			}
			runAwayFrom(bot, danger);
			return true;
		}
		
		if (!bot.isSitting())
		{
			bot.getAI().setIntention(Intention.REST);
			bot.sitDown();
		}
		
		return true;
	}
	
	private static void relocateStuckBot(Player bot)
	{
		PhantomConfig.FarmZone zone = PhantomConfig.getIdealZone(bot.getLevel());
		Location loc = new Location(zone.spotX + Rnd.get(-150, 150), zone.spotY + Rnd.get(-150, 150), zone.spotZ);
		
		PhantomEngine.movePhantomTo(bot, loc, "Relocalizando");
		PhantomState.STUCK_COUNTERS.put(bot.getObjectId(), 0);
		bot.setTarget(null);
	}
	
	private static Player findPvPAttacker(Player bot)
	{
		for (Player p : World.getInstance().getVisibleObjectsInRange(bot, Player.class, 1500))
		{
			if ((p == bot) || p.isDead() || p.isGM())
			{
				continue;
			}
			
			if ((bot.getParty() != null) && (bot.getParty() == p.getParty()))
			{
				continue;
			}
			
			if ((p.isAttackingNow() || p.isCastingNow()) && (p.getTarget() == bot || (bot.getSummon() != null && p.getTarget() == bot.getSummon())))
			{
				return p;
			}
		}
		return null;
	}
	
	private static Player findAggressiveTarget(Player bot)
	{
		if (!PhantomState.isAggressive(bot.getObjectId()))
		{
			return null;
		}
		
		for (Player p : World.getInstance().getVisibleObjectsInRange(bot, Player.class, 1200))
		{
			if ((p == bot) || p.isDead() || p.isGM())
			{
				continue;
			}
			if ((bot.getParty() != null) && (bot.getParty() == p.getParty()))
			{
				continue;
			}
			if (p.getPvpFlag() > 0)
			{
				return p;
			}
		}
		return null;
	}
	
	private static void makePkIfNeeded(Player bot, Player victim)
	{
		if (victim.getPvpFlag() <= 0)
		{
			bot.startPvPFlag();
		}
	}
	
	private static Monster findFarmTarget(Player bot)
	{
		int botId = bot.getObjectId();
		
		Monster currentLocked = ULTIMO_TARGET.get(botId);
		if (currentLocked != null)
		{
			if (!currentLocked.isDead() && currentTargetValid(bot, currentLocked))
			{
				double dist = LocationUtil.calculateDistance(bot, currentLocked, true, false);
				if (dist <= 3500 && PhantomGeo.canSeeTarget(bot, currentLocked))
				{
					return currentLocked; // Retornamos o mesmo alvo sem recalcular
				}
			}
			// Se o alvo salvo morreu, desapareceu ou foi muito longe, esquecemos dele
			ULTIMO_TARGET.remove(botId);
		}

		Monster closestAggroMob = null;
		double bestAggroDistance = Double.MAX_VALUE;
		
		Monster closestPassiveMob = null;
		double bestPassiveDistance = Double.MAX_VALUE;
		
		for (Monster m : World.getInstance().getVisibleObjectsInRange(bot, Monster.class, 3000))
		{
			if ((m == null) || m.isDead() || !m.isAttackable() || m.isRaid())
			{
				continue;
			}
			
			if (m.getName() != null)
			{
				String name = m.getName();
				if (name.contains("Chest") || name.contains("chest") || name.contains("Box") || name.contains("box"))
				{
					continue;
				}
			}
			
			double distance = LocationUtil.calculateDistance(bot, m, true, false);
			
			boolean isAttackingUs = (m.getTarget() == bot) || (bot.getSummon() != null && m.getTarget() == bot.getSummon());
			
			if (isAttackingUs)
			{
				if (distance < bestAggroDistance)
				{
					if (PhantomGeo.canSeeTarget(bot, m))
					{
						bestAggroDistance = distance;
						closestAggroMob = m;
					}
				}
			}
			else
			{
				if (!m.isChampion() && m != bot.getTarget())
				{
					if (isTargetedByAnother(bot, m))
					{
						continue;
					}
				}
				if (distance < bestPassiveDistance)
				{
					if (PhantomGeo.canSeeTarget(bot, m))
					{
						bestPassiveDistance = distance;
						closestPassiveMob = m;
					}
				}
			}
		}

		if (closestAggroMob != null)
		{
			PhantomState.STUCK_COUNTERS.put(botId, 0);
			ULTIMO_TARGET.put(botId, closestAggroMob); // Guardar fixacao (Lock)
			return closestAggroMob;
		}
		
		if (closestPassiveMob != null)
		{
			PhantomState.STUCK_COUNTERS.put(botId, 0);
			ULTIMO_TARGET.put(botId, closestPassiveMob); // Guardar fixacao (Lock)
			return closestPassiveMob;
		}
		
		return null;
	}

	// Pequena funcao auxiliar para evitar um erro de Null Pointer
	private static boolean currentTargetValid(Player bot, Monster m)
	{
		return m != null && m.isSpawned() && m.isAttackable();
	}

	private static boolean isTargetedByAnother(Player bot, Monster m)
	{
		if (m.getTarget() == bot || (bot.getSummon() != null && m.getTarget() == bot.getSummon()))
		{
			return false;
		}

		if (m.getTarget() != null)
		{
			boolean targetingBot = (m.getTarget() == bot);
			boolean targetingPet = (bot.getSummon() != null && m.getTarget() == bot.getSummon());
			
			if (!targetingBot && !targetingPet)
			{
				return true;
			}
		}

		for (Player p : World.getInstance().getVisibleObjectsInRange(m, Player.class, 1000))
		{
			if ((p != null) && (p != bot) && !p.isAlikeDead())
			{
				if (p.getTarget() == m)
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	private static void handleNoFarmTarget(Player bot)
	{
		int objectId = bot.getObjectId();
		
		ULTIMO_TARGET.remove(objectId); // Quebramos o Lock se ja nao houver monstros

		Intention currentIntention = bot.getAI().getIntention();
		if (currentIntention == Intention.ATTACK || currentIntention == Intention.CAST || currentIntention == Intention.MOVE_TO)
		{
			bot.abortAttack();
			bot.abortCast();
			bot.setTarget(null);
			bot.getAI().setIntention(Intention.ACTIVE);
		}
		else if (currentIntention == Intention.IDLE)
		{
			bot.getAI().setIntention(Intention.ACTIVE);
		}
		
		int count = PhantomState.EMPTY_TARGET_COUNTERS.getOrDefault(objectId, 0) + 1;
		PhantomState.EMPTY_TARGET_COUNTERS.put(objectId, count);
		
		if (count >= EMPTY_TARGET_RELOCATE_LIMIT)
		{
			PhantomState.EMPTY_TARGET_COUNTERS.put(objectId, 0);
			
			PhantomState.LAST_ATTACK_TIME.put(objectId, System.currentTimeMillis());
			PhantomState.STUCK_COUNTERS.put(objectId, 0);
			
			bot.setTarget(null);
			bot.stopMove(null);
			PhantomHuntingSpots.travelToLevelSpot(bot, "Nuevo spot por falta de bichos");
		}
	}
	
	public static boolean tryKite(Player bot, Monster mob)
	{
		int classId = bot.getActiveClass();
		boolean isArcher = (classId == 9 || classId == 24 || classId == 37 || classId == 109 || classId == 92 || classId == 102 || classId == 130 || classId == 134);
		
		if (!isArcher)
		{
			return false;
		}

		long now = System.currentTimeMillis();
		long lastKite = LAST_KITE_TIME.getOrDefault(bot.getObjectId(), 0L);
		
		// 1. Proteger o movimento enquanto foge
		if (bot.isMoving() && (now - lastKite < 2500))
		{
			return true;
		}

		// 2. Cooldown 
		if (now - lastKite < 5000)
		{
			return false;
		}

		// 3. INTELIGENCIA HUMANA
		if (mob.getCurrentHpPercent() <= 40)
		{
			return false;
		}

		double distance = LocationUtil.calculateDistance(bot, mob, true, false);
		
		// 4. RADAR  
		if (distance > 350)
		{
			return false;
		}

		// 5. DISTANCIA DE FUGA: 
		int kiteDistance = 350; 
		double angle = Math.atan2(bot.getY() - mob.getY(), bot.getX() - mob.getX());
		
		int newX = (int) (bot.getX() + (kiteDistance * Math.cos(angle)));
		int newY = (int) (bot.getY() + (kiteDistance * Math.sin(angle)));
		int newZ = bot.getZ();

		LAST_KITE_TIME.put(bot.getObjectId(), now);
		
		bot.abortAttack();
		bot.abortCast();
		
		Location loc = new Location(newX, newY, newZ);
		bot.getAI().setIntention(Intention.MOVE_TO, loc);
		
		return true; 
	}
	
	private static boolean canUseSummons(Player bot)
	{
		int classId = bot.getActiveClass();
		
		switch (classId)
		{
			case 6: // Dark Avenger
            case 91: // Hell Knight
            case 5: // Paladin
            case 90: // Phoenix Knight
            case 33: // Shillien Knight
            case 106: // Shillien Templar
            case 20: // Temple Knight
            case 99: // Eva's Templar
            case 14: // Warlock
            case 96: // Arcana Lord
            case 28: // Elemental Summoner
            case 104: // Elemental Master
            case 41: // Phantom Summoner
            case 111: // Spectral Master
            case 56: // Artisan (Anao)
            case 57: // Warsmith (Anao)
            case 118: // Maestro (Anao)
				return true;
			default:
				return false;
		}
	}
	
	private static void executeAttackPlan(Player bot, Creature target)
	{
		if ((target != null) && target.isPlayer() && (bot.getParty() != null) && (bot.getParty() == target.asPlayer().getParty()))
		{
			bot.setTarget(null);
			bot.getAI().setIntention(Intention.IDLE);
			return;
		}

		if ((bot.getParty() != null) && (target instanceof com.l2journey.gameserver.model.actor.Attackable))
		{
			for (Player member : bot.getParty().getMembers())
			{
				if ((member != null) && (member.getClient() != null)) 
				{
					((com.l2journey.gameserver.model.actor.Attackable) target).addDamageHate(member, 1, 0);
				}
			}
		}

		// NOVO SISTEMA ANTI-PAREDES
		if (!PhantomGeo.canSeeTarget(bot, target))
		{
			// 1. Tentamos nos mover para uma posicao proxima valida (contornando o obstaculo)
			Location detourPos = PhantomGeo.getRandomReachableLocation(bot, 200);
			
			// Se encontrou um desvio valido, vai para la
			if (detourPos != null && detourPos.getX() != bot.getX())
			{
				bot.getAI().setIntention(Intention.MOVE_TO, detourPos);
				return;
			}
			
			// 2. Se nao encontrar caminho valido para contornar, abandona o target (esta atras de uma parede intransponivel)
			bot.abortAttack();
			bot.abortCast();
			bot.setTarget(null);
			bot.getAI().setIntention(Intention.ACTIVE);
			ULTIMO_TARGET.remove(bot.getObjectId()); // Quebra o Lock para que busque outro mob visivel
			return;
		}
		
		if (canUseSummons(bot))
		{
			if (PhantomSkills.handleCombatSummons(bot, target))
			{
				return;
			}
		}
		
		if (bot.getSummon() != null)
		{
			if (bot.getSummon().getTarget() != target)
			{
				bot.getSummon().setTarget(target);
				bot.getSummon().getAI().setIntention(Intention.ATTACK, target);
			}
		}
		
		if (bot.isMageClass())
		{
			Skill bestMagic = PhantomSkills.getMageAttackSkill(bot, target);
			if (bestMagic == null)
				bestMagic = PhantomSkills.getBestOffensiveSkill(bot);
			
			if ((bestMagic != null) && (bot.getCurrentMpPercent() > 15) && !bot.isSkillDisabled(bestMagic))
			{
				if (bot.getAI().getIntention() != Intention.CAST)
				{
					bot.setChargedShot(ShotType.BLESSED_SPIRITSHOTS, true);
					bot.getAI().setIntention(Intention.CAST, bestMagic, target);
				}
				return;
			}
		}
		
		long now = System.currentTimeMillis();
		long lastSkill = LAST_FIGHTER_SKILL_TIME.getOrDefault(bot.getObjectId(), 0L);
		
		Skill fighterSkill = bot.isMageClass() ? null : PhantomSkills.getFighterAttackSkill(bot, target);
		
		boolean skillReady = (fighterSkill != null) && !bot.isSkillDisabled(fighterSkill) && ((now - lastSkill) >= 6000);
		
		if (skillReady)
		{
			LAST_FIGHTER_SKILL_TIME.put(bot.getObjectId(), now);
			
			if (bot.getAI().getIntention() != Intention.CAST)
			{
				bot.setChargedShot(ShotType.SOULSHOTS, true);
				bot.getAI().setIntention(Intention.CAST, fighterSkill, target);
			}
		}
		else
		{
			if (bot.getAI().getIntention() != Intention.ATTACK)
			{
				bot.setChargedShot(ShotType.SOULSHOTS, true);
				bot.getAI().setIntention(Intention.ATTACK, target);
			}
		}
	}
	
	private static void runAwayFrom(Player bot, Player danger)
	{
		int dx = bot.getX() - danger.getX();
		int dy = bot.getY() - danger.getY();
		
		int newX = bot.getX() + (dx > 0 ? 1200 : -1200);
		int newY = bot.getY() + (dy > 0 ? 1200 : -1200);
		
		Location loc = new Location(newX, newY, bot.getZ());
		bot.getAI().setIntention(Intention.MOVE_TO, loc);
	}
	
	private static void useHpPotion(Player bot)
	{
		// CORRECAO RAM: cooldown de 10 segundos
		long now = System.currentTimeMillis();
		long lastPot = LAST_HP_POTION_TIME.getOrDefault(bot.getObjectId(), 0L);
		if (now - lastPot < 10000) return;

		Item potion = bot.getInventory().getItemByItemId(1539);
		if (potion != null)
		{
			LAST_HP_POTION_TIME.put(bot.getObjectId(), now);
			EtcItem etcItem = (EtcItem) potion.getTemplate();
			IItemHandler handler = ItemHandler.getInstance().getHandler(etcItem);
			if (handler != null)
			{
				handler.useItem(bot, potion, false);
			}
		}
	}
	
	private static void useManaPotion(Player bot)
	{
		// CORRECAO RAM: cooldown de 10 segundos para nao spammar os logs
		long now = System.currentTimeMillis();
		long lastPot = LAST_MP_POTION_TIME.getOrDefault(bot.getObjectId(), 0L);
		if (now - lastPot < 10000) return;

		Item potion = bot.getInventory().getItemByItemId(728);
		if (potion != null)
		{
			LAST_MP_POTION_TIME.put(bot.getObjectId(), now);
			EtcItem etcItem = (EtcItem) potion.getTemplate();
			IItemHandler handler = ItemHandler.getInstance().getHandler(etcItem);
			if (handler != null)
			{
				handler.useItem(bot, potion, false);
				PhantomManager.logToFile(bot.getName(), "Usando Pocion de Mana");
			}
		}
	}
	
	private static void tryCasualChat(Player bot)
	{
		if (Rnd.get(100) < 1)
		{
			Player nearby = World.getInstance().getVisibleObjectsInRange(bot, Player.class, 1000).stream()
				.filter(p -> !p.isDead() && !PhantomEngine.activePhantoms.contains(p)).findFirst().orElse(null);
			
			if (nearby != null)
			{
				PhantomChat.requestChatResponse(bot, nearby, "", false);
			}
		}
	}
	
	private static void trace(Player bot, String message)
	{
		long now = System.currentTimeMillis();
		long last = PhantomState.LAST_AI_TRACE.getOrDefault(bot.getObjectId(), 0L);
		
		if ((now - last) >= 15000L)
		{
			PhantomState.LAST_AI_TRACE.put(bot.getObjectId(), now);
			PhantomManager.logToFile(bot.getName(), "AI: " + message);
		}
	}
}