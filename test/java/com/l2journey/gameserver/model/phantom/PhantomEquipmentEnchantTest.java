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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.item.type.CrystalType;
import com.l2journey.gameserver.model.itemcontainer.PlayerInventory;

/**
 * Unit tests for {@link PhantomEquipment#targetEnchantForGrade}.
 */
class PhantomEquipmentEnchantTest
{
	@Test
	void noneAndDWeaponsStayUnenchanted()
	{
		assertEquals(0, PhantomEquipment.targetEnchantForGrade(CrystalType.NONE, true));
		assertEquals(0, PhantomEquipment.targetEnchantForGrade(CrystalType.D, true));
	}

	@Test
	void noneAndDNonWeaponsTargetSixteen()
	{
		assertEquals(16, PhantomEquipment.targetEnchantForGrade(CrystalType.NONE, false));
		assertEquals(16, PhantomEquipment.targetEnchantForGrade(CrystalType.D, false));
	}

	@Test
	void cGradeWeaponsTargetThree()
	{
		assertEquals(3, PhantomEquipment.targetEnchantForGrade(CrystalType.C, true));
		assertEquals(16, PhantomEquipment.targetEnchantForGrade(CrystalType.C, false));
	}

	@Test
	void bGradeWeaponsTargetEight()
	{
		assertEquals(8, PhantomEquipment.targetEnchantForGrade(CrystalType.B, true));
		assertEquals(16, PhantomEquipment.targetEnchantForGrade(CrystalType.B, false));
	}

	@Test
	void aGradeWeaponsTargetTen()
	{
		assertEquals(10, PhantomEquipment.targetEnchantForGrade(CrystalType.A, true));
		assertEquals(16, PhantomEquipment.targetEnchantForGrade(CrystalType.A, false));
	}

	@Test
	void sGradeWeaponsTargetFifteen()
	{
		assertEquals(15, PhantomEquipment.targetEnchantForGrade(CrystalType.S, true));
		assertEquals(16, PhantomEquipment.targetEnchantForGrade(CrystalType.S, false));
	}

	@Test
	void s84TargetsSixteenForWeaponsAndNonWeapons()
	{
		assertEquals(16, PhantomEquipment.targetEnchantForGrade(CrystalType.S84, true));
		assertEquals(16, PhantomEquipment.targetEnchantForGrade(CrystalType.S84, false));
	}

	@Test
	void s80StaysUnenchanted()
	{
		assertEquals(0, PhantomEquipment.targetEnchantForGrade(CrystalType.S80, true));
		assertEquals(0, PhantomEquipment.targetEnchantForGrade(CrystalType.S80, false));
	}

	@Test
	void enforceEquipmentAppliesGradeEnchant() throws Exception
	{
		Player bot = mock(Player.class);
		PlayerInventory inventory = mock(PlayerInventory.class);
		Item item = mock(Item.class);
		ItemTemplate template = mock(ItemTemplate.class);
		when(bot.getInventory()).thenReturn(inventory);
		when(bot.isMageClass()).thenReturn(true);
		when(bot.getLevel()).thenReturn(84);
		when(bot.getBaseClass()).thenReturn(10);
		when(inventory.getItems()).thenReturn(List.of(item));
		when(item.getId()).thenReturn(15893);
		when(item.isEquipable()).thenReturn(true);
		when(item.isWeapon()).thenReturn(true);
		when(item.getTemplate()).thenReturn(template);
		when(template.getCrystalType()).thenReturn(CrystalType.S84);
		when(item.getEnchantLevel()).thenReturn(0);
		when(item.isEquipped()).thenReturn(true);

		Method method = PhantomEquipment.class.getDeclaredMethod("enforceEquipment", Player.class);
		method.setAccessible(true);
		method.invoke(null, bot);

		verify(item).setEnchantLevel(16);
	}
}
