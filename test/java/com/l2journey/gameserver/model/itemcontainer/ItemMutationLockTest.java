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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.l2journey.commons.network.ReadableBuffer;
import com.l2journey.commons.network.ReadablePacket;
import com.l2journey.gameserver.data.xml.EnchantItemData;
import com.l2journey.gameserver.managers.IdManager;
import com.l2journey.gameserver.managers.ItemManager;
import com.l2journey.gameserver.managers.MercTicketManager;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Creature;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enchant.EnchantScroll;
import com.l2journey.gameserver.model.item.enums.ItemLocation;
import com.l2journey.gameserver.model.item.enums.ItemProcessType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.network.GameClient;
import com.l2journey.gameserver.network.clientpackets.RequestEnchantItem;
import com.l2journey.gameserver.network.serverpackets.ServerPacket;
import com.l2journey.gameserver.taskmanagers.ItemLifeTimeTaskManager;

/**
 * Private-final item mutation locks (java:S2445).
 */
class ItemMutationLockTest
{
	private static ItemTemplate template(int id, boolean stackable)
	{
		final ItemTemplate template = mock(ItemTemplate.class);
		when(template.getId()).thenReturn(id);
		when(template.getName()).thenReturn("TestItem" + id);
		when(template.getTime()).thenReturn(-1L);
		when(template.getDuration()).thenReturn(-1);
		when(template.getAgathionMaxEnergy()).thenReturn(-1);
		when(template.isPetItem()).thenReturn(false);
		when(template.hasSkills()).thenReturn(false);
		when(template.isStackable()).thenReturn(stackable);
		when(template.isEquipable()).thenReturn(false);
		when(template.getWeight()).thenReturn(0);
		return template;
	}
	
	private static Item newItem(int objectId, ItemTemplate template)
	{
		return new Item(objectId, template);
	}
	
	private static final class TestContainer extends ItemContainer
	{
		private final Creature _owner;
		
		TestContainer(Creature owner)
		{
			_owner = owner;
		}
		
		void put(Item item)
		{
			_items.add(item);
		}
		
		@Override
		protected Creature getOwner()
		{
			return _owner;
		}
		
		@Override
		protected ItemLocation getBaseLocation()
		{
			return ItemLocation.INVENTORY;
		}
	}
	
	private static final class TestInventory extends Inventory
	{
		private final Creature _owner;
		
		TestInventory(Creature owner)
		{
			_owner = owner;
		}
		
		void put(Item item)
		{
			_items.add(item);
		}
		
		@Override
		protected Creature getOwner()
		{
			return _owner;
		}
		
		@Override
		protected ItemLocation getBaseLocation()
		{
			return ItemLocation.INVENTORY;
		}
		
		@Override
		protected ItemLocation getEquipLocation()
		{
			return ItemLocation.PAPERDOLL;
		}
	}
	
	@Test
	void mutationLockIsStablePerItemAndNotShared()
	{
		final Item first = newItem(1, template(1835, false));
		final Item second = newItem(2, template(1835, false));
		
		assertSame(first.getMutationLock(), first.getMutationLock());
		assertNotSame(first.getMutationLock(), second.getMutationLock());
	}
	
	@Test
	void destroyItemWithNoneProcessClearsItemState()
	{
		final Item item = newItem(10, template(1835, true));
		item.setCount(5);
		item.setOwnerId(99);
		item.setItemLocation(ItemLocation.INVENTORY);
		
		final World world = mock(World.class);
		final IdManager idManager = mock(IdManager.class);
		
		try (MockedStatic<World> worldStatic = mockStatic(World.class);
			MockedStatic<IdManager> idStatic = mockStatic(IdManager.class))
		{
			worldStatic.when(World::getInstance).thenReturn(world);
			idStatic.when(IdManager::getInstance).thenReturn(idManager);
			
			ItemManager.destroyItem(ItemProcessType.NONE, item, null, null);
		}
		
		assertEquals(0, item.getCount());
		assertEquals(0, item.getOwnerId());
		assertEquals(ItemLocation.VOID, item.getItemLocation());
		verify(world).removeObject(item);
		verify(idManager).releaseId(10);
	}
	
	@Test
	void destroyItemBlocksWhileMutationLockHeld() throws Exception
	{
		final Item item = newItem(11, template(1835, true));
		item.setCount(1);
		
		final World world = mock(World.class);
		final IdManager idManager = mock(IdManager.class);
		final CountDownLatch started = new CountDownLatch(1);
		final AtomicBoolean finished = new AtomicBoolean(false);
		
		final Thread worker = new Thread(() ->
		{
			started.countDown();
			ItemManager.destroyItem(ItemProcessType.NONE, item, null, null);
			finished.set(true);
		});
		
		try (MockedStatic<World> worldStatic = mockStatic(World.class);
			MockedStatic<IdManager> idStatic = mockStatic(IdManager.class))
		{
			worldStatic.when(World::getInstance).thenReturn(world);
			idStatic.when(IdManager::getInstance).thenReturn(idManager);
			
			synchronized (item.getMutationLock())
			{
				worker.start();
				assertTrue(started.await(2, TimeUnit.SECONDS));
				Thread.sleep(200);
				assertFalse(finished.get());
			}
			
			worker.join(2_000);
			assertTrue(finished.get());
		}
		finally
		{
			worker.interrupt();
			worker.join(1_000);
		}
	}
	
	@Test
	void destroyItemRejectsWhenCountTooHighAndReentrantlyDestroys()
	{
		final Creature owner = mock(Creature.class);
		final TestContainer container = new TestContainer(owner);
		final Item item = spy(newItem(12, template(1835, true)));
		item.setCount(3);
		container.put(item);
		
		doNothing().when(item).updateDatabase();
		doNothing().when(item).updateDatabase(anyBoolean());
		doNothing().when(item).stopAllTasks();
		
		assertNull(container.destroyItem(ItemProcessType.DESTROY, item, 5, null, null));
		assertTrue(container.getItems().contains(item));
		assertEquals(3, item.getCount());
		
		final World world = mock(World.class);
		final IdManager idManager = mock(IdManager.class);
		final ItemLifeTimeTaskManager lifeTime = mock(ItemLifeTimeTaskManager.class);
		
		try (MockedStatic<World> worldStatic = mockStatic(World.class);
			MockedStatic<IdManager> idStatic = mockStatic(IdManager.class);
			MockedStatic<ItemLifeTimeTaskManager> lifeStatic = mockStatic(ItemLifeTimeTaskManager.class))
		{
			worldStatic.when(World::getInstance).thenReturn(world);
			idStatic.when(IdManager::getInstance).thenReturn(idManager);
			lifeStatic.when(ItemLifeTimeTaskManager::getInstance).thenReturn(lifeTime);
			
			synchronized (item.getMutationLock())
			{
				assertSame(item, container.destroyItem(ItemProcessType.DESTROY, item, 3, null, null));
			}
		}
		
		assertFalse(container.getItems().contains(item));
		assertEquals(0, item.getCount());
	}
	
	@Test
	void transferItemMovesNonStackableBetweenContainers()
	{
		final Creature owner = mock(Creature.class);
		when(owner.getObjectId()).thenReturn(42);
		final TestContainer source = new TestContainer(owner);
		final TestContainer target = new TestContainer(owner);
		final Item item = spy(newItem(13, template(1835, false)));
		containerPutAndLocate(source, item);
		
		doNothing().when(item).updateDatabase();
		doNothing().when(item).updateDatabase(anyBoolean());
		
		final Item moved = source.transferItem(ItemProcessType.TRANSFER, 13, 1, target, null, null);
		assertSame(item, moved);
		assertFalse(source.getItems().contains(item));
		assertTrue(target.getItems().contains(item));
	}
	
	private static void containerPutAndLocate(TestContainer container, Item item)
	{
		container.put(item);
		item.setOwnerId(42);
		item.setItemLocation(ItemLocation.INVENTORY);
	}
	
	@Test
	void dropItemReturnsNullWhenItemNotInInventory()
	{
		final Creature owner = mock(Creature.class);
		final TestInventory inventory = new TestInventory(owner);
		final Item item = newItem(14, template(1835, false));
		
		assertNull(inventory.dropItem(ItemProcessType.DROP, item, null, null));
	}
	
	@Test
	void dropItemByObjectIdDropsWholeStackThroughItemOverload()
	{
		final Creature owner = mock(Creature.class);
		final TestInventory inventory = new TestInventory(owner);
		final Item item = spy(newItem(15, template(1835, true)));
		item.setCount(2);
		item.setOwnerId(7);
		item.setItemLocation(ItemLocation.INVENTORY);
		inventory.put(item);
		
		doNothing().when(item).updateDatabase();
		doNothing().when(item).updateDatabase(anyBoolean());
		
		final Item dropped = inventory.dropItem(ItemProcessType.DROP, 15, 2, null, null);
		assertSame(item, dropped);
		assertFalse(inventory.getItems().contains(item));
		assertEquals(ItemLocation.VOID, item.getItemLocation());
		assertEquals(0, item.getOwnerId());
	}
	
	@Test
	void pickupMeClearsSpawnedUnderMutationLock()
	{
		final Item item = newItem(16, template(1835, false));
		item.setSpawned(true);
		
		final Creature creature = mock(Creature.class);
		when(creature.getObjectId()).thenReturn(100);
		doNothing().when(creature).broadcastPacket(any(ServerPacket.class));
		
		final MercTicketManager mercTickets = mock(MercTicketManager.class);
		when(mercTickets.getTicketCastleId(anyInt())).thenReturn(0);
		final World world = mock(World.class);
		
		try (MockedStatic<MercTicketManager> mercStatic = mockStatic(MercTicketManager.class);
			MockedStatic<World> worldStatic = mockStatic(World.class))
		{
			mercStatic.when(MercTicketManager::getInstance).thenReturn(mercTickets);
			worldStatic.when(World::getInstance).thenReturn(world);
			
			item.pickupMe(creature);
		}
		
		assertFalse(item.isSpawned());
		verify(world).removeVisibleObject(eq(item), any());
	}
	
	@Test
	void requestEnchantItemReturnsOnOwnerMismatchInsideLock() throws Exception
	{
		final int itemOid = 20;
		final int scrollOid = 21;
		final Player player = mock(Player.class);
		final GameClient client = mock(GameClient.class);
		final PlayerInventory inventory = mock(PlayerInventory.class);
		final Item item = mock(Item.class);
		final Item scroll = mock(Item.class);
		final EnchantScroll scrollTemplate = mock(EnchantScroll.class);
		final EnchantItemData enchantData = mock(EnchantItemData.class);
		
		when(client.getPlayer()).thenReturn(player);
		when(client.isDetached()).thenReturn(false);
		when(player.isOnline()).thenReturn(true);
		when(player.isProcessingTransaction()).thenReturn(false);
		when(player.isInStoreMode()).thenReturn(false);
		when(player.getInventory()).thenReturn(inventory);
		when(player.getObjectId()).thenReturn(1);
		when(player.getActiveEnchantItemId()).thenReturn(scrollOid);
		when(player.getActiveEnchantSupportItemId()).thenReturn(Player.ID_NONE);
		when(player.getActiveEnchantTimestamp()).thenReturn(System.currentTimeMillis() - 3_000);
		when(scroll.getObjectId()).thenReturn(scrollOid);
		when(inventory.getItemByObjectId(itemOid)).thenReturn(item);
		when(inventory.getItemByObjectId(scrollOid)).thenReturn(scroll);
		when(inventory.destroyItem(eq(ItemProcessType.FEE), eq(scrollOid), eq(1L), eq(player), eq(item))).thenReturn(scroll);
		when(enchantData.getEnchantScroll(scroll)).thenReturn(scrollTemplate);
		when(scrollTemplate.isValid(eq(item), any())).thenReturn(true);
		when(scrollTemplate.getMaxEnchantLevel()).thenReturn(20);
		when(item.getOwnerId()).thenReturn(999);
		when(item.getEnchantLevel()).thenReturn(0);
		when(item.getMutationLock()).thenReturn(new Object());
		
		final RequestEnchantItem packet = new RequestEnchantItem();
		final Field objectIdField = RequestEnchantItem.class.getDeclaredField("_objectId");
		objectIdField.setAccessible(true);
		objectIdField.setInt(packet, itemOid);
		final Field supportIdField = RequestEnchantItem.class.getDeclaredField("_supportId");
		supportIdField.setAccessible(true);
		supportIdField.setInt(packet, 0);
		
		final Method readableInit = ReadablePacket.class.getDeclaredMethod("init", com.l2journey.commons.network.Client.class, ReadableBuffer.class);
		readableInit.setAccessible(true);
		readableInit.invoke(packet, client, mock(ReadableBuffer.class));
		
		try (MockedStatic<EnchantItemData> enchantStatic = mockStatic(EnchantItemData.class))
		{
			enchantStatic.when(EnchantItemData::getInstance).thenReturn(enchantData);
			
			final Method runImpl = RequestEnchantItem.class.getDeclaredMethod("runImpl");
			runImpl.setAccessible(true);
			runImpl.invoke(packet);
		}
		
		verify(player).setActiveEnchantItemId(Player.ID_NONE);
		assertNotNull(item.getMutationLock());
	}
}
