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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for LoginController Blowfish key generation and selection.
 */
class LoginControllerBlowfishKeyTest
{
	private static final int BLOWFISH_KEYS = 20;
	private static final int KEY_LENGTH = 16;
	
	@Test
	void getBlowfishKeyReturnsPoolEntriesAndVaries() throws Exception
	{
		final LoginController controller = allocateWithoutConstructor();
		final byte[][] keys = new byte[BLOWFISH_KEYS][KEY_LENGTH];
		for (int i = 0; i < BLOWFISH_KEYS; i++)
		{
			keys[i][0] = (byte) i;
		}
		setBlowfishKeys(controller, keys);
		
		final Set<byte[]> seen = new HashSet<>();
		for (int i = 0; i < 40; i++)
		{
			final byte[] selected = controller.getBlowfishKey();
			assertNotNull(selected);
			boolean found = false;
			for (byte[] key : keys)
			{
				if (selected == key)
				{
					found = true;
					break;
				}
			}
			assertTrue(found, "Selected key must be one of the pool arrays by identity");
			seen.add(selected);
		}
		assertTrue(seen.size() > 1, "Expected more than one Blowfish key across 40 selections");
	}
	
	@Test
	void generateBlowFishKeysCreatesTwentyNonZeroKeys() throws Exception
	{
		final LoginController controller = allocateWithoutConstructor();
		final Method generate = LoginController.class.getDeclaredMethod("generateBlowFishKeys");
		generate.setAccessible(true);
		generate.invoke(controller);
		
		final byte[][] keys = getBlowfishKeys(controller);
		assertEquals(BLOWFISH_KEYS, keys.length);
		boolean anyNonZero = false;
		for (byte[] key : keys)
		{
			assertEquals(KEY_LENGTH, key.length);
			for (byte value : key)
			{
				if (value != 0)
				{
					anyNonZero = true;
					break;
				}
			}
		}
		assertTrue(anyNonZero, "Generated Blowfish keys must not be all zeros");
	}
	
	private static void setBlowfishKeys(LoginController controller, byte[][] keys) throws Exception
	{
		final Field field = LoginController.class.getDeclaredField("_blowfishKeys");
		field.setAccessible(true);
		field.set(controller, keys);
	}
	
	private static byte[][] getBlowfishKeys(LoginController controller) throws Exception
	{
		final Field field = LoginController.class.getDeclaredField("_blowfishKeys");
		field.setAccessible(true);
		return (byte[][]) field.get(controller);
	}
	
	private static LoginController allocateWithoutConstructor() throws Exception
	{
		try
		{
			final Class<?> reflectionFactoryClass = Class.forName("sun.reflect.ReflectionFactory");
			final Object reflectionFactory = reflectionFactoryClass.getMethod("getReflectionFactory").invoke(null);
			final Constructor<?> objectConstructor = Object.class.getDeclaredConstructor();
			final Constructor<?> serializationConstructor = (Constructor<?>) reflectionFactoryClass
				.getMethod("newConstructorForSerialization", Class.class, Constructor.class)
				.invoke(reflectionFactory, LoginController.class, objectConstructor);
			serializationConstructor.setAccessible(true);
			return (LoginController) serializationConstructor.newInstance();
		}
		catch (ReflectiveOperationException _)
		{
			final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
			final var theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			final Object unsafe = theUnsafe.get(null);
			return (LoginController) unsafeClass.getMethod("allocateInstance", Class.class).invoke(unsafe, LoginController.class);
		}
	}
}
