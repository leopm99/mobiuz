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
package com.l2journey.commons.crypt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Round-trip and helper characterization for {@link NewCrypt}.
 */
class NewCryptTest
{
	private static final byte[] KEY_BYTES = "0123456789ABCDEF".getBytes();
	private static final String KEY_STRING = "0123456789ABCDEF";
	
	/** Captured from current {@link NewCrypt#encXORPass(byte[], int)} with input 1..16 and key 0x12345678. */
	private static final byte[] ENC_XOR_GOLDEN = new byte[]
	{
		1, 2, 3, 4, 120, 90, 60, 18, 125, 92, 59, 26, 13, 14, 15, 16
	};
	
	@Test
	void cryptDecryptByteKeyRoundTrip()
	{
		final NewCrypt crypt = new NewCrypt(KEY_BYTES);
		final byte[] data = multiBlockWithTail();
		final byte[] original = Arrays.copyOf(data, data.length);
		
		crypt.crypt(data, 2, 16);
		assertFalse(Arrays.equals(original, data));
		assertEqualsTail(original, data);
		
		crypt.decrypt(data, 2, 16);
		assertArrayEquals(original, data);
	}
	
	@Test
	void cryptDecryptStringKeyRoundTrip()
	{
		final NewCrypt crypt = new NewCrypt(KEY_STRING);
		final byte[] data = multiBlockWithTail();
		final byte[] original = Arrays.copyOf(data, data.length);
		
		crypt.crypt(data, 2, 16);
		assertFalse(Arrays.equals(original, data));
		assertEqualsTail(original, data);
		
		crypt.decrypt(data, 2, 16);
		assertArrayEquals(original, data);
	}
	
	@Test
	void appendThenVerifyChecksum()
	{
		final byte[] raw = new byte[]
		{
			1, 2, 3, 4, 5, 6, 7, 8, 0, 0, 0, 0
		};
		NewCrypt.appendChecksum(raw);
		assertTrue(NewCrypt.verifyChecksum(raw));
		
		raw[0] ^= 0x01;
		assertFalse(NewCrypt.verifyChecksum(raw));
	}
	
	@Test
	void verifyChecksumRejectsBadSize()
	{
		assertFalse(NewCrypt.verifyChecksum(new byte[]
		{
			1, 2, 3
		}));
		assertFalse(NewCrypt.verifyChecksum(new byte[]
		{
			1, 2, 3, 4
		}));
		assertFalse(NewCrypt.verifyChecksum(new byte[8], 0, 6));
	}
	
	@Test
	void encXorPassMatchesGoldenVector()
	{
		final byte[] raw = new byte[]
		{
			1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16
		};
		NewCrypt.encXORPass(raw, 0x12345678);
		assertArrayEquals(ENC_XOR_GOLDEN, raw);
	}
	
	private static byte[] multiBlockWithTail()
	{
		final byte[] data = new byte[22];
		for (int i = 0; i < data.length; i++)
		{
			data[i] = (byte) (i + 1);
		}
		return data;
	}
	
	private static void assertEqualsTail(byte[] original, byte[] data)
	{
		assertArrayEquals(Arrays.copyOfRange(original, 18, original.length), Arrays.copyOfRange(data, 18, data.length));
	}
}
