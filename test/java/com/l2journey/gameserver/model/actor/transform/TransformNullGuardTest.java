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
package com.l2journey.gameserver.model.actor.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.StatSet;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.actor.appearance.PlayerAppearance;
import com.l2journey.gameserver.model.actor.templates.PlayerTemplate;
import com.l2journey.gameserver.model.itemcontainer.Inventory;

/**
 * Unit tests for null-safe transform template fallbacks in {@link Transform}.
 */
class TransformNullGuardTest
{
	private static final float HEIGHT = 23.5f;
	private static final float RADIUS = 9.5f;
	private static final int RANGE = 55;
	private static final int CHEST_DEF = 42;
	private static final float FALLBACK_HEIGHT = 12.0f;
	private static final float FALLBACK_RADIUS = 7.0f;
	private static final int FALLBACK_RANGE = 40;
	private static final int FALLBACK_CHEST = 10;
	
	private Transform transform;
	private Player player;
	private TransformTemplate maleTemplate;
	
	@BeforeEach
	void setUp()
	{
		final StatSet transformSet = new StatSet();
		transformSet.set("id", 1);
		transform = new Transform(transformSet);
		
		final StatSet templateSet = new StatSet();
		templateSet.set("height", HEIGHT);
		templateSet.set("radius", RADIUS);
		templateSet.set("range", RANGE);
		templateSet.set("chest", CHEST_DEF);
		maleTemplate = new TransformTemplate(templateSet);
		
		player = mock(Player.class);
		when(player.getAppearance()).thenReturn(new PlayerAppearance((byte) 0, (byte) 0, (byte) 0, false));
	}
	
	@Test
	void collisionHeightUsesTemplateWhenPresent()
	{
		transform.setTemplate(true, maleTemplate);
		assertEquals(HEIGHT, transform.getCollisionHeight(player));
	}
	
	@Test
	void collisionHeightFallsBackToPlayerWhenTemplateMissing()
	{
		when(player.getCollisionHeight()).thenReturn(FALLBACK_HEIGHT);
		assertEquals(FALLBACK_HEIGHT, transform.getCollisionHeight(player));
	}
	
	@Test
	void collisionHeightReturnsZeroWhenPlayerNull()
	{
		assertEquals(0f, transform.getCollisionHeight(null));
	}
	
	@Test
	void collisionRadiusUsesTemplateWhenPresent()
	{
		transform.setTemplate(true, maleTemplate);
		assertEquals(RADIUS, transform.getCollisionRadius(player));
	}
	
	@Test
	void collisionRadiusFallsBackToPlayerWhenTemplateMissing()
	{
		when(player.getCollisionRadius()).thenReturn(FALLBACK_RADIUS);
		assertEquals(FALLBACK_RADIUS, transform.getCollisionRadius(player));
	}
	
	@Test
	void collisionRadiusReturnsZeroWhenPlayerNull()
	{
		assertEquals(0f, transform.getCollisionRadius(null));
	}
	
	@Test
	void baseAttackRangeUsesTemplateWhenPresent()
	{
		transform.setTemplate(true, maleTemplate);
		assertEquals(RANGE, transform.getBaseAttackRange(player));
	}
	
	@Test
	void baseAttackRangeFallsBackToPlayerWhenTemplateMissing()
	{
		final PlayerTemplate playerTemplate = mock(PlayerTemplate.class);
		when(player.getTemplate()).thenReturn(playerTemplate);
		when(playerTemplate.getBaseAttackRange()).thenReturn(FALLBACK_RANGE);
		assertEquals(FALLBACK_RANGE, transform.getBaseAttackRange(player));
	}
	
	@Test
	void baseAttackRangeReturnsZeroWhenPlayerNull()
	{
		assertEquals(0, transform.getBaseAttackRange(null));
	}
	
	@Test
	void baseDefBySlotUsesTemplateWhenPresent()
	{
		transform.setTemplate(true, maleTemplate);
		assertEquals(CHEST_DEF, transform.getBaseDefBySlot(player, Inventory.PAPERDOLL_CHEST));
	}
	
	@Test
	void baseDefBySlotFallsBackToPlayerWhenTemplateMissing()
	{
		final PlayerTemplate playerTemplate = mock(PlayerTemplate.class);
		when(player.getTemplate()).thenReturn(playerTemplate);
		when(playerTemplate.getBaseDefBySlot(Inventory.PAPERDOLL_CHEST)).thenReturn(FALLBACK_CHEST);
		assertEquals(FALLBACK_CHEST, transform.getBaseDefBySlot(player, Inventory.PAPERDOLL_CHEST));
	}
	
	@Test
	void baseDefBySlotReturnsZeroWhenPlayerNull()
	{
		assertEquals(0, transform.getBaseDefBySlot(null, Inventory.PAPERDOLL_CHEST));
	}
}
