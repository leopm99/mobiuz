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
package com.l2journey.gameserver.network.serverpackets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.gameserver.data.xml.ItemData;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.network.serverpackets.SystemMessage.SMParam;

/**
 * Null-safe item template lookup in {@link SystemMessage#addItemName(int)}.
 */
class SystemMessageAddItemNameNullGuardTest
{
	private static final int ITEM_ID = 57;
	
	@Test
	void addItemNameKeepsIdWhenTemplateMissing()
	{
		final ItemData itemData = mock(ItemData.class);
		when(itemData.getTemplate(ITEM_ID)).thenReturn(null);
		try (MockedStatic<ItemData> mocked = mockStatic(ItemData.class))
		{
			mocked.when(ItemData::getInstance).thenReturn(itemData);
			final SystemMessage message = new SystemMessage("item");
			assertSame(message, message.addItemName(ITEM_ID));
			final SMParam param = message.getParams()[1];
			assertEquals(SystemMessage.TYPE_ITEM_NAME, param.getType());
			assertEquals(ITEM_ID, param.getIntValue());
		}
	}
	
	@Test
	void addItemNameKeepsIdWhenDisplayIdMatches()
	{
		final ItemTemplate template = mock(ItemTemplate.class);
		when(template.getDisplayId()).thenReturn(ITEM_ID);
		final ItemData itemData = mock(ItemData.class);
		when(itemData.getTemplate(ITEM_ID)).thenReturn(template);
		try (MockedStatic<ItemData> mocked = mockStatic(ItemData.class))
		{
			mocked.when(ItemData::getInstance).thenReturn(itemData);
			final SystemMessage message = new SystemMessage("item");
			message.addItemName(ITEM_ID);
			final SMParam param = message.getParams()[1];
			assertEquals(SystemMessage.TYPE_ITEM_NAME, param.getType());
			assertEquals(ITEM_ID, param.getIntValue());
		}
	}
	
	@Test
	void addItemNameUsesDisplayNameWhenDisplayIdDiffers()
	{
		final ItemTemplate template = mock(ItemTemplate.class);
		when(template.getDisplayId()).thenReturn(999);
		when(template.getName()).thenReturn("Display Name");
		final ItemData itemData = mock(ItemData.class);
		when(itemData.getTemplate(ITEM_ID)).thenReturn(template);
		try (MockedStatic<ItemData> mocked = mockStatic(ItemData.class))
		{
			mocked.when(ItemData::getInstance).thenReturn(itemData);
			final SystemMessage message = new SystemMessage("item");
			message.addItemName(ITEM_ID);
			final SMParam param = message.getParams()[1];
			assertEquals(SystemMessage.TYPE_TEXT, param.getType());
			assertEquals("Display Name", param.getStringValue());
		}
	}
}
