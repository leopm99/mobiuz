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
package com.l2journey.gameserver.model.actor.templates;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NpcTemplate#isAssignableTo(Class, Class)} and the object overload.
 */
class NpcTemplateIsAssignableToTest
{
	interface Marker
	{
	}
	
	interface ChildMarker extends Marker
	{
	}
	
	static class Base implements Marker
	{
	}
	
	static class Child extends Base
	{
	}
	
	static class Direct implements ChildMarker
	{
	}
	
	static class Unrelated
	{
	}
	
	@Test
	void sameConcreteClassIsAssignable()
	{
		assertTrue(NpcTemplate.isAssignableTo(Base.class, Base.class));
	}
	
	@Test
	void interfaceComparedToItselfIsAssignable()
	{
		assertTrue(NpcTemplate.isAssignableTo(Marker.class, Marker.class));
	}
	
	@Test
	void subclassIsAssignableToSuperclass()
	{
		assertTrue(NpcTemplate.isAssignableTo(Child.class, Base.class));
	}
	
	@Test
	void superclassIsNotAssignableToSubclass()
	{
		assertFalse(NpcTemplate.isAssignableTo(Base.class, Child.class));
	}
	
	@Test
	void classIsAssignableToDirectInterface()
	{
		assertTrue(NpcTemplate.isAssignableTo(Direct.class, ChildMarker.class));
	}
	
	@Test
	void classIsAssignableToInheritedInterface()
	{
		assertTrue(NpcTemplate.isAssignableTo(Child.class, Marker.class));
	}
	
	@Test
	void interfaceIsAssignableToSuperinterface()
	{
		assertTrue(NpcTemplate.isAssignableTo(ChildMarker.class, Marker.class));
	}
	
	@Test
	void unrelatedTypeIsNotAssignable()
	{
		assertFalse(NpcTemplate.isAssignableTo(Unrelated.class, Marker.class));
		assertFalse(NpcTemplate.isAssignableTo(Unrelated.class, Base.class));
	}
	
	@Test
	void objectOverloadUsesRuntimeClass()
	{
		assertTrue(NpcTemplate.isAssignableTo(new Child(), Base.class));
		assertTrue(NpcTemplate.isAssignableTo(new Child(), Marker.class));
		assertFalse(NpcTemplate.isAssignableTo(new Unrelated(), Marker.class));
	}
	
	@Test
	void nullArgumentsThrowNullPointerException()
	{
		assertThrows(NullPointerException.class, () -> NpcTemplate.isAssignableTo((Class<?>) null, Base.class));
		assertThrows(NullPointerException.class, () -> NpcTemplate.isAssignableTo(Base.class, null));
		assertThrows(NullPointerException.class, () -> NpcTemplate.isAssignableTo((Object) null, Base.class));
	}
}
