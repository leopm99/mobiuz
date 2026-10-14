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
package com.l2journey.gameserver.util;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.WorldObject;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;

/**
 * Unit tests for {@link BuffTargetResolver}.
 */
class BuffTargetResolverTest
{
	@Test
	void creatureObjectIdReturnsCreatureWithoutPlayerLookup()
	{
		final Creature creature = mock(Creature.class);
		when(creature.isCreature()).thenReturn(true);
		when(creature.asCreature()).thenReturn(creature);
		
		@SuppressWarnings("unchecked")
		final IntFunction<WorldObject> findObject = mock(IntFunction.class);
		@SuppressWarnings("unchecked")
		final Function<String, Player> findPlayer = mock(Function.class);
		when(findObject.apply(12345)).thenReturn(creature);
		
		assertSame(creature, BuffTargetResolver.resolve("12345", findObject, findPlayer));
		verify(findObject).apply(12345);
		verify(findPlayer, never()).apply(anyString());
	}
	
	@Test
	void nonCreatureObjectIdReturnsNullWithoutPlayerLookup()
	{
		final WorldObject item = mock(WorldObject.class);
		when(item.isCreature()).thenReturn(false);
		
		@SuppressWarnings("unchecked")
		final IntFunction<WorldObject> findObject = mock(IntFunction.class);
		@SuppressWarnings("unchecked")
		final Function<String, Player> findPlayer = mock(Function.class);
		when(findObject.apply(99)).thenReturn(item);
		
		assertNull(BuffTargetResolver.resolve("99", findObject, findPlayer));
		verify(findObject).apply(99);
		verify(findPlayer, never()).apply(anyString());
	}
	
	@Test
	void unknownObjectIdReturnsNullWithoutPlayerLookup()
	{
		@SuppressWarnings("unchecked")
		final IntFunction<WorldObject> findObject = mock(IntFunction.class);
		@SuppressWarnings("unchecked")
		final Function<String, Player> findPlayer = mock(Function.class);
		when(findObject.apply(404)).thenReturn(null);
		
		assertNull(BuffTargetResolver.resolve("404", findObject, findPlayer));
		verify(findObject).apply(404);
		verify(findPlayer, never()).apply(anyString());
	}
	
	@Test
	void playerNameUsesPlayerLookupWithoutObjectLookup()
	{
		final Player player = mock(Player.class);
		
		@SuppressWarnings("unchecked")
		final IntFunction<WorldObject> findObject = mock(IntFunction.class);
		@SuppressWarnings("unchecked")
		final Function<String, Player> findPlayer = mock(Function.class);
		when(findPlayer.apply("HeroName")).thenReturn(player);
		
		assertSame(player, BuffTargetResolver.resolve("HeroName", findObject, findPlayer));
		verify(findPlayer).apply("HeroName");
		verify(findObject, never()).apply(anyInt());
	}
}
