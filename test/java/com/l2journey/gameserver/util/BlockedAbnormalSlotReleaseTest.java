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
package com.l2journey.gameserver.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.skill.AbnormalType;

/**
 * Unit tests for {@link BlockedAbnormalSlotRelease}.
 */
class BlockedAbnormalSlotReleaseTest
{
	@Test
	void releasesAllWhenNothingElseBlocks()
	{
		final Set<AbnormalType> exiting = EnumSet.of(AbnormalType.AVOID_UP, AbnormalType.ATTACK_TIME_DOWN);
		final Set<AbnormalType> released = BlockedAbnormalSlotRelease.slotsToRelease(exiting, List.of());
		assertEquals(EnumSet.of(AbnormalType.AVOID_UP, AbnormalType.ATTACK_TIME_DOWN), released);
		assertEquals(EnumSet.of(AbnormalType.AVOID_UP, AbnormalType.ATTACK_TIME_DOWN), exiting);
	}
	
	@Test
	void releasesOnlySlotsNotHeldByOthers()
	{
		final Set<AbnormalType> exiting = EnumSet.of(AbnormalType.AVOID_UP, AbnormalType.ATTACK_TIME_DOWN);
		final Set<AbnormalType> remaining = EnumSet.of(AbnormalType.AVOID_UP);
		final Set<AbnormalType> released = BlockedAbnormalSlotRelease.slotsToRelease(exiting, List.of(remaining));
		assertEquals(EnumSet.of(AbnormalType.ATTACK_TIME_DOWN), released);
		assertEquals(EnumSet.of(AbnormalType.AVOID_UP, AbnormalType.ATTACK_TIME_DOWN), exiting);
		assertEquals(EnumSet.of(AbnormalType.AVOID_UP), remaining);
	}
	
	@Test
	void releasesNothingWhenAnotherEffectHoldsSameSlots()
	{
		final Set<AbnormalType> exiting = EnumSet.of(AbnormalType.AVOID_UP, AbnormalType.ATTACK_TIME_DOWN);
		final Set<AbnormalType> remaining = EnumSet.of(AbnormalType.AVOID_UP, AbnormalType.ATTACK_TIME_DOWN);
		final Set<AbnormalType> released = BlockedAbnormalSlotRelease.slotsToRelease(exiting, List.of(remaining));
		assertTrue(released.isEmpty());
		assertEquals(EnumSet.of(AbnormalType.AVOID_UP, AbnormalType.ATTACK_TIME_DOWN), exiting);
	}
	
	@Test
	void emptyExitingSetReleasesNothing()
	{
		assertTrue(BlockedAbnormalSlotRelease.slotsToRelease(EnumSet.noneOf(AbnormalType.class), List.of()).isEmpty());
		assertTrue(BlockedAbnormalSlotRelease.slotsToRelease(null, List.of()).isEmpty());
	}
}
