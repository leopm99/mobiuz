package com.l2journey.gameserver.model.phantom;

import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Monster;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.util.LocationUtil;

/**
 * Spoil and sweep helpers for Scavenger, Bounty Hunter, and Fortune Seeker phantoms.
 */
public final class PhantomSpoil
{
	private static final int SPOIL_SKILL_ID = 254;
	private static final int SWEEP_SKILL_ID = 42;
	private static final double SWEEP_APPROACH_DISTANCE = 150;

	private PhantomSpoil()
	{
	}

	/**
	 * @param bot the phantom
	 * @return {@code true} when the bot is a spoiler class
	 */
	public static boolean isSpoiler(Player bot)
	{
		if (bot == null)
		{
			return false;
		}
		int classId = bot.getActiveClass();
		return (classId == 54) || (classId == 55) || (classId == 117);
	}

	/**
	 * Casts Spoil on a live monster when the skill is ready. Retries are allowed until {@link Monster#isSpoiled()}.
	 * @param bot the phantom
	 * @param target the combat target
	 * @return {@code true} when this tick was consumed by a spoil cast
	 */
	public static boolean trySpoil(Player bot, Creature target)
	{
		if (!isSpoiler(bot) || (target == null) || !target.isMonster() || target.isDead())
		{
			return false;
		}

		Monster mob = (Monster) target;
		if (mob.isSpoiled())
		{
			return false;
		}

		// Spoil takes longer than a think tick. Hold the tick while it is casting or on reuse so the next swing does not cancel it.
		if (bot.isCastingNow())
		{
			return true;
		}

		Skill spoilSkill = bot.getKnownSkill(SPOIL_SKILL_ID);
		if (spoilSkill == null)
		{
			return false;
		}
		if (bot.isSkillDisabled(spoilSkill))
		{
			return true;
		}
		if (bot.getCurrentMp() < spoilSkill.getMpConsume())
		{
			return false;
		}

		if (bot.getAI().getIntention() != Intention.CAST)
		{
			bot.setTarget(mob);
			bot.getAI().setIntention(Intention.CAST, spoilSkill, mob);
		}
		return true;
	}

	/**
	 * Walks to a spoiled corpse and casts Sweep when close enough.
	 * @param bot the phantom
	 * @param mob the spoiled corpse
	 * @return {@code true} when this tick was consumed by approach or sweep
	 */
	public static boolean trySweep(Player bot, Monster mob)
	{
		if (!isSpoiler(bot) || (mob == null) || !mob.isDead() || !mob.isSpawned())
		{
			return false;
		}

		if (!mob.isSpoiled() && !mob.isSweepActive())
		{
			return false;
		}

		if (!mob.checkSpoilOwner(bot, false))
		{
			return false;
		}

		Skill sweepSkill = bot.getKnownSkill(SWEEP_SKILL_ID);
		if ((sweepSkill == null) || bot.isSkillDisabled(sweepSkill))
		{
			return false;
		}

		double distance = LocationUtil.calculateDistance(bot, mob, true, false);
		if (distance > SWEEP_APPROACH_DISTANCE)
		{
			bot.setTarget(mob);
			bot.getAI().setIntention(Intention.MOVE_TO, mob.getLocation());
			return true;
		}

		if (bot.getAI().getIntention() != Intention.CAST)
		{
			if (bot.isMoving())
			{
				bot.stopMove(null);
			}
			bot.setTarget(mob);
			bot.getAI().setIntention(Intention.CAST, sweepSkill, mob);
		}
		return true;
	}

	/**
	 * @param bot the phantom
	 * @param mob the locked corpse
	 * @return {@code true} when the corpse is still this bot's to sweep, but Sweeper is casting or on reuse
	 */
	public static boolean isSweepOnCooldown(Player bot, Monster mob)
	{
		if (!shouldSweepCorpse(bot, mob))
		{
			return false;
		}
		if (bot.isCastingNow())
		{
			return true;
		}
		Skill sweepSkill = bot.getKnownSkill(SWEEP_SKILL_ID);
		return (sweepSkill != null) && bot.isSkillDisabled(sweepSkill);
	}

	/**
	 * @param bot the phantom
	 * @param mob the locked farm target
	 * @return {@code true} when the corpse should be swept before other loot
	 */
	public static boolean shouldSweepCorpse(Player bot, Monster mob)
	{
		return isSpoiler(bot)
			&& (mob != null)
			&& mob.isDead()
			&& mob.isSpawned()
			&& (mob.isSpoiled() || mob.isSweepActive())
			&& mob.checkSpoilOwner(bot, false);
	}
}
