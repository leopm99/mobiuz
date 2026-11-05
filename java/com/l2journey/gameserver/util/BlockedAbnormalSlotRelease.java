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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import com.l2journey.gameserver.model.skill.AbnormalType;

/**
 * Computes which blocked abnormal slots may leave when an effect exits.
 */
public final class BlockedAbnormalSlotRelease
{
	private BlockedAbnormalSlotRelease()
	{
	}
	
	/**
	 * Returns the slots that should be unblocked when an effect exits.
	 * @param exitingSlots the slots the exiting effect blocked
	 * @param remainingSlotSets slot sets still held by other active effects
	 * @return a new set of slots to release; never the caller's input set
	 */
	public static Set<AbnormalType> slotsToRelease(Set<AbnormalType> exitingSlots, Iterable<? extends Set<AbnormalType>> remainingSlotSets)
	{
		if ((exitingSlots == null) || exitingSlots.isEmpty())
		{
			return Collections.emptySet();
		}
		
		final Set<AbnormalType> slots = EnumSet.copyOf(exitingSlots);
		if (remainingSlotSets != null)
		{
			for (Set<AbnormalType> remaining : remainingSlotSets)
			{
				if ((remaining != null) && !remaining.isEmpty())
				{
					slots.removeAll(remaining);
				}
			}
		}
		return slots;
	}
}
