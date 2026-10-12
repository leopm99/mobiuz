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
package com.l2journey.gameserver.model.actor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import com.l2journey.gameserver.network.serverpackets.InventoryUpdate;

/**
 * Offline players must not accumulate inventory updates in the coalesced map.
 * A {@link Answers#CALLS_REAL_METHODS} mock has a null client, so {@code isInOfflineMode()} is true.
 * The early return must run before {@code getItemEntries()} feeds {@code putAll}.
 */
class PlayerInventoryUpdateTest
{
	@Test
	void offlineSendInventoryUpdateDoesNotWriteCoalescedMap()
	{
		Player player = mock(Player.class, Answers.CALLS_REAL_METHODS);
		InventoryUpdate incoming = mock(InventoryUpdate.class);
		
		player.sendInventoryUpdate(incoming);
		
		verify(incoming, never()).getItemEntries();
	}
}
