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
package com.l2journey.gameserver.data.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.l2journey.gameserver.model.actor.enums.player.ShortcutType;
import com.l2journey.gameserver.model.actor.holders.player.Shortcut;

/**
 * Unit tests for null-safe shortcut attribute parsing in {@link InitialShortcutData}.
 */
class InitialShortcutDataNullGuardTest
{
	@TempDir
	File tempDir;
	
	@Test
	void parseShortcutsSkipsSlotsMissingIdsAndKeepsValidSlot() throws Exception
	{
		final File xmlFile = new File(tempDir, "initialShortcuts.xml");
		Files.writeString(xmlFile.toPath(), """
			<?xml version="1.0" encoding="UTF-8"?>
			<list>
				<shortcuts>
					<page pageId="0">
						<slot shortcutType="ACTION" shortcutId="2" />
						<slot slotId="3" shortcutType="ACTION" />
						<slot slotId="11" shortcutType="ITEM" shortcutId="5588" />
					</page>
				</shortcuts>
			</list>
			""", StandardCharsets.UTF_8);
		
		final TestableInitialShortcutData data = new TestableInitialShortcutData();
		data.parseFile(xmlFile);
		
		final List<Shortcut> shortcuts = data.getGlobalMacroList();
		assertEquals(1, shortcuts.size());
		assertEquals(11, shortcuts.get(0).getSlot());
		assertEquals(ShortcutType.ITEM, shortcuts.get(0).getType());
		assertEquals(5588, shortcuts.get(0).getId());
	}
	
	private static final class TestableInitialShortcutData extends InitialShortcutData
	{
		private TestableInitialShortcutData()
		{
			super();
		}
		
		@Override
		public void load()
		{
		}
		
		@Override
		public boolean isValidating()
		{
			return false;
		}
	}
}
