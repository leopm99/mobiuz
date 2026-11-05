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
package com.l2journey.gameserver.network.serverpackets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.l2journey.commons.network.WritableBuffer;
import com.l2journey.gameserver.model.ItemInfo;
import com.l2journey.gameserver.network.GameClient;

/**
 * Null-safe item template writes in {@link AbstractItemPacket}.
 */
class AbstractItemPacketNullGuardTest
{
	@Test
	void writeItemReturnsWhenTemplateMissing()
	{
		final ItemInfo item = mock(ItemInfo.class);
		when(item.getItem()).thenReturn(null);
		final WritableBuffer buffer = mock(WritableBuffer.class);
		final ProbePacket packet = new ProbePacket();
		
		assertDoesNotThrow(() -> packet.writeItemPublic(item, buffer));
		verify(buffer, never()).writeInt(anyInt());
	}
	
	private static final class ProbePacket extends AbstractItemPacket
	{
		void writeItemPublic(ItemInfo item, WritableBuffer buffer)
		{
			writeItem(item, buffer);
		}
		
		@Override
		public void writeImpl(GameClient client, WritableBuffer buffer)
		{
			// Unused.
		}
	}
}
