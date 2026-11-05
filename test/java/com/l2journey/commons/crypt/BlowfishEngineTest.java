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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Round-trip and guard characterization for {@link BlowfishEngine}.
 */
class BlowfishEngineTest
{
	private static final byte[] KEY = "0123456789ABCDEF".getBytes();
	
	@Test
	void algorithmNameAndBlockSize()
	{
		final BlowfishEngine engine = new BlowfishEngine();
		assertEquals("Blowfish", engine.getAlgorithmName());
		assertEquals(8, engine.getBlockSize());
	}
	
	@Test
	void encryptDecryptInPlaceRoundTrip()
	{
		final BlowfishEngine engine = new BlowfishEngine();
		engine.init(KEY);
		
		final byte[] data = twoBlocksAtOffset();
		final byte[] original = Arrays.copyOf(data, data.length);
		
		engine.encryptBlock(data, 2);
		engine.encryptBlock(data, 10);
		assertFalse(Arrays.equals(original, data));
		
		engine.decryptBlock(data, 2);
		engine.decryptBlock(data, 10);
		assertArrayEquals(original, data);
	}
	
	@Test
	void encryptDecryptSeparateBuffersRoundTrip()
	{
		final BlowfishEngine engine = new BlowfishEngine();
		engine.init(KEY);
		
		final byte[] src = twoBlocksAtOffset();
		final byte[] original = Arrays.copyOf(src, src.length);
		final byte[] dst = new byte[src.length];
		
		engine.encryptBlock(src, 2, dst, 2);
		engine.encryptBlock(src, 10, dst, 10);
		assertFalse(Arrays.equals(Arrays.copyOfRange(original, 2, 18), Arrays.copyOfRange(dst, 2, 18)));
		
		final byte[] recovered = new byte[src.length];
		engine.decryptBlock(dst, 2, recovered, 2);
		engine.decryptBlock(dst, 10, recovered, 10);
		assertArrayEquals(Arrays.copyOfRange(original, 2, 18), Arrays.copyOfRange(recovered, 2, 18));
	}
	
	@Test
	void tryEncryptDecryptRoundTrip() throws IOException
	{
		final BlowfishEngine engine = new BlowfishEngine();
		engine.init(KEY);
		
		final byte[] data = twoBlocksAtOffset();
		final byte[] original = Arrays.copyOf(data, data.length);
		
		engine.tryEncryptBlock(data, 2);
		engine.tryEncryptBlock(data, 10);
		assertFalse(Arrays.equals(original, data));
		
		engine.tryDecryptBlock(data, 2);
		engine.tryDecryptBlock(data, 10);
		assertArrayEquals(original, data);
	}
	
	@Test
	void tryEncryptDecryptSeparateBuffersRoundTrip() throws IOException
	{
		final BlowfishEngine engine = new BlowfishEngine();
		engine.init(KEY);
		
		final byte[] src = twoBlocksAtOffset();
		final byte[] original = Arrays.copyOf(src, src.length);
		final byte[] dst = new byte[src.length];
		
		engine.tryEncryptBlock(src, 2, dst, 2);
		engine.tryEncryptBlock(src, 10, dst, 10);
		assertFalse(Arrays.equals(Arrays.copyOfRange(original, 2, 18), Arrays.copyOfRange(dst, 2, 18)));
		
		final byte[] recovered = new byte[src.length];
		engine.tryDecryptBlock(dst, 2, recovered, 2);
		engine.tryDecryptBlock(dst, 10, recovered, 10);
		assertArrayEquals(Arrays.copyOfRange(original, 2, 18), Arrays.copyOfRange(recovered, 2, 18));
	}
	
	@Test
	void uninitializedTryEncryptThrows()
	{
		final BlowfishEngine engine = new BlowfishEngine();
		assertThrows(IllegalStateException.class, () -> engine.tryEncryptBlock(new byte[8], 0));
	}
	
	@Test
	void uninitializedTryDecryptThrows()
	{
		final BlowfishEngine engine = new BlowfishEngine();
		assertThrows(IllegalStateException.class, () -> engine.tryDecryptBlock(new byte[8], 0));
	}
	
	@Test
	void shortSourceTryEncryptThrows()
	{
		final BlowfishEngine engine = new BlowfishEngine();
		engine.init(KEY);
		assertThrows(IOException.class, () -> engine.tryEncryptBlock(new byte[7], 0));
	}
	
	@Test
	void shortDestinationTryEncryptThrows()
	{
		final BlowfishEngine engine = new BlowfishEngine();
		engine.init(KEY);
		assertThrows(IOException.class, () -> engine.tryEncryptBlock(new byte[8], 0, new byte[7], 0));
	}
	
	@Test
	void shortSourceTryDecryptThrows()
	{
		// tryDecryptBlock compares dstIndex + 8 with src.length; short destination is not the condition under test.
		final BlowfishEngine engine = new BlowfishEngine();
		engine.init(KEY);
		assertThrows(IOException.class, () -> engine.tryDecryptBlock(new byte[7], 0));
	}
	
	private static byte[] twoBlocksAtOffset()
	{
		final byte[] data = new byte[20];
		for (int i = 0; i < data.length; i++)
		{
			data[i] = (byte) (i + 1);
		}
		return data;
	}
}
