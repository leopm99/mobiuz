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
package com.l2journey.gameserver.model.itemcontainer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;

/**
 * Null-safe item template access in {@link PlayerInventory#validateCapacityByItemId}.
 */
class PlayerInventoryCapacityNullGuardTest
{
	private static final int ITEM_ID = 57;
	
	@TempDir
	File tempDir;
	
	private File previousDatapackRoot;
	
	@BeforeEach
	void saveConfig()
	{
		previousDatapackRoot = Config.DATAPACK_ROOT;
		Config.DATAPACK_ROOT = tempDir;
	}
	
	@AfterEach
	void restoreConfig()
	{
		Config.DATAPACK_ROOT = previousDatapackRoot;
	}
	
	@Test
	void returnsFalseWhenTemplateMissing()
	{
		final Player owner = mock(Player.class);
		final PlayerInventory inventory = spy(new PlayerInventory(owner));
		final ItemData itemData = mock(ItemData.class);
		
		try (MockedStatic<ItemData> itemDataStatic = mockStatic(ItemData.class))
		{
			itemDataStatic.when(ItemData::getInstance).thenReturn(itemData);
			when(itemData.getTemplate(ITEM_ID)).thenReturn(null);
			
			assertFalse(inventory.validateCapacityByItemId(ITEM_ID));
		}
	}
	
	@Test
	void validatesCapacityWhenTemplatePresent()
	{
		final Player owner = mock(Player.class);
		final PlayerInventory inventory = spy(new PlayerInventory(owner));
		final ItemData itemData = mock(ItemData.class);
		final ItemTemplate template = mock(ItemTemplate.class);
		
		try (MockedStatic<ItemData> itemDataStatic = mockStatic(ItemData.class))
		{
			itemDataStatic.when(ItemData::getInstance).thenReturn(itemData);
			when(itemData.getTemplate(ITEM_ID)).thenReturn(template);
			when(template.isQuestItem()).thenReturn(true);
			doReturn(null).when(inventory).getItemByItemId(ITEM_ID);
			doReturn(true).when(inventory).validateCapacity(1L, true);
			
			assertTrue(inventory.validateCapacityByItemId(ITEM_ID));
			verify(inventory).validateCapacity(1L, true);
		}
	}
}
