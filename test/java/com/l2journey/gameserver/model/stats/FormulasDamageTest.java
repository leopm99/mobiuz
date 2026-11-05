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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;
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
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.instance.Chest;
import com.l2journey.gameserver.model.actor.instance.Cubic;
import com.l2journey.gameserver.model.actor.stat.CreatureStat;
import com.l2journey.gameserver.model.actor.stat.NpcStat;
import com.l2journey.gameserver.model.actor.stat.PlayerStat;
import com.l2journey.gameserver.model.effects.EffectType;
import com.l2journey.gameserver.model.item.Armor;
import com.l2journey.gameserver.model.item.Weapon;
import com.l2journey.gameserver.model.item.type.WeaponType;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.stats.modulators.Modulators;
import com.l2journey.gameserver.network.SystemMessageId;
import com.l2journey.gameserver.network.serverpackets.SystemMessage;

/**
 * Characterization of physical and magic damage formulas in {@link Formulas}.
 */
class FormulasDamageTest
{
	private static final double P_ATK = 1000;
	private static final double P_DEF = 500;
	private static final double M_ATK = 900;
	private static final double M_DEF = 400;
	private static final double SKILL_POWER = 200;
	private static final int SHIELD_DEF = 100;
	private static final int EQUAL_LEVEL = 80;
	
	private static int previousMinNpcLevelDmgPenalty;
	private static float[] previousNpcDmgPenalty;
	private static float[] previousNpcCritDmgPenalty;
	private static float[] previousNpcSkillDmgPenalty;
	private static int previousMinNpcLevelMagicPenalty;
	private static float[] previousNpcSkillChancePenalty;
	private static boolean previousMagicFailures;
	private static boolean previousAltShieldBlocks;
	private static boolean previousCalculateMagicSuccessBySkillMagicLevel;
	
	@BeforeAll
	static void loadDamageConfig()
	{
		previousMinNpcLevelDmgPenalty = Config.MIN_NPC_LEVEL_DMG_PENALTY;
		previousNpcDmgPenalty = Config.NPC_DMG_PENALTY;
		previousNpcCritDmgPenalty = Config.NPC_CRIT_DMG_PENALTY;
		previousNpcSkillDmgPenalty = Config.NPC_SKILL_DMG_PENALTY;
		previousMinNpcLevelMagicPenalty = Config.MIN_NPC_LEVEL_MAGIC_PENALTY;
		previousNpcSkillChancePenalty = Config.NPC_SKILL_CHANCE_PENALTY;
		previousMagicFailures = Config.ALT_GAME_MAGICFAILURES;
		previousAltShieldBlocks = Config.ALT_GAME_SHIELD_BLOCKS;
		previousCalculateMagicSuccessBySkillMagicLevel = Config.CALCULATE_MAGIC_SUCCESS_BY_SKILL_MAGIC_LEVEL;
		
		Config.MIN_NPC_LEVEL_DMG_PENALTY = 78;
		Config.NPC_DMG_PENALTY = new float[]
		{
			0.7f,
			0.6f,
			0.6f,
			0.55f
		};
		Config.NPC_CRIT_DMG_PENALTY = new float[]
		{
			0.75f,
			0.65f,
			0.6f,
			0.58f
		};
		Config.NPC_SKILL_DMG_PENALTY = new float[]
		{
			0.8f,
			0.7f,
			0.65f,
			0.62f
		};
		Config.MIN_NPC_LEVEL_MAGIC_PENALTY = 78;
		Config.NPC_SKILL_CHANCE_PENALTY = new float[]
		{
			2.5f,
			3.0f,
			3.25f,
			3.5f
		};
		Config.ALT_GAME_MAGICFAILURES = true;
		Config.ALT_GAME_SHIELD_BLOCKS = false;
		Config.CALCULATE_MAGIC_SUCCESS_BY_SKILL_MAGIC_LEVEL = true;
	}
	
	@AfterAll
	static void restoreDamageConfig()
	{
		Config.MIN_NPC_LEVEL_DMG_PENALTY = previousMinNpcLevelDmgPenalty;
		Config.NPC_DMG_PENALTY = previousNpcDmgPenalty;
		Config.NPC_CRIT_DMG_PENALTY = previousNpcCritDmgPenalty;
		Config.NPC_SKILL_DMG_PENALTY = previousNpcSkillDmgPenalty;
		Config.MIN_NPC_LEVEL_MAGIC_PENALTY = previousMinNpcLevelMagicPenalty;
		Config.NPC_SKILL_CHANCE_PENALTY = previousNpcSkillChancePenalty;
		Config.ALT_GAME_MAGICFAILURES = previousMagicFailures;
		Config.ALT_GAME_SHIELD_BLOCKS = previousAltShieldBlocks;
		Config.CALCULATE_MAGIC_SUCCESS_BY_SKILL_MAGIC_LEVEL = previousCalculateMagicSuccessBySkillMagicLevel;
	}
	
	@Test
	void blowPerfectBlockReturnsOne()
	{
		final Creature attacker = mockCreature(false, false);
		final Creature target = mockCreature(false, false);
		final Skill skill = mockSkill(false);
		
		assertEquals(1, Formulas.calcBlowDamage(attacker, target, skill, Formulas.SHIELD_DEFENSE_PERFECT_BLOCK, false));
	}
	
	@Test
	void backstabPerfectBlockReturnsOne()
	{
		final Creature attacker = mockCreature(false, false);
		final Creature target = mockCreature(false, false);
		final Skill skill = mockSkill(false);
		
		assertEquals(1, Formulas.calcBackstabDamage(attacker, target, skill, Formulas.SHIELD_DEFENSE_PERFECT_BLOCK, false));
	}
	
	@Test
	void physPerfectBlockReturnsOne()
	{
		final Creature attacker = mockCreature(false, false);
		final Creature target = mockCreature(false, false);
		
		assertEquals(1, Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_PERFECT_BLOCK, false, false));
	}
	
	@Test
	void magicPerfectBlockReturnsOne()
	{
		final Creature attacker = mockCreature(false, false);
		final Creature target = mockCreature(false, false);
		final Skill skill = mockSkill(true);
		
		assertEquals(1, Formulas.calcMagicDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_PERFECT_BLOCK, false, false, false));
	}
	
	@Test
	void cubicMagicPerfectBlockReturnsOne()
	{
		final Cubic cubic = mock(Cubic.class);
		final Creature target = mockCreature(false, false);
		final Skill skill = mockSkill(true);
		
		assertEquals(1, Formulas.calcMagicDam(cubic, target, skill, false, Formulas.SHIELD_DEFENSE_PERFECT_BLOCK));
	}
	
	@Test
	void manaPerfectBlockReturnsOne()
	{
		final Creature attacker = mockCreature(false, false);
		final Creature target = mockCreature(false, false);
		when(target.getMaxMp()).thenReturn(970);
		final Skill skill = mockSkill(true);
		
		assertEquals(1, Formulas.calcManaDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_PERFECT_BLOCK, false, false, false));
	}
	
	@Test
	void blowShieldSuccessAddsShieldDefence()
	{
		final Creature attacker = playableAttacker();
		final Creature target = attackableTarget(EQUAL_LEVEL);
		when(target.getShldDef()).thenReturn(SHIELD_DEF);
		final Skill skill = mockSkill(false);
		stubFront(attacker, target);
		
		final double withShield = Formulas.calcBlowDamage(attacker, target, skill, Formulas.SHIELD_DEFENSE_SUCCEED, false);
		final double withoutShield = Formulas.calcBlowDamage(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false);
		assertTrue(withShield < withoutShield);
		assertTrue(withoutShield > 1);
	}
	
	@Test
	void backstabShieldSuccessAddsShieldDefence()
	{
		final Creature attacker = playableAttacker();
		final Creature target = attackableTarget(EQUAL_LEVEL);
		when(target.getShldDef()).thenReturn(SHIELD_DEF);
		final Skill skill = mockSkill(false);
		stubFront(attacker, target);
		
		final double withShield = Formulas.calcBackstabDamage(attacker, target, skill, Formulas.SHIELD_DEFENSE_SUCCEED, false);
		final double withoutShield = Formulas.calcBackstabDamage(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false);
		assertTrue(withShield < withoutShield);
		assertTrue(withoutShield > 1);
	}
	
	@Test
	void physShieldSuccessAddsShieldDefenceWhenAltBlocksDisabled()
	{
		final Creature attacker = playableAttacker();
		final Creature target = attackableTarget(EQUAL_LEVEL);
		when(target.getShldDef()).thenReturn(SHIELD_DEF);
		stubFront(attacker, target);
		
		final double withShield = Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_SUCCEED, false, false);
		final double withoutShield = Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		assertTrue(withShield < withoutShield);
		assertTrue(withoutShield > 1);
	}
	
	@Test
	void physAltShieldBlocksSubtractsShieldAndClampsAtZero()
	{
		final boolean previous = Config.ALT_GAME_SHIELD_BLOCKS;
		Config.ALT_GAME_SHIELD_BLOCKS = true;
		try
		{
			final Creature attacker = playableAttacker();
			when(attacker.getPAtk(any())).thenReturn(10d);
			final Creature target = attackableTarget(EQUAL_LEVEL);
			when(target.getPDef(any())).thenReturn(500d);
			when(target.getShldDef()).thenReturn(10000);
			stubFront(attacker, target);
			
			assertEquals(0, Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_SUCCEED, false, false));
		}
		finally
		{
			Config.ALT_GAME_SHIELD_BLOCKS = previous;
		}
	}
	
	@Test
	void blowProximityBehindIsTwentyPercentAboveFront()
	{
		final Creature attacker = playableAttacker();
		final Creature target = attackableTarget(EQUAL_LEVEL);
		final Skill skill = mockSkill(false);
		
		stubFront(attacker, target);
		final double front = Formulas.calcBlowDamage(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false);
		when(attacker.isBehind(target)).thenReturn(true);
		when(attacker.isInFrontOf(target)).thenReturn(false);
		final double behind = Formulas.calcBlowDamage(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false);
		assertEquals(front * 1.2, behind, 0.0001);
	}
	
	@Test
	void blowProximitySideIsTenPercentAboveFront()
	{
		final Creature attacker = playableAttacker();
		final Creature target = attackableTarget(EQUAL_LEVEL);
		final Skill skill = mockSkill(false);
		
		stubFront(attacker, target);
		final double front = Formulas.calcBlowDamage(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false);
		when(attacker.isBehind(target)).thenReturn(false);
		when(attacker.isInFrontOf(target)).thenReturn(false);
		final double side = Formulas.calcBlowDamage(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false);
		assertEquals(front * 1.1, side, 0.0001);
	}
	
	@Test
	void backstabSoulshotMultipliesBaseDamage()
	{
		final Creature attacker = playableAttacker();
		final Creature target = attackableTarget(EQUAL_LEVEL);
		final Skill skill = mockSkill(false);
		stubFront(attacker, target);
		
		final double withoutSs = Formulas.calcBackstabDamage(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false);
		final double withSs = Formulas.calcBackstabDamage(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, true);
		assertEquals(withoutSs * 1.458, withSs, 0.0001);
	}
	
	@Test
	void blowPlayableTargetUsesPvpPowerWhileBackstabDoesNot()
	{
		final Creature attacker = playableAttacker();
		final Creature summonTarget = mockCreature(true, false);
		when(summonTarget.getPDef(any())).thenReturn(P_DEF);
		when(summonTarget.getLevel()).thenReturn(EQUAL_LEVEL);
		when(summonTarget.isAttackable()).thenReturn(false);
		when(summonTarget.isRaid()).thenReturn(false);
		when(summonTarget.isRaidMinion()).thenReturn(false);
		stubFront(attacker, summonTarget);
		
		final Skill skill = mockSkill(false);
		when(skill.getPower(true, false)).thenReturn(300d);
		when(skill.getPower(false, false)).thenReturn(SKILL_POWER);
		
		final double blow = Formulas.calcBlowDamage(attacker, summonTarget, skill, Formulas.SHIELD_DEFENSE_FAILED, false);
		final double backstab = Formulas.calcBackstabDamage(attacker, summonTarget, skill, Formulas.SHIELD_DEFENSE_FAILED, false);
		assertTrue(blow > backstab);
		assertTrue(blow > 1);
		assertTrue(backstab > 1);
	}
	
	@Test
	void physSoulshotDoublesAttackContribution()
	{
		final Creature attacker = playableAttacker();
		final Creature target = attackableTarget(EQUAL_LEVEL);
		stubFront(attacker, target);
		
		final double withoutSs = Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		final double withSs = Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_FAILED, false, true);
		assertEquals(withoutSs * 2, withSs, 0.0001);
	}
	
	@Test
	void physCritIsHigherThanNonCrit()
	{
		final Creature attacker = playableAttacker();
		final Creature target = attackableTarget(EQUAL_LEVEL);
		stubFront(attacker, target);
		
		final double nonCrit = Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		final double crit = Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_FAILED, true, false);
		assertTrue(crit > nonCrit);
		assertTrue(nonCrit > 1);
	}
	
	@Test
	void physSkillUsesPhysicalSkillPowerStat()
	{
		final Creature attacker = playableAttacker();
		when(attacker.calcStat(eq(Stat.PHYSICAL_SKILL_POWER), anyDouble(), any(), any())).thenAnswer(invocation -> ((Double) invocation.getArgument(1)) * 1.5);
		final Creature target = attackableTarget(EQUAL_LEVEL);
		final Skill skill = mockSkill(false);
		stubFront(attacker, target);
		
		final double withSkill = Formulas.calcPhysDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		final double autoAttack = Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		assertTrue(withSkill > autoAttack);
	}
	
	@Test
	void physBowVersusAttackableUsesPveBowStat()
	{
		final Creature attacker = playableAttacker();
		final Weapon bow = mock(Weapon.class);
		when(bow.getItemType()).thenReturn(WeaponType.BOW);
		when(attacker.getActiveWeaponItem()).thenReturn(bow);
		when(attacker.calcStat(eq(Stat.PVE_BOW_DMG), anyDouble(), any(), any())).thenReturn(2d);
		final Creature target = attackableTarget(EQUAL_LEVEL);
		stubFront(attacker, target);
		
		final double withBow = Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		when(attacker.getActiveWeaponItem()).thenReturn(null);
		when(attacker.calcStat(eq(Stat.PVE_PHYSICAL_DMG), anyDouble(), any(), any())).thenReturn(1d);
		final double withoutBow = Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		assertEquals(withoutBow * 2, withBow, 0.0001);
	}
	
	@Test
	void physBowSkillVersusAttackableUsesPveBowSkillStat()
	{
		final Creature attacker = playableAttacker();
		final Weapon bow = mock(Weapon.class);
		when(bow.getItemType()).thenReturn(WeaponType.CROSSBOW);
		when(attacker.getActiveWeaponItem()).thenReturn(bow);
		when(attacker.calcStat(eq(Stat.PVE_BOW_SKILL_DMG), anyDouble(), any(), any())).thenReturn(2d);
		final Creature target = attackableTarget(EQUAL_LEVEL);
		final Skill skill = mockSkill(false);
		stubFront(attacker, target);
		
		final double withBowSkill = Formulas.calcPhysDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		when(attacker.getActiveWeaponItem()).thenReturn(null);
		final double withoutBow = Formulas.calcPhysDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		assertEquals(withoutBow * 2, withBowSkill, 0.0001);
	}
	
	@Test
	void physChestTargetAppliesChestStat()
	{
		final Creature attacker = playableAttacker();
		when(attacker.calcStat(eq(Stat.PVE_CHEST_DMG), anyDouble(), any(), any())).thenReturn(1.5d);
		final Chest chest = mock(Chest.class);
		stubCreatureStats(chest, false, false, NpcStat.class);
		when(chest.isAttackable()).thenReturn(true);
		when(chest.getPDef(any())).thenReturn(P_DEF);
		when(chest.getLevel()).thenReturn(EQUAL_LEVEL);
		when(chest.isRaid()).thenReturn(false);
		when(chest.isRaidMinion()).thenReturn(false);
		stubFront(attacker, chest);
		
		final double againstChest = Formulas.calcPhysDam(attacker, chest, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		final Creature normal = attackableTarget(EQUAL_LEVEL);
		stubFront(attacker, normal);
		final double againstNormal = Formulas.calcPhysDam(attacker, normal, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		assertEquals(againstNormal * 1.5, againstChest, 0.0001);
	}
	
	@Test
	void physNpcSkillPenaltyUsesInRangeIndex()
	{
		final Player attacker = mockPlayerAttacker(EQUAL_LEVEL);
		final Creature target = attackableTarget(EQUAL_LEVEL + 3);
		final Skill skill = mockSkill(false);
		stubFront(attacker, target);
		
		final double gapped = Formulas.calcPhysDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		final Creature sameLevel = attackableTarget(EQUAL_LEVEL);
		stubFront(attacker, sameLevel);
		final double baseline = Formulas.calcPhysDam(attacker, sameLevel, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		// levelDiff = targetLevel - attackerLevel - 1 = 2
		assertEquals(baseline * Config.NPC_SKILL_DMG_PENALTY[2], gapped, 0.0001);
	}
	
	@Test
	void physNpcCritPenaltyUsesLastIndexWhenClamped()
	{
		final Player attacker = mockPlayerAttacker(EQUAL_LEVEL);
		final Creature target = attackableTarget(EQUAL_LEVEL + 20);
		stubFront(attacker, target);
		
		final double gapped = Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_FAILED, true, false);
		final Creature sameLevel = attackableTarget(EQUAL_LEVEL);
		stubFront(attacker, sameLevel);
		final double baseline = Formulas.calcPhysDam(attacker, sameLevel, null, Formulas.SHIELD_DEFENSE_FAILED, true, false);
		assertEquals(baseline * Config.NPC_CRIT_DMG_PENALTY[Config.NPC_CRIT_DMG_PENALTY.length - 1], gapped, 0.0001);
	}
	
	@Test
	void physNpcAutoAttackPenaltyUsesInRangeIndex()
	{
		final Player attacker = mockPlayerAttacker(EQUAL_LEVEL);
		final Creature target = attackableTarget(EQUAL_LEVEL + 2);
		stubFront(attacker, target);
		
		final double gapped = Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		final Creature sameLevel = attackableTarget(EQUAL_LEVEL);
		stubFront(attacker, sameLevel);
		final double baseline = Formulas.calcPhysDam(attacker, sameLevel, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		// levelDiff = targetLevel - attackerLevel - 1 = 1
		assertEquals(baseline * Config.NPC_DMG_PENALTY[1], gapped, 0.0001);
	}
	
	@Test
	void physNpcPenaltySkippedForRaid()
	{
		final Player attacker = mockPlayerAttacker(EQUAL_LEVEL);
		final Creature target = attackableTarget(EQUAL_LEVEL + 5);
		when(target.isRaid()).thenReturn(true);
		stubFront(attacker, target);
		
		final double againstRaid = Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		final Creature sameLevel = attackableTarget(EQUAL_LEVEL);
		stubFront(attacker, sameLevel);
		final double baseline = Formulas.calcPhysDam(attacker, sameLevel, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		assertEquals(baseline, againstRaid, 0.0001);
	}
	
	@Test
	void physNpcPenaltySkippedWhenAsPlayerNull()
	{
		final Creature attacker = playableAttacker();
		when(attacker.asPlayer()).thenReturn(null);
		final Creature target = attackableTarget(EQUAL_LEVEL + 5);
		stubFront(attacker, target);
		
		final double gapped = Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		final Creature sameLevel = attackableTarget(EQUAL_LEVEL);
		stubFront(attacker, sameLevel);
		final double baseline = Formulas.calcPhysDam(attacker, sameLevel, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		assertEquals(baseline, gapped, 0.0001);
	}
	
	@Test
	void blowNpcSkillPenaltyUsesLastIndexWhenClamped()
	{
		final Player attacker = mockPlayerAttacker(EQUAL_LEVEL);
		final Creature target = attackableTarget(EQUAL_LEVEL + 20);
		final Skill skill = mockSkill(false);
		stubFront(attacker, target);
		
		final double gapped = Formulas.calcBlowDamage(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false);
		final Creature sameLevel = attackableTarget(EQUAL_LEVEL);
		stubFront(attacker, sameLevel);
		final double baseline = Formulas.calcBlowDamage(attacker, sameLevel, skill, Formulas.SHIELD_DEFENSE_FAILED, false);
		assertEquals(baseline * Config.NPC_SKILL_DMG_PENALTY[Config.NPC_SKILL_DMG_PENALTY.length - 1], gapped, 0.0001);
	}
	
	@Test
	void backstabNpcSkillPenaltyUsesInRangeIndex()
	{
		final Player attacker = mockPlayerAttacker(EQUAL_LEVEL);
		final Creature target = attackableTarget(EQUAL_LEVEL + 3);
		final Skill skill = mockSkill(false);
		stubFront(attacker, target);
		
		final double gapped = Formulas.calcBackstabDamage(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false);
		final Creature sameLevel = attackableTarget(EQUAL_LEVEL);
		stubFront(attacker, sameLevel);
		final double baseline = Formulas.calcBackstabDamage(attacker, sameLevel, skill, Formulas.SHIELD_DEFENSE_FAILED, false);
		// levelDiff = targetLevel - attackerLevel - 1 = 2
		assertEquals(baseline * Config.NPC_SKILL_DMG_PENALTY[2], gapped, 0.0001);
	}
	
	@ParameterizedTest
	@MethodSource("weaponArmorCases")
	void physWeaponVersusArmorAppliesModulator(WeaponType weaponType, boolean heavy, boolean light, boolean robe, double modulator)
	{
		final Player attacker = mockPlayerAttacker(EQUAL_LEVEL);
		final Weapon weapon = mock(Weapon.class);
		when(weapon.getItemType()).thenReturn(weaponType);
		when(attacker.getActiveWeaponItem()).thenReturn(weapon);
		when(attacker.isTransformed()).thenReturn(false);
		
		final Player target = mockPlayerTarget(EQUAL_LEVEL);
		final Armor armor = mock(Armor.class);
		when(target.getActiveChestArmorItem()).thenReturn(armor);
		when(target.isWearingHeavyArmor()).thenReturn(heavy);
		when(target.isWearingLightArmor()).thenReturn(light);
		when(target.isWearingMagicArmor()).thenReturn(robe);
		stubFront(attacker, target);
		
		final double withArmor = Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		when(target.getActiveChestArmorItem()).thenReturn(null);
		final double withoutArmor = Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		assertEquals(withoutArmor * modulator, withArmor, 0.0001);
	}
	
	@Test
	void physSkipsArmorModulatorsWhenNoChestOrTransformed()
	{
		final Player attacker = mockPlayerAttacker(EQUAL_LEVEL);
		final Weapon weapon = mock(Weapon.class);
		when(weapon.getItemType()).thenReturn(WeaponType.SWORD);
		when(attacker.getActiveWeaponItem()).thenReturn(weapon);
		when(attacker.isTransformed()).thenReturn(true);
		
		final Player target = mockPlayerTarget(EQUAL_LEVEL);
		final Armor armor = mock(Armor.class);
		when(target.getActiveChestArmorItem()).thenReturn(armor);
		when(target.isWearingHeavyArmor()).thenReturn(true);
		stubFront(attacker, target);
		
		final double transformed = Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		when(attacker.isTransformed()).thenReturn(false);
		when(target.getActiveChestArmorItem()).thenReturn(null);
		final double noArmor = Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		assertEquals(noArmor, transformed, 0.0001);
	}
	
	@Test
	void physDefaultWeaponTypeLeavesDamageUnmodulated()
	{
		final Player attacker = mockPlayerAttacker(EQUAL_LEVEL);
		final Weapon weapon = mock(Weapon.class);
		when(weapon.getItemType()).thenReturn(WeaponType.FISHINGROD);
		when(attacker.getActiveWeaponItem()).thenReturn(weapon);
		when(attacker.isTransformed()).thenReturn(false);
		
		final Player target = mockPlayerTarget(EQUAL_LEVEL);
		final Armor armor = mock(Armor.class);
		when(target.getActiveChestArmorItem()).thenReturn(armor);
		when(target.isWearingHeavyArmor()).thenReturn(true);
		stubFront(attacker, target);
		
		final double withDefaultWeapon = Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		when(target.getActiveChestArmorItem()).thenReturn(null);
		final double withoutArmor = Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		assertEquals(withoutArmor, withDefaultWeapon, 0.0001);
	}
	
	@Test
	void magicSpiritShotAndBlessedShotScaleAttack()
	{
		final Creature attacker = playableAttacker();
		final Creature target = attackableTarget(EQUAL_LEVEL);
		final Skill skill = mockSkill(true);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			final double none = Formulas.calcMagicDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, false);
			final double sps = Formulas.calcMagicDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, true, false, false);
			final double bss = Formulas.calcMagicDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, true, false);
			assertEquals(none * Math.sqrt(2), sps, 0.0001);
			assertEquals(none * 2, bss, 0.0001);
			assertTrue(none > 1);
		}
	}
	
	@Test
	void magicCritUsesPlayerVersusPlayerFactor()
	{
		final Player attacker = mockPlayerAttacker(EQUAL_LEVEL);
		final Player target = mockPlayerTarget(EQUAL_LEVEL);
		final Skill skill = mockSkill(true);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			final double nonCrit = Formulas.calcMagicDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, false);
			final double crit = Formulas.calcMagicDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, true);
			assertEquals(nonCrit * 2.5 * Modulators.MCRIT_REDUCE, crit, 0.0001);
		}
	}
	
	@Test
	void magicCritUsesNonPlayerFactor()
	{
		final Creature attacker = playableAttacker();
		final Creature target = attackableTarget(EQUAL_LEVEL);
		final Skill skill = mockSkill(true);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			final double nonCrit = Formulas.calcMagicDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, false);
			final double crit = Formulas.calcMagicDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, true);
			assertEquals(nonCrit * 3 * Modulators.MCRIT_REDUCE, crit, 0.0001);
		}
	}
	
	@Test
	void magicHalfDamageFailureOnSecondSuccessRoll()
	{
		final Player attacker = mockPlayerAttacker(EQUAL_LEVEL);
		final Creature target = attackableTarget(EQUAL_LEVEL);
		final Skill skill = mockSkill(true);
		when(skill.hasEffectType(EffectType.HP_DRAIN)).thenReturn(false);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			final double success = Formulas.calcMagicDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, false);
			
			final AtomicInteger calls = new AtomicInteger();
			rnd.when(() -> Rnd.get(100)).thenAnswer(_ -> calls.getAndIncrement() == 0 ? 99 : 0);
			final double half = Formulas.calcMagicDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, false);
			assertEquals(success / 2, half, 0.0001);
			verify(attacker).sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
		}
	}
	
	@Test
	void magicDrainHalfDamageSendsDrainPacket()
	{
		final Player attacker = mockPlayerAttacker(EQUAL_LEVEL);
		final Creature target = attackableTarget(EQUAL_LEVEL);
		final Skill skill = mockSkill(true);
		when(skill.hasEffectType(EffectType.HP_DRAIN)).thenReturn(true);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			final AtomicInteger calls = new AtomicInteger();
			rnd.when(() -> Rnd.get(100)).thenAnswer(_ -> calls.getAndIncrement() == 0 ? 99 : 0);
			Formulas.calcMagicDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, false);
			verify(attacker).sendPacket(SystemMessageId.DRAIN_WAS_ONLY_50_PERCENT_SUCCESSFUL);
		}
	}
	
	@Test
	void magicFullResistSetsDamageToOne()
	{
		final Player attacker = mockPlayerAttacker(EQUAL_LEVEL);
		when(attacker.getName()).thenReturn("Attacker");
		final Creature target = attackableTarget(EQUAL_LEVEL);
		when(target.getName()).thenReturn("Target");
		final Skill skill = mockSkill(true);
		when(skill.getId()).thenReturn(1001);
		when(skill.getLevel()).thenReturn(1);
		when(skill.getDisplayId()).thenReturn(1001);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(99);
			assertEquals(1, Formulas.calcMagicDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, false));
			
			final ArgumentCaptor<SystemMessage> captor = ArgumentCaptor.forClass(SystemMessage.class);
			verify(attacker).sendPacket(captor.capture());
			assertEquals(SystemMessageId.C1_HAS_RESISTED_YOUR_S2, captor.getValue().getSystemMessageId());
		}
	}
	
	@Test
	void magicNonPlayerAttackerKeepsDamageAndNotifiesPlayerTarget()
	{
		final Creature attacker = playableAttacker();
		when(attacker.isPlayer()).thenReturn(false);
		when(attacker.getName()).thenReturn("Npc");
		final Player target = mockPlayerTarget(EQUAL_LEVEL);
		final Skill skill = mockSkill(true);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			final double success = Formulas.calcMagicDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, false);
			rnd.when(() -> Rnd.get(100)).thenReturn(99);
			final double failed = Formulas.calcMagicDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, false);
			assertEquals(success, failed, 0.0001);
			
			final ArgumentCaptor<SystemMessage> captor = ArgumentCaptor.forClass(SystemMessage.class);
			verify(target).sendPacket(captor.capture());
			assertEquals(SystemMessageId.YOU_RESISTED_C1_S_MAGIC, captor.getValue().getSystemMessageId());
		}
	}
	
	@Test
	void magicNpcSkillPenaltyUsesInRangeIndex()
	{
		final Player attacker = mockPlayerAttacker(EQUAL_LEVEL);
		final Creature target = attackableTarget(EQUAL_LEVEL + 3);
		final Skill skill = mockSkill(true);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			final double gapped = Formulas.calcMagicDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, false);
			final Creature sameLevel = attackableTarget(EQUAL_LEVEL);
			final double baseline = Formulas.calcMagicDam(attacker, sameLevel, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, false);
			// levelDiff = targetLevel - attackerLevel - 1 = 2
			assertEquals(baseline * Config.NPC_SKILL_DMG_PENALTY[2], gapped, 0.0001);
		}
	}
	
	@Test
	void magicShieldSuccessAddsShieldDefence()
	{
		final Creature attacker = playableAttacker();
		final Creature target = attackableTarget(EQUAL_LEVEL);
		when(target.getShldDef()).thenReturn(SHIELD_DEF);
		final Skill skill = mockSkill(true);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			final double withShield = Formulas.calcMagicDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_SUCCEED, false, false, false);
			final double withoutShield = Formulas.calcMagicDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, false);
			assertTrue(withShield < withoutShield);
		}
	}
	
	@Test
	void cubicMagicCritTriplesDamage()
	{
		final Player owner = mockPlayerAttacker(EQUAL_LEVEL);
		final Cubic cubic = mock(Cubic.class);
		when(cubic.getOwner()).thenReturn(owner);
		when(cubic.getCubicPower()).thenReturn(500);
		final Creature target = attackableTarget(EQUAL_LEVEL);
		final Skill skill = mockSkill(true);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			final double nonCrit = Formulas.calcMagicDam(cubic, target, skill, false, Formulas.SHIELD_DEFENSE_FAILED);
			final double crit = Formulas.calcMagicDam(cubic, target, skill, true, Formulas.SHIELD_DEFENSE_FAILED);
			assertEquals(nonCrit * 3, crit, 0.0001);
			assertTrue(nonCrit > 1);
		}
	}
	
	@Test
	void cubicMagicHalfDamageFailure()
	{
		final Player owner = mockPlayerAttacker(EQUAL_LEVEL);
		final Cubic cubic = mock(Cubic.class);
		when(cubic.getOwner()).thenReturn(owner);
		when(cubic.getCubicPower()).thenReturn(500);
		final Creature target = attackableTarget(EQUAL_LEVEL);
		final Skill skill = mockSkill(true);
		when(skill.hasEffectType(EffectType.HP_DRAIN)).thenReturn(false);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			final double success = Formulas.calcMagicDam(cubic, target, skill, false, Formulas.SHIELD_DEFENSE_FAILED);
			final AtomicInteger calls = new AtomicInteger();
			rnd.when(() -> Rnd.get(100)).thenAnswer(_ -> calls.getAndIncrement() == 0 ? 99 : 0);
			when(skill.getMagicLevel()).thenReturn(EQUAL_LEVEL);
			final double half = Formulas.calcMagicDam(cubic, target, skill, false, Formulas.SHIELD_DEFENSE_FAILED);
			assertEquals(success / 2, half, 0.0001);
			verify(owner).sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
		}
	}
	
	@Test
	void cubicMagicFullResistSetsDamageToOne()
	{
		final Player owner = mockPlayerAttacker(EQUAL_LEVEL);
		when(owner.getName()).thenReturn("Owner");
		final Cubic cubic = mock(Cubic.class);
		when(cubic.getOwner()).thenReturn(owner);
		when(cubic.getCubicPower()).thenReturn(500);
		final Creature target = attackableTarget(EQUAL_LEVEL);
		when(target.getName()).thenReturn("Target");
		final Skill skill = mockSkill(true);
		when(skill.getId()).thenReturn(1001);
		when(skill.getLevel()).thenReturn(1);
		when(skill.getDisplayId()).thenReturn(1001);
		when(skill.getMagicLevel()).thenReturn(EQUAL_LEVEL);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(99);
			assertEquals(1, Formulas.calcMagicDam(cubic, target, skill, false, Formulas.SHIELD_DEFENSE_FAILED));
		}
	}
	
	@Test
	void cubicMagicNullOwnerAgainstAttackableThrows()
	{
		final Cubic cubic = mock(Cubic.class);
		when(cubic.getOwner()).thenReturn(null);
		when(cubic.getCubicPower()).thenReturn(500);
		final Creature target = attackableTarget(EQUAL_LEVEL);
		final Skill skill = mockSkill(true);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			assertThrows(NullPointerException.class, () -> Formulas.calcMagicDam(cubic, target, skill, false, Formulas.SHIELD_DEFENSE_FAILED));
		}
	}
	
	@Test
	void cubicMagicNpcPenaltyUsesLastIndexWhenClamped()
	{
		final Player owner = mockPlayerAttacker(EQUAL_LEVEL);
		final Cubic cubic = mock(Cubic.class);
		when(cubic.getOwner()).thenReturn(owner);
		when(cubic.getCubicPower()).thenReturn(500);
		// Gap of 5 keeps magic-success rate positive while levelDiff (4) clamps to the last penalty index.
		final Creature target = attackableTarget(EQUAL_LEVEL + 5);
		final Skill skill = mockSkill(true);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			final double gapped = Formulas.calcMagicDam(cubic, target, skill, false, Formulas.SHIELD_DEFENSE_FAILED);
			final Creature sameLevel = attackableTarget(EQUAL_LEVEL);
			final double baseline = Formulas.calcMagicDam(cubic, sameLevel, skill, false, Formulas.SHIELD_DEFENSE_FAILED);
			assertEquals(baseline * Config.NPC_SKILL_DMG_PENALTY[Config.NPC_SKILL_DMG_PENALTY.length - 1], gapped, 0.0001);
		}
	}
	
	@Test
	void manaSpiritShotScalesAndCritTriples()
	{
		final Creature attacker = playableAttacker();
		final Creature target = attackableTarget(EQUAL_LEVEL);
		when(target.getMaxMp()).thenReturn(970);
		final Skill skill = mockSkill(true);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			final double none = Formulas.calcManaDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, false);
			final double sps = Formulas.calcManaDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, true, false, false);
			final double bss = Formulas.calcManaDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, true, false);
			final double crit = Formulas.calcManaDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, true);
			assertEquals(none * Math.sqrt(2), sps, 0.0001);
			assertEquals(none * 2, bss, 0.0001);
			assertEquals(none * 3, crit, 0.0001);
			assertTrue(none > 1);
			verify(attacker).sendPacket(SystemMessageId.MAGIC_CRITICAL_HIT);
		}
	}
	
	@Test
	void manaFailureHalvesForPlayerAttackerWithoutSecondRoll()
	{
		final Player attacker = mockPlayerAttacker(EQUAL_LEVEL);
		when(attacker.getName()).thenReturn("Attacker");
		final Creature target = attackableTarget(EQUAL_LEVEL);
		when(target.getMaxMp()).thenReturn(970);
		when(target.getName()).thenReturn("Target");
		final Skill skill = mockSkill(true);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			final double success = Formulas.calcManaDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, false);
			rnd.when(() -> Rnd.get(100)).thenReturn(99);
			final double failed = Formulas.calcManaDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, false);
			assertEquals(success / 2, failed, 0.0001);
		}
	}
	
	@Test
	void manaShieldSuccessAddsShieldDefence()
	{
		final Creature attacker = playableAttacker();
		final Creature target = attackableTarget(EQUAL_LEVEL);
		when(target.getMaxMp()).thenReturn(970);
		when(target.getShldDef()).thenReturn(SHIELD_DEF);
		final Skill skill = mockSkill(true);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			final double withShield = Formulas.calcManaDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_SUCCEED, false, false, false);
			final double withoutShield = Formulas.calcManaDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, false);
			assertTrue(withShield < withoutShield);
		}
	}
	
	@Test
	void manaNpcSkillPenaltyUsesInRangeIndex()
	{
		final Player attacker = mockPlayerAttacker(EQUAL_LEVEL);
		final Creature target = attackableTarget(EQUAL_LEVEL + 3);
		when(target.getMaxMp()).thenReturn(970);
		final Skill skill = mockSkill(true);
		
		try (MockedStatic<Rnd> rnd = mockStatic(Rnd.class))
		{
			rnd.when(() -> Rnd.get(100)).thenReturn(0);
			final double gapped = Formulas.calcManaDam(attacker, target, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, false);
			final Creature sameLevel = attackableTarget(EQUAL_LEVEL);
			when(sameLevel.getMaxMp()).thenReturn(970);
			final double baseline = Formulas.calcManaDam(attacker, sameLevel, skill, Formulas.SHIELD_DEFENSE_FAILED, false, false, false);
			// levelDiff = targetLevel - attackerLevel - 1 = 2
			assertEquals(baseline * Config.NPC_SKILL_DMG_PENALTY[2], gapped, 0.0001);
		}
	}
	
	@Test
	void physPvpUsesPhysicalDefenceAndDamageStats()
	{
		final Creature attacker = playableAttacker();
		when(attacker.calcStat(eq(Stat.PVP_PHYSICAL_DMG), anyDouble(), any(), any())).thenReturn(1.5d);
		final Player target = mockPlayerTarget(EQUAL_LEVEL);
		when(target.calcStat(eq(Stat.PVP_PHYSICAL_DEF), anyDouble(), any(), any())).thenReturn(2d);
		stubFront(attacker, target);
		
		final double pvp = Formulas.calcPhysDam(attacker, target, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		final Creature pveTarget = attackableTarget(EQUAL_LEVEL);
		stubFront(attacker, pveTarget);
		final double pve = Formulas.calcPhysDam(attacker, pveTarget, null, Formulas.SHIELD_DEFENSE_FAILED, false, false);
		assertEquals(pve * 1.5 / 2, pvp, 0.0001);
	}
	
	static Stream<Arguments> weaponArmorCases()
	{
		return Stream.of(
			Arguments.of(WeaponType.SWORD, true, false, false, Modulators.SWORD_VS_HEAVY),
			Arguments.of(WeaponType.SWORD, false, true, false, Modulators.SWORD_VS_LIGHT),
			Arguments.of(WeaponType.SWORD, false, false, true, Modulators.SWORD_VS_ROBE),
			Arguments.of(WeaponType.BLUNT, true, false, false, Modulators.BLUNT_VS_HEAVY),
			Arguments.of(WeaponType.BLUNT, false, true, false, Modulators.BLUNT_VS_LIGHT),
			Arguments.of(WeaponType.BLUNT, false, false, true, Modulators.BLUNT_VS_ROBE),
			Arguments.of(WeaponType.DAGGER, true, false, false, Modulators.DAGGER_VS_HEAVY),
			Arguments.of(WeaponType.DAGGER, false, true, false, Modulators.DAGGER_VS_LIGHT),
			Arguments.of(WeaponType.DAGGER, false, false, true, Modulators.DAGGER_VS_ROBE),
			Arguments.of(WeaponType.BOW, true, false, false, Modulators.BOW_VS_HEAVY),
			Arguments.of(WeaponType.BOW, false, true, false, Modulators.BOW_VS_LIGHT),
			Arguments.of(WeaponType.BOW, false, false, true, Modulators.BOW_VS_ROBE),
			Arguments.of(WeaponType.POLE, true, false, false, Modulators.POLE_VS_HEAVY),
			Arguments.of(WeaponType.POLE, false, true, false, Modulators.POLE_VS_LIGHT),
			Arguments.of(WeaponType.POLE, false, false, true, Modulators.POLE_VS_ROBE),
			Arguments.of(WeaponType.DUAL, true, false, false, Modulators.DUAL_VS_HEAVY),
			Arguments.of(WeaponType.DUAL, false, true, false, Modulators.DUAL_VS_LIGHT),
			Arguments.of(WeaponType.DUAL, false, false, true, Modulators.DUAL_VS_ROBE),
			Arguments.of(WeaponType.DUALFIST, true, false, false, Modulators.FIST_VS_HEAVY),
			Arguments.of(WeaponType.DUALFIST, false, true, false, Modulators.FIST_VS_LIGHT),
			Arguments.of(WeaponType.DUALFIST, false, false, true, Modulators.FIST_VS_ROBE),
			Arguments.of(WeaponType.RAPIER, true, false, false, Modulators.RAPIER_VS_HEAVY),
			Arguments.of(WeaponType.RAPIER, false, true, false, Modulators.RAPIER_VS_LIGHT),
			Arguments.of(WeaponType.RAPIER, false, false, true, Modulators.RAPIER_VS_ROBE),
			Arguments.of(WeaponType.ANCIENTSWORD, true, false, false, Modulators.ANCIENTSWORD_VS_HEAVY),
			Arguments.of(WeaponType.ANCIENTSWORD, false, true, false, Modulators.ANCIENTSWORD_VS_LIGHT),
			Arguments.of(WeaponType.ANCIENTSWORD, false, false, true, Modulators.ANCIENTSWORD_VS_ROBE),
			Arguments.of(WeaponType.CROSSBOW, true, false, false, Modulators.CROSSBOW_VS_HEAVY),
			Arguments.of(WeaponType.CROSSBOW, false, true, false, Modulators.CROSSBOW_VS_LIGHT),
			Arguments.of(WeaponType.CROSSBOW, false, false, true, Modulators.CROSSBOW_VS_ROBE),
			Arguments.of(WeaponType.DUALDAGGER, true, false, false, Modulators.DUALDAGGER_VS_HEAVY),
			Arguments.of(WeaponType.DUALDAGGER, false, true, false, Modulators.DUALDAGGER_VS_LIGHT),
			Arguments.of(WeaponType.DUALDAGGER, false, false, true, Modulators.DUALDAGGER_VS_ROBE));
	}
	
	private static Creature playableAttacker()
	{
		final Creature attacker = mockCreature(true, false);
		when(attacker.getPAtk(any())).thenReturn(P_ATK);
		when(attacker.getMAtk(any(), any())).thenReturn(M_ATK);
		when(attacker.getLevel()).thenReturn(EQUAL_LEVEL);
		when(attacker.asPlayer()).thenReturn(null);
		return attacker;
	}
	
	private static Player mockPlayerAttacker(int level)
	{
		final Player attacker = mock(Player.class);
		stubCreatureStats(attacker, true, true, PlayerStat.class);
		when(attacker.getPAtk(any())).thenReturn(P_ATK);
		when(attacker.getMAtk(any(), any())).thenReturn(M_ATK);
		when(attacker.getLevel()).thenReturn(level);
		when(attacker.asPlayer()).thenReturn(attacker);
		when(attacker.isTransformed()).thenReturn(false);
		return attacker;
	}
	
	private static Creature attackableTarget(int level)
	{
		final Creature target = mockCreature(false, false);
		when(target.isAttackable()).thenReturn(true);
		when(target.getPDef(any())).thenReturn(P_DEF);
		when(target.getMDef(any(), any())).thenReturn(M_DEF);
		when(target.getLevel()).thenReturn(level);
		when(target.isRaid()).thenReturn(false);
		when(target.isRaidMinion()).thenReturn(false);
		when(target.getShldDef()).thenReturn(0);
		return target;
	}
	
	private static Player mockPlayerTarget(int level)
	{
		final Player target = mock(Player.class);
		stubCreatureStats(target, true, true, PlayerStat.class);
		when(target.getPDef(any())).thenReturn(P_DEF);
		when(target.getMDef(any(), any())).thenReturn(M_DEF);
		when(target.getLevel()).thenReturn(level);
		when(target.isAttackable()).thenReturn(false);
		when(target.isRaid()).thenReturn(false);
		when(target.isRaidMinion()).thenReturn(false);
		when(target.getShldDef()).thenReturn(0);
		when(target.getActiveChestArmorItem()).thenReturn(null);
		return target;
	}
	
	private static Creature mockCreature(boolean playable, boolean player)
	{
		final Creature creature = mock(Creature.class);
		stubCreatureStats(creature, playable, player, CreatureStat.class);
		return creature;
	}
	
	private static void stubCreatureStats(Creature creature, boolean playable, boolean player, Class<? extends CreatureStat> statType)
	{
		final CreatureStat stat = mock(statType);
		final float[] defenceTraits = new float[TraitType.values().length];
		java.util.Arrays.fill(defenceTraits, 1.0f);
		when(creature.getStat()).thenReturn(stat);
		when(stat.getDefenceTraits()).thenReturn(defenceTraits);
		when(stat.hasAttackTrait(any())).thenReturn(false);
		when(stat.hasDefenceTrait(any())).thenReturn(false);
		when(stat.isTraitInvul(any())).thenReturn(false);
		when(stat.calcStat(any(Stat.class), anyDouble())).thenAnswer(invocation -> invocation.getArgument(1));
		when(stat.calcStat(any(Stat.class), anyDouble(), any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
		when(creature.calcStat(any(Stat.class), anyDouble())).thenAnswer(invocation -> invocation.getArgument(1));
		when(creature.calcStat(any(Stat.class), anyDouble(), any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
		when(creature.getRandomDamageMultiplier()).thenReturn(1d);
		when(creature.getAttackType()).thenReturn(WeaponType.SWORD);
		when(creature.isPlayable()).thenReturn(playable);
		when(creature.isPlayer()).thenReturn(player);
		when(creature.isAttackable()).thenReturn(false);
		when(creature.getAttackElement()).thenReturn((byte) 0);
		when(creature.getAttackElementValue(org.mockito.ArgumentMatchers.anyByte())).thenReturn(0);
		when(creature.getDefenseElementValue(org.mockito.ArgumentMatchers.anyByte())).thenReturn(0);
		when(creature.getActiveWeaponItem()).thenReturn(null);
	}
	
	private static Skill mockSkill(boolean magic)
	{
		final Skill skill = mock(Skill.class);
		when(skill.getPower()).thenReturn(SKILL_POWER);
		when(skill.getPower(any(Boolean.class), any(Boolean.class))).thenReturn(SKILL_POWER);
		when(skill.getPower(any(), any(), any(Boolean.class), any(Boolean.class))).thenReturn(SKILL_POWER);
		when(skill.isMagic()).thenReturn(magic);
		when(skill.getTraitType()).thenReturn(TraitType.NONE);
		when(skill.getElement()).thenReturn((byte) -1);
		when(skill.getElementPower()).thenReturn(0);
		when(skill.getMagicLevel()).thenReturn(EQUAL_LEVEL);
		when(skill.hasEffectType(any(EffectType.class))).thenReturn(false);
		when(skill.hasEffectType(any(EffectType.class), any(EffectType[].class))).thenReturn(false);
		return skill;
	}
	
	private static void stubFront(Creature attacker, Creature target)
	{
		when(attacker.isBehind(target)).thenReturn(false);
		when(attacker.isInFrontOf(target)).thenReturn(true);
	}
}
