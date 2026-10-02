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
package com.l2journey.gameserver.model.phantom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PhantomState} RETURN_UNTIL lifecycle.
 */
class PhantomStateTest
{
	@AfterEach
	void tearDown()
	{
		PhantomState.clear();
	}
	
	@Test
	void registerSetsReturnUntilToZero()
	{
		PhantomState.register(101, false, false);
		
		assertEquals(0L, PhantomState.RETURN_UNTIL.get(101));
	}
	
	@Test
	void unregisterRemovesReturnUntil()
	{
		PhantomState.register(202, true, false);
		PhantomState.RETURN_UNTIL.put(202, System.currentTimeMillis() + 1000L);
		
		PhantomState.unregister(202);
		
		assertFalse(PhantomState.RETURN_UNTIL.containsKey(202));
	}
	
	@Test
	void clearWipesReturnUntil()
	{
		PhantomState.register(303, false, true);
		PhantomState.RETURN_UNTIL.put(303, 999L);
		
		PhantomState.clear();
		
		assertTrue(PhantomState.RETURN_UNTIL.isEmpty());
	}
}
