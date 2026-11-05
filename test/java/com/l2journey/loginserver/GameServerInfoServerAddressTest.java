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
package com.l2journey.loginserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

import com.l2journey.loginserver.GameServerTable.GameServerInfo;

/**
 * Subnet membership lookup for {@link GameServerInfo#getServerAddress} (java:S2159).
 */
class GameServerInfoServerAddressTest
{
	private static final byte[] HEX_ID = new byte[]
	{
		1,
		2,
		3,
		4
	};
	
	@Test
	void returnsHostWhenClientIsInSubnet() throws UnknownHostException
	{
		final GameServerInfo gsi = new GameServerInfo(1, HEX_ID);
		gsi.addServerAddress("192.168.0.0/16", "203.0.113.10");
		
		assertEquals("203.0.113.10", gsi.getServerAddress(InetAddress.getByName("192.168.1.10")));
	}
	
	@Test
	void returnsNullWhenClientIsOutsideSubnet() throws UnknownHostException
	{
		final GameServerInfo gsi = new GameServerInfo(1, HEX_ID);
		gsi.addServerAddress("192.168.0.0/16", "203.0.113.10");
		
		assertNull(gsi.getServerAddress(InetAddress.getByName("10.0.0.1")));
	}
	
	@Test
	void returnsFirstMatchingSubnetHost() throws UnknownHostException
	{
		final GameServerInfo gsi = new GameServerInfo(1, HEX_ID);
		gsi.addServerAddress("0.0.0.0/0", "198.51.100.1");
		gsi.addServerAddress("192.168.1.0/24", "203.0.113.10");
		
		assertEquals("198.51.100.1", gsi.getServerAddress(InetAddress.getByName("192.168.1.10")));
	}
	
	@Test
	void returnsNullForNullAddress() throws UnknownHostException
	{
		final GameServerInfo gsi = new GameServerInfo(1, HEX_ID);
		gsi.addServerAddress("192.168.0.0/16", "203.0.113.10");
		
		assertNull(gsi.getServerAddress(null));
	}
}
