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
 * 
 * ---
 * 
 * Portions of this software are derived from the L2JMobius Project, 
 * shared under the MIT License. The original license terms are preserved where 
 * applicable..
 * 
 */
package com.l2journey.gameserver.network.clientpackets;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import com.l2journey.gameserver.network.PacketLogger;

/**
 * Format: c dddd
 * @author KenM
 */
public class GameGuardReply extends ClientPacket
{
	private static final byte[] VALID =
	{
		(byte) 0x88,
		0x40,
		0x1c,
		(byte) 0xa7,
		(byte) 0x83,
		0x42,
		(byte) 0xe9,
		0x15,
		(byte) 0xde,
		(byte) 0xc3,
		0x68,
		(byte) 0xf6,
		0x2d,
		0x23,
		(byte) 0xf1,
		0x3f,
		(byte) 0xee,
		0x68,
		0x5b,
		(byte) 0xc5,
	};
	
	private final byte[] _reply = new byte[8];
	
	@Override
	protected void readImpl()
	{
		_reply[0] = readByte();
		_reply[1] = readByte();
		_reply[2] = readByte();
		_reply[3] = readByte();
		readInt();
		_reply[4] = readByte();
		_reply[5] = readByte();
		_reply[6] = readByte();
		_reply[7] = readByte();
	}
	
	@Override
	protected void runImpl()
	{
		try
		{
			// Server SHA-1 of the 8-byte client reply vs the 20-byte VALID token; the client does not hash. Another algorithm never matches (#6).
			final MessageDigest md = MessageDigest.getInstance("SHA");
			final byte[] result = md.digest(_reply);
			if (Arrays.equals(result, VALID))
			{
				getClient().setGameGuardOk(true);
			}
		}
		catch (NoSuchAlgorithmException e)
		{
			PacketLogger.warning(getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
}
