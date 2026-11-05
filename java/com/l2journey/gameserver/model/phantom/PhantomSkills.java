package com.l2journey.gameserver.model.phantom;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.data.xml.SkillData;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.Summon;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.enums.ShotType;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.targets.TargetType;
import com.l2journey.gameserver.util.LocationUtil;

public class PhantomSkills
{
	private static final int SPIRIT_ORE = 3031;
	private static final int PET_SOULSHOT = 20332;
	private static final int[] CRYSTALS =
	{
		1458,
		1459,
		1460,
		1461,
		1462
	};
	private static final int INQUISITOR_BISHOP = 1520;
	private static final int INQUISITOR_ELDER = 1521;
	private static final int INQUISITOR_SHILLIEN = 1522;
	private static final int DIVINE_PUNISHMENT = 1523;
	private static final int SURRENDER_TO_DIVINE = 1524;
	private static final int SWITCH_STANCE = 838;
	private static final List<Integer> CUBIC_SKILL_IDS = Arrays.asList(22, 278, 33, 10, 67, 449, 1280, 1281, 1279);
	private static final List<Integer> BANNED_ATTACK_SKILLS = Arrays.asList(162, 228, 1400, 1181, 1147, 314, 315, 316, 328, 329, 330, SWITCH_STANCE, 1528);
	private static final Map<String, Long> CAST_TIMERS = new ConcurrentHashMap<>();
	private static final Map<Integer, Integer> FIGHTER_ATTACK_PROFILES = new ConcurrentHashMap<>();
	private static final Map<Integer, MageAttackProfile> MAGE_ATTACK_PROFILES = new ConcurrentHashMap<>();
	private static final List<Integer> FORBIDDEN_FALLBACK = Arrays.asList(1151, 1159, 1288, 1266, 1267, 1417);

	static
	{
		// Linha de magos humanos
		registerMageProfile(11, 1220, 0); // Human Wizard: Blaze
		registerMageProfile(12, 1230, 0); // Sorcerer: Prominence + Surrender to Fire
		registerMageProfile(94, 1230, 0); // Archmage: Prominence + Surrender to Fire (3a classe)
		registerMageProfile(13, 1148, 0); // Necromancer: Death Spike
		registerMageProfile(95, 1148, 0); // Soultaker: Death Spike (3a classe)
		registerMageProfile(14, 1220, 0); // Warlock: Blaze
		registerMageProfile(96, 1220, 0); // Arcana Lord: Blaze (3a classe)

		// Linha de magos elfos
		registerMageProfile(26, 1175, 0); // Elven Wizard: Aqua Swirl
		registerMageProfile(27, 1235, 0); // Spellsinger: Hydro Blast + Surrender to Water
		registerMageProfile(103, 1235, 0); // Mystic Muse: Hydro Blast + Surrender to Water (3a classe)
		registerMageProfile(28, 1114, 0); // Elemental Summoner: Aqua Swirl
		registerMageProfile(104, 1114, 0); // Elemental Master: Aqua Swirl (3a classe)

		// Linha de magos sombrios
		registerMageProfile(39, 1178, 0); // Dark Wizard: Twister
		registerMageProfile(40, 1239, 0); // Spellhowler: Hurricane + Surrender to Wind
		registerMageProfile(110, 1239, 0); // Storm Screamer: Hurricane + Surrender to Wind (3a classe)
		registerMageProfile(41, 1530, 0); // Phantom Summoner: Death Spike
		registerMageProfile(111, 1530, 0); // Spectral Master: Death Spike (3a classe)

		// --- Skills para Guerreiros Lvl 40-85 ---
		registerFighterProfile(3, 920); // Warlord
		registerFighterProfile(89, 920); // Dreadnought
		registerFighterProfile(33, 223); // shilen knight
		registerFighterProfile(106, 223); // shilen templar
		registerFighterProfile(23, 30); // Plains Walker
		registerFighterProfile(22, 16); // Elven Scout
		registerFighterProfile(24, 101); // silver ranger
		registerFighterProfile(101, 30); // wind rider
		registerFighterProfile(34, 986); // Blade Dancer
		registerFighterProfile(107, 986); // spectral Dancer
		registerFighterProfile(57, 260); // Warsmith
		registerFighterProfile(118, 260); // maestro
		registerFighterProfile(55, 263); // Bounty Hunter
		registerFighterProfile(117, 263); // Fortune Seeker
		registerFighterProfile(5, 92); // Paladin
		registerFighterProfile(90, 400); // phoenix knight
		registerFighterProfile(6, 92); // Dark Avenger
		registerFighterProfile(91, 401); // hell knight
		registerFighterProfile(8, 263); // Treasure Hunter
		registerFighterProfile(93, 263); // adventur
		registerFighterProfile(9, 101); // Hawkeye
		registerFighterProfile(92, 56); // sagitarius
		registerFighterProfile(20, 984); // Temple Knight
		registerFighterProfile(99, 400); // evas templar
		registerFighterProfile(21, 986); // SwordSinger
		registerFighterProfile(100, 986); // Sword mouse
		registerFighterProfile(37, 56); // Phantom Ranger
		registerFighterProfile(109, 56); // ghost sentinel
		registerFighterProfile(46, 190); // Destroyer
		registerFighterProfile(113, 315); // titan
		registerFighterProfile(48, 280); // Tyrant
		registerFighterProfile(114, 280); // grand khavatari
		registerFighterProfile(127, 468); // Berserker
		registerFighterProfile(131, 468); // doombrenger
		registerFighterProfile(128, 478); // Male Soul Breaker
		registerFighterProfile(132, 478); // male soul hound
		registerFighterProfile(129, 478); // Female Soul Breaker
		registerFighterProfile(133, 478); // Female soul hound
		registerFighterProfile(130, 507); // Arbalester
		registerFighterProfile(134, 507); // trickster
		registerFighterProfile(36, 263); // abyss walker
		registerFighterProfile(108, 263); // ghost hunter
		registerFighterProfile(2, 1); // gladiator
		registerFighterProfile(88, 1); // duelist

		// Skills para Guerreiros Lvl 10
		registerFighterProfile(0, 3); // Human Fighter
		registerFighterProfile(1, 3); // warrior
		registerFighterProfile(4, 3); // human knight
		registerFighterProfile(7, 16); // rogue
		registerFighterProfile(32, 3); // palus knight
		registerFighterProfile(35, 16); // assassin
		registerFighterProfile(56, 100); // artisan
		registerFighterProfile(45, 29); // orc raider
		registerFighterProfile(19, 3); // elven knight
		registerFighterProfile(44, 3); // Orc Fighter
		registerFighterProfile(125, 468); // Trooper
		registerFighterProfile(18, 3); // elven Fighter
		registerFighterProfile(31, 3); // Dark Fighter
		registerFighterProfile(47, 120); // Orc Monk: Stunning Fist
		registerFighterProfile(126, 487); // Warder: Penetrating Shot
		registerFighterProfile(135, 504); // Inspector: Triple Thrust
		// registerFighterProfile(54, 100); // scavenger

	}
	private PhantomSkills()
	{
	}

	/**
	 * Preferred summon skill ids for the bot's class, highest priority first.
	 * @param bot the phantom
	 * @return ordered summon skill ids, or an empty list
	 */
	static List<Integer> getSummonPriority(Player bot)
	{
		if (bot == null)
		{
			return List.of();
		}

		return switch (bot.getActiveClass())
		{
			case 14 -> List.of(1276); // Warlock: Kai the Cat
			case 96 -> List.of(1406, 1276); // Arcana Lord: Feline King, Kai
			case 28 -> List.of(1277); // Elemental Summoner: Merrow
			case 104 -> List.of(1407, 1277); // Elemental Master: Magnus, Merrow
			case 13 -> List.of(1129); // Necromancer: Reanimated Man
			case 95 -> List.of(1334); // Soultaker: Cursed Man
			case 41 -> List.of(1278); // Phantom Summoner: Soulless
			case 111 -> List.of(1408, 1278); // Spectral Master: Spectral Lord, Soulless
			case 6, 91 -> List.of(283); // Dark Avenger / Hell Knight: Dark Panther
			case 90 -> List.of(912); // Phoenix Knight: Imperial Phoenix
			case 56, 57, 118 -> List.of(25); // Artisan / Warsmith / Maestro: Mechanic Golem
			default -> List.of();
		};
	}

	static Skill getPreferredSummonSkill(Player bot)
	{
		if (bot == null)
		{
			return null;
		}
		for (int summonId : getSummonPriority(bot))
		{
			Skill skill = bot.getKnownSkill(summonId);
			if (skill != null)
			{
				return skill;
			}
		}
		return null;
	}

	/**
	 * @param bot the phantom
	 * @return the Inquisitor skill for Bishop / Elder / Shillien Elder lines, or {@code null}
	 */
	static Skill getInquisitorSkill(Player bot)
	{
		if (bot == null)
		{
			return null;
		}
		Skill skill = bot.getKnownSkill(INQUISITOR_BISHOP);
		if (skill == null)
		{
			skill = bot.getKnownSkill(INQUISITOR_ELDER);
		}
		if (skill == null)
		{
			skill = bot.getKnownSkill(INQUISITOR_SHILLIEN);
		}
		return skill;
	}

	/**
	 * Casts Inquisitor when the bot knows it and is not already in stance.
	 * @param bot the phantom
	 * @return {@code true} when this tick was consumed by the transform cast
	 */
	public static boolean handleInquisitorForm(Player bot)
	{
		if ((bot == null) || bot.isDead() || bot.isCastingNow() || bot.isInStance())
		{
			return false;
		}

		Skill inquisitor = getInquisitorSkill(bot);
		if ((inquisitor == null) || bot.isSkillDisabled(inquisitor) || (bot.getCurrentMp() < inquisitor.getMpConsume()))
		{
			return false;
		}

		bot.stopMove(null);
		bot.getAI().setIntention(Intention.CAST, inquisitor, bot);
		return true;
	}

	/**
	 * While in Inquisitor stance, casts Surrender to the Divine then Divine Punishment.
	 * The cast is issued even when the target is still out of range, so the combat AI closes the same way a Spellsinger or Spellhowler does.
	 * @param bot the phantom
	 * @param target the combat target
	 * @return {@code true} when this tick stays on the Inquisitor cast. {@code false} when the stance spells are unavailable and the normal mage attack should run
	 */
	public static boolean tryInquisitorAttack(Player bot, Creature target)
	{
		if ((bot == null) || !bot.isInStance())
		{
			return false;
		}

		if ((target == null) || target.isDead() || bot.isCastingNow())
		{
			return true;
		}

		Skill attack = selectInquisitorAttackSkill(bot, target);
		if (attack == null)
		{
			return false;
		}

		if (bot.getAI().getIntention() != Intention.CAST)
		{
			bot.setChargedShot(ShotType.BLESSED_SPIRITSHOTS, true);
			bot.setTarget(target);
			bot.getAI().setIntention(Intention.CAST, attack, target);
		}
		return true;
	}

	/**
	 * @param bot the phantom in Inquisitor stance
	 * @param target the combat target
	 * @return Surrender to the Divine when the target does not have it, otherwise Divine Punishment, or {@code null} when neither is ready
	 */
	static Skill selectInquisitorAttackSkill(Player bot, Creature target)
	{
		if ((bot == null) || (target == null))
		{
			return null;
		}

		Skill surrender = bot.getTransformSkill(SURRENDER_TO_DIVINE);
		if (isInquisitorSkillReady(bot, surrender) && !target.isAffectedBySkill(SURRENDER_TO_DIVINE))
		{
			return surrender;
		}

		Skill punishment = bot.getTransformSkill(DIVINE_PUNISHMENT);
		if (isInquisitorSkillReady(bot, punishment))
		{
			return punishment;
		}

		return null;
	}

	private static boolean isInquisitorSkillReady(Player bot, Skill skill)
	{
		return (skill != null) && !bot.isSkillDisabled(skill) && (bot.getCurrentMp() >= skill.getMpConsume());
	}

	private static void registerMageProfile(int classId, int mainNukeId, int debuffId)
	{
		MAGE_ATTACK_PROFILES.put(classId, new MageAttackProfile(mainNukeId, debuffId));
	}

	// --- Metodo para registrar o perfil de Guerreiro ---
	private static void registerFighterProfile(int classId, int skillId)
	{
		FIGHTER_ATTACK_PROFILES.put(classId, skillId);
	}

	private static final class MageAttackProfile
	{
		private final int mainNukeId;
		private final int debuffId;

		private MageAttackProfile(int mainNukeId, int debuffId)
		{
			this.mainNukeId = mainNukeId;
			this.debuffId = debuffId;
		}
	}

	public static Skill getMageAttackSkill(Player bot)
	{
		return getMageAttackSkill(bot, null);
	}

	public static Skill getMageAttackSkill(Player bot, Creature target)
	{
		if ((bot == null) || !bot.isMageClass())
		{
			return null;
		}

		if (bot.getLevel() < 20)
		{
			return null;
		}

		int classId = getPlayerClassId(bot);
		MageAttackProfile profile = MAGE_ATTACK_PROFILES.get(classId);
		if (profile == null)
		{
			return null;
		}

		Skill debuff = bot.getKnownSkill(profile.debuffId);
		Skill mainNuke = bot.getKnownSkill(profile.mainNukeId);

		if ((debuff != null) && (target != null) && !bot.isSkillDisabled(debuff) && (bot.getCurrentMp() >= debuff.getMpConsume()))
		{
			if ((!target.isAffectedBySkill(debuff.getId())) && ((debuff.getCastRange() > 0) && (LocationUtil.calculateDistance(bot, target, true, false) <= debuff.getCastRange())))
			{
				return debuff;
			}
		}

		if ((mainNuke != null) && !bot.isSkillDisabled(mainNuke) && (bot.getCurrentMp() >= mainNuke.getMpConsume()))
		{
			if (((target != null) && (mainNuke.getCastRange() > 0)) && (LocationUtil.calculateDistance(bot, target, true, false) > mainNuke.getCastRange()))
			{
				return null;
			}
			return mainNuke;
		}

		return null;
	}

	// Metodo para obter a skill de ataque do guerreiro ---
	public static Skill getFighterAttackSkill(Player bot)
	{
		// Se e mago ou nao existe, ignoramos
		if ((bot == null) || bot.isMageClass())
		{
			return null;
		}

		int classId = getPlayerClassId(bot);
		Integer skillId = FIGHTER_ATTACK_PROFILES.get(classId);

		// Se nao tem skill registrada para sua classe
		if (skillId == null)
		{
			return null;
		}

		Skill attackSkill = bot.getKnownSkill(skillId);

		// Nunca usamos skills banidas como skill de ataque automatico
		if (BANNED_ATTACK_SKILLS.contains(skillId))
		{
			return null;
		}

		// Verificamos se tem a skill, se nao esta em cooldown e tem MP suficiente
		if ((attackSkill != null) && !bot.isSkillDisabled(attackSkill) && (bot.getCurrentMp() >= attackSkill.getMpConsume()))
		{
			return attackSkill;
		}

		return null;
	}

	private static int getPlayerClassId(Player bot)
	{
		try
		{
			return bot.getPlayerClass().getId();
		}
		catch (Exception _)
		{
			return -1;
		}
	}

	public static boolean handleCombatSummons(Player bot, Creature target)
	{
		if ((bot == null) || bot.isCastingNow())
		{
			return true;
		}

		if (bot.isAffectedBySkill(5076))
		{
			bot.getEffectList().stopSkillEffects(com.l2journey.gameserver.model.skill.enums.SkillFinishType.REMOVED, 5076);
		}

		if (bot.getInventory().getInventoryItemCount(SPIRIT_ORE, -1) < 100)
		{
			bot.getInventory().addItem(ItemProcessType.NONE, SPIRIT_ORE, 500, bot, null);
		}
		if (bot.getInventory().getInventoryItemCount(PET_SOULSHOT, -1) < 100)
		{
			bot.getInventory().addItem(ItemProcessType.NONE, PET_SOULSHOT, 500, bot, null);
		}

		for (int cryId : CRYSTALS)
		{
			if (bot.getInventory().getInventoryItemCount(cryId, -1) < 10)
			{
				bot.getInventory().addItem(ItemProcessType.NONE, cryId, 100, bot, null);
			}
		}

		Summon summon = bot.getSummon();
		boolean hasActiveSummon = (summon != null) && !summon.isDead();

		if (!hasActiveSummon)
		{
			Skill summonSkill = getPreferredSummonSkill(bot);
			if ((summonSkill != null) && !bot.isSkillDisabled(summonSkill) && (bot.getCurrentMp() >= summonSkill.getMpConsume()))
			{
				String key = bot.getObjectId() + "_summon";
				long lastCast = CAST_TIMERS.getOrDefault(key, 0L);

				if ((System.currentTimeMillis() - lastCast) > 10000)
				{
					CAST_TIMERS.put(key, System.currentTimeMillis());
					bot.stopMove(null);
					bot.getAI().setIntention(Intention.CAST, summonSkill, bot);
					return true;
				}
			}
		}

		int knownCubics = 0;
		for (int cubicId : CUBIC_SKILL_IDS)
		{
			if (bot.getKnownSkill(cubicId) != null)
			{
				knownCubics++;
			}
		}

		int maxCubics = Math.min(knownCubics, 3);
		int currentCubics = (bot.getCubics() == null) ? 0 : bot.getCubics().size();

		if (currentCubics == 0)
		{
			for (int cubicId : CUBIC_SKILL_IDS)
			{
				CAST_TIMERS.remove(bot.getObjectId() + "_cubic_" + cubicId);
			}
		}

		if (currentCubics < maxCubics)
		{
			for (int cubicId : CUBIC_SKILL_IDS)
			{
				Skill cubicSkill = bot.getKnownSkill(cubicId);
				if (((cubicSkill != null) && !bot.isSkillDisabled(cubicSkill)) && (bot.getCurrentMp() >= cubicSkill.getMpConsume()))
				{
					String key = bot.getObjectId() + "_cubic_" + cubicId;
					long lastCast = CAST_TIMERS.getOrDefault(key, 0L);

					if ((System.currentTimeMillis() - lastCast) > 900000)
					{
						CAST_TIMERS.put(key, System.currentTimeMillis());
						bot.stopMove(null);
						bot.getAI().setIntention(Intention.CAST, cubicSkill, bot);
						return true;
					}
				}
			}
		}

		if (hasActiveSummon && (summon != null))
		{
			if (LocationUtil.calculateDistance(bot, summon, false, false) > 2500)
			{
				summon.teleToLocation(bot.getX(), bot.getY(), bot.getZ());
			}
			else
			{
				givePhantomPetBuffs(summon);

				if (!bot.getAutoSoulShot().contains(PET_SOULSHOT))
				{
					bot.addAutoSoulShot(PET_SOULSHOT);
				}

				if ((target != null) && !target.isDead())
				{
					String attackKey = bot.getObjectId() + "_summon_attack";
					long now = System.currentTimeMillis();
					boolean targetChanged = summon.getTarget() != target;
					boolean immobilized = summon.isMovementDisabled();
					boolean intentionLost = summon.getAI().getIntention() != Intention.ATTACK;
					boolean inactive = !summon.isMoving() && !summon.isAttackingNow() && !summon.isCastingNow();
					long lastOrder = CAST_TIMERS.getOrDefault(attackKey, 0L);
					boolean retryAttack = ((now - lastOrder) >= 3000) && (intentionLost || inactive);

					if (!immobilized && (targetChanged || retryAttack))
					{
						CAST_TIMERS.put(attackKey, now);
						summon.setTarget(target);
						summon.getAI().setIntention(Intention.ATTACK, target);
					}
				}
			}
		}

		return false;
	}

	public static Skill getBestOffensiveSkill(Player bot)
	{
		if (bot == null)
		{
			return null;
		}

		return bot.getAllSkills().stream()
			.filter(s -> s != null)
			.filter(s -> !s.isPassive() && s.isMagic() && (s.getPower() > 0))
			.filter(s -> s.getTargetType() != TargetType.GROUND)
			.filter(s -> !FORBIDDEN_FALLBACK.contains(s.getId()))
			.filter(s -> s.getCastRange() >= 400)
			.filter(s -> !bot.isSkillDisabled(s) && (bot.getCurrentMp() >= s.getMpConsume()))
			.sorted((s1, s2) -> Double.compare(s2.getPower(), s1.getPower()))
			.findFirst()
			.orElse(null);
	}

	public static void applyBasicBuffs(Player bot)
	{
		if (bot.getEffectList().getBuffCount() < 5)
		{
			boolean isMage = bot.isMageClass();

			int[] mageBuffs =
			{
				1085,
				1059,
				1303,
				1078,
				1389,
				1062,
				1045,
				1048,
				1204,
				1036,
				1040,
				1413,
				273,
				276,
				365,
				264,
				267,
				304
			};
			int[] fighterBuffs =
			{
				1086,
				1068,
				1077,
				1542,
				1045,
				1048,
				1388,
				1242,
				1240,
				1268,
				1204,
				1040,
				1036,
				1062,
				1413,
				271,
				274,
				275,
				269,
				264,
				267,
				304,
				310
			};

			int[] buffsToApply = isMage ? mageBuffs : fighterBuffs;

			for (int id : buffsToApply)
			{
				int maxLevel = SkillData.getInstance().getMaxLevel(id);
				Skill buff = SkillData.getInstance().getSkill(id, maxLevel);

				if (buff != null)
				{
					buff.applyEffects(bot, bot);
				}
			}
		}
	}

	public static void givePhantomPetBuffs(Summon pet)
	{
		if ((pet == null) || pet.isDead())
		{
			return;
		}

		if (pet.getEffectList().getBuffCount() >= 5)
		{
			return;
		}

		int[][] buffs =
		{
			{
				1086,
				2
			},
			{
				1204,
				2
			},
			{
				1068,
				3
			},
			{
				1040,
				1
			},
			{
				1077,
				3
			},
			{
				1242,
				3
			},
			{
				310,
				1
			},
			{
				1062,
				1
			},
			{
				1036,
				1
			},
			{
				915,
				1
			},
			{
				1268,
				4
			},
			{
				1240,
				1
			},
			{
				271,
				1
			},
			{
				274,
				1
			},
			{
				275,
				1
			},
			{
				269,
				1
			},
			{
				264,
				1
			},
			{
				267,
				1
			},
			{
				277,
				1
			},
			{
				1363,
				1
			},
			{
				1078,
				1
			}
		};

		for (int[] buffInfo : buffs)
		{
			Skill buffSkill = SkillData.getInstance().getSkill(buffInfo[0], buffInfo[1]);
			if (buffSkill != null)
			{
				buffSkill.applyEffects(pet, pet);
			}
		}
	}

	/**
	 * Resolves the class maintenance buff (Rapid Shot, Furious Soul, or Combat Aura).
	 * Tanks are checked after bows so Eva's Templar (99) gets Combat Aura.
	 * @param classId active class id
	 * @return skill id, or {@code 0} when the class has no maintenance buff
	 */
	static int resolveMaintenanceSkillId(int classId)
	{
		switch (classId)
		{
			case 9, 24, 37, 92, 102, 109:
				return 99; // Rapid Shot
			case 126, 130, 134:
				return 482; // Furious Soul
			case 5, 6, 19, 33, 90, 91, 99, 106:
				return 982; // Combat Aura
			default:
				return 0;
		}
	}

	public static boolean handleSelfBuffs(Player bot)
	{
		if ((bot == null) || bot.isDead() || bot.isCastingNow())
		{
			return false;
		}

		int skillId = resolveMaintenanceSkillId(getPlayerClassId(bot));
		if (skillId <= 0)
		{
			return false;
		}

		Skill skill = bot.getKnownSkill(skillId);
		if ((skill == null) || bot.isAffectedBySkill(skillId) || bot.isSkillDisabled(skill) || (bot.getCurrentMp() < skill.getMpConsume()))
		{
			return false;
		}

		bot.stopMove(null);
		bot.getAI().setIntention(Intention.CAST, skill, bot);
		return true;
	}

	public static void unregisterBot(int objectId)
	{
		CAST_TIMERS.entrySet().removeIf(entry -> entry.getKey().startsWith(objectId + "_"));
	}
}
