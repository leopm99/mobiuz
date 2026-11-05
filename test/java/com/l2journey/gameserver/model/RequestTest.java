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
package com.l2journey.gameserver.model;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.network.clientpackets.ClientPacket;

/**
 * Getter/setter synchronization for {@link Request} (java:S2886).
 */
class RequestTest
{
	@Test
	void getPartnerAndGetRequestPacketAreSynchronized() throws Exception
	{
		assertTrue(Modifier.isSynchronized(Request.class.getDeclaredMethod("getPartner").getModifiers()));
		assertTrue(Modifier.isSynchronized(Request.class.getDeclaredMethod("getRequestPacket").getModifiers()));
	}
	
	@Test
	void getPartnerAndGetRequestPacketRoundTripIncludingNull()
	{
		final Request request = new Request(null);
		assertNull(request.getPartner());
		assertNull(request.getRequestPacket());
		
		final Player partner = mock(Player.class);
		final ClientPacket packet = mock(ClientPacket.class);
		request._partner = partner;
		request._requestPacket = packet;
		
		assertSame(partner, request.getPartner());
		assertSame(packet, request.getRequestPacket());
	}
}
