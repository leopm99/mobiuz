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
package com.l2journey.commons.network;

import java.nio.charset.StandardCharsets;

/**
 * Represents a buffer capable of writing various data types.<br>
 * This abstract class provides methods to write data in different formats to the buffer, including primitives and strings.
 * @author JoeAlisson
 */
public abstract class WritableBuffer implements Buffer
{
	/**
	 * Write <b>char</b> to the buffer.<br>
	 * 16 bit char
	 * @param value the char to be put on data.
	 */
	public abstract void writeChar(char value);
	
	/**
	 * Write a <b>byte</b> to the buffer.<br>
	 * 8bit integer (00) If the underlying data array can't hold a new byte its size is increased 20%
	 * @param value to be written.
	 */
	public abstract void writeByte(byte value);
	
	/**
	 * Write a int to the buffer, the int is casted to a byte.
	 * @param value to be written.
	 */
	public void writeByte(int value)
	{
		writeByte((byte) value);
	}
	
	/**
	 * Writes a boolean value to the buffer as a byte.<br>
	 * Writes 1 if true, otherwise 0. 8-bit integer (00)
	 * @param value The boolean value to be written.
	 */
	public void writeByte(boolean value)
	{
		writeByte((byte) (value ? 1 : 0));
	}
	
	/**
	 * Writes a byte array to the buffer.<br>
	 * 8-bit integer array (00 ...)
	 * @param value The byte array to be written.
	 */
	public abstract void writeBytes(byte... value);
	
	/**
	 * Writes a short value to the buffer.<br>
	 * 16-bit integer (00 00)
	 * @param value The short value to be written.
	 */
	public abstract void writeShort(short value);
	
	/**
	 * Write <b>short</b> to the buffer.<br>
	 * 16bit integer (00 00)
	 * @param value to be written.
	 */
	public void writeShort(int value)
	{
		writeShort((short) value);
	}
	
	/**
	 * Writes a boolean value to the buffer as a short.<br>
	 * Writes 1 if true, otherwise 0. 16-bit integer (00 00)
	 * @param value The boolean value to be written.
	 */
	public void writeShort(boolean value)
	{
		writeShort((short) (value ? 1 : 0));
	}
	
	/**
	 * Write <b>int</b> to the buffer.<br>
	 * 32bit integer (00 00 00 00)
	 * @param value to be written.
	 */
	public abstract void writeInt(int value);
	
	/**
	 * Write <b>boolean</b> to the buffer.<br>
	 * If the value is true so write a <b>byte</b> with value 1, otherwise 0 32bit integer (00 00 00 00)
	 * @param value to be written.
	 */
	public void writeInt(boolean value)
	{
		writeInt(value ? 1 : 0);
	}
	
	/**
	 * Write <b>float</b> to the buffer.<br>
	 * 32bit float point number (00 00 00 00)
	 * @param value to be written.
	 */
	public abstract void writeFloat(float value);
	
	/**
	 * Write <b>long</b> to the buffer.<br>
	 * 64bit integer (00 00 00 00 00 00 00 00)
	 * @param value to be written.
	 */
	public abstract void writeLong(long value);
	
	/**
	 * Write <b>double</b> to the buffer.<br>
	 * 64bit double precision float (00 00 00 00 00 00 00 00)
	 * @param value to be written.
	 */
	public abstract void writeDouble(double value);
	
	/**
	 * Write a <b>String</b> to the buffer with a null termination (\000). Each character is a 16bit char
	 * @param text to be written.
	 */
	public void writeString(CharSequence text)
	{
		if (text == null)
		{
			writeChar('\000');
			return;
		}
		
		writeStringWithCharset(text);
		writeChar('\000');
	}
	
	private void writeStringWithCharset(CharSequence text)
	{
		writeBytes(text.toString().getBytes(StandardCharsets.UTF_16LE));
	}
	
	/**
	 * Writes a String to the buffer preceded by a short 16-bit indicating the string's length, without a null termination.<br>
	 * Each character is a 16-bit char.
	 * @param text The text to be written.
	 */
	public void writeSizedString(CharSequence text)
	{
		if ((text != null) && !text.isEmpty())
		{
			writeShort(text.length());
			writeStringWithCharset(text);
		}
		else
		{
			writeShort(0);
		}
	}
}
