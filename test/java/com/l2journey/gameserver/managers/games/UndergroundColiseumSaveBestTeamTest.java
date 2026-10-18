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
package com.l2journey.gameserver.managers.games;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.model.undergroundColiseum.UCBestTeam;

/**
 * Unit tests for {@link UndergroundColiseumManager#saveBestTeam}.
 */
class UndergroundColiseumSaveBestTeamTest
{
	@Test
	void saveBestTeamUpdateBindsLeaderWinsAndArenaId() throws Exception
	{
		final Connection connection = mock(Connection.class);
		final PreparedStatement statement = mock(PreparedStatement.class);
		when(connection.prepareStatement(contains("UPDATE underground_coliseum"))).thenReturn(statement);
		
		final UndergroundColiseumManager manager = allocateWithoutConstructor();
		final UCBestTeam team = new UCBestTeam(7, "LeaderName", 12);
		
		try (MockedStatic<DatabaseFactory> database = mockStatic(DatabaseFactory.class))
		{
			database.when(DatabaseFactory::getConnection).thenReturn(connection);
			manager.saveBestTeam(team, false);
		}
		
		verify(connection).prepareStatement(contains("UPDATE underground_coliseum"));
		verify(statement).setString(1, "LeaderName");
		verify(statement).setInt(2, 12);
		verify(statement).setInt(3, 7);
		verify(statement).execute();
	}
	
	private static UndergroundColiseumManager allocateWithoutConstructor() throws Exception
	{
		try
		{
			final Class<?> reflectionFactoryClass = Class.forName("sun.reflect.ReflectionFactory");
			final Object reflectionFactory = reflectionFactoryClass.getMethod("getReflectionFactory").invoke(null);
			final Constructor<?> objectConstructor = Object.class.getDeclaredConstructor();
			final Constructor<?> serializationConstructor = (Constructor<?>) reflectionFactoryClass
				.getMethod("newConstructorForSerialization", Class.class, Constructor.class)
				.invoke(reflectionFactory, UndergroundColiseumManager.class, objectConstructor);
			serializationConstructor.setAccessible(true);
			return (UndergroundColiseumManager) serializationConstructor.newInstance();
		}
		catch (ReflectiveOperationException _)
		{
			final Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
			final var theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			final Object unsafe = theUnsafe.get(null);
			return (UndergroundColiseumManager) unsafeClass.getMethod("allocateInstance", Class.class).invoke(unsafe, UndergroundColiseumManager.class);
		}
	}
}
