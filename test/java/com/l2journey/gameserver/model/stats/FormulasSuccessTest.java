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
 */
package com.l2journey.gameserver.model.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.commons.util.Rnd;
import com.l2journey.gameserver.model.EffectList;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Cubic;
import com.l2journey.gameserver.model.actor.instance.SiegeFlag;
import com.l2journey.gameserver.model.actor.instance.StaticObject;
import com.l2journey.gameserver.model.actor.stat.CreatureStat;
import com.l2journey.gameserver.model.actor.stat.PlayerStat;
import com.l2journey.gameserver.model.item.Armor;
import com.l2journey.gameserver.model.item.Weapon;
import com.l2journey.gameserver.model.item.enums.ShotType;
import com.l2journey.gameserver.model.item.type.ArmorType;
import com.l2journey.gameserver.model.item.type.WeaponType;
import com.l2journey.gameserver.model.skill.BuffInfo;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * Characterization of hit, shield, success, and cancel formulas in {@link Formulas}.
 */
class FormulasSuccessTest
{
	private static final int EQUAL_LEVEL = 80;
	private static final int BASE_STAT = 40;
	
	private static File previousDatapackRoot;
	private static boolean previousCancelCast;
	private static boolean previousCancelBow;
	private static int previousPerfectShield;
	private static boolean previousFallingDamage;
	private static boolean previousMagicSuccessBySkillLevel;
	private static int previousMinNpcLevelMagicPenalty;
	private static float[] previousNpcSkillChancePenalty;
	
	static
	{
		previousDatapackRoot = Config.DATAPACK_ROOT;
		Config.DATAPACK_ROOT = new File("dist/game");
	}
	
	@BeforeAll
	static void setUpConfig()
	{
		Config.DATAPACK_ROOT = new File("dist/game");
		previousCancelCast = Config.ALT_GAME_CANCEL_CAST;
		previousCancelBow = Config.ALT_GAME_CANCEL_BOW;
		previousPerfectShield = Config.ALT_PERFECT_SHLD_BLOCK;
		previousFallingDamage = Config.ENABLE_FALLING_DAMAGE;
		previousMagicSuccessBySkillLevel = Config.CALCULATE_MAGIC_SUCCESS_BY_SKILL_MAGIC_LEVEL;
		previousMinNpcLevelMagicPenalty = Config.MIN_NPC_LEVEL_MAGIC_PENALTY;
		previousNpcSkillChancePenalty = Config.NPC_SKILL_CHANCE_PENALTY;
		
		Config.ALT_GAME_CANCEL_CAST = true;
		Config.ALT_GAME_CANCEL_BOW = true;
		Config.ALT_PERFECT_SHLD_BLOCK = 10;
		Config.ENABLE_FALLING_DAMAGE = true;
		Config.CALCULATE_MAGIC_SUCCESS_BY_SKILL_MAGIC_LEVEL = true;
		Config.MIN_NPC_LEVEL_MAGIC_PENALTY = 78;
		Config.NPC_SKILL_CHANCE_PENALTY = new float[]
		{
			2.5f,
			3.0f,
			3.25f,
			3.5f
		};
	}
	
	@AfterAll
	static void restoreConfig()
	{
		Config.DATAPACK_ROOT = previousDatapackRoot;
		Config.ALT_GAME_CANCEL_CAST = previousCancelCast;
		Config.ALT_GAME_CANCEL_BOW = previousCancelBow;
		Config.ALT_PERFECT_SHLD_BLOCK = previousPerfectShield;
		Config.ENABLE_FALLING_DAMAGE = previousFallingDamage;
		Config.CALCULATE_MAGIC_SUCCESS_BY_SKILL_MAGIC_LEVEL = previousMagicSuccessBySkillLevel;
		Config.MIN_NPC_LEVEL_MAGIC_PENALTY = previousMinNpcLevelMagicPenalty;
		Config.NPC_SKILL_CHANCE_PENALTY = previousNpcSkillChancePenalty;
	}
	
	@Test
	void calcCritWithoutSkillUsesPositionRate()
	{
		final Creature attacker = mockCreature(false, false);
		final Creature target = mockCreature(false, false);
		when(attacker.getStat().getCriticalHit(target, null)).thenReturn(100);
		when(attacker.getStat().calcStat(eq(Stat.CRITICAL_RATE_POS), anyDouble(), eq(target), eq((Skill) null))).thenReturn(200d);
		when(target.getStat().calcStat(eq(Stat.DEFENCE_CRITICAL_RATE), eq(200d), eq(null), eq(null))).thenReturn(200d);
		when(target.getStat().calcStat(eq(Stat.DEFENCE_CRITICAL_RATE_ADD), eq(0d), eq(null), eq(null))).thenReturn(0d);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(1000)).thenReturn(199);
			assertTrue(Formulas.calcCrit(attacker, target));
			rnd.when(() -> Rnd.get(1000)).thenReturn(200);
			assertFalse(Formulas.calcCrit(attacker, target));
		}
	}
	
	@Test
	void calcCritWithSkillUsesBaseCritAndStr()
	{
		final Creature attacker = mockCreature(false, false);
		final Creature target = mockCreature(false, false);
		final Skill skill = mockSkill();
		when(skill.getBaseCritRate()).thenReturn(10);
		when(target.getStat().calcStat(eq(Stat.DEFENCE_CRITICAL_RATE), anyDouble(), eq(null), eq(null))).thenAnswer(inv -> inv.getArgument(1));
		when(target.getStat().calcStat(eq(Stat.DEFENCE_CRITICAL_RATE_ADD), eq(0d), eq(null), eq(null))).thenReturn(0d);
		
		// rate = 10 * 10 * STR(40)=1.2 = 120
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(1000)).thenReturn(119);
			assertTrue(Formulas.calcCrit(attacker, target, skill));
			rnd.when(() -> Rnd.get(1000)).thenReturn(120);
			assertFalse(Formulas.calcCrit(attacker, target, skill));
		}
	}
	
	@Test
	void calcMCritComparesRateToThousandRoll()
	{
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(1000)).thenReturn(500);
			assertTrue(Formulas.calcMCrit(501));
			assertFalse(Formulas.calcMCrit(500));
		}
	}
	
	@Test
	void calcPAtkSpdUsesFloorForSlowRate()
	{
		assertEquals(2700, Formulas.calcPAtkSpd(1.9));
		assertEquals(470000 / 470, Formulas.calcPAtkSpd(470));
	}
	
	@Test
	void calcAtkSpdScalesByMagicFlag()
	{
		final Creature attacker = mockCreature(false, false);
		when(attacker.getMAtkSpd()).thenReturn(333);
		when(attacker.getPAtkSpd()).thenReturn(300d);
		final Skill magic = mockSkill();
		when(magic.isMagic()).thenReturn(true);
		final Skill physical = mockSkill();
		when(physical.isMagic()).thenReturn(false);
		
		assertEquals(333, Formulas.calcAtkSpd(attacker, magic, 333));
		assertEquals(300, Formulas.calcAtkSpd(attacker, physical, 300));
	}
	
	@Test
	void calcAtkBreakSkipsChannelingRaidInvulAndIdle()
	{
		final Creature target = mockCreature(false, false);
		when(target.isChanneling()).thenReturn(true);
		assertFalse(Formulas.calcAtkBreak(target, 100));
		
		when(target.isChanneling()).thenReturn(false);
		when(target.isCastingNow()).thenReturn(false);
		when(target.isAttackingNow()).thenReturn(false);
		assertFalse(Formulas.calcAtkBreak(target, 100));
		
		when(target.isCastingNow()).thenReturn(true);
		when(target.isRaid()).thenReturn(true);
		assertFalse(Formulas.calcAtkBreak(target, 100));
		
		when(target.isRaid()).thenReturn(false);
		when(target.isInvul()).thenReturn(true);
		assertFalse(Formulas.calcAtkBreak(target, 100));
	}
	
	@Test
	void calcAtkBreakBowCancelRequiresBowWeapon()
	{
		final boolean previousCast = Config.ALT_GAME_CANCEL_CAST;
		final boolean previousBow = Config.ALT_GAME_CANCEL_BOW;
		Config.ALT_GAME_CANCEL_CAST = false;
		Config.ALT_GAME_CANCEL_BOW = true;
		try
		{
			final Creature target = mockCreature(false, false);
			when(target.isAttackingNow()).thenReturn(true);
			when(target.getActiveWeaponItem()).thenReturn(null);
			assertFalse(Formulas.calcAtkBreak(target, 100));
			
			final Weapon sword = mock(Weapon.class);
			when(sword.getItemType()).thenReturn(WeaponType.SWORD);
			when(target.getActiveWeaponItem()).thenReturn(sword);
			assertFalse(Formulas.calcAtkBreak(target, 100));
			
			final Weapon bow = mock(Weapon.class);
			when(bow.getItemType()).thenReturn(WeaponType.BOW);
			when(target.getActiveWeaponItem()).thenReturn(bow);
			try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
			{
				rnd.when(() -> Rnd.get(100)).thenReturn(0);
				assertTrue(Formulas.calcAtkBreak(target, 100));
			}
		}
		finally
		{
			Config.ALT_GAME_CANCEL_CAST = previousCast;
			Config.ALT_GAME_CANCEL_BOW = previousBow;
		}
	}
	
	@Test
	void calcAtkBreakClampRequiresEdgeRolls()
	{
		final Creature target = mockCreature(false, false);
		when(target.isCastingNow()).thenReturn(true);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			// MEN(40)=2.65 → init 15 - (265-100) = -150 → clamp 1
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertTrue(Formulas.calcAtkBreak(target, 0));
			rnd.when(() -> Rnd.get(100)).thenReturn(1);
			assertFalse(Formulas.calcAtkBreak(target, 0));
			
			// Large damage clamps to 99
			rnd.when(() -> Rnd.get(100)).thenReturn(98);
			assertTrue(Formulas.calcAtkBreak(target, 100000));
			rnd.when(() -> Rnd.get(100)).thenReturn(99);
			assertFalse(Formulas.calcAtkBreak(target, 100000));
		}
	}
	
	@Test
	void calcShldUseIgnoresShieldSigilAndLowRate()
	{
		final Creature attacker = mockCreature(false, false);
		final Creature target = mockCreature(false, false);
		final Skill ignore = mockSkill();
		when(ignore.ignoreShield()).thenReturn(true);
		assertEquals(0, Formulas.calcShldUse(attacker, target, ignore, false));
		
		final Skill skill = mockSkill();
		when(target.getSecondaryWeaponItem()).thenReturn(mock(Weapon.class));
		assertEquals(0, Formulas.calcShldUse(attacker, target, skill, false));
		
		final Armor sigil = mock(Armor.class);
		when(sigil.getItemType()).thenReturn(ArmorType.SIGIL);
		when(target.getSecondaryWeaponItem()).thenReturn(sigil);
		assertEquals(0, Formulas.calcShldUse(attacker, target, skill, false));
		
		final Armor shield = mock(Armor.class);
		when(shield.getItemType()).thenReturn(ArmorType.SHIELD);
		when(target.getSecondaryWeaponItem()).thenReturn(shield);
		when(target.calcStat(eq(Stat.SHIELD_RATE), eq(0d), eq(attacker), eq(null))).thenReturn(0d);
		assertEquals(0, Formulas.calcShldUse(attacker, target, skill, false));
	}
	
	@Test
	void calcShldUseAngleBlocksOutsideArc()
	{
		final Creature attacker = mockCreature(false, false);
		final Creature target = mockCreature(false, false);
		final Armor shield = mock(Armor.class);
		when(shield.getItemType()).thenReturn(ArmorType.SHIELD);
		when(target.getSecondaryWeaponItem()).thenReturn(shield);
		when(target.calcStat(eq(Stat.SHIELD_RATE), eq(0d), eq(attacker), eq(null))).thenReturn(100d);
		when(target.calcStat(eq(Stat.SHIELD_DEFENCE_ANGLE), eq(0d), eq(null), eq(null))).thenReturn(0d);
		when(target.getHeading()).thenReturn(0);
		when(target.calculateDirectionTo(attacker)).thenReturn(180d);
		assertEquals(0, Formulas.calcShldUse(attacker, target, mockSkill(), false));
	}
	
	@Test
	void calcShldUseCoversPerfectSuccessFailurePacketsAndOverloads()
	{
		final Creature attacker = mockCreature(false, false);
		final Player target = mockPlayer();
		final Armor shield = mock(Armor.class);
		when(shield.getItemType()).thenReturn(ArmorType.SHIELD);
		when(target.getSecondaryWeaponItem()).thenReturn(shield);
		when(target.calcStat(eq(Stat.SHIELD_RATE), eq(0d), eq(attacker), eq(null))).thenReturn(50d);
		when(target.calcStat(eq(Stat.SHIELD_DEFENCE_ANGLE), eq(0d), eq(null), eq(null))).thenReturn(240d); // degreeside 360
		when(target.getHeading()).thenReturn(0);
		when(target.calculateDirectionTo(attacker)).thenReturn(0d);
		
		final Weapon bow = mock(Weapon.class);
		when(bow.getItemType()).thenReturn(WeaponType.BOW);
		when(attacker.getActiveWeaponItem()).thenReturn(bow);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(95); // 90 < 95 → perfect
			assertEquals(Formulas.SHIELD_DEFENSE_PERFECT_BLOCK, Formulas.calcShldUse(attacker, target, mockSkill(), true));
			verify(target).sendPacket(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
			
			rnd.when(() -> Rnd.get(100)).thenReturn(40); // not perfect; 50*1.3 DEX>40
			assertEquals(Formulas.SHIELD_DEFENSE_SUCCEED, Formulas.calcShldUse(attacker, target, mockSkill(), true));
			verify(target).sendPacket(SystemMessageId.YOUR_SHIELD_DEFENSE_HAS_SUCCEEDED);
			
			clearInvocations(target);
			rnd.when(() -> Rnd.get(100)).thenReturn(80);
			assertEquals(Formulas.SHIELD_DEFENSE_FAILED, Formulas.calcShldUse(attacker, target, mockSkill(), false));
			verify(target, never()).sendPacket(SystemMessageId.YOUR_SHIELD_DEFENSE_HAS_SUCCEEDED);
			verify(target, never()).sendPacket(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
		}
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(80);
			assertEquals(Formulas.SHIELD_DEFENSE_FAILED, Formulas.calcShldUse(attacker, target, mockSkill()));
			assertEquals(Formulas.SHIELD_DEFENSE_FAILED, Formulas.calcShldUse(attacker, target));
		}
	}
	
	@Test
	void calcMagicAffectedUsesDefenceImmunityAndGaussian()
	{
		final Creature actor = mockCreature(false, false);
		final Creature target = mockCreature(false, false);
		when(actor.getMAtk(any(), any())).thenReturn(100d);
		when(target.getMDef(any(), any())).thenReturn(50d);
		
		final Skill good = mockSkill();
		when(good.isActive()).thenReturn(false);
		when(good.isBad()).thenReturn(false);
		when(good.isDebuff()).thenReturn(false);
		when(good.getTraitType()).thenReturn(TraitType.NONE);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(Rnd::nextGaussian).thenReturn(0d);
			assertTrue(Formulas.calcMagicAffected(actor, target, good));
		}
		
		final Skill bad = mockSkill();
		when(bad.isActive()).thenReturn(true);
		when(bad.isBad()).thenReturn(true);
		when(bad.isDebuff()).thenReturn(true);
		when(bad.getTraitType()).thenReturn(TraitType.NONE);
		when(target.calcStat(eq(Stat.DEBUFF_IMMUNITY), eq(0d), eq(null), eq(bad))).thenReturn(1d);
		assertFalse(Formulas.calcMagicAffected(actor, target, bad));
		
		when(target.calcStat(eq(Stat.DEBUFF_IMMUNITY), eq(0d), eq(null), eq(bad))).thenReturn(0d);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(Rnd::nextGaussian).thenReturn(-2d);
			assertFalse(Formulas.calcMagicAffected(actor, target, bad));
			rnd.when(Rnd::nextGaussian).thenReturn(2d);
			assertTrue(Formulas.calcMagicAffected(actor, target, bad));
		}
	}
	
	@Test
	void calcLvlBonusModUsesSkillMagicLevelOrAttackerLevel()
	{
		final Creature attacker = mockCreature(false, false);
		final Creature target = mockCreature(false, false);
		when(attacker.getLevel()).thenReturn(80);
		when(target.getLevel()).thenReturn(70);
		final Skill skill = mockSkill();
		when(skill.getMagicLevel()).thenReturn(0);
		when(skill.getLvlBonusRate()).thenReturn(10);
		assertEquals(1.1 * 1.1, Formulas.calcLvlBonusMod(attacker, target, skill), 0.0001);
		
		when(skill.getMagicLevel()).thenReturn(90);
		assertEquals(1.1 * 1.2, Formulas.calcLvlBonusMod(attacker, target, skill), 0.0001);
	}
	
	@Test
	void calcEffectSuccessRejectsStaticTargetsAndImmunity()
	{
		final Creature attacker = mockCreature(true, true);
		when(attacker.isPlayer()).thenReturn(true);
		final Skill skill = landingSkill(BaseStat.MEN, 50, false);
		
		final Creature door = mockCreature(false, false);
		when(door.isDoor()).thenReturn(true);
		assertFalse(Formulas.calcEffectSuccess(attacker, door, skill));
		
		assertFalse(Formulas.calcEffectSuccess(attacker, mock(SiegeFlag.class), skill));
		assertFalse(Formulas.calcEffectSuccess(attacker, mock(StaticObject.class), skill));
		
		final Creature target = mockCreature(false, false);
		when(target.getName()).thenReturn("Target");
		when(skill.isDebuff()).thenReturn(true);
		when(target.calcStat(eq(Stat.DEBUFF_IMMUNITY), eq(0d), eq(attacker), eq(skill))).thenReturn(1d);
		assertFalse(Formulas.calcEffectSuccess(attacker, target, skill));
		final ArgumentCaptor<SystemMessage> captor = ArgumentCaptor.forClass(SystemMessage.class);
		verify(attacker).sendPacket(captor.capture());
		assertEquals(SystemMessageId.C1_HAS_RESISTED_YOUR_S2, captor.getValue().getSystemMessageId());
	}
	
	@Test
	void calcEffectSuccessEarlyTrueAndBaseStatArms()
	{
		final Creature attacker = mockCreature(false, false);
		final Creature target = mockCreature(false, false);
		
		final Skill always = landingSkill(BaseStat.NONE, 50, false);
		assertTrue(Formulas.calcEffectSuccess(attacker, target, always));
		
		final Skill high = landingSkill(BaseStat.MEN, 100, false);
		assertTrue(Formulas.calcEffectSuccess(attacker, target, high));
		
		final Skill minusOne = landingSkill(BaseStat.MEN, -1, false);
		assertTrue(Formulas.calcEffectSuccess(attacker, target, minusOne));
	}
	
	@ParameterizedTest
	@MethodSource("basicProperties")
	void calcEffectSuccessReadsEachBasicProperty(BaseStat property)
	{
		final Creature attacker = mockCreature(false, false);
		final Creature target = mockCreature(false, false);
		final Skill skill = landingSkill(property, 50, false);
		when(skill.getMagicLevel()).thenReturn(-1); // becomes targetLevel+3
		when(skill.getLvlBonusRate()).thenReturn(0);
		when(skill.getMinChance()).thenReturn(0);
		when(skill.getMaxChance()).thenReturn(100);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertTrue(Formulas.calcEffectSuccess(attacker, target, skill));
		}
	}
	
	static Stream<Arguments> basicProperties()
	{
		return Stream.of(
			Arguments.of(BaseStat.STR),
			Arguments.of(BaseStat.DEX),
			Arguments.of(BaseStat.CON),
			Arguments.of(BaseStat.INT),
			Arguments.of(BaseStat.MEN),
			Arguments.of(BaseStat.WIT));
	}
	
	@Test
	void calcEffectSuccessMagicShotTraitZeroAndResistPacket()
	{
		final Creature attacker = mockCreature(false, false);
		final Creature target = mockCreature(false, false);
		when(target.getName()).thenReturn("Target");
		when(attacker.getMAtk(null, null)).thenReturn(100d);
		when(target.getMDef(null, null)).thenReturn(100d);
		
		final Skill magic = landingSkill(BaseStat.MEN, 50, true);
		when(magic.getMagicLevel()).thenReturn(EQUAL_LEVEL);
		when(magic.getLvlBonusRate()).thenReturn(0);
		when(magic.getMinChance()).thenReturn(0);
		when(magic.getMaxChance()).thenReturn(100);
		when(attacker.isChargedShot(ShotType.BLESSED_SPIRITSHOTS)).thenReturn(true);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertTrue(Formulas.calcEffectSuccess(attacker, target, magic));
		}
		
		when(attacker.isChargedShot(ShotType.BLESSED_SPIRITSHOTS)).thenReturn(false);
		final Skill debuffLand = landingSkill(BaseStat.MEN, 50, false);
		when(debuffLand.isDebuff()).thenReturn(true);
		when(debuffLand.getMagicLevel()).thenReturn(EQUAL_LEVEL);
		when(debuffLand.getLvlBonusRate()).thenReturn(0);
		when(debuffLand.getMinChance()).thenReturn(0);
		when(debuffLand.getMaxChance()).thenReturn(100);
		when(target.calcStat(eq(Stat.DEBUFF_IMMUNITY), eq(0d), eq(attacker), eq(debuffLand))).thenReturn(0d);
		when(target.calcStat(eq(Stat.DEBUFF_VULN), eq(1d), eq(null), eq(null))).thenReturn(0d);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertTrue(Formulas.calcEffectSuccess(attacker, target, debuffLand));
		}
		
		when(magic.getTraitType()).thenReturn(TraitType.POISON);
		when(target.getStat().isTraitInvul(TraitType.POISON)).thenReturn(true);
		assertFalse(Formulas.calcEffectSuccess(attacker, target, magic));
		
		when(magic.getTraitType()).thenReturn(TraitType.NONE);
		when(magic.getMinChance()).thenReturn(10);
		when(magic.getMaxChance()).thenReturn(10);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(10);
			assertFalse(Formulas.calcEffectSuccess(attacker, target, magic));
			final ArgumentCaptor<SystemMessage> captor = ArgumentCaptor.forClass(SystemMessage.class);
			verify(attacker, times(2)).sendPacket(captor.capture());
			assertEquals(SystemMessageId.C1_HAS_RESISTED_YOUR_S2, captor.getValue().getSystemMessageId());
		}
	}
	
	@Test
	void calcCubicSkillSuccessCoversDebuffShieldReflectionAndRoll()
	{
		final Cubic cubic = mock(Cubic.class);
		final Player owner = mockPlayer();
		when(cubic.getOwner()).thenReturn(owner);
		final Creature target = mockCreature(false, false);
		final Skill skill = landingSkill(BaseStat.MEN, 50, false);
		when(skill.isDebuff()).thenReturn(true);
		when(skill.getPower()).thenReturn(-1d);
		assertTrue(Formulas.calcCubicSkillSuccess(cubic, target, skill, Formulas.SHIELD_DEFENSE_FAILED));
		
		when(skill.getPower()).thenReturn(50d);
		when(target.calcStat(eq(Stat.DEBUFF_IMMUNITY), eq(0d), eq(null), eq(skill))).thenReturn(1d);
		assertFalse(Formulas.calcCubicSkillSuccess(cubic, target, skill, Formulas.SHIELD_DEFENSE_FAILED));
		
		when(target.calcStat(eq(Stat.DEBUFF_IMMUNITY), eq(0d), eq(null), eq(skill))).thenReturn(0d);
		assertFalse(Formulas.calcCubicSkillSuccess(cubic, target, skill, Formulas.SHIELD_DEFENSE_PERFECT_BLOCK));
		
		when(skill.getActivateRate()).thenReturn(50);
		when(target.calcStat(eq(Stat.REFLECT_SKILL_PHYSIC), eq(0d), eq(null), eq(skill))).thenReturn(100d);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertFalse(Formulas.calcCubicSkillSuccess(cubic, target, skill, Formulas.SHIELD_DEFENSE_FAILED));
		}
		
		when(target.calcStat(eq(Stat.REFLECT_SKILL_PHYSIC), eq(0d), eq(null), eq(skill))).thenReturn(0d);
		when(skill.getMinChance()).thenReturn(20);
		when(skill.getMaxChance()).thenReturn(80);
		when(skill.getLvlBonusRate()).thenReturn(0);
		when(skill.getMagicLevel()).thenReturn(EQUAL_LEVEL);
		when(owner.getLevel()).thenReturn(EQUAL_LEVEL);
		when(target.getLevel()).thenReturn(EQUAL_LEVEL);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertTrue(Formulas.calcCubicSkillSuccess(cubic, target, skill, Formulas.SHIELD_DEFENSE_FAILED));
			rnd.when(() -> Rnd.get(100)).thenReturn(99);
			assertFalse(Formulas.calcCubicSkillSuccess(cubic, target, skill, Formulas.SHIELD_DEFENSE_FAILED));
		}
	}
	
	@Test
	void calcMagicSuccessCoversPowerConfigNpcPenaltyAndResist()
	{
		final Player attacker = mockPlayer();
		when(attacker.getLevel()).thenReturn(EQUAL_LEVEL);
		final Creature target = mockCreature(false, false);
		when(target.getLevel()).thenReturn(EQUAL_LEVEL);
		final Skill skill = mockSkill();
		when(skill.getPower()).thenReturn(-1d);
		assertTrue(Formulas.calcMagicSuccess(attacker, target, skill));
		
		when(skill.getPower()).thenReturn(50d);
		when(skill.getMagicLevel()).thenReturn(EQUAL_LEVEL);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertTrue(Formulas.calcMagicSuccess(attacker, target, skill));
		}
		
		final boolean previous = Config.CALCULATE_MAGIC_SUCCESS_BY_SKILL_MAGIC_LEVEL;
		Config.CALCULATE_MAGIC_SUCCESS_BY_SKILL_MAGIC_LEVEL = false;
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertTrue(Formulas.calcMagicSuccess(attacker, target, skill));
		}
		finally
		{
			Config.CALCULATE_MAGIC_SUCCESS_BY_SKILL_MAGIC_LEVEL = previous;
		}
		
		when(target.isAttackable()).thenReturn(true);
		when(target.getLevel()).thenReturn(EQUAL_LEVEL + 5);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertTrue(Formulas.calcMagicSuccess(attacker, target, skill));
		}
		
		when(target.getLevel()).thenReturn(EQUAL_LEVEL + 20);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			// lvlModifier * clamp penalty drives rate below zero
			assertFalse(Formulas.calcMagicSuccess(attacker, target, skill));
		}
		
		when(target.getLevel()).thenReturn(EQUAL_LEVEL);
		when(target.isRaid()).thenReturn(true);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertTrue(Formulas.calcMagicSuccess(attacker, target, skill));
		}
		when(target.isRaid()).thenReturn(false);
		when(target.isRaidMinion()).thenReturn(true);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertTrue(Formulas.calcMagicSuccess(attacker, target, skill));
		}
		
		when(target.isRaidMinion()).thenReturn(false);
		when(target.calcStat(eq(Stat.MAGIC_SUCCESS_RES), eq(1d), eq(null), eq(skill))).thenReturn(1000d);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertFalse(Formulas.calcMagicSuccess(attacker, target, skill));
		}
	}
	
	@Test
	void calculateSkillResurrectRestorePercentAsWritten()
	{
		final Creature caster = mockCreature(false, false);
		assertEquals(0, Formulas.calculateSkillResurrectRestorePercent(0, caster), 0.0001);
		assertEquals(100, Formulas.calculateSkillResurrectRestorePercent(100, caster), 0.0001);
		
		when(caster.getWIT()).thenReturn(10); // bonus 0.61
		assertEquals(50, Formulas.calculateSkillResurrectRestorePercent(50, caster), 0.0001);
		
		when(caster.getWIT()).thenReturn(25); // bonus 1.28; base 50 → 64, delta 14
		assertEquals(64, Formulas.calculateSkillResurrectRestorePercent(50, caster), 0.0001);
		
		when(caster.getWIT()).thenReturn(34); // bonus 1.98; base 30 → 59.4 + 20 = 79.4
		assertEquals(79.4, Formulas.calculateSkillResurrectRestorePercent(30, caster), 0.0001);
		
		when(caster.getWIT()).thenReturn(34); // base 50 → 99 + 20 = 119 → 90
		assertEquals(90, Formulas.calculateSkillResurrectRestorePercent(50, caster), 0.0001);
	}
	
	@Test
	void calcPhysicalSkillEvasionCoversBranchesAndPackets()
	{
		final Creature creature = mockCreature(true, true);
		final Creature target = mockCreature(true, true);
		when(creature.getName()).thenReturn("Attacker");
		when(target.getName()).thenReturn("Target");
		when(creature.asPlayer()).thenReturn(mock(Player.class));
		when(target.asPlayer()).thenReturn(mock(Player.class));
		
		final Skill magic = mockSkill();
		when(magic.isMagic()).thenReturn(true);
		assertFalse(Formulas.calcPhysicalSkillEvasion(creature, target, magic));
		
		final Skill debuff = mockSkill();
		when(debuff.isMagic()).thenReturn(false);
		when(debuff.isDebuff()).thenReturn(true);
		assertFalse(Formulas.calcPhysicalSkillEvasion(creature, target, debuff));
		
		final Skill skill = mockSkill();
		when(skill.isMagic()).thenReturn(false);
		when(skill.isDebuff()).thenReturn(false);
		when(target.calcStat(eq(Stat.P_SKILL_EVASION), eq(0d), eq(null), eq(skill))).thenReturn(50d);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(50);
			assertFalse(Formulas.calcPhysicalSkillEvasion(creature, target, skill));
			
			rnd.when(() -> Rnd.get(100)).thenReturn(10);
			assertTrue(Formulas.calcPhysicalSkillEvasion(creature, target, skill));
			final ArgumentCaptor<SystemMessage> attackerMsg = ArgumentCaptor.forClass(SystemMessage.class);
			verify(creature.asPlayer()).sendPacket(attackerMsg.capture());
			assertEquals(SystemMessageId.C1_DODGES_THE_ATTACK, attackerMsg.getValue().getSystemMessageId());
			final ArgumentCaptor<SystemMessage> targetMsg = ArgumentCaptor.forClass(SystemMessage.class);
			verify(target.asPlayer()).sendPacket(targetMsg.capture());
			assertEquals(SystemMessageId.YOU_HAVE_AVOIDED_C1_S_ATTACK_2, targetMsg.getValue().getSystemMessageId());
		}
		
		final Creature npcAttacker = mockCreature(false, false);
		final Creature npcTarget = mockCreature(false, false);
		when(npcTarget.calcStat(eq(Stat.P_SKILL_EVASION), eq(0d), eq(null), eq(skill))).thenReturn(50d);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(10);
			assertTrue(Formulas.calcPhysicalSkillEvasion(npcAttacker, npcTarget, skill));
		}
	}
	
	@Test
	void calcSkillMasteryCoversStaticZeroNonPlayerAndPlayerValues()
	{
		final Creature actor = mockCreature(false, false);
		final Skill skill = mockSkill();
		when(skill.isStatic()).thenReturn(true);
		assertFalse(Formulas.calcSkillMastery(actor, skill));
		
		when(skill.isStatic()).thenReturn(false);
		when(actor.getStat().calcStat(eq(Stat.SKILL_MASTERY), eq(0d), eq(null), eq(null))).thenReturn(0d);
		assertFalse(Formulas.calcSkillMastery(actor, skill));
		
		when(actor.getStat().calcStat(eq(Stat.SKILL_MASTERY), eq(0d), eq(null), eq(null))).thenReturn(1d);
		assertFalse(Formulas.calcSkillMastery(actor, skill));
		
		final Player player = mockPlayer();
		when(player.getStat().calcStat(eq(Stat.SKILL_MASTERY), eq(0d), eq(null), eq(null))).thenReturn(1d);
		when(player.getStat().calcStat(eq(Stat.SKILL_MASTERY_RATE), eq(1d), eq(null), eq(null))).thenReturn(1d);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertTrue(Formulas.calcSkillMastery(player, skill));
		}
		
		when(player.getStat().calcStat(eq(Stat.SKILL_MASTERY), eq(0d), eq(null), eq(null))).thenReturn(4d);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(99);
			assertFalse(Formulas.calcSkillMastery(player, skill));
		}
		
		when(player.getStat().calcStat(eq(Stat.SKILL_MASTERY), eq(0d), eq(null), eq(null))).thenReturn(2d);
		assertFalse(Formulas.calcSkillMastery(player, skill));
	}
	
	@ParameterizedTest
	@MethodSource("attributeCases")
	void calcAttributeBonusCoversBands(int attack, int defence, boolean playable)
	{
		final Creature attacker = mockCreature(playable, false);
		final Creature target = mockCreature(playable, false);
		when(attacker.getAttackElement()).thenReturn((byte) 0);
		when(attacker.getAttackElementValue((byte) 0)).thenReturn(attack);
		when(target.getDefenseElementValue((byte) 0)).thenReturn(defence);
		
		final double result = Formulas.calcAttributeBonus(attacker, target, null);
		assertTrue(result > 0);
	}
	
	static Stream<Arguments> attributeCases()
	{
		// attack must exceed defence to enter the band table (early return otherwise).
		return Stream.of(
			Arguments.of(0, 0, false),
			Arguments.of(100, 100, false),
			Arguments.of(500, 450, false),
			Arguments.of(500, 350, false),
			Arguments.of(500, 0, false),
			Arguments.of(350, 300, false),
			Arguments.of(350, 150, false),
			Arguments.of(350, 0, false),
			Arguments.of(200, 150, false),
			Arguments.of(200, 0, false),
			Arguments.of(200, -50, false),
			Arguments.of(-50, -100, false),
			Arguments.of(0, -50, false),
			Arguments.of(-150, -200, false),
			Arguments.of(500, 100, false),
			Arguments.of(300, 100, false),
			Arguments.of(100, 50, false),
			Arguments.of(50, 250, false));
	}
	
	@Test
	void calcAttributeBonusSkillMismatchAndPlayableFloor()
	{
		final Creature attacker = mockCreature(true, false);
		final Creature target = mockCreature(true, false);
		when(attacker.getAttackElement()).thenReturn((byte) 0);
		when(attacker.getAttackElementValue((byte) 0)).thenReturn(0);
		when(target.getDefenseElementValue((byte) 0)).thenReturn(-1);
		
		final Skill mismatch = mockSkill();
		when(mismatch.getElement()).thenReturn((byte) 1);
		assertEquals(1, Formulas.calcAttributeBonus(attacker, target, mismatch), 0.0001);
		
		final Skill match = mockSkill();
		when(match.getElement()).thenReturn((byte) 0);
		when(match.getElementPower()).thenReturn(0);
		// Raw result is below 1; playable combat floors it.
		assertEquals(1.0, Formulas.calcAttributeBonus(attacker, target, match), 0.0001);
		
		final Creature npcAttacker = mockCreature(false, false);
		final Creature npcTarget = mockCreature(false, false);
		when(npcAttacker.getAttackElement()).thenReturn((byte) 0);
		when(npcAttacker.getAttackElementValue((byte) 0)).thenReturn(0);
		when(npcTarget.getDefenseElementValue((byte) 0)).thenReturn(-1);
		assertTrue(Formulas.calcAttributeBonus(npcAttacker, npcTarget, match) < 1.0);
		
		final Skill none = mockSkill();
		when(none.getElement()).thenReturn((byte) -1);
		assertEquals(1, Formulas.calcAttributeBonus(attacker, target, none), 0.0001);
	}
	
	@Test
	void calcGeneralTraitBonusCoversTypes()
	{
		final Creature attacker = mockCreature(false, false);
		final Creature target = mockCreature(false, false);
		assertEquals(1.0, Formulas.calcGeneralTraitBonus(attacker, target, TraitType.NONE, false), 0.0001);
		
		when(target.getStat().isTraitInvul(TraitType.POISON)).thenReturn(true);
		assertEquals(0, Formulas.calcGeneralTraitBonus(attacker, target, TraitType.POISON, false), 0.0001);
		
		assertEquals(1.0, Formulas.calcGeneralTraitBonus(attacker, target, TraitType.SWORD, false), 0.0001);
		
		when(attacker.getStat().hasAttackTrait(TraitType.BUG_WEAKNESS)).thenReturn(false);
		assertEquals(1.0, Formulas.calcGeneralTraitBonus(attacker, target, TraitType.BUG_WEAKNESS, false), 0.0001);
		
		when(attacker.getStat().hasAttackTrait(TraitType.BUG_WEAKNESS)).thenReturn(true);
		when(target.getStat().hasDefenceTrait(TraitType.BUG_WEAKNESS)).thenReturn(true);
		when(attacker.getStat().getAttackTrait(TraitType.BUG_WEAKNESS)).thenReturn(2.0f);
		when(target.getStat().getDefenceTrait(TraitType.BUG_WEAKNESS)).thenReturn(0.0f);
		assertEquals(2.0, Formulas.calcGeneralTraitBonus(attacker, target, TraitType.BUG_WEAKNESS, false), 0.0001);
		
		when(attacker.getStat().getAttackTrait(TraitType.BUG_WEAKNESS)).thenReturn(0.0f);
		when(target.getStat().getDefenceTrait(TraitType.BUG_WEAKNESS)).thenReturn(2.0f);
		assertEquals(0.05, Formulas.calcGeneralTraitBonus(attacker, target, TraitType.BUG_WEAKNESS, false), 0.0001);
		
		when(target.getStat().isTraitInvul(TraitType.POISON)).thenReturn(false);
		assertEquals(1.0, Formulas.calcGeneralTraitBonus(attacker, target, TraitType.POISON, true), 0.0001);
		when(attacker.getStat().getAttackTrait(TraitType.POISON)).thenReturn(1.5f);
		when(target.getStat().getDefenceTrait(TraitType.POISON)).thenReturn(0.5f);
		assertEquals(2.0, Formulas.calcGeneralTraitBonus(attacker, target, TraitType.POISON, false), 0.0001);
	}
	
	@Test
	void calcWeaponAndAttackTraitBonus()
	{
		final Creature attacker = mockCreature(false, false);
		final Creature target = mockCreature(false, false);
		when(attacker.getAttackType()).thenReturn(WeaponType.SWORD);
		final float[] defence = target.getStat().getDefenceTraits();
		defence[TraitType.SWORD.ordinal()] = 1.0f;
		assertEquals(1.0, Formulas.calcWeaponTraitBonus(attacker, target), 0.0001);
		
		defence[TraitType.SWORD.ordinal()] = 2.0f;
		assertEquals(0.0, Formulas.calcWeaponTraitBonus(attacker, target), 0.0001);
		assertEquals(0.0, Formulas.calcAttackTraitBonus(attacker, target), 0.0001);
		
		defence[TraitType.SWORD.ordinal()] = 1.0f;
		when(target.getStat().isTraitInvul(TraitType.BUG_WEAKNESS)).thenReturn(true);
		assertEquals(0.0, Formulas.calcAttackTraitBonus(attacker, target), 0.0001);
		
		when(target.getStat().isTraitInvul(any())).thenReturn(false);
		when(attacker.getStat().hasAttackTrait(any())).thenReturn(false);
		assertEquals(1.0, Formulas.calcAttackTraitBonus(attacker, target), 0.0001);
		
		defence[TraitType.SWORD.ordinal()] = 0.5f; // weapon bonus 1.5
		when(attacker.getStat().hasAttackTrait(TraitType.BUG_WEAKNESS)).thenReturn(true);
		when(target.getStat().hasDefenceTrait(TraitType.BUG_WEAKNESS)).thenReturn(true);
		when(attacker.getStat().getAttackTrait(TraitType.BUG_WEAKNESS)).thenReturn(2.0f);
		when(target.getStat().getDefenceTrait(TraitType.BUG_WEAKNESS)).thenReturn(0.0f);
		assertEquals(2.0, Formulas.calcAttackTraitBonus(attacker, target), 0.0001);
	}
	
	@Test
	void calcDamageReflectedCoversGuardsCounterAndCrit()
	{
		final Creature attacker = mockCreature(true, true);
		final Creature target = mockCreature(true, true);
		when(attacker.getName()).thenReturn("Attacker");
		when(target.getName()).thenReturn("Target");
		when(target.getPAtk(attacker)).thenReturn(100d);
		when(attacker.getPDef(target)).thenReturn(100d);
		
		final Skill magic = mockSkill();
		when(magic.isMagic()).thenReturn(true);
		Formulas.calcDamageReflected(attacker, target, magic, false);
		verify(attacker, never()).reduceCurrentHp(anyDouble(), any(), any(Skill.class));
		
		final Skill ranged = mockSkill();
		when(ranged.isMagic()).thenReturn(false);
		when(ranged.getCastRange()).thenReturn(100);
		Formulas.calcDamageReflected(attacker, target, ranged, false);
		verify(attacker, never()).reduceCurrentHp(anyDouble(), any(), any(Skill.class));
		
		final Skill melee = mockSkill();
		when(melee.isMagic()).thenReturn(false);
		when(melee.getCastRange()).thenReturn(40);
		when(melee.getTraitType()).thenReturn(TraitType.NONE);
		when(melee.getElement()).thenReturn((byte) -1);
		when(target.calcStat(eq(Stat.VENGEANCE_SKILL_PHYSICAL_DAMAGE), eq(0d), eq(target), eq(melee))).thenReturn(50d);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(50);
			Formulas.calcDamageReflected(attacker, target, melee, false);
			verify(attacker, never()).reduceCurrentHp(anyDouble(), any(), any(Skill.class));
			
			rnd.when(() -> Rnd.get(100)).thenReturn(10);
			Formulas.calcDamageReflected(attacker, target, melee, false);
			verify(attacker, times(1)).reduceCurrentHp(anyDouble(), eq(target), eq(melee));
			final ArgumentCaptor<SystemMessage> targetMsg = ArgumentCaptor.forClass(SystemMessage.class);
			verify(target).sendPacket(targetMsg.capture());
			assertEquals(SystemMessageId.YOU_COUNTERED_C1_S_ATTACK, targetMsg.getValue().getSystemMessageId());
			final ArgumentCaptor<SystemMessage> attackerMsg = ArgumentCaptor.forClass(SystemMessage.class);
			verify(attacker).sendPacket(attackerMsg.capture());
			assertEquals(SystemMessageId.C1_IS_PERFORMING_A_COUNTERATTACK, attackerMsg.getValue().getSystemMessageId());
			
			Formulas.calcDamageReflected(attacker, target, melee, true);
			verify(attacker, times(3)).reduceCurrentHp(anyDouble(), eq(target), eq(melee));
		}
	}
	
	@Test
	void calcBuffDebuffReflectionCoversGuardsAndRoll()
	{
		final Creature target = mockCreature(false, false);
		final Skill buff = mockSkill();
		when(buff.isDebuff()).thenReturn(false);
		assertFalse(Formulas.calcBuffDebuffReflection(target, buff));
		
		final Skill always = mockSkill();
		when(always.isDebuff()).thenReturn(true);
		when(always.getActivateRate()).thenReturn(-1);
		assertFalse(Formulas.calcBuffDebuffReflection(target, always));
		
		final Skill magic = mockSkill();
		when(magic.isDebuff()).thenReturn(true);
		when(magic.getActivateRate()).thenReturn(50);
		when(magic.isMagic()).thenReturn(true);
		when(target.calcStat(eq(Stat.REFLECT_SKILL_MAGIC), eq(0d), eq(null), eq(magic))).thenReturn(40d);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(39);
			assertTrue(Formulas.calcBuffDebuffReflection(target, magic));
			rnd.when(() -> Rnd.get(100)).thenReturn(40);
			assertFalse(Formulas.calcBuffDebuffReflection(target, magic));
		}
		
		final Skill physical = mockSkill();
		when(physical.isDebuff()).thenReturn(true);
		when(physical.getActivateRate()).thenReturn(50);
		when(physical.isMagic()).thenReturn(false);
		when(target.calcStat(eq(Stat.REFLECT_SKILL_PHYSIC), eq(0d), eq(null), eq(physical))).thenReturn(10d);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertTrue(Formulas.calcBuffDebuffReflection(target, physical));
		}
	}
	
	@Test
	void calcFallDamRespectsConfigHeightAndStat()
	{
		final Creature creature = mockCreature(false, false);
		when(creature.getMaxHp()).thenReturn(1000);
		when(creature.calcStat(eq(Stat.FALL), anyDouble(), eq(null), eq(null))).thenAnswer(inv -> inv.getArgument(1));
		
		final boolean previous = Config.ENABLE_FALLING_DAMAGE;
		Config.ENABLE_FALLING_DAMAGE = false;
		try
		{
			assertEquals(0, Formulas.calcFallDam(creature, 10), 0.0001);
		}
		finally
		{
			Config.ENABLE_FALLING_DAMAGE = previous;
		}
		
		assertEquals(0, Formulas.calcFallDam(creature, -1), 0.0001);
		assertEquals(50, Formulas.calcFallDam(creature, 50), 0.0001);
	}
	
	@Test
	void calcBlowSuccessCoversPositionsRateAndRnd()
	{
		final Creature creature = mockCreature(false, false);
		final Creature target = mockCreature(false, false);
		final Skill skill = mockSkill();
		when(skill.getBlowChance()).thenReturn(50);
		
		when(creature.isInFrontOf(target)).thenReturn(true);
		when(creature.isBehind(target)).thenReturn(false);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertTrue(Formulas.calcBlowSuccess(creature, target, skill));
		}
		
		when(creature.isInFrontOf(target)).thenReturn(false);
		when(creature.isBehind(target)).thenReturn(true);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertTrue(Formulas.calcBlowSuccess(creature, target, skill));
		}
		
		when(creature.isBehind(target)).thenReturn(false);
		when(creature.calcStat(eq(Stat.BLOW_RATE), anyDouble(), eq(target), eq(null))).thenAnswer(inv -> inv.getArgument(1));
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(99);
			assertFalse(Formulas.calcBlowSuccess(creature, target, skill));
		}
	}
	
	@Test
	void calcCancelSuccessAndStealEffects()
	{
		final Skill cancelSkill = mockSkill();
		when(cancelSkill.getMinChance()).thenReturn(10);
		when(cancelSkill.getMaxChance()).thenReturn(90);
		final BuffInfo info = mock(BuffInfo.class);
		final Skill buffSkill = mockSkill();
		when(info.getSkill()).thenReturn(buffSkill);
		when(buffSkill.getMagicLevel()).thenReturn(70);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertTrue(Formulas.calcCancelSuccess(info, 80, 50, cancelSkill));
			when(buffSkill.getMagicLevel()).thenReturn(0);
			assertTrue(Formulas.calcCancelSuccess(info, 80, 50, cancelSkill));
			rnd.when(() -> Rnd.get(100)).thenReturn(99);
			assertFalse(Formulas.calcCancelSuccess(info, 80, 50, cancelSkill));
		}
		
		assertThrows(NullPointerException.class, () -> Formulas.calcCancelSuccess(info, 80, 50, null));
		
		final Creature creature = mockCreature(false, false);
		final Creature target = mockCreature(false, false);
		final EffectList effects = mock(EffectList.class);
		when(target.getEffectList()).thenReturn(effects);
		when(effects.hasBuffs()).thenReturn(false);
		when(effects.hasTriggered()).thenReturn(false);
		when(effects.hasDances()).thenReturn(false);
		when(effects.getBuffs()).thenReturn(new ConcurrentLinkedQueue<>());
		when(effects.getTriggered()).thenReturn(new ConcurrentLinkedQueue<>());
		when(effects.getDances()).thenReturn(new ConcurrentLinkedQueue<>());
		when(effects.getDebuffs()).thenReturn(new ConcurrentLinkedQueue<>());
		
		assertTrue(Formulas.calcCancelStealEffects(creature, target, cancelSkill, "other", 50, 1).isEmpty());
		
		final BuffInfo stealable = mock(BuffInfo.class);
		final Skill stealSkill = mockSkill();
		when(stealable.getSkill()).thenReturn(stealSkill);
		when(stealSkill.canBeStolen()).thenReturn(true);
		when(stealSkill.getMagicLevel()).thenReturn(70);
		final BuffInfo notStealable = mock(BuffInfo.class);
		final Skill locked = mockSkill();
		when(notStealable.getSkill()).thenReturn(locked);
		when(locked.canBeStolen()).thenReturn(false);
		
		final ConcurrentLinkedQueue<BuffInfo> buffs = new ConcurrentLinkedQueue<>();
		buffs.add(notStealable);
		buffs.add(stealable);
		when(effects.hasBuffs()).thenReturn(true);
		when(effects.getBuffs()).thenReturn(buffs);
		
		final ConcurrentLinkedQueue<BuffInfo> triggered = new ConcurrentLinkedQueue<>();
		final BuffInfo triggeredInfo = mock(BuffInfo.class);
		final Skill triggeredSkill = mockSkill();
		when(triggeredInfo.getSkill()).thenReturn(triggeredSkill);
		when(triggeredSkill.canBeStolen()).thenReturn(true);
		when(triggeredSkill.getMagicLevel()).thenReturn(70);
		triggered.add(triggeredInfo);
		when(effects.hasTriggered()).thenReturn(true);
		when(effects.getTriggered()).thenReturn(triggered);
		
		final ConcurrentLinkedQueue<BuffInfo> dances = new ConcurrentLinkedQueue<>();
		final BuffInfo danceInfo = mock(BuffInfo.class);
		final Skill danceSkill = mockSkill();
		when(danceInfo.getSkill()).thenReturn(danceSkill);
		when(danceSkill.canBeStolen()).thenReturn(true);
		when(danceSkill.getMagicLevel()).thenReturn(70);
		dances.add(danceInfo);
		when(effects.hasDances()).thenReturn(true);
		when(effects.getDances()).thenReturn(dances);
		
		when(target.calcStat(eq(Stat.CANCEL_VULN), eq(0d), eq(target), eq(null))).thenReturn(0d);
		when(creature.calcStat(eq(Stat.CANCEL_PROF), eq(0d), eq(target), eq(null))).thenReturn(0d);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			final List<BuffInfo> canceled = Formulas.calcCancelStealEffects(creature, target, cancelSkill, "buff", 50, 2);
			assertEquals(2, canceled.size());
		}
		
		when(creature.getLevel()).thenReturn(EQUAL_LEVEL);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertThrows(NullPointerException.class, () -> Formulas.calcCancelStealEffects(creature, target, null, "buff", 50, 1));
		}
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(99);
			assertTrue(Formulas.calcCancelStealEffects(creature, target, cancelSkill, "buff", 50, 5).isEmpty());
		}
		
		when(effects.hasBuffs()).thenReturn(false);
		when(effects.hasTriggered()).thenReturn(false);
		when(effects.hasDances()).thenReturn(false);
		when(effects.getBuffs()).thenReturn(new ConcurrentLinkedQueue<>());
		assertTrue(Formulas.calcCancelStealEffects(creature, target, cancelSkill, "buff", 50, 1).isEmpty());
		
		final BuffInfo debuff = mock(BuffInfo.class);
		final Skill debuffSkill = mockSkill();
		when(debuff.getSkill()).thenReturn(debuffSkill);
		when(debuffSkill.isDebuff()).thenReturn(true);
		when(debuffSkill.canBeDispeled()).thenReturn(true);
		final BuffInfo notDebuff = mock(BuffInfo.class);
		final Skill notDebuffSkill = mockSkill();
		when(notDebuff.getSkill()).thenReturn(notDebuffSkill);
		when(notDebuffSkill.isDebuff()).thenReturn(false);
		final BuffInfo lockedDebuff = mock(BuffInfo.class);
		final Skill lockedDebuffSkill = mockSkill();
		when(lockedDebuff.getSkill()).thenReturn(lockedDebuffSkill);
		when(lockedDebuffSkill.isDebuff()).thenReturn(true);
		when(lockedDebuffSkill.canBeDispeled()).thenReturn(false);
		final ConcurrentLinkedQueue<BuffInfo> debuffs = new ConcurrentLinkedQueue<>();
		debuffs.add(notDebuff);
		debuffs.add(lockedDebuff);
		debuffs.add(debuff);
		when(effects.getDebuffs()).thenReturn(debuffs);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			final List<BuffInfo> canceled = Formulas.calcCancelStealEffects(creature, target, cancelSkill, "debuff", 50, 1);
			assertEquals(1, canceled.size());
			assertEquals(debuff, canceled.get(0));
			
			rnd.when(() -> Rnd.get(100)).thenReturn(51);
			assertTrue(Formulas.calcCancelStealEffects(creature, target, cancelSkill, "debuff", 50, 5).isEmpty());
		}
	}
	
	@Test
	void calcEffectAbnormalTimeCoversPassiveServitorMasteryAndDebuff()
	{
		final Creature caster = mockCreature(false, false);
		final Creature target = mockCreature(false, false);
		final Skill passive = mockSkill();
		when(passive.isPassive()).thenReturn(true);
		assertEquals(-1, Formulas.calcEffectAbnormalTime(caster, target, passive));
		
		final Skill toggle = mockSkill();
		when(toggle.isToggle()).thenReturn(true);
		assertEquals(-1, Formulas.calcEffectAbnormalTime(caster, target, toggle));
		
		final Skill skill = mockSkill();
		when(skill.getAbnormalTime()).thenReturn(60);
		when(skill.isAbnormalInstant()).thenReturn(true);
		when(target.isServitor()).thenReturn(true);
		when(caster.getStat().calcStat(eq(Stat.SKILL_MASTERY), eq(0d), eq(null), eq(null))).thenReturn(0d);
		assertEquals(30, Formulas.calcEffectAbnormalTime(caster, target, skill));
		
		when(target.isServitor()).thenReturn(false);
		when(skill.isAbnormalInstant()).thenReturn(false);
		final Player player = mockPlayer();
		when(player.getStat().calcStat(eq(Stat.SKILL_MASTERY), eq(0d), eq(null), eq(null))).thenReturn(1d);
		when(player.getStat().calcStat(eq(Stat.SKILL_MASTERY_RATE), eq(1d), eq(null), eq(null))).thenReturn(100d);
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertEquals(120, Formulas.calcEffectAbnormalTime(player, target, skill));
		}
		
		// Null caster still enters calcSkillMastery before the debuff null-guard.
		assertThrows(NullPointerException.class, () -> Formulas.calcEffectAbnormalTime(null, target, skill));
		assertEquals(60, Formulas.calcEffectAbnormalTime(caster, null, skill));
		
		when(skill.isDebuff()).thenReturn(true);
		when(skill.getBasicProperty()).thenReturn(BaseStat.NONE);
		when(skill.getTraitType()).thenReturn(TraitType.NONE);
		when(skill.getElement()).thenReturn((byte) -1);
		when(skill.getMagicLevel()).thenReturn(EQUAL_LEVEL);
		when(skill.getLvlBonusRate()).thenReturn(0);
		when(caster.getLevel()).thenReturn(EQUAL_LEVEL);
		when(target.getLevel()).thenReturn(EQUAL_LEVEL);
		when(caster.getStat().calcStat(eq(Stat.SKILL_MASTERY), eq(0d), eq(null), eq(null))).thenReturn(0d);
		assertEquals(60, Formulas.calcEffectAbnormalTime(caster, target, skill));
	}
	
	@Test
	void calcProbabilityUsesAttributeTraitAndRnd()
	{
		final Creature attacker = mockCreature(false, false);
		final Creature target = mockCreature(false, false);
		when(target.getLevel()).thenReturn(EQUAL_LEVEL);
		when(target.getINT()).thenReturn(BASE_STAT);
		final Skill skill = mockSkill();
		when(skill.getMagicLevel()).thenReturn(EQUAL_LEVEL);
		when(skill.getTraitType()).thenReturn(TraitType.NONE);
		when(skill.getElement()).thenReturn((byte) -1);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertTrue(Formulas.calcProbability(50, attacker, target, skill));
			rnd.when(() -> Rnd.get(100)).thenReturn(99);
			assertFalse(Formulas.calcProbability(50, attacker, target, skill));
		}
	}
	
	@ParameterizedTest
	@MethodSource("karmaCases")
	void calculateKarmaGainAsWritten(int pkCount, boolean summon, int expected)
	{
		assertEquals(expected, Formulas.calculateKarmaGain(pkCount, summon));
	}
	
	static Stream<Arguments> karmaCases()
	{
		return Stream.of(
			Arguments.of(0, false, 720),
			Arguments.of(50, false, (int) ((((50 * 0.5) + 1) * 60) * 12)),
			Arguments.of(100, false, (int) ((((100 * 0.125) + 37.75) * 60) * 12)),
			Arguments.of(180, false, 43200),
			Arguments.of(120, true, 10800),
			Arguments.of(0, true, 720));
	}
	
	private static Creature mockCreature(boolean playable, boolean player)
	{
		final Creature creature = mock(Creature.class);
		stubCreature(creature, playable, player, CreatureStat.class);
		return creature;
	}
	
	private static Player mockPlayer()
	{
		final Player player = mock(Player.class);
		stubCreature(player, true, true, PlayerStat.class);
		when(player.asPlayer()).thenReturn(player);
		return player;
	}
	
	private static void stubCreature(Creature creature, boolean playable, boolean player, Class<? extends CreatureStat> statType)
	{
		final CreatureStat stat = mock(statType);
		final float[] defenceTraits = new float[TraitType.values().length];
		java.util.Arrays.fill(defenceTraits, 1.0f);
		when(creature.getStat()).thenReturn(stat);
		when(stat.getDefenceTraits()).thenReturn(defenceTraits);
		when(stat.hasAttackTrait(any())).thenReturn(false);
		when(stat.hasDefenceTrait(any())).thenReturn(false);
		when(stat.isTraitInvul(any())).thenReturn(false);
		when(stat.getAttackTrait(any())).thenReturn(1.0f);
		when(stat.getDefenceTrait(any())).thenReturn(1.0f);
		when(stat.calcStat(any(Stat.class), anyDouble())).thenAnswer(invocation -> invocation.getArgument(1));
		when(stat.calcStat(any(Stat.class), anyDouble(), any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
		when(creature.calcStat(any(Stat.class), anyDouble())).thenAnswer(invocation -> invocation.getArgument(1));
		when(creature.calcStat(any(Stat.class), anyDouble(), any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
		when(creature.isPlayable()).thenReturn(playable);
		when(creature.isPlayer()).thenReturn(player);
		when(creature.isAttackable()).thenReturn(false);
		when(creature.isDoor()).thenReturn(false);
		when(creature.isRaid()).thenReturn(false);
		when(creature.isRaidMinion()).thenReturn(false);
		when(creature.isChanneling()).thenReturn(false);
		when(creature.isCastingNow()).thenReturn(false);
		when(creature.isAttackingNow()).thenReturn(false);
		when(creature.isInvul()).thenReturn(false);
		when(creature.isServitor()).thenReturn(false);
		when(creature.getLevel()).thenReturn(EQUAL_LEVEL);
		when(creature.getSTR()).thenReturn(BASE_STAT);
		when(creature.getDEX()).thenReturn(BASE_STAT);
		when(creature.getCON()).thenReturn(BASE_STAT);
		when(creature.getINT()).thenReturn(BASE_STAT);
		when(creature.getMEN()).thenReturn(BASE_STAT);
		when(creature.getWIT()).thenReturn(BASE_STAT);
		when(creature.getAttackType()).thenReturn(WeaponType.SWORD);
		when(creature.getAttackElement()).thenReturn((byte) 0);
		when(creature.getAttackElementValue(org.mockito.ArgumentMatchers.anyByte())).thenReturn(0);
		when(creature.getDefenseElementValue(org.mockito.ArgumentMatchers.anyByte())).thenReturn(0);
		when(creature.getActiveWeaponItem()).thenReturn(null);
		when(creature.getSecondaryWeaponItem()).thenReturn(null);
		when(creature.getMAtk(any(), any())).thenReturn(100d);
		when(creature.getMDef(any(), any())).thenReturn(100d);
		when(creature.getPAtk(any())).thenReturn(100d);
		when(creature.getPDef(any())).thenReturn(100d);
		when(creature.getMAtkSpd()).thenReturn(333);
		when(creature.getPAtkSpd()).thenReturn(300d);
		when(creature.getMaxHp()).thenReturn(1000);
		when(creature.getHeading()).thenReturn(0);
		when(creature.calculateDirectionTo(any())).thenReturn(0d);
		when(creature.isInFrontOf(any())).thenReturn(true);
		when(creature.isBehind(any())).thenReturn(false);
		when(creature.isChargedShot(any())).thenReturn(false);
		when(creature.asPlayer()).thenReturn(null);
	}
	
	private static Skill mockSkill()
	{
		final Skill skill = mock(Skill.class);
		when(skill.getPower()).thenReturn(50d);
		when(skill.isMagic()).thenReturn(false);
		when(skill.isDebuff()).thenReturn(false);
		when(skill.isActive()).thenReturn(true);
		when(skill.isBad()).thenReturn(false);
		when(skill.isPassive()).thenReturn(false);
		when(skill.isToggle()).thenReturn(false);
		when(skill.isStatic()).thenReturn(false);
		when(skill.isAbnormalInstant()).thenReturn(false);
		when(skill.ignoreShield()).thenReturn(false);
		when(skill.canBeStolen()).thenReturn(true);
		when(skill.canBeDispeled()).thenReturn(true);
		when(skill.getTraitType()).thenReturn(TraitType.NONE);
		when(skill.getElement()).thenReturn((byte) -1);
		when(skill.getElementPower()).thenReturn(0);
		when(skill.getMagicLevel()).thenReturn(EQUAL_LEVEL);
		when(skill.getLvlBonusRate()).thenReturn(0);
		when(skill.getActivateRate()).thenReturn(50);
		when(skill.getMinChance()).thenReturn(0);
		when(skill.getMaxChance()).thenReturn(100);
		when(skill.getBasicProperty()).thenReturn(BaseStat.NONE);
		when(skill.getBlowChance()).thenReturn(50);
		when(skill.getAbnormalTime()).thenReturn(60);
		when(skill.getCastRange()).thenReturn(40);
		when(skill.getBaseCritRate()).thenReturn(10);
		return skill;
	}
	
	private static Skill landingSkill(BaseStat property, int activateRate, boolean magic)
	{
		final Skill skill = mockSkill();
		when(skill.getBasicProperty()).thenReturn(property);
		when(skill.getActivateRate()).thenReturn(activateRate);
		when(skill.isMagic()).thenReturn(magic);
		when(skill.getTraitType()).thenReturn(TraitType.NONE);
		when(skill.getElement()).thenReturn((byte) -1);
		return skill;
	}
}
