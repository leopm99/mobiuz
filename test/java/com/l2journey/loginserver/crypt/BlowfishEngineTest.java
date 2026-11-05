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
package com.l2journey.loginserver.crypt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.l2journey.commons.network.Buffer;

/**
 * Round-trip and guard characterization for login-server {@link BlowfishEngine}.
 */
class BlowfishEngineTest
{
	private static final byte[] KEY = "0123456789ABCDEF".getBytes();
	
	@Test
	void blockSizeIsEight()
	{
		assertEquals(8, new BlowfishEngine().getBlockSize());
	}
	
	@Test
	void processBlockEncryptDecryptRoundTrip() throws IOException
	{
		final BlowfishEngine encrypt = new BlowfishEngine();
		encrypt.init(true, KEY);
		final BlowfishEngine decrypt = new BlowfishEngine();
		decrypt.init(false, KEY);
		
		final ByteArrayBuffer data = twoBlocksAtOffset();
		final byte[] original = data.copyBytes();
		
		encrypt.processBlock(data, 2);
		encrypt.processBlock(data, 10);
		assertFalse(Arrays.equals(original, data.copyBytes()));
		
		decrypt.processBlock(data, 2);
		decrypt.processBlock(data, 10);
		assertArrayEquals(original, data.copyBytes());
	}
	
	@Test
	void uninitializedProcessBlockThrows()
	{
		final BlowfishEngine engine = new BlowfishEngine();
		final ByteArrayBuffer data = new ByteArrayBuffer(8);
		assertThrows(IllegalStateException.class, () -> engine.processBlock(data, 0));
	}
	
	@Test
	void shortBufferProcessBlockThrows() throws IOException
	{
		final BlowfishEngine engine = new BlowfishEngine();
		engine.init(true, KEY);
		final ByteArrayBuffer data = new ByteArrayBuffer(7);
		assertThrows(IOException.class, () -> engine.processBlock(data, 0));
	}
	
	private static ByteArrayBuffer twoBlocksAtOffset()
	{
		final byte[] bytes = new byte[20];
		for (int i = 0; i < bytes.length; i++)
		{
			bytes[i] = (byte) (i + 1);
		}
		return new ByteArrayBuffer(bytes);
	}
	
	/**
	 * Little-endian {@link Buffer} stub matching {@code ArrayPacketBuffer} int layout.
	 */
	static final class ByteArrayBuffer implements Buffer
	{
		private final byte[] _data;
		private int _limit;
		
		ByteArrayBuffer(int size)
		{
			_data = new byte[size];
			_limit = size;
		}
		
		ByteArrayBuffer(byte[] data)
		{
			_data = Arrays.copyOf(data, data.length);
			_limit = data.length;
		}
		
		byte[] copyBytes()
		{
			return Arrays.copyOf(_data, _data.length);
		}
		
		@Override
		public byte readByte(int index)
		{
			return _data[index];
		}
		
		@Override
		public void writeByte(int index, byte value)
		{
			_data[index] = value;
		}
		
		@Override
		public short readShort(int index)
		{
			return (short) ((_data[index] & 0xff) | ((_data[index + 1] & 0xff) << 8));
		}
		
		@Override
		public void writeShort(int index, short value)
		{
			_data[index] = (byte) value;
			_data[index + 1] = (byte) (value >>> 8);
		}
		
		@Override
		public int readInt(int index)
		{
			return (_data[index] & 0xff) | ((_data[index + 1] & 0xff) << 8) | ((_data[index + 2] & 0xff) << 16) | ((_data[index + 3] & 0xff) << 24);
		}
		
		@Override
		public void writeInt(int index, int value)
		{
			_data[index] = (byte) value;
			_data[index + 1] = (byte) (value >>> 8);
			_data[index + 2] = (byte) (value >>> 16);
			_data[index + 3] = (byte) (value >>> 24);
		}
		
		@Override
		public int limit()
		{
			return _limit;
		}
		
		@Override
		public void limit(int newLimit)
		{
			_limit = newLimit;
		}
	}
}
