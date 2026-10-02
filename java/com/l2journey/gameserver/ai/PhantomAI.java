package com.l2journey.gameserver.ai;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.PhantomConfig;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.GeoData;
import com.l2journey.gameserver.handler.IItemHandler;
import com.l2journey.gameserver.handler.ItemHandler;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Attackable;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Monster;
import com.l2journey.gameserver.model.item.EtcItem;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.enums.ShotType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.phantom.PhantomChat;
import com.l2journey.gameserver.model.phantom.PhantomClan;
import com.l2journey.gameserver.model.phantom.PhantomCursedHunt;
import com.l2journey.gameserver.model.phantom.PhantomEngine;
import com.l2journey.gameserver.model.phantom.PhantomEquipment;
import com.l2journey.gameserver.model.phantom.PhantomGeo;
import com.l2journey.gameserver.model.phantom.PhantomHuntingSpots;
import com.l2journey.gameserver.model.phantom.PhantomManager;
import com.l2journey.gameserver.model.phantom.PhantomSkills;
import com.l2journey.gameserver.model.phantom.PhantomState;
import com.l2journey.gameserver.model.phantom.PhantomWalk;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.util.LocationUtil;

public class PhantomAI
{
	private PhantomAI()
	{
	}

	private static final int EMPTY_TARGET_RELOCATE_LIMIT = 500;

	private static final int GOAL_LEVEL_STEP_MIN = 4;
	private static final int GOAL_LEVEL_STEP_MAX = 6;

	private static final int CITY_REST_MIN_TIME = 25000;
	private static final int CITY_REST_MAX_TIME = 45000;

	private static final int MAX_CHASE_DISTANCE = 10000;
	private static final int LOOT_SCAN_RANGE = 200;
	private static final int LOOT_PICKUP_RANGE = 70;

	private static final Map<Integer, Boolean> EQUIPPED_PHANTOMS = new ConcurrentHashMap<>();
	private static final Map<Integer, Integer> LAST_SKILL_LEVEL = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> LAST_KITE_TIME = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> LAST_FIGHTER_SKILL_TIME = new ConcurrentHashMap<>();

	private static final Map<Integer, Long> LAST_HP_POTION_TIME = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> LAST_MP_POTION_TIME = new ConcurrentHashMap<>();

	private static final Map<Integer, Monster> LAST_TARGET = new ConcurrentHashMap<>();

	public static void unregisterBot(int objectId)
	{
		EQUIPPED_PHANTOMS.remove(objectId);
		LAST_SKILL_LEVEL.remove(objectId);
		LAST_KITE_TIME.remove(objectId);
		LAST_FIGHTER_SKILL_TIME.remove(objectId);
		LAST_TARGET.remove(objectId);

		LAST_HP_POTION_TIME.remove(objectId);
		LAST_MP_POTION_TIME.remove(objectId);

		PhantomSkills.unregisterBot(objectId);
		PhantomCursedHunt.unregisterBot(objectId);
		PhantomEquipment.unregisterBot(objectId);
		PhantomEngine.cancelReturnRelocate(objectId);
		PhantomState.unregister(objectId);
	}

	public static void clearRuntimeState()
	{
		EQUIPPED_PHANTOMS.clear();
		LAST_SKILL_LEVEL.clear();
		LAST_KITE_TIME.clear();
		LAST_FIGHTER_SKILL_TIME.clear();
		LAST_TARGET.clear();

		LAST_HP_POTION_TIME.clear();
		LAST_MP_POTION_TIME.clear();
		PhantomCursedHunt.clear();
		PhantomEquipment.clearRuntimeState();
		PhantomEngine.clearPendingReturns();
	}

	// CICLO PRINCIPAL DA IA
	public static void thinkAndFarm(Player bot)
	{
		if ((bot == null) || bot.isDead() || !PhantomEngine.isRunning || !PhantomEngine.activePhantoms.contains(bot))
		{
			return;
		}

		if (System.currentTimeMillis() < PhantomState.RETURN_UNTIL.getOrDefault(bot.getObjectId(), 0L))
		{
			return;
		}

		final boolean cursedWielder = bot.isCursedWeaponEquipped();
		final boolean cursedHunter = !cursedWielder && PhantomCursedHunt.hasActiveWeapon() && PhantomCursedHunt.isHunter(bot);
		PhantomWalk.update(bot, shouldSkipTownVisit(cursedWielder, cursedHunter));
		if (shouldPauseThinkForTownRest(PhantomWalk.isResting(bot), cursedWielder, cursedHunter) || handlePartyLeash(bot))
		{
			return;
		}

		int botId = bot.getObjectId();
		if (!checkStuckAndWatchdog(bot, botId, System.currentTimeMillis(), bot.getLocation()))
		{
			return;
		}

		int[] shots = resolveShotIds(bot.getLevel());
		handleSkillLevelUp(bot, botId, shots[0], shots[1]);
		handleItemRefill(bot, botId, shots[0], shots[1]);

		if (bot.getCurrentHpPercent() <= 70)
		{
			useHpPotion(bot);
		}

		if (handleCursedEarlyExit(bot))
		{
			return;
		}

		continueNormalThink(bot, botId, cursedHunter);
	}
	
	/**
	 * @param cursedWielder {@code true} when the bot holds a cursed weapon
	 * @param cursedHunter {@code true} when the bot is on an active cursed-weapon hunt squad
	 * @return {@code true} when the periodic town visit must not run
	 */
	static boolean shouldSkipTownVisit(boolean cursedWielder, boolean cursedHunter)
	{
		return cursedWielder || cursedHunter;
	}
	
	/**
	 * @param resting {@code true} when {@link PhantomWalk} has the bot in a town rest
	 * @param cursedWielder {@code true} when the bot holds a cursed weapon
	 * @param cursedHunter {@code true} when the bot is on an active cursed-weapon hunt squad
	 * @return {@code true} when the think tick should stop for town rest
	 */
	static boolean shouldPauseThinkForTownRest(boolean resting, boolean cursedWielder, boolean cursedHunter)
	{
		return resting && !shouldSkipTownVisit(cursedWielder, cursedHunter);
	}

	private static void continueNormalThink(Player bot, int botId, boolean cursedHunter)
	{
		if (handleLifeGoal(bot) || handleMageMpRecovery(bot))
		{
			return;
		}

		if (bot.isSitting())
		{
			bot.standUp();
			bot.getAI().setIntention(Intention.IDLE);
		}

		if ((PhantomState.STUCK_COUNTERS.getOrDefault(botId, 0) >= 2) && !cursedHunter)
		{
			trace(bot, "stuck relocating");
			relocateStuckBot(bot);
			return;
		}

		if (bot.isCastingNow() || PhantomSkills.handleSelfBuffs(bot))
		{
			return;
		}

		if (PhantomHuntingSpots.relocateForLevelIfNeeded(bot))
		{
			trace(bot, "teleport by level");
			return;
		}

		PhantomSkills.applyBasicBuffs(bot);
		PhantomEquipment.checkProgression(bot);
		PhantomEquipment.cleanInventory(bot);
		tryCasualChat(bot);
		PhantomClan.gestionarClanParaBot(bot);

		if (!handleCombatSelection(bot, botId))
		{
			handleFarmLootOrIdle(bot, botId);
		}
	}

	/**
	 * @param bot the phantom
	 * @return {@code true} if this think tick was consumed by party leash teleport
	 */
	private static boolean handlePartyLeash(Player bot)
	{
		if (bot.getParty() == null)
		{
			return false;
		}
		Player leader = bot.getParty().getLeader();
		if ((leader == null) || (leader == bot))
		{
			return false;
		}

		boolean cursedWeaponActive = PhantomCursedHunt.hasActiveWeapon();
		boolean botChasing = false;
		boolean leaderChasing = false;
		if (cursedWeaponActive)
		{
			botChasing = PhantomCursedHunt.isHunter(bot);
			leaderChasing = PhantomCursedHunt.isHunter(leader);
		}
		if (skipPartyLeash(cursedWeaponActive, botChasing, leaderChasing, bot.getLevel(), LocationUtil.calculateDistance(bot, leader, true, false)))
		{
			return false;
		}

		bot.teleToLocation(leader.getX(), leader.getY(), leader.getZ());
		if (bot.getSummon() != null)
		{
			bot.getSummon().teleToLocation(leader.getX(), leader.getY(), leader.getZ());
		}
		return true;
	}
	
	/**
	 * @param cursedWeaponActive {@code true} when any cursed weapon is active
	 * @param botHunter {@code true} when the bot is a cursed-hunt squad member
	 * @param leaderHunter {@code true} when the party leader is a cursed-hunt squad member
	 * @param botLevel bot level
	 * @param distanceToLeader distance to the party leader
	 * @return {@code true} when the party leash teleport should not run
	 */
	static boolean skipPartyLeash(boolean cursedWeaponActive, boolean botHunter, boolean leaderHunter, int botLevel, double distanceToLeader)
	{
		boolean huntSkip = false;
		if (cursedWeaponActive)
		{
			huntSkip = botHunter || ((botLevel <= 20) && leaderHunter);
		}
		return huntSkip || (distanceToLeader <= 10000);
	}

	private static int[] resolveShotIds(int currentLevel)
	{
		if (currentLevel >= 76)
		{
			return new int[]
			{
				1462,
				3952
			};
		}
		if (currentLevel >= 61)
		{
			return new int[]
			{
				1467,
				3951
			};
		}
		if (currentLevel >= 52)
		{
			return new int[]
			{
				1466,
				3950
			};
		}
		if (currentLevel >= 40)
		{
			return new int[]
			{
				1465,
				3949
			};
		}
		if (currentLevel >= 20)
		{
			return new int[]
			{
				1464,
				3948
			};
		}
		return new int[]
		{
			1463,
			3947
		};
	}

	private static void handleSkillLevelUp(Player bot, int botId, int ssId, int bssId)
	{
		int currentLevel = bot.getLevel();
		int lastLevel = LAST_SKILL_LEVEL.getOrDefault(botId, 0);
		if (currentLevel > lastLevel)
		{
			bot.giveAvailableSkills(true, true, true);
			LAST_SKILL_LEVEL.put(botId, currentLevel);
			bot.addAutoSoulShot(ssId);
			bot.addAutoSoulShot(bssId);
		}
	}

	/**
	 * Cursed wielder / hunter early path. Isolated to avoid regressions.
	 * @param bot the phantom
	 * @return {@code true} if this think tick was consumed
	 */
	static boolean handleCursedEarlyExit(Player bot)
	{
		boolean cursedWielder = bot.isCursedWeaponEquipped();
		boolean cursedHunter = !cursedWielder && PhantomCursedHunt.hasActiveWeapon() && PhantomCursedHunt.isHunter(bot);

		if (cursedWielder)
		{
			if (bot.isSitting())
			{
				bot.standUp();
				bot.getAI().setIntention(Intention.IDLE);
			}
			if (bot.isCastingNow())
			{
				return true;
			}
			PhantomCursedHunt.handleWielder(bot);
			return true;
		}

		if (!cursedHunter)
		{
			return false;
		}

		if (bot.isSitting())
		{
			bot.standUp();
			bot.getAI().setIntention(Intention.IDLE);
		}
		if (bot.isCastingNow())
		{
			return true;
		}
		return PhantomCursedHunt.handleHunter(bot);
	}

	/**
	 * @param bot the phantom
	 * @param botId the phantom object id
	 * @return {@code true} if a PvP/PK target was engaged this tick
	 */
	private static boolean handleCombatSelection(Player bot, int botId)
	{
		Player aggressor = findPvPAttacker(bot);
		if (aggressor != null)
		{
			LAST_TARGET.remove(botId);
			bot.setTarget(aggressor);
			executeAttackPlan(bot, aggressor);
			return true;
		}

		Player victim = findAggressiveTarget(bot);
		if (victim != null)
		{
			makePkIfNeeded(bot, victim);
			LAST_TARGET.remove(botId);
			bot.setTarget(victim);
			executeAttackPlan(bot, victim);
			return true;
		}
		return false;
	}

	/**
	 * Loot, farm target engagement, or empty-target handling for this think tick.
	 * @param bot the phantom
	 * @param botId the phantom object id
	 */
	private static void handleFarmLootOrIdle(Player bot, int botId)
	{
		if (!hasLiveLockedFarmTarget(bot) && tryPickupLoot(bot))
		{
			return;
		}

		Monster farmTarget = findFarmTarget(bot);
		if (farmTarget == null)
		{
			handleNoFarmTarget(bot);
			return;
		}

		if (bot.getParty() != null)
		{
			for (Player partyMember : bot.getParty().getMembers())
			{
				if ((partyMember != null) && (partyMember.getClient() != null))
				{
					farmTarget.addDamageHate(partyMember, 1, 0);
				}
			}
		}

		if (bot.getAI().getIntention() == Intention.IDLE)
		{
			bot.getAI().setIntention(Intention.ACTIVE);
		}

		PhantomState.EMPTY_TARGET_COUNTERS.put(botId, 0);
		if (tryKite(bot, farmTarget))
		{
			return;
		}

		if (bot.getTarget() != farmTarget)
		{
			bot.abortAttack();
			bot.abortCast();
			bot.setTarget(farmTarget);
			bot.getAI().setIntention(Intention.ACTIVE);
		}
		else
		{
			bot.setTarget(farmTarget);
		}

		executeAttackPlan(bot, farmTarget);
	}

	public static Player findRandomBotToAttack(Player bot)
	{
		java.util.List<Player> nearbyTargets = World.getInstance().getVisibleObjects(bot, Player.class).stream()
			.filter(obj -> (obj != null) && (obj != bot) && !obj.isAlikeDead() && !obj.isInvisible() && (obj.getClient() == null))
			.toList();

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

		final boolean cursedActivity = bot.isCursedWeaponEquipped() || PhantomCursedHunt.isHunter(bot);
		Location lastPos = PhantomState.LAST_POS.getOrDefault(botId, currentPos);

		if (bot.getAI().getIntention() == Intention.MOVE_TO)
		{
			return handleMoveStuck(bot, botId, now, currentPos, lastPos, cursedActivity);
		}
		return handleIdleWatchdog(bot, botId, now, currentPos, cursedActivity);
	}

	static boolean handleMoveStuck(Player bot, int botId, long now, Location currentPos, Location lastPos, boolean cursedActivity)
	{
		if (LocationUtil.calculateDistance(currentPos, lastPos, false, false) < 20)
		{
			long lastTime = PhantomState.STUCK_TIMER.getOrDefault(botId, now);
			if ((now - lastTime) > 55000)
			{
				bot.setTarget(null);
				bot.getAI().setIntention(Intention.IDLE);
				bot.stopMove(null);
				PhantomState.STUCK_TIMER.put(botId, now);
				PhantomState.LAST_POS.put(botId, currentPos);
				// Cursed wielders/hunters must not be farm-relocated; that fights the hunt teleports and OOMs.
				if (!cursedActivity)
				{
					relocateStuckBot(bot);
					return false;
				}
			}
		}
		else
		{
			PhantomState.LAST_POS.put(botId, currentPos);
			PhantomState.STUCK_TIMER.put(botId, now);
		}
		return true;
	}

	static boolean handleIdleWatchdog(Player bot, int botId, long now, Location currentPos, boolean cursedActivity)
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
			if (isResting || cursedActivity)
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
		return true;
	}

	public static void handleItemRefill(Player bot, int botId, int ssId, int bssId)
	{
		refillIfBelow(bot, 3031, 1000, 5000);
		refillIfBelow(bot, 1458, 500, 2000);
		refillIfBelow(bot, 1459, 500, 2000);
		refillIfBelow(bot, 1460, 500, 2000);

		if (!EQUIPPED_PHANTOMS.containsKey(botId))
		{
			refillIfBelow(bot, ssId, 500, 1000);
			refillIfBelow(bot, bssId, 500, 1000);
			refillIfBelow(bot, 728, 100, 500);
			refillIfBelow(bot, 1539, 100, 500);
			bot.setOverloaded(false);
			EQUIPPED_PHANTOMS.put(botId, true);
		}
		else
		{
			refillIfBelow(bot, 728, 100, 500);
			refillIfBelow(bot, 1539, 100, 500);
			refillIfBelow(bot, ssId, 300, 1000);
			refillIfBelow(bot, bssId, 300, 1000);
		}
	}

	private static void refillIfBelow(Player bot, int itemId, long minCount, long addCount)
	{
		if (bot.getInventory().getInventoryItemCount(itemId, -1) < minCount)
		{
			bot.getInventory().addItem(ItemProcessType.NONE, itemId, addCount, bot, null);
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

			PhantomHuntingSpots.travelToLevelSpot(bot, "Rest ended");
			return true;
		}

		int goalLevel = PhantomState.GOAL_LEVEL.getOrDefault(objectId, 0);
		if ((goalLevel > 0) && (bot.getLevel() >= goalLevel))
		{
			travelToNearestTown(bot, "Goal reached");
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
			long dx = (long) bot.getX() - zone.townX;
			long dy = (long) bot.getY() - zone.townY;
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
		if (!PhantomEngine.movePhantomToWithReturn(bot, safeTown, reason))
		{
			return;
		}
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
			trace(bot, "Critical MP (no potions). Mage sits to rest.");
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
			trace(bot, "MP recovered to 85%. Mage stands up.");
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

		PhantomEngine.movePhantomToWithReturn(bot, loc, "Relocating");
		PhantomState.STUCK_COUNTERS.put(bot.getObjectId(), 0);
		bot.setTarget(null);
	}

	private static Player findPvPAttacker(Player bot)
	{
		for (Player p : World.getInstance().getVisibleObjectsInRange(bot, Player.class, 1500))
		{
			if ((p == bot) || p.isDead() || p.isGM() || ((bot.getParty() != null) && (bot.getParty() == p.getParty())))
			{
				continue;
			}

			if ((p.isAttackingNow() || p.isCastingNow()) && ((p.getTarget() == bot) || ((bot.getSummon() != null) && (p.getTarget() == bot.getSummon()))))
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
			if ((p == bot) || p.isDead() || p.isGM() || ((bot.getParty() != null) && (bot.getParty() == p.getParty())))
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

	private static boolean hasLiveLockedFarmTarget(Player bot)
	{
		Monster locked = LAST_TARGET.get(bot.getObjectId());
		if ((locked == null) || locked.isDead() || !isCurrentTargetValid(locked))
		{
			return false;
		}

		double dist = LocationUtil.calculateDistance(bot, locked, true, false);
		return (dist <= 3500) && PhantomGeo.canSeeTarget(bot, locked);
	}

	/**
	 * Picks up nearby ground loot that belongs to this phantom (or is unprotected).
	 * @param bot The phantom trying to pick up loot
	 * @return {@code true} if the bot is moving to loot or picked something up this tick
	 */
	private static boolean tryPickupLoot(Player bot)
	{
		Item closest = null;
		double bestDistance = Double.MAX_VALUE;

		for (Item droppedItem : World.getInstance().getVisibleObjectsInRange(bot, Item.class, LOOT_SCAN_RANGE))
		{
			final boolean shouldSkipItem = (droppedItem == null)
				|| !droppedItem.isSpawned()
				|| (droppedItem.isProtected() && (droppedItem.getOwnerId() != bot.getObjectId()))
				|| !GeoData.getInstance().canMove(bot.getX(), bot.getY(), bot.getZ(), droppedItem.getX(), droppedItem.getY(), droppedItem.getZ(), bot.getInstanceId());

			if (shouldSkipItem)
			{
				continue;
			}

			double distance = bot.calculateDistance2D(droppedItem);
			if (distance < bestDistance)
			{
				bestDistance = distance;
				closest = droppedItem;
			}
		}

		if (closest == null)
		{
			return false;
		}

		bot.abortAttack();
		bot.abortCast();
		bot.setTarget(null);

		if (bestDistance > LOOT_PICKUP_RANGE)
		{
			if (!bot.isMoving())
			{
				bot.getAI().setIntention(Intention.MOVE_TO, closest);
			}
			return true;
		}

		bot.doPickupItem(closest);
		return true;
	}

	private static Monster findFarmTarget(Player bot)
	{
		int botId = bot.getObjectId();
		Monster locked = resolveLockedFarmTarget(bot, botId);
		if (locked != null)
		{
			return locked;
		}

		Monster chosen = scanFarmTargets(bot);
		if (chosen != null)
		{
			PhantomState.STUCK_COUNTERS.put(botId, 0);
			LAST_TARGET.put(botId, chosen);
		}
		return chosen;
	}

	private static Monster scanFarmTargets(Player bot)
	{
		Monster closestAggroMob = null;
		double bestAggroDistance = Double.MAX_VALUE;
		Monster closestPassiveMob = null;
		double bestPassiveDistance = Double.MAX_VALUE;

		for (Monster m : World.getInstance().getVisibleObjectsInRange(bot, Monster.class, 3000))
		{
			if (!isFarmCandidate(m))
			{
				continue;
			}

			double distance = LocationUtil.calculateDistance(bot, m, true, false);
			boolean isAttackingUs = (m.getTarget() == bot) || ((bot.getSummon() != null) && (m.getTarget() == bot.getSummon()));

			if (isAttackingUs)
			{
				if ((distance < bestAggroDistance) && PhantomGeo.canSeeTarget(bot, m))
				{
					bestAggroDistance = distance;
					closestAggroMob = m;
				}
			}
			else if ((m.isChampion() || (m == bot.getTarget()) || !isTargetedByAnother(bot, m)) && (distance < bestPassiveDistance) && PhantomGeo.canSeeTarget(bot, m))
			{
				bestPassiveDistance = distance;
				closestPassiveMob = m;
			}
		}

		return (closestAggroMob != null) ? closestAggroMob : closestPassiveMob;
	}

	private static Monster resolveLockedFarmTarget(Player bot, int botId)
	{
		Monster currentLocked = LAST_TARGET.get(botId);
		if (currentLocked == null)
		{
			return null;
		}
		if (!currentLocked.isDead() && isCurrentTargetValid(currentLocked))
		{
			double dist = LocationUtil.calculateDistance(bot, currentLocked, true, false);
			if ((dist <= 3500) && PhantomGeo.canSeeTarget(bot, currentLocked))
			{
				return currentLocked;
			}
		}
		LAST_TARGET.remove(botId);
		return null;
	}

	private static boolean isFarmCandidate(Monster m)
	{
		if ((m == null) || m.isDead() || !m.isAttackable() || m.isRaid())
		{
			return false;
		}
		if (m.getName() != null)
		{
			String name = m.getName();
			if (name.contains("Chest") || name.contains("chest") || name.contains("Box") || name.contains("box"))
			{
				return false;
			}
		}
		return true;
	}

	private static boolean isCurrentTargetValid(Monster m)
	{
		return (m != null) && m.isSpawned() && m.isAttackable();
	}

	private static boolean isTargetedByAnother(Player bot, Monster m)
	{
		if ((m.getTarget() == bot) || ((bot.getSummon() != null) && (m.getTarget() == bot.getSummon())))
		{
			return false;
		}

		if ((m.getTarget() != null) && (m.getTarget() != bot) && ((bot.getSummon() == null) || (m.getTarget() != bot.getSummon())))
		{
			return true;
		}

		for (Player p : World.getInstance().getVisibleObjectsInRange(m, Player.class, 1000))
		{
			if ((p != null) && (p != bot) && !p.isAlikeDead() && (p.getTarget() == m))
			{
				return true;
			}
		}

		return false;
	}

	private static void handleNoFarmTarget(Player bot)
	{
		int botObjectId = bot.getObjectId();

		LAST_TARGET.remove(botObjectId);

		Intention currentIntention = bot.getAI().getIntention();
		if ((currentIntention == Intention.ATTACK) || (currentIntention == Intention.CAST) || (currentIntention == Intention.MOVE_TO))
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

		int count = PhantomState.EMPTY_TARGET_COUNTERS.getOrDefault(botObjectId, 0) + 1;
		PhantomState.EMPTY_TARGET_COUNTERS.put(botObjectId, count);

		if (count >= EMPTY_TARGET_RELOCATE_LIMIT)
		{
			PhantomState.EMPTY_TARGET_COUNTERS.put(botObjectId, 0);

			PhantomState.LAST_ATTACK_TIME.put(botObjectId, System.currentTimeMillis());
			PhantomState.STUCK_COUNTERS.put(botObjectId, 0);

			bot.setTarget(null);
			bot.stopMove(null);
			PhantomHuntingSpots.travelToLevelSpot(bot, "New spot due to no monsters");
		}
	}

	public static boolean tryKite(Player bot, Monster mob)
	{
		int classId = bot.getActiveClass();

		final boolean isArcher = (classId == 9)
			|| (classId == 24)
			|| (classId == 37)
			|| (classId == 92)
			|| (classId == 102)
			|| (classId == 109)
			|| (classId == 130)
			|| (classId == 134);

		if (!isArcher)
		{
			return false;
		}

		long now = System.currentTimeMillis();
		long lastKite = LAST_KITE_TIME.getOrDefault(bot.getObjectId(), 0L);

		// 1. Proteger o movimento enquanto foge
		if (bot.isMoving() && ((now - lastKite) < 2500))
		{
			return true;
		}

		// 2. Cooldown
		if ((now - lastKite) < 5000)
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
		double angle = Math.atan2((double) bot.getY() - mob.getY(), (double) bot.getX() - mob.getX());

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
		return switch (classId)
		{
			case 6, 91, // Dark Avenger / Hell Knight
				5, 90, // Paladin / Phoenix Knight
				33, 106, // Shillien Knight / Shillien Templar
				20, 99, // Temple Knight / Eva's Templar
				14, 96, // Warlock / Arcana Lord
				28, 104, // Elemental Summoner / Elemental Master
				41, 111, // Phantom Summoner / Spectral Master
				56, 57, 118 // Artisan / Warsmith / Maestro
				-> true;
			default -> false;
		};
	}

	private static void executeAttackPlan(Player bot, Creature target)
	{
		attackTarget(bot, target);
	}

	/**
	 * Shared attack plan for phantom combat (farm, PvP, cursed-sword hunting).
	 * @param bot the phantom
	 * @param target the creature to attack
	 */
	public static void attackTarget(Player bot, Creature target)
	{
		if ((target != null) && target.isPlayer() && (bot.getParty() != null) && (bot.getParty() == target.asPlayer().getParty()))
		{
			bot.setTarget(null);
			bot.getAI().setIntention(Intention.IDLE);
			return;
		}

		if ((target != null) && (LocationUtil.calculateDistance(bot, target, true, false) > MAX_CHASE_DISTANCE))
		{
			abandonAttackTarget(bot);
			return;
		}

		sharePartyHate(bot, target);

		if (!PhantomGeo.canSeeTarget(bot, target))
		{
			handleLostSight(bot);
			return;
		}

		if (canUseSummons(bot) && PhantomSkills.handleCombatSummons(bot, target))
		{
			return;
		}

		assistSummonAttack(bot, target);

		if (tryMageAttack(bot, target))
		{
			return;
		}

		executeFighterAttack(bot, target);
	}

	private static void abandonAttackTarget(Player bot)
	{
		bot.abortAttack();
		bot.abortCast();
		bot.setTarget(null);
		bot.getAI().setIntention(Intention.ACTIVE);
		LAST_TARGET.remove(bot.getObjectId());
	}

	private static void sharePartyHate(Player bot, Creature target)
	{
		if ((bot.getParty() == null) || !(target instanceof Attackable attackable))
		{
			return;
		}
		for (Player member : bot.getParty().getMembers())
		{
			if ((member != null) && (member.getClient() != null))
			{
				attackable.addDamageHate(member, 1, 0);
			}
		}
	}

	private static void handleLostSight(Player bot)
	{
		Location detourPos = PhantomGeo.getRandomReachableLocation(bot, 200);
		if ((detourPos != null) && (detourPos.getX() != bot.getX()))
		{
			bot.getAI().setIntention(Intention.MOVE_TO, detourPos);
			return;
		}
		abandonAttackTarget(bot);
	}

	private static void assistSummonAttack(Player bot, Creature target)
	{
		if ((bot.getSummon() != null) && (bot.getSummon().getTarget() != target))
		{
			bot.getSummon().setTarget(target);
			bot.getSummon().getAI().setIntention(Intention.ATTACK, target);
		}
	}

	private static boolean tryMageAttack(Player bot, Creature target)
	{
		if (!bot.isMageClass())
		{
			return false;
		}

		Skill bestMagic = PhantomSkills.getMageAttackSkill(bot, target);
		if (bestMagic == null)
		{
			bestMagic = PhantomSkills.getBestOffensiveSkill(bot);
		}

		if ((bestMagic == null) || (bot.getCurrentMpPercent() <= 15) || bot.isSkillDisabled(bestMagic))
		{
			return false;
		}

		if (bot.getAI().getIntention() != Intention.CAST)
		{
			bot.setChargedShot(ShotType.BLESSED_SPIRITSHOTS, true);
			bot.getAI().setIntention(Intention.CAST, bestMagic, target);
		}
		return true;
	}

	private static void executeFighterAttack(Player bot, Creature target)
	{
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
		else if (bot.getAI().getIntention() != Intention.ATTACK)
		{
			bot.setChargedShot(ShotType.SOULSHOTS, true);
			bot.getAI().setIntention(Intention.ATTACK, target);
		}
	}

	/**
	 * Clears the locked farm monster for a phantom (e.g. when switching to PvP / cursed hunt).
	 * @param bot the phantom
	 */
	public static void clearFarmTargetLock(Player bot)
	{
		if (bot != null)
		{
			LAST_TARGET.remove(bot.getObjectId());
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
		long now = System.currentTimeMillis();
		long lastPot = LAST_HP_POTION_TIME.getOrDefault(bot.getObjectId(), 0L);
		if ((now - lastPot) < 10000)
		{
			return;
		}

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
		long now = System.currentTimeMillis();
		long lastPot = LAST_MP_POTION_TIME.getOrDefault(bot.getObjectId(), 0L);
		if ((now - lastPot) < 10000)
		{
			return;
		}

		Item potion = bot.getInventory().getItemByItemId(728);
		if (potion != null)
		{
			LAST_MP_POTION_TIME.put(bot.getObjectId(), now);
			EtcItem etcItem = (EtcItem) potion.getTemplate();
			IItemHandler handler = ItemHandler.getInstance().getHandler(etcItem);
			if (handler != null)
			{
				handler.useItem(bot, potion, false);
				PhantomManager.logToFile(bot.getName(), "Using Mana Potion");
			}
		}
	}

	private static void tryCasualChat(Player bot)
	{
		if (Rnd.get(100) < 1)
		{
			Player nearby = World.getInstance().getVisibleObjectsInRange(bot, Player.class, 1000).stream().filter(p -> !p.isDead() && !PhantomEngine.activePhantoms.contains(p)).findFirst().orElse(null);

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
