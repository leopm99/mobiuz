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
package com.l2journey.gameserver.model.phantom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.ai.CreatureAI;
import com.l2journey.gameserver.ai.Intention;
import com.l2journey.gameserver.model.EffectList;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.Summon;
import com.l2journey.gameserver.model.actor.enums.player.PlayerClass;
import com.l2journey.gameserver.model.itemcontainer.PlayerInventory;
import com.l2journey.gameserver.model.skill.Skill;
import com.l2journey.gameserver.model.skill.targets.TargetType;

/**
 * Regression tests for phantom mage offensive skill fallback selection.
 */
class PhantomSkillsTest
{
	@AfterEach
	void clearSummonTimers()
	{
		PhantomSkills.unregisterBot(14);
		PhantomSkills.unregisterBot(96);
		PhantomSkills.unregisterBot(77);
	}

	private static final int VOLCANO_ID = 1419;
	private static final int PROMINENCE_ID = 1230;
	
	@Test
	void getBestOffensiveSkillSkipsGroundChannelSkills()
	{
		Skill volcano = mockOffensiveSkill(VOLCANO_ID, 500, 900, TargetType.GROUND, 36);
		Skill prominence = mockOffensiveSkill(PROMINENCE_ID, 102, 900, TargetType.ONE, 80);
		
		Player bot = mock(Player.class);
		when(bot.getAllSkills()).thenReturn(List.of(volcano, prominence));
		when(bot.getCurrentMp()).thenReturn(1000.0);
		when(bot.isSkillDisabled(volcano)).thenReturn(false);
		when(bot.isSkillDisabled(prominence)).thenReturn(true);
		
		assertNull(PhantomSkills.getBestOffensiveSkill(bot));
	}
	
	@Test
	void getBestOffensiveSkillPrefersReadyTargetedNukeOverGround()
	{
		Skill volcano = mockOffensiveSkill(VOLCANO_ID, 500, 900, TargetType.GROUND, 36);
		Skill prominence = mockOffensiveSkill(PROMINENCE_ID, 102, 900, TargetType.ONE, 80);
		
		Player bot = mock(Player.class);
		when(bot.getAllSkills()).thenReturn(List.of(volcano, prominence));
		when(bot.getCurrentMp()).thenReturn(1000.0);
		when(bot.isSkillDisabled(volcano)).thenReturn(false);
		when(bot.isSkillDisabled(prominence)).thenReturn(false);
		
		Skill best = PhantomSkills.getBestOffensiveSkill(bot);
		assertEquals(PROMINENCE_ID, best.getId());
	}

	@Test
	void resolveMaintenanceSkillIdMapsBowCrossbowAndTank()
	{
		assertEquals(99, PhantomSkills.resolveMaintenanceSkillId(9));
		assertEquals(99, PhantomSkills.resolveMaintenanceSkillId(24));
		assertEquals(99, PhantomSkills.resolveMaintenanceSkillId(102));
		assertEquals(482, PhantomSkills.resolveMaintenanceSkillId(126));
		assertEquals(482, PhantomSkills.resolveMaintenanceSkillId(130));
		assertEquals(482, PhantomSkills.resolveMaintenanceSkillId(134));
		assertEquals(982, PhantomSkills.resolveMaintenanceSkillId(99));
		assertEquals(982, PhantomSkills.resolveMaintenanceSkillId(5));
		assertEquals(0, PhantomSkills.resolveMaintenanceSkillId(0));
	}

	@Test
	void newFighterProfilesResolveKnownAttackSkills()
	{
		Skill fist = mock(Skill.class);
		when(fist.getId()).thenReturn(120);
		when(fist.getMpConsume()).thenReturn(10);
		Player monk = mock(Player.class);
		when(monk.isMageClass()).thenReturn(false);
		when(monk.getPlayerClass()).thenReturn(PlayerClass.ORC_MONK);
		when(monk.getKnownSkill(120)).thenReturn(fist);
		when(monk.isSkillDisabled(fist)).thenReturn(false);
		when(monk.getCurrentMp()).thenReturn(100.0);
		assertEquals(120, PhantomSkills.getFighterAttackSkill(monk).getId());

		Skill shot = mock(Skill.class);
		when(shot.getId()).thenReturn(487);
		when(shot.getMpConsume()).thenReturn(10);
		Player warder = mock(Player.class);
		when(warder.isMageClass()).thenReturn(false);
		when(warder.getPlayerClass()).thenReturn(PlayerClass.WARDER);
		when(warder.getKnownSkill(487)).thenReturn(shot);
		when(warder.isSkillDisabled(shot)).thenReturn(false);
		when(warder.getCurrentMp()).thenReturn(100.0);
		assertEquals(487, PhantomSkills.getFighterAttackSkill(warder).getId());

		Skill thrust = mock(Skill.class);
		when(thrust.getId()).thenReturn(504);
		when(thrust.getMpConsume()).thenReturn(10);
		Player inspector = mock(Player.class);
		when(inspector.isMageClass()).thenReturn(false);
		when(inspector.getPlayerClass()).thenReturn(PlayerClass.INSPECTOR);
		when(inspector.getKnownSkill(504)).thenReturn(thrust);
		when(inspector.isSkillDisabled(thrust)).thenReturn(false);
		when(inspector.getCurrentMp()).thenReturn(100.0);
		assertEquals(504, PhantomSkills.getFighterAttackSkill(inspector).getId());
	}

	@Test
	void handleSelfBuffsRejectsUnavailableStates()
	{
		assertFalse(PhantomSkills.handleSelfBuffs(null));

		Player dead = mock(Player.class);
		when(dead.isDead()).thenReturn(true);
		assertFalse(PhantomSkills.handleSelfBuffs(dead));

		Player casting = mock(Player.class);
		when(casting.isDead()).thenReturn(false);
		when(casting.isCastingNow()).thenReturn(true);
		assertFalse(PhantomSkills.handleSelfBuffs(casting));

		Player fighter = mock(Player.class);
		when(fighter.isDead()).thenReturn(false);
		when(fighter.isCastingNow()).thenReturn(false);
		when(fighter.getPlayerClass()).thenReturn(PlayerClass.FIGHTER);
		assertFalse(PhantomSkills.handleSelfBuffs(fighter));
	}

	@Test
	void handleSelfBuffsCastsMaintenanceSkillWhenReady()
	{
		Skill rapidShot = mock(Skill.class);
		when(rapidShot.getMpConsume()).thenReturn(20);
		Player bot = mock(Player.class);
		CreatureAI ai = mock(CreatureAI.class);
		when(bot.isDead()).thenReturn(false);
		when(bot.isCastingNow()).thenReturn(false);
		when(bot.getPlayerClass()).thenReturn(PlayerClass.HAWKEYE);
		when(bot.getKnownSkill(99)).thenReturn(rapidShot);
		when(bot.isAffectedBySkill(99)).thenReturn(false);
		when(bot.isSkillDisabled(rapidShot)).thenReturn(false);
		when(bot.getCurrentMp()).thenReturn(100.0);
		when(bot.getAI()).thenReturn(ai);

		assertTrue(PhantomSkills.handleSelfBuffs(bot));
		verify(bot).stopMove(null);
		verify(ai).setIntention(eq(Intention.CAST), eq(rapidShot), eq(bot));
	}

	@Test
	void handleSelfBuffsSkipsWhenAlreadyBuffedOrLowMp()
	{
		Skill furious = mock(Skill.class);
		when(furious.getMpConsume()).thenReturn(50);
		Player bot = mock(Player.class);
		when(bot.isDead()).thenReturn(false);
		when(bot.isCastingNow()).thenReturn(false);
		when(bot.getPlayerClass()).thenReturn(PlayerClass.WARDER);
		when(bot.getKnownSkill(482)).thenReturn(furious);
		when(bot.isAffectedBySkill(482)).thenReturn(true);
		assertFalse(PhantomSkills.handleSelfBuffs(bot));

		when(bot.isAffectedBySkill(482)).thenReturn(false);
		when(bot.isSkillDisabled(furious)).thenReturn(true);
		assertFalse(PhantomSkills.handleSelfBuffs(bot));

		when(bot.isSkillDisabled(furious)).thenReturn(false);
		when(bot.getCurrentMp()).thenReturn(10.0);
		assertFalse(PhantomSkills.handleSelfBuffs(bot));

		when(bot.getKnownSkill(482)).thenReturn(null);
		assertFalse(PhantomSkills.handleSelfBuffs(bot));
	}

	@Test
	void handleSelfBuffsUsesCombatAuraForTanks()
	{
		Skill aura = mock(Skill.class);
		when(aura.getMpConsume()).thenReturn(10);
		Player bot = mock(Player.class);
		CreatureAI ai = mock(CreatureAI.class);
		when(bot.isDead()).thenReturn(false);
		when(bot.isCastingNow()).thenReturn(false);
		when(bot.getPlayerClass()).thenReturn(PlayerClass.EVA_TEMPLAR);
		when(bot.getKnownSkill(982)).thenReturn(aura);
		when(bot.isAffectedBySkill(982)).thenReturn(false);
		when(bot.isSkillDisabled(aura)).thenReturn(false);
		when(bot.getCurrentMp()).thenReturn(100.0);
		when(bot.getAI()).thenReturn(ai);

		assertTrue(PhantomSkills.handleSelfBuffs(bot));
		verify(ai).setIntention(eq(Intention.CAST), eq(aura), any());
	}

	@Test
	void summonPriorityFollowsClassPets()
	{
		Player warlock = mock(Player.class);
		when(warlock.getActiveClass()).thenReturn(14);
		assertEquals(List.of(1276), PhantomSkills.getSummonPriority(warlock));

		Player arcana = mock(Player.class);
		when(arcana.getActiveClass()).thenReturn(96);
		assertEquals(List.of(1406, 1276), PhantomSkills.getSummonPriority(arcana));

		Player necro = mock(Player.class);
		when(necro.getActiveClass()).thenReturn(13);
		assertEquals(List.of(1129), PhantomSkills.getSummonPriority(necro));

		Player soultaker = mock(Player.class);
		when(soultaker.getActiveClass()).thenReturn(95);
		assertEquals(List.of(1334), PhantomSkills.getSummonPriority(soultaker));

		Player unknown = mock(Player.class);
		when(unknown.getActiveClass()).thenReturn(0);
		assertTrue(PhantomSkills.getSummonPriority(unknown).isEmpty());
	}

	@Test
	void inquisitorAttackPrefersSurrenderThenPunishmentNeverFlash()
	{
		Skill surrender = mock(Skill.class);
		when(surrender.getMpConsume()).thenReturn(10);
		when(surrender.getCastRange()).thenReturn(750);
		Skill punishment = mock(Skill.class);
		when(punishment.getMpConsume()).thenReturn(10);
		when(punishment.getCastRange()).thenReturn(750);

		Player bot = mock(Player.class);
		CreatureAI ai = mock(CreatureAI.class);
		Player target = mock(Player.class);
		when(bot.isInStance()).thenReturn(true);
		when(bot.isCastingNow()).thenReturn(false);
		when(bot.getTransformSkill(1524)).thenReturn(surrender);
		when(bot.getTransformSkill(1523)).thenReturn(punishment);
		when(bot.getTransformSkill(1528)).thenReturn(mock(Skill.class));
		when(bot.isSkillDisabled(surrender)).thenReturn(false);
		when(bot.isSkillDisabled(punishment)).thenReturn(false);
		when(bot.getCurrentMp()).thenReturn(500.0);
		when(bot.getAI()).thenReturn(ai);
		when(bot.getX()).thenReturn(0);
		when(bot.getY()).thenReturn(0);
		when(bot.getZ()).thenReturn(0);
		when(target.isDead()).thenReturn(false);
		when(target.isAffectedBySkill(1524)).thenReturn(false);
		when(target.getX()).thenReturn(100);
		when(target.getY()).thenReturn(0);
		when(target.getZ()).thenReturn(0);

		assertTrue(PhantomSkills.tryInquisitorAttack(bot, target));
		verify(ai).setIntention(eq(Intention.CAST), eq(surrender), eq(target));

		when(target.isAffectedBySkill(1524)).thenReturn(true);
		assertTrue(PhantomSkills.tryInquisitorAttack(bot, target));
		verify(ai).setIntention(eq(Intention.CAST), eq(punishment), eq(target));

		when(target.getX()).thenReturn(5000);
		when(target.isAffectedBySkill(1524)).thenReturn(false);
		assertEquals(surrender, PhantomSkills.selectInquisitorAttackSkill(bot, target));
		assertTrue(PhantomSkills.tryInquisitorAttack(bot, target));
	}

	@Test
	void inquisitorAttackFallsThroughWhenStanceSpellsAreMissing()
	{
		Player bot = mock(Player.class);
		CreatureAI ai = mock(CreatureAI.class);
		Player target = mock(Player.class);
		when(bot.isInStance()).thenReturn(true);
		when(bot.isCastingNow()).thenReturn(false);
		when(bot.getAI()).thenReturn(ai);
		when(bot.getTransformSkill(1524)).thenReturn(null);
		when(bot.getTransformSkill(1523)).thenReturn(null);
		when(target.isDead()).thenReturn(false);

		assertFalse(PhantomSkills.tryInquisitorAttack(bot, target));
		verify(ai, never()).setIntention(any(Intention.class), any(), any());
	}

	@Test
	void inquisitorAttackSkipsUnreadyStanceSpell()
	{
		Skill surrender = mock(Skill.class);
		when(surrender.getMpConsume()).thenReturn(40);
		Player bot = mock(Player.class);
		Player target = mock(Player.class);
		when(bot.getTransformSkill(1524)).thenReturn(surrender);
		when(bot.getTransformSkill(1523)).thenReturn(null);
		when(bot.isSkillDisabled(surrender)).thenReturn(false);
		when(bot.getCurrentMp()).thenReturn(10.0);
		when(target.isAffectedBySkill(1524)).thenReturn(false);
		assertNull(PhantomSkills.selectInquisitorAttackSkill(bot, target));

		when(bot.getCurrentMp()).thenReturn(100.0);
		when(bot.isSkillDisabled(surrender)).thenReturn(true);
		assertNull(PhantomSkills.selectInquisitorAttackSkill(bot, target));
		assertNull(PhantomSkills.selectInquisitorAttackSkill(null, target));
	}

	@Test
	void handleInquisitorFormCastsWhenKnownAndNotInStance()
	{
		Skill inquisitor = mock(Skill.class);
		when(inquisitor.getMpConsume()).thenReturn(20);
		Player bot = mock(Player.class);
		CreatureAI ai = mock(CreatureAI.class);
		when(bot.isDead()).thenReturn(false);
		when(bot.isCastingNow()).thenReturn(false);
		when(bot.isInStance()).thenReturn(false);
		when(bot.getKnownSkill(1520)).thenReturn(inquisitor);
		when(bot.isSkillDisabled(inquisitor)).thenReturn(false);
		when(bot.getCurrentMp()).thenReturn(100.0);
		when(bot.getAI()).thenReturn(ai);

		assertTrue(PhantomSkills.handleInquisitorForm(bot));
		verify(ai).setIntention(eq(Intention.CAST), eq(inquisitor), eq(bot));
	}

	@Test
	void handleInquisitorFormSkipsStanceDeadAndMissingSkill()
	{
		assertFalse(PhantomSkills.handleInquisitorForm(null));

		Player inStance = mock(Player.class);
		when(inStance.isDead()).thenReturn(false);
		when(inStance.isCastingNow()).thenReturn(false);
		when(inStance.isInStance()).thenReturn(true);
		assertFalse(PhantomSkills.handleInquisitorForm(inStance));

		Player noSkill = mock(Player.class);
		when(noSkill.isDead()).thenReturn(false);
		when(noSkill.isCastingNow()).thenReturn(false);
		when(noSkill.isInStance()).thenReturn(false);
		when(noSkill.getKnownSkill(1520)).thenReturn(null);
		when(noSkill.getKnownSkill(1521)).thenReturn(null);
		when(noSkill.getKnownSkill(1522)).thenReturn(null);
		assertFalse(PhantomSkills.handleInquisitorForm(noSkill));
	}

	@Test
	void getInquisitorSkillFallsBackAcrossLines()
	{
		Player bot = mock(Player.class);
		Skill elder = mock(Skill.class);
		when(bot.getKnownSkill(1520)).thenReturn(null);
		when(bot.getKnownSkill(1521)).thenReturn(elder);
		when(bot.getKnownSkill(1522)).thenReturn(null);
		assertEquals(elder, PhantomSkills.getInquisitorSkill(bot));
		assertNull(PhantomSkills.getInquisitorSkill(null));
	}

	@Test
	void summonPriorityCoversRemainingSummonClasses()
	{
		Player elemental = mock(Player.class);
		when(elemental.getActiveClass()).thenReturn(28);
		assertEquals(List.of(1277), PhantomSkills.getSummonPriority(elemental));

		Player master = mock(Player.class);
		when(master.getActiveClass()).thenReturn(104);
		assertEquals(List.of(1407, 1277), PhantomSkills.getSummonPriority(master));

		Player phantom = mock(Player.class);
		when(phantom.getActiveClass()).thenReturn(41);
		assertEquals(List.of(1278), PhantomSkills.getSummonPriority(phantom));

		Player spectral = mock(Player.class);
		when(spectral.getActiveClass()).thenReturn(111);
		assertEquals(List.of(1408, 1278), PhantomSkills.getSummonPriority(spectral));

		Player da = mock(Player.class);
		when(da.getActiveClass()).thenReturn(6);
		assertEquals(List.of(283), PhantomSkills.getSummonPriority(da));

		Player phoenix = mock(Player.class);
		when(phoenix.getActiveClass()).thenReturn(90);
		assertEquals(List.of(912), PhantomSkills.getSummonPriority(phoenix));

		Player artisan = mock(Player.class);
		when(artisan.getActiveClass()).thenReturn(56);
		assertEquals(List.of(25), PhantomSkills.getSummonPriority(artisan));
	}
	
	@Test
	void inquisitorAttackHoldsTheTickWithoutACastableSpell()
	{
		Player bot = mock(Player.class);
		when(bot.isInStance()).thenReturn(false);
		assertFalse(PhantomSkills.tryInquisitorAttack(bot, mock(Player.class)));

		when(bot.isInStance()).thenReturn(true);
		assertTrue(PhantomSkills.tryInquisitorAttack(bot, null));

		Player dead = mock(Player.class);
		when(dead.isDead()).thenReturn(true);
		assertTrue(PhantomSkills.tryInquisitorAttack(bot, dead));

		when(bot.isCastingNow()).thenReturn(true);
		Player target = mock(Player.class);
		when(target.isDead()).thenReturn(false);
		assertTrue(PhantomSkills.tryInquisitorAttack(bot, target));
	}

	@Test
	void handleCombatSummonsCastsPreferredPetAndReattacks()
	{
		assertTrue(PhantomSkills.getSummonPriority(null).isEmpty());
		assertNull(PhantomSkills.getPreferredSummonSkill(null));
		assertNull(PhantomSkills.getPreferredSummonSkill(summonBot(14)));
		assertTrue(PhantomSkills.handleCombatSummons(null, null));

		Skill kai = mock(Skill.class);
		when(kai.getMpConsume()).thenReturn(20);
		Player warlock = summonBot(14);
		PlayerInventory inventory = mock(PlayerInventory.class);
		CreatureAI ai = mock(CreatureAI.class);
		when(warlock.getInventory()).thenReturn(inventory);
		when(inventory.getInventoryItemCount(org.mockito.ArgumentMatchers.anyInt(), eq(-1))).thenReturn(500L);
		when(warlock.getKnownSkill(1276)).thenReturn(kai);
		when(warlock.isSkillDisabled(kai)).thenReturn(false);
		when(warlock.getCurrentMp()).thenReturn(200.0);
		when(warlock.getAI()).thenReturn(ai);
		when(warlock.getSummon()).thenReturn(null);
		when(warlock.getCubics()).thenReturn(null);

		assertTrue(PhantomSkills.handleCombatSummons(warlock, null));
		verify(ai).setIntention(eq(Intention.CAST), eq(kai), eq(warlock));
		assertFalse(PhantomSkills.handleCombatSummons(warlock, null));

		Skill feline = mock(Skill.class);
		when(feline.getMpConsume()).thenReturn(20);
		Player arcana = summonBot(96);
		when(arcana.getInventory()).thenReturn(inventory);
		when(arcana.getKnownSkill(1406)).thenReturn(null);
		when(arcana.getKnownSkill(1276)).thenReturn(feline);
		when(arcana.isSkillDisabled(feline)).thenReturn(false);
		when(arcana.getCurrentMp()).thenReturn(200.0);
		when(arcana.getAI()).thenReturn(ai);
		when(arcana.getSummon()).thenReturn(null);
		when(arcana.getCubics()).thenReturn(null);
		assertTrue(PhantomSkills.handleCombatSummons(arcana, null));

		Player owner = summonBot(77);
		Summon summon = mock(Summon.class);
		CreatureAI summonAi = mock(CreatureAI.class);
		EffectList effects = mock(EffectList.class);
		Player target = mock(Player.class);
		Set<Integer> shots = new HashSet<>();
		when(owner.getInventory()).thenReturn(inventory);
		when(owner.getSummon()).thenReturn(summon);
		when(owner.getCubics()).thenReturn(null);
		when(owner.getAutoSoulShot()).thenReturn(shots);
		when(owner.getX()).thenReturn(0);
		when(owner.getY()).thenReturn(0);
		when(owner.getZ()).thenReturn(0);
		when(summon.isDead()).thenReturn(false);
		when(summon.getX()).thenReturn(0);
		when(summon.getY()).thenReturn(0);
		when(summon.getZ()).thenReturn(0);
		when(summon.getEffectList()).thenReturn(effects);
		when(effects.getBuffCount()).thenReturn(5);
		when(summon.getTarget()).thenReturn(null);
		when(summon.isMovementDisabled()).thenReturn(false);
		when(summon.getAI()).thenReturn(summonAi);
		when(summonAi.getIntention()).thenReturn(Intention.IDLE);
		when(summon.isMoving()).thenReturn(false);
		when(summon.isAttackingNow()).thenReturn(false);
		when(summon.isCastingNow()).thenReturn(false);
		when(target.isDead()).thenReturn(false);

		assertFalse(PhantomSkills.handleCombatSummons(owner, target));
		verify(summonAi).setIntention(eq(Intention.ATTACK), eq(target));
		verify(owner).addAutoSoulShot(20332);

		when(summon.getX()).thenReturn(4000);
		assertFalse(PhantomSkills.handleCombatSummons(owner, target));
		verify(summon).teleToLocation(0, 0, 0);
	}

	private static Player summonBot(int objectId)
	{
		Player bot = mock(Player.class);
		when(bot.getObjectId()).thenReturn(objectId);
		when(bot.getActiveClass()).thenReturn(objectId);
		when(bot.isCastingNow()).thenReturn(false);
		when(bot.isAffectedBySkill(5076)).thenReturn(false);
		return bot;
	}

	private static Skill mockOffensiveSkill(int id, double power, int castRange, TargetType targetType, int mpConsume)
	{
		Skill skill = mock(Skill.class);
		when(skill.getId()).thenReturn(id);
		when(skill.isPassive()).thenReturn(false);
		when(skill.isMagic()).thenReturn(true);
		when(skill.getPower()).thenReturn(power);
		when(skill.getCastRange()).thenReturn(castRange);
		when(skill.getTargetType()).thenReturn(targetType);
		when(skill.getMpConsume()).thenReturn(mpConsume);
		return skill;
	}
}
