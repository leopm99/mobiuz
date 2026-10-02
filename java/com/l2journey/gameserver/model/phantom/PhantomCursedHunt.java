package com.l2journey.gameserver.model.phantom;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.Config;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.ai.PhantomAI;
import com.l2journey.gameserver.managers.CursedWeaponsManager;
import com.l2journey.gameserver.model.CursedWeapon;
import com.l2journey.gameserver.model.Location;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Monster;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.util.LocationUtil;

/**
 * Phantom behavior while hunting cursed sword wielders or picking up dropped cursed weapons.
 */
public class PhantomCursedHunt
{
	private static final int MIN_HUNTER_LEVEL = 21;
	private static final int SCAN_RANGE = 1500;
	private static final int RUN_IN_MIN = 1200;
	private static final int RUN_IN_MAX = 1800;
	private static final int RETELEPORT_DISTANCE = 2500;
	private static final int RETELEPORT_COOLDOWN_MS = 20000;
	private static final int FIRST_TELEPORT_STAGGER_MS = 15000;
	private static final int PHANTOM_WIELDER_PAUSE_MS = 15000;
	private static final int LOOT_PICKUP_RANGE = 70;
	private static final int SQUAD_CACHE_MS = 2000;
	private static final int ACTIVE_CACHE_MS = 1000;
	private static final int WIELDER_EJECT_COOLDOWN_MS = 10000;
	private static final int WIELDER_EJECT_SPOT_ATTEMPTS = 8;
	
	/** Per phantom: next allowed cursed-hunt teleport time. */
	private static final Map<Integer, Long> TELEPORT_READY_AT = new ConcurrentHashMap<>();
	/** Per phantom wielder: next allowed town eject Return. */
	private static final Map<Integer, Long> WIELDER_EJECT_READY_AT = new ConcurrentHashMap<>();
	/** Per phantom: last cursed-hunt teleport time. */
	private static final Map<Integer, Long> LAST_TELEPORT_AT = new ConcurrentHashMap<>();
	/** Per cursed weapon item id: squad may attack a phantom wielder after this time. */
	private static final Map<Integer, Long> PHANTOM_WIELDER_PAUSE_UNTIL = new ConcurrentHashMap<>();
	/** Per cursed weapon item id: designated looter object id while the sword is on the ground. */
	static final Map<Integer, Integer> DESIGNATED_LOOTER = new ConcurrentHashMap<>();
	/** Per cursed weapon item id: remembered reference level for squad selection. */
	private static final Map<Integer, Integer> REFERENCE_LEVEL = new ConcurrentHashMap<>();
	/** Per cursed weapon item id: cached squad member object ids. */
	private static final Map<Integer, CachedSquad> SQUAD_CACHE = new ConcurrentHashMap<>();
	
	private static volatile long _activeCheckAt = 0L;
	private static volatile boolean _hasActiveWeapon = false;
	
	private static final class CachedSquad
	{
		private final long builtAt;
		private final Set<Integer> memberIds;
		
		private CachedSquad(long builtAt, Set<Integer> memberIds)
		{
			this.builtAt = builtAt;
			this.memberIds = memberIds;
		}
	}
	
	private PhantomCursedHunt()
	{
	}
	
	public static void unregisterBot(int objectId)
	{
		TELEPORT_READY_AT.remove(objectId);
		LAST_TELEPORT_AT.remove(objectId);
		WIELDER_EJECT_READY_AT.remove(objectId);
		DESIGNATED_LOOTER.entrySet().removeIf(e -> e.getValue() == objectId);
		SQUAD_CACHE.clear();
	}
	
	public static void clear()
	{
		TELEPORT_READY_AT.clear();
		LAST_TELEPORT_AT.clear();
		WIELDER_EJECT_READY_AT.clear();
		PHANTOM_WIELDER_PAUSE_UNTIL.clear();
		DESIGNATED_LOOTER.clear();
		REFERENCE_LEVEL.clear();
		SQUAD_CACHE.clear();
		_activeCheckAt = 0L;
		_hasActiveWeapon = false;
	}
	
	/**
	 * @return {@code true} if any cursed weapon is currently activated or dropped
	 */
	public static boolean hasActiveWeapon()
	{
		long now = System.currentTimeMillis();
		if ((now - _activeCheckAt) < ACTIVE_CACHE_MS)
		{
			return _hasActiveWeapon;
		}
		
		boolean active = false;
		for (CursedWeapon cw : CursedWeaponsManager.getInstance().getCursedWeapons())
		{
			if (cw.isActive())
			{
				active = true;
				break;
			}
		}
		_hasActiveWeapon = active;
		_activeCheckAt = now;
		return active;
	}
	
	/**
	 * @param bot the phantom
	 * @return {@code true} if this phantom is assigned to a cursed-weapon hunt (squad member)
	 */
	public static boolean isHunter(Player bot)
	{
		return findAssignedWeapon(bot) != null;
	}
	
	/**
	 * Handles wielder combat AI. Caller must already know the bot has a cursed weapon equipped.
	 * @param bot the phantom holding the cursed sword
	 * @return always {@code true} (consumes the think tick)
	 */
	public static boolean handleWielder(Player bot)
	{
		if (tryEjectWielderFromTown(bot))
		{
			return true;
		}
		
		PhantomAI.clearFarmTargetLock(bot);
		
		Player target = findWielderTarget(bot);
		if (target != null)
		{
			bot.setTarget(target);
			PhantomAI.attackTarget(bot, target);
			return true;
		}
		
		Monster mob = findWielderMobTarget(bot);
		if (mob != null)
		{
			bot.setTarget(mob);
			PhantomAI.attackTarget(bot, mob);
			return true;
		}
		
		// Do not spam new wander paths while already moving or mid-swing.
		if (bot.isAttackingNow() || bot.isCastingNow() || (bot.getAI().getIntention() == Intention.MOVE_TO))
		{
			return true;
		}
		
		Location wander = PhantomGeo.getRandomReachableLocation(bot, 400);
		if (wander != null)
		{
			bot.getAI().setIntention(Intention.MOVE_TO, wander);
		}
		return true;
	}
	
	/**
	 * Handles squad chase / death-loot AI when this phantom is assigned to a cursed weapon hunt.
	 * @param bot the phantom
	 * @return {@code true} if the think tick was consumed
	 */
	public static boolean handleHunter(Player bot)
	{
		CursedWeapon cw = findAssignedWeapon(bot);
		if (cw == null)
		{
			return false;
		}
		
		if (cw.isDropped())
		{
			return handleDroppedSword(bot, cw);
		}
		
		if (cw.isActivated())
		{
			return handleChaseWielder(bot, cw);
		}
		
		cleanupWeapon(cw.getItemId());
		return false;
	}
	
	/**
	 * Called when a cursed weapon is activated on a player so hunters pause before attacking a phantom wielder.
	 * @param cw the cursed weapon
	 * @param player the new owner
	 */
	public static void onWeaponActivated(CursedWeapon cw, Player player)
	{
		if ((cw == null) || (player == null))
		{
			return;
		}
		
		noteWeaponActivated(cw.getItemId(), player.getLevel(), isPhantomPlayer(player));
	}
	
	/**
	 * Records activation state for squad selection and the post-pickup pause.
	 * @param itemId cursed weapon item id
	 * @param ownerLevel owner level used as squad reference
	 * @param ownerIsPhantom {@code true} when the owner is an active phantom
	 */
	static void noteWeaponActivated(int itemId, int ownerLevel, boolean ownerIsPhantom)
	{
		DESIGNATED_LOOTER.remove(itemId);
		REFERENCE_LEVEL.put(itemId, ownerLevel);
		SQUAD_CACHE.remove(itemId);
		_activeCheckAt = 0L;
		
		if (ownerIsPhantom)
		{
			PHANTOM_WIELDER_PAUSE_UNTIL.put(itemId, System.currentTimeMillis() + PHANTOM_WIELDER_PAUSE_MS);
		}
		else
		{
			PHANTOM_WIELDER_PAUSE_UNTIL.remove(itemId);
		}
	}
	
	/**
	 * @param itemId cursed weapon item id
	 * @param now current time millis
	 * @param wielderIsPhantom {@code true} when the current wielder is a phantom
	 * @return {@code true} if hunters should hold during the post-pickup pause
	 */
	static boolean holdsForPhantomWielder(int itemId, long now, boolean wielderIsPhantom)
	{
		long pauseUntil = PHANTOM_WIELDER_PAUSE_UNTIL.getOrDefault(itemId, 0L);
		return (pauseUntil > now) && wielderIsPhantom;
	}
	
	/**
	 * @param wielderInPeace {@code true} when the wielder is in a peace zone
	 * @return {@code true} when hunters should fall through to farming
	 */
	static boolean shouldYieldChaseToFarming(boolean wielderInPeace)
	{
		return wielderInPeace;
	}
	
	/**
	 * @param realPlayerAvailable {@code true} when a valid real-player target exists
	 * @return {@code true} when the real player should be preferred over a phantom
	 */
	static boolean preferRealTarget(boolean realPlayerAvailable)
	{
		return realPlayerAvailable;
	}
	
	private static boolean tryEjectWielderFromTown(Player bot)
	{
		if ((bot == null) || (!bot.isInsideZone(ZoneId.PEACE) && !bot.isInsideZone(ZoneId.TOWN)))
		{
			return false;
		}
		
		final int botId = bot.getObjectId();
		final long now = System.currentTimeMillis();
		if (now < WIELDER_EJECT_READY_AT.getOrDefault(botId, 0L))
		{
			return true;
		}
		
		final Location spot = PhantomHuntingSpots.getRandomSpotOutsidePeaceOrTown(bot.getLevel(), WIELDER_EJECT_SPOT_ATTEMPTS);
		if (spot == null)
		{
			return true;
		}
		
		if (PhantomHuntingSpots.travelToLevelSpot(bot, spot, "Cursed wielder leave town"))
		{
			WIELDER_EJECT_READY_AT.put(botId, now + WIELDER_EJECT_COOLDOWN_MS);
		}
		return true;
	}
	
	private static boolean handleDroppedSword(Player bot, CursedWeapon cw)
	{
		Item drop = cw.getItem();
		Location dropLoc = cw.getWorldPosition();
		if ((drop == null) || !drop.isSpawned() || (dropLoc == null))
		{
			return true;
		}
		
		int looterId = resolveLooter(cw);
		if (bot.getObjectId() == looterId)
		{
			return runToPickup(bot, drop);
		}
		
		// Non-looters hold at run-in distance; do not crowd the drop.
		double dist = LocationUtil.calculateDistance(bot.getX(), bot.getY(), bot.getZ(), dropLoc.getX(), dropLoc.getY(), dropLoc.getZ(), true, false);
		if (dist > RETELEPORT_DISTANCE)
		{
			maybeTeleportNear(bot, dropLoc, cw.getItemId());
		}
		return true;
	}
	
	private static boolean handleChaseWielder(Player bot, CursedWeapon cw)
	{
		Player wielder = cw.getPlayer();
		if ((wielder == null) || !wielder.isOnline() || wielder.isDead())
		{
			return true;
		}
		
		REFERENCE_LEVEL.put(cw.getItemId(), wielder.getLevel());
		
		if (shouldYieldChaseToFarming(wielder.isInsideZone(ZoneId.PEACE)))
		{
			return false; // Fall through to farming until they leave peace.
		}
		
		if (holdsForPhantomWielder(cw.getItemId(), System.currentTimeMillis(), isPhantomPlayer(wielder)))
		{
			return true; // Hold position during the post-pickup pause.
		}
		
		double dist = LocationUtil.calculateDistance(bot, wielder, true, false);
		if ((dist <= SCAN_RANGE) && PhantomGeo.canSeeTarget(bot, wielder))
		{
			PhantomAI.clearFarmTargetLock(bot);
			bot.setTarget(wielder);
			PhantomAI.attackTarget(bot, wielder);
			return true;
		}
		
		if (dist > RETELEPORT_DISTANCE)
		{
			maybeTeleportNear(bot, wielder.getLocation(), cw.getItemId());
		}
		else if (!bot.isMoving())
		{
			bot.getAI().setIntention(Intention.MOVE_TO, wielder.getLocation());
		}
		return true;
	}
	
	private static boolean runToPickup(Player bot, Item drop)
	{
		bot.abortAttack();
		bot.abortCast();
		bot.setTarget(null);
		
		double distance = bot.calculateDistance2D(drop);
		if (distance > LOOT_PICKUP_RANGE)
		{
			if (!bot.isMoving())
			{
				bot.getAI().setIntention(Intention.MOVE_TO, drop);
			}
			return true;
		}
		
		bot.doPickupItem(drop);
		return true;
	}
	
	private static int resolveLooter(CursedWeapon cw)
	{
		int itemId = cw.getItemId();
		Integer existing = DESIGNATED_LOOTER.get(itemId);
		if ((existing != null) && isLivingHunterCandidate(World.getInstance().getPlayer(existing)))
		{
			return existing;
		}
		
		List<Player> squad = selectSquad(cw);
		Player killer = World.getInstance().getPlayer(cw.getLastKillerId());
		boolean killerInSquad = (killer != null) && isLivingHunterCandidate(killer) && squadContains(squad, killer);
		
		Location dropLoc = cw.getWorldPosition();
		Map<Integer, Double> distanceByMemberId = new HashMap<>();
		for (Player member : squad)
		{
			if ((dropLoc == null) || (member == null))
			{
				continue;
			}
			double d = LocationUtil.calculateDistance(member.getX(), member.getY(), member.getZ(), dropLoc.getX(), dropLoc.getY(), dropLoc.getZ(), true, false);
			distanceByMemberId.put(member.getObjectId(), d);
		}
		
		int chosen = chooseLooterId(cw.getLastKillerId(), killerInSquad, distanceByMemberId);
		if (chosen != -1)
		{
			DESIGNATED_LOOTER.put(itemId, chosen);
		}
		return chosen;
	}
	
	/**
	 * Picks the designated looter: last killer when in the squad, otherwise the closest member.
	 * @param lastKillerId last killer object id
	 * @param killerInSquad {@code true} when the killer is an eligible squad member
	 * @param distanceByMemberId member object id to distance from the drop
	 * @return looter object id, or {@code -1} when none
	 */
	static int chooseLooterId(int lastKillerId, boolean killerInSquad, Map<Integer, Double> distanceByMemberId)
	{
		if (killerInSquad)
		{
			return lastKillerId;
		}
		
		int closest = -1;
		double best = Double.MAX_VALUE;
		if (distanceByMemberId != null)
		{
			for (Map.Entry<Integer, Double> entry : distanceByMemberId.entrySet())
			{
				if ((entry.getValue() != null) && (entry.getValue() < best))
				{
					best = entry.getValue();
					closest = entry.getKey();
				}
			}
		}
		return closest;
	}
	
	private static void maybeTeleportNear(Player bot, Location center, int weaponItemId)
	{
		long now = System.currentTimeMillis();
		int botId = bot.getObjectId();
		
		TELEPORT_READY_AT.computeIfAbsent(botId, id -> now + Rnd.get(0, FIRST_TELEPORT_STAGGER_MS));
		if (now < TELEPORT_READY_AT.get(botId))
		{
			return;
		}
		
		long last = LAST_TELEPORT_AT.getOrDefault(botId, 0L);
		if ((last > 0) && ((now - last) < RETELEPORT_COOLDOWN_MS))
		{
			return;
		}
		
		Location dest = scatterAround(center);
		if (dest == null)
		{
			return;
		}
		
		PhantomEngine.movePhantomTo(bot, dest, "Cursed sword hunt " + weaponItemId);
		LAST_TELEPORT_AT.put(botId, now);
		TELEPORT_READY_AT.put(botId, now);
	}
	
	private static Location scatterAround(Location center)
	{
		if (center == null)
		{
			return null;
		}
		
		double angle = Math.toRadians(Rnd.get(360));
		int distance = Rnd.get(RUN_IN_MIN, RUN_IN_MAX);
		int x = center.getX() + (int) (Math.cos(angle) * distance);
		int y = center.getY() + (int) (Math.sin(angle) * distance);
		int z = PhantomGeo.getHeight(x, y, center.getZ());
		Location candidate = new Location(x, y, z);
		Location safe = PhantomGeo.getNpcLikeSpawn(candidate);
		return (safe != null) ? safe : candidate;
	}
	
	private static CursedWeapon findAssignedWeapon(Player bot)
	{
		if ((bot == null) || (bot.getLevel() < MIN_HUNTER_LEVEL) || bot.isCursedWeaponEquipped() || !hasActiveWeapon())
		{
			return null;
		}
		
		CursedWeapon assigned = null;
		double bestDistance = Double.MAX_VALUE;
		
		for (CursedWeapon cw : CursedWeaponsManager.getInstance().getCursedWeapons())
		{
			if (!cw.isActive())
			{
				cleanupWeapon(cw.getItemId());
				continue;
			}
			
			if (!isSquadMember(cw, bot))
			{
				continue;
			}
			
			Location pos = cw.getWorldPosition();
			if (pos == null)
			{
				continue;
			}
			
			double dist = LocationUtil.calculateDistance(bot.getX(), bot.getY(), bot.getZ(), pos.getX(), pos.getY(), pos.getZ(), true, false);
			if (dist < bestDistance)
			{
				bestDistance = dist;
				assigned = cw;
			}
		}
		
		return assigned;
	}
	
	private static boolean isSquadMember(CursedWeapon cw, Player bot)
	{
		CachedSquad cached = SQUAD_CACHE.get(cw.getItemId());
		long now = System.currentTimeMillis();
		if ((cached != null) && ((now - cached.builtAt) < SQUAD_CACHE_MS) && isCacheStillValid(cached))
		{
			return cached.memberIds.contains(bot.getObjectId());
		}
		
		List<Player> squad = buildSquad(cw);
		Set<Integer> ids = new HashSet<>(squad.size());
		for (Player member : squad)
		{
			ids.add(member.getObjectId());
		}
		SQUAD_CACHE.put(cw.getItemId(), new CachedSquad(now, ids));
		return ids.contains(bot.getObjectId());
	}
	
	private static boolean isCacheStillValid(CachedSquad cached)
	{
		for (Integer id : cached.memberIds)
		{
			Player member = World.getInstance().getPlayer(id);
			if (!isLivingHunterCandidate(member))
			{
				return false;
			}
		}
		return true;
	}
	
	private static List<Player> selectSquad(CursedWeapon cw)
	{
		CachedSquad cached = SQUAD_CACHE.get(cw.getItemId());
		long now = System.currentTimeMillis();
		if ((cached != null) && ((now - cached.builtAt) < SQUAD_CACHE_MS) && isCacheStillValid(cached))
		{
			List<Player> result = new ArrayList<>(cached.memberIds.size());
			for (Integer id : cached.memberIds)
			{
				Player member = World.getInstance().getPlayer(id);
				if (isLivingHunterCandidate(member))
				{
					result.add(member);
				}
			}
			return result;
		}
		
		List<Player> squad = buildSquad(cw);
		Set<Integer> ids = new HashSet<>(squad.size());
		for (Player member : squad)
		{
			ids.add(member.getObjectId());
		}
		SQUAD_CACHE.put(cw.getItemId(), new CachedSquad(now, ids));
		return squad;
	}
	
	private static List<Player> buildSquad(CursedWeapon cw)
	{
		Player wielder = cw.getPlayer();
		int referenceLevel = REFERENCE_LEVEL.getOrDefault(cw.getItemId(), (wielder != null) ? wielder.getLevel() : 40);
		if (wielder != null)
		{
			referenceLevel = wielder.getLevel();
			REFERENCE_LEVEL.put(cw.getItemId(), referenceLevel);
		}
		
		final int wielderId = (wielder != null) ? wielder.getObjectId() : -1;
		final int limit = Math.max(0, Config.PHANTOM_CURSED_HUNTER_LIMIT);
		
		List<Player> candidates = new ArrayList<>();
		for (Player phantom : PhantomEngine.activePhantoms)
		{
			if (isSquadCandidate(phantom, wielderId))
			{
				candidates.add(phantom);
			}
		}
		
		return rankSquad(candidates, referenceLevel, wielderId, limit);
	}
	
	/**
	 * @param phantom candidate phantom
	 * @param wielderObjectId cursed weapon wielder object id, or {@code -1}
	 * @return {@code true} when the phantom may join a hunt squad
	 */
	static boolean isSquadCandidate(Player phantom, int wielderObjectId)
	{
		if (!isLivingHunterCandidate(phantom))
		{
			return false;
		}
		if (phantom.getObjectId() == wielderObjectId)
		{
			return false;
		}
		return !phantom.isCursedWeaponEquipped();
	}
	
	/**
	 * Sorts candidates by level distance to the reference, then object id, and caps at {@code limit}.
	 * @param candidates living hunter candidates
	 * @param referenceLevel target level for ranking
	 * @param wielderObjectId excluded wielder object id
	 * @param limit maximum squad size
	 * @return ranked and capped squad
	 */
	static List<Player> rankSquad(List<Player> candidates, int referenceLevel, int wielderObjectId, int limit)
	{
		List<Player> working = new ArrayList<>();
		if (candidates != null)
		{
			for (Player p : candidates)
			{
				if ((p != null) && (p.getObjectId() != wielderObjectId))
				{
					working.add(p);
				}
			}
		}
		
		working.sort(Comparator.comparingInt((Player p) -> Math.abs(p.getLevel() - referenceLevel)).thenComparingInt(Player::getObjectId));
		
		final int cappedLimit = Math.max(0, limit);
		if (working.size() > cappedLimit)
		{
			return new ArrayList<>(working.subList(0, cappedLimit));
		}
		return working;
	}
	
	private static boolean squadContains(List<Player> squad, Player bot)
	{
		if ((squad == null) || (bot == null))
		{
			return false;
		}
		for (Player member : squad)
		{
			if ((member != null) && (member.getObjectId() == bot.getObjectId()))
			{
				return true;
			}
		}
		return false;
	}
	
	static boolean isLivingHunterCandidate(Player p)
	{
		return (p != null) && p.isOnline() && !p.isDead() && (p.getLevel() >= MIN_HUNTER_LEVEL) && PhantomEngine.activePhantoms.contains(p);
	}
	
	private static boolean isPhantomPlayer(Player p)
	{
		return (p != null) && PhantomEngine.activePhantoms.contains(p);
	}
	
	/**
	 * Nearest attackable monster for a cursed wielder when no player target exists.
	 * Prefers mobs already attacking the wielder (or summon), otherwise the nearest visible mob.
	 */
	private static Monster findWielderMobTarget(Player bot)
	{
		Monster closestAggro = null;
		double bestAggroDist = Double.MAX_VALUE;
		Monster closestAny = null;
		double bestAnyDist = Double.MAX_VALUE;
		
		for (Monster m : World.getInstance().getVisibleObjectsInRange(bot, Monster.class, SCAN_RANGE))
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
			
			if (!PhantomGeo.canSeeTarget(bot, m))
			{
				continue;
			}
			
			double dist = LocationUtil.calculateDistance(bot, m, true, false);
			boolean isAttackingUs = (m.getTarget() == bot) || ((bot.getSummon() != null) && (m.getTarget() == bot.getSummon()));
			if (isAttackingUs)
			{
				if (dist < bestAggroDist)
				{
					bestAggroDist = dist;
					closestAggro = m;
				}
			}
			else if (dist < bestAnyDist)
			{
				bestAnyDist = dist;
				closestAny = m;
			}
		}
		
		return (closestAggro != null) ? closestAggro : closestAny;
	}
	
	private static Player findWielderTarget(Player bot)
	{
		Player bestReal = null;
		double bestRealDist = Double.MAX_VALUE;
		Player bestPhantom = null;
		double bestPhantomDist = Double.MAX_VALUE;
		
		for (Player p : World.getInstance().getVisibleObjectsInRange(bot, Player.class, SCAN_RANGE))
		{
			if (!isValidWielderTarget(bot, p))
			{
				continue;
			}
			
			double dist = LocationUtil.calculateDistance(bot, p, true, false);
			if (p.getClient() != null)
			{
				if (dist < bestRealDist)
				{
					bestRealDist = dist;
					bestReal = p;
				}
			}
			else if (dist < bestPhantomDist)
			{
				bestPhantomDist = dist;
				bestPhantom = p;
			}
		}
		
		return preferRealTarget(bestReal != null) ? bestReal : bestPhantom;
	}
	
	private static boolean isValidWielderTarget(Player bot, Player target)
	{
		if ((target == null) || (target == bot))
		{
			return false;
		}
		
		boolean invalidIdentity = target.isDead() || target.isGM() || target.isInvisible();
		boolean levelTooLow = target.getLevel() <= 20;
		boolean inRestrictedZone = target.isInsideZone(ZoneId.PEACE) || bot.isInsideZone(ZoneId.PEACE) || target.isInsideZone(ZoneId.NO_PVP) || bot.isInsideZone(ZoneId.NO_PVP);
		boolean sameClan = (bot.getClan() != null) && (bot.getClan() == target.getClan());
		boolean canSee = PhantomGeo.canSeeTarget(bot, target);
		boolean blockedByTargetNewbieBlessing = target.isProtectionBlessingAffected() && ((bot.getLevel() - target.getLevel()) >= 10) && (bot.getKarma() > 0) && !target.isInsideZone(ZoneId.PVP);
		boolean blockedByBotNewbieBlessing = bot.isProtectionBlessingAffected() && ((target.getLevel() - bot.getLevel()) >= 10) && (target.getKarma() > 0) && !bot.isInsideZone(ZoneId.PVP);
		
		return passesWielderTargetRules(invalidIdentity, levelTooLow, inRestrictedZone, sameClan, canSee, blockedByTargetNewbieBlessing, blockedByBotNewbieBlessing);
	}
	
	/**
	 * Pure target rules for a cursed wielder choosing a player to attack.
	 * @param invalidIdentity dead, GM, or invisible
	 * @param levelTooLow target level 20 or below
	 * @param inRestrictedZone peace or no-pvp for either side
	 * @param sameClan same clan as the wielder
	 * @param canSee line of sight
	 * @param blockedByTargetNewbieBlessing newbie blessing on the target blocks the attack
	 * @param blockedByBotNewbieBlessing newbie blessing on the bot blocks the attack
	 * @return {@code true} when the target is valid
	 */
	static boolean passesWielderTargetRules(boolean invalidIdentity, boolean levelTooLow, boolean inRestrictedZone, boolean sameClan, boolean canSee, boolean blockedByTargetNewbieBlessing, boolean blockedByBotNewbieBlessing)
	{
		if (invalidIdentity || levelTooLow || inRestrictedZone || sameClan || !canSee)
		{
			return false;
		}
		if (blockedByTargetNewbieBlessing || blockedByBotNewbieBlessing)
		{
			return false;
		}
		return true;
	}
	
	private static void cleanupWeapon(int itemId)
	{
		PHANTOM_WIELDER_PAUSE_UNTIL.remove(itemId);
		DESIGNATED_LOOTER.remove(itemId);
		REFERENCE_LEVEL.remove(itemId);
		SQUAD_CACHE.remove(itemId);
		_activeCheckAt = 0L;
	}
}
