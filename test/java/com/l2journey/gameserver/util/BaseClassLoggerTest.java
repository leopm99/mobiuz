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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.siege.clanhalls.ClanHallSiegeEngine;
import com.l2journey.gameserver.taskmanagers.PersistentTaskManager.ExecutedTask;
import com.l2journey.gameserver.taskmanagers.tasks.PersistentTask;
import com.l2journey.gameserver.taskmanagers.tasks.TaskRestart;

/**
 * Regression tests: base-class loggers must resolve to the runtime subclass name.
 */
class BaseClassLoggerTest
{
	@Test
	void persistentTaskLoggerUsesRuntimeClassName() throws Exception
	{
		PersistentTask task = new TaskRestart();
		Logger logger = readLogger(PersistentTask.class, task);
		assertEquals(TaskRestart.class.getName(), logger.getName());
	}
	
	@Test
	void documentBaseLoggerUsesRuntimeClassName() throws Exception
	{
		DocumentBase document = new DocumentBase(new File("dummy.xml"))
		{
			@Override
			protected void parseDocument(Document document)
			{
			}
			
			@Override
			protected StatSet getStatSet()
			{
				return new StatSet();
			}
			
			@Override
			protected String getTableValue(String name)
			{
				return null;
			}
			
			@Override
			protected String getTableValue(String name, int idx)
			{
				return null;
			}
		};
		
		Logger logger = readLogger(DocumentBase.class, document);
		assertEquals(document.getClass().getName(), logger.getName());
		assertFalse(logger.getName().equals(DocumentBase.class.getName()));
	}
	
	@Test
	void clanHallSiegeEngineLoggerIsInstanceBoundToRuntimeClass() throws Exception
	{
		Field field = ClanHallSiegeEngine.class.getDeclaredField("LOGGER");
		assertFalse(Modifier.isStatic(field.getModifiers()), "LOGGER must be an instance field so subclasses keep their own log category");
		assertEquals(Logger.class, field.getType());
	}
	
	@Test
	void anonymousPersistentTaskLoggerUsesSubclassName() throws Exception
	{
		PersistentTask task = new PersistentTask()
		{
			@Override
			public String getName()
			{
				return "unit-test-task";
			}
			
			@Override
			public void onTimeElapsed(ExecutedTask executedTask)
			{
			}
		};
		
		Logger logger = readLogger(PersistentTask.class, task);
		assertEquals(task.getClass().getName(), logger.getName());
	}
	
	private static Logger readLogger(Class<?> owner, Object instance) throws Exception
	{
		Field field = owner.getDeclaredField("LOGGER");
		field.setAccessible(true);
		return (Logger) field.get(instance);
	}
}
