package com.l2journey.gameserver.auctionhouse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import com.l2journey.Config;
import com.l2journey.commons.threads.ThreadPool;
import com.l2journey.gameserver.auctionhouse.AuctionHouseStore.Reservation;
import com.l2journey.gameserver.auctionhouse.AuctionHouseStore.Row;
import com.l2journey.gameserver.model.AccessLevel;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.ItemTemplate;
import com.l2journey.gameserver.model.item.enums.ItemLocation;
import com.l2journey.gameserver.model.item.type.CrystalType;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.itemcontainer.PlayerInventory;
import com.l2journey.gameserver.model.zone.ZoneId;
import com.l2journey.gameserver.network.GameClient;

class AuctionHouseManagerTest
{
	private AuctionHouseStore store;
	private AuctionHouseManager manager;
	private Player buyer;
	private PlayerInventory inventory;
	private Item item;
	private Item adena;
	private AtomicLong funds;
	private MockedStatic<World> worldStatic;
	private MockedStatic<ThreadPool> poolStatic;
	private MockedStatic<com.l2journey.gameserver.data.sql.CharInfoTable> namesStatic;
	private MockedStatic<com.l2journey.gameserver.data.xml.ItemData> itemDataStatic;
	private boolean enabled, peace, mail, boardPeace;
	private double percent;
	private long minimum, maxAdena;
	private int maxListings;
	private final Reservation op = new Reservation(1, "test-operation");

	@BeforeEach void setup() throws Exception
	{
		enabled=Config.COMMUNITY_AUCTION_ENABLED; peace=Config.COMMUNITY_AUCTION_PEACE_ONLY; mail=Config.ALLOW_MAIL;
		boardPeace=Config.COMMUNITYBOARD_PEACE_ONLY; percent=Config.COMMUNITY_AUCTION_FEE_PERCENT;
		minimum=Config.COMMUNITY_AUCTION_MIN_FEE; maxListings=Config.COMMUNITY_AUCTION_MAX_LISTINGS; maxAdena=Config.MAX_ADENA;
		Config.COMMUNITY_AUCTION_ENABLED=true; Config.COMMUNITY_AUCTION_PEACE_ONLY=true; Config.COMMUNITYBOARD_PEACE_ONLY=false;
		Config.ALLOW_MAIL=true; Config.COMMUNITY_AUCTION_FEE_PERCENT=0.5; Config.COMMUNITY_AUCTION_MIN_FEE=10000;
		Config.COMMUNITY_AUCTION_MAX_LISTINGS=10; Config.MAX_ADENA=99900000000L;
		poolStatic=mockStatic(ThreadPool.class);
		worldStatic=mockStatic(World.class);
		namesStatic=mockStatic(com.l2journey.gameserver.data.sql.CharInfoTable.class);
		var names=mock(com.l2journey.gameserver.data.sql.CharInfoTable.class);
		namesStatic.when(com.l2journey.gameserver.data.sql.CharInfoTable::getInstance).thenReturn(names);
		when(names.getNameById(10)).thenReturn("Seller");
		World world=mock(World.class); worldStatic.when(World::getInstance).thenReturn(world);
		store=mock(AuctionHouseStore.class);
		item=mock(Item.class); ItemTemplate template=mock(ItemTemplate.class);
		itemDataStatic=mockStatic(com.l2journey.gameserver.data.xml.ItemData.class);
		var itemData=mock(com.l2journey.gameserver.data.xml.ItemData.class);
		itemDataStatic.when(com.l2journey.gameserver.data.xml.ItemData::getInstance).thenReturn(itemData);
		when(itemData.getTemplate(57001)).thenReturn(template); when(template.getDisplayId()).thenReturn(57001);
		when(item.getObjectId()).thenReturn(100); when(item.getOwnerId()).thenReturn(10); when(item.getId()).thenReturn(57_001);
		when(item.getCount()).thenReturn(40L); when(item.getItemLocation()).thenReturn(ItemLocation.AUCTION);
		when(item.getLocationSlot()).thenReturn(1); when(item.getTemplate()).thenReturn(template);
		when(template.getName()).thenReturn("Test item"); when(template.getCrystalType()).thenReturn(CrystalType.S);
		Row row=new Row(1,10,100,57_001,40,100,System.currentTimeMillis()+3600000,"ACTIVE");
		when(store.load()).thenReturn(List.of(row)); when(store.restore(row)).thenReturn(item);
		when(store.reserve(eq(1), anyString(), anyInt(), eq(item), anyLong(), anyLong(), anyLong())).thenReturn(op);
		buyer=mock(Player.class); inventory=mock(PlayerInventory.class); adena=mock(Item.class);
		funds=new AtomicLong(100000); when(buyer.getAdena()).thenAnswer(i -> funds.get());
		when(buyer.getName()).thenReturn("Buyer"); when(buyer.getObjectId()).thenReturn(20); when(buyer.getClient()).thenReturn(mock(GameClient.class));
		when(buyer.getInventory()).thenReturn(inventory); when(inventory.getItemByItemId(57)).thenReturn(adena);
		when(adena.getCount()).thenAnswer(i -> funds.get()); when(inventory.validateCapacity(anyLong())).thenReturn(true);
		when(inventory.validateWeight(anyLong())).thenReturn(true); when(buyer.isInsideZone(ZoneId.PEACE)).thenReturn(true);
		AccessLevel access=mock(AccessLevel.class); when(access.allowTransaction()).thenReturn(true); when(buyer.getAccessLevel()).thenReturn(access);
		when(buyer.reduceAdena(any(),anyLong(),any(),eq(true))).thenAnswer(i -> { funds.addAndGet(-i.getArgument(1,Long.class)); return true; });
		manager=new AuctionHouseManager(store); manager.start(); assertTrue(manager.isReady());
	}
	@AfterEach void cleanup()
	{
		manager.shutdown(); worldStatic.close(); poolStatic.close(); namesStatic.close(); itemDataStatic.close();
		Config.COMMUNITY_AUCTION_ENABLED=enabled; Config.COMMUNITY_AUCTION_PEACE_ONLY=peace; Config.ALLOW_MAIL=mail;
		Config.COMMUNITYBOARD_PEACE_ONLY=boardPeace; Config.COMMUNITY_AUCTION_FEE_PERCENT=percent;
		Config.COMMUNITY_AUCTION_MIN_FEE=minimum; Config.COMMUNITY_AUCTION_MAX_LISTINGS=maxListings; Config.MAX_ADENA=maxAdena;
	}
	@Test void staleQuantityAndNonpositiveQuantityNeverCharge() throws Exception
	{
		for(long quantity : new long[]{60,0,-1}) { assertThrows(IllegalArgumentException.class, () -> manager.buy(buyer,1,quantity,100)); }
		verify(buyer,never()).reduceAdena(any(),anyLong(),any(),anyBoolean());
		verify(store,never()).reserve(anyInt(),anyString(),anyInt(),any(),anyLong(),anyLong(),anyLong());
	}
	@Test void ownerCannotPurchaseOwnListing()
	{
		when(buyer.getObjectId()).thenReturn(10);
		assertThrows(IllegalArgumentException.class, () -> manager.buy(buyer,1,1,100));
		verify(buyer,never()).reduceAdena(any(),anyLong(),any(),anyBoolean());
	}
	@Test void peaceAndStoreRestrictionsActuallyStopOperations()
	{
		when(buyer.isInsideZone(ZoneId.PEACE)).thenReturn(false);
		assertThrows(IllegalArgumentException.class, () -> manager.buy(buyer,1,1,100));
		when(buyer.isInsideZone(ZoneId.PEACE)).thenReturn(true); when(buyer.isInStoreMode()).thenReturn(true);
		assertThrows(IllegalArgumentException.class, () -> manager.buy(buyer,1,1,100));
		verify(buyer,never()).reduceAdena(any(),anyLong(),any(),anyBoolean());
	}
	@Test void fullInventoryAndWeightAreCheckedBeforeReservation() throws Exception
	{
		when(inventory.validateCapacity(anyLong())).thenReturn(false);
		assertThrows(IllegalArgumentException.class, () -> manager.buy(buyer,1,1,100));
		when(inventory.validateCapacity(anyLong())).thenReturn(true); when(inventory.validateWeight(anyLong())).thenReturn(false);
		assertThrows(IllegalArgumentException.class, () -> manager.buy(buyer,1,1,100));
		verify(store,never()).reserve(anyInt(),anyString(),anyInt(),any(),anyLong(),anyLong(),anyLong());
	}
	@Test void reservationFailureStopsServiceWithoutCharging() throws Exception
	{
		when(store.reserve(anyInt(),anyString(),anyInt(),any(),anyLong(),anyLong(),anyLong())).thenThrow(new SQLException("Unavailable"));
		assertThrows(IllegalArgumentException.class, () -> manager.buy(buyer,1,1,100));
		verify(buyer,never()).reduceAdena(any(),anyLong(),any(),anyBoolean()); assertFalse(manager.isReady());
	}
	@Test void failedChargeVerificationQuarantinesAndCannotChargeAgain() throws Exception
	{
		doThrow(new SQLException("Inventory write failed")).when(store).verifyAdena(20,99900);
		assertThrows(IllegalArgumentException.class, () -> manager.buy(buyer,1,1,100));
		assertThrows(IllegalArgumentException.class, () -> manager.buy(buyer,1,1,100));
		verify(buyer,times(1)).reduceAdena(any(),anyLong(),any(),anyBoolean()); verify(store).review(1);
		verify(store,never()).finish(any(),anyInt(),anyLong(),anyString());
	}
	@Test void runtimeFailureAfterChargeAlsoQuarantines() throws Exception
	{
		doThrow(new NullPointerException("Injected native failure")).when(store).checkpoint(op,"CHARGED",0,0);
		assertThrows(IllegalArgumentException.class, () -> manager.buy(buyer,1,1,100));
		verify(store).review(1); assertThrows(IllegalArgumentException.class, () -> manager.listing(1));
	}
	@Test void failedDebitWithVerifiedUnchangedBalanceReopensListing() throws Exception
	{
		when(buyer.reduceAdena(any(),anyLong(),any(),eq(true))).thenReturn(false);
		assertThrows(IllegalArgumentException.class, () -> manager.buy(buyer,1,1,100));
		verify(store).finish(op,100,40,"ACTIVE"); verify(store,never()).review(anyInt()); assertEquals(40,manager.listing(1).quantity());
	}
	@Test void forgedCancellationCannotAffectAnotherOwner() throws Exception
	{
		assertThrows(IllegalArgumentException.class, () -> manager.cancel(buyer,1));
		verify(store,never()).reserve(anyInt(),anyString(),anyInt(),any(),anyLong(),anyLong(),anyLong());
	}
	@Test void interruptedReservationIsNotRestoredOrReplayed() throws Exception
	{
		AuctionHouseStore other=mock(AuctionHouseStore.class);
		when(other.load()).thenReturn(List.of(new Row(9,10,100,57_001,40,100,Long.MAX_VALUE,"BUSY")));
		AuctionHouseManager restarted=new AuctionHouseManager(other); restarted.start();
		assertTrue(restarted.isReady()); assertTrue(restarted.listings(0).isEmpty()); verify(other,never()).restore(any()); restarted.shutdown();
	}
	@Test void escrowIdentityDriftIsQuarantinedBeforeCharge() throws Exception
	{
		when(item.getLocationSlot()).thenReturn(99);
		assertThrows(IllegalArgumentException.class, () -> manager.buy(buyer,1,1,100));
		verify(store).review(1); verify(buyer,never()).reduceAdena(any(),anyLong(),any(),anyBoolean());
	}
	private AuctionEscrow mockEscrow() throws Exception
	{
		var entriesField = AuctionHouseManager.class.getDeclaredField("entries"); entriesField.setAccessible(true);
		Object entry = ((java.util.Map<?, ?>) entriesField.get(manager)).get(1);
		var escrowField = entry.getClass().getDeclaredField("escrow"); escrowField.setAccessible(true);
		AuctionEscrow escrow = mock(AuctionEscrow.class); escrowField.set(entry, escrow);
		when(escrow.getItemByObjectId(100)).thenReturn(item);
		return escrow;
	}

	@Test void failedTransferWithUnchangedEscrowRefundsExactlyOnce() throws Exception
	{
		mockEscrow();
		when(inventory.addItem(eq(com.l2journey.gameserver.model.item.enums.ItemProcessType.REFUND),eq(57),anyLong(),eq(buyer),isNull()))
			.thenAnswer(i -> { funds.addAndGet(i.getArgument(2,Long.class)); return adena; });
		assertThrows(IllegalArgumentException.class, () -> manager.buy(buyer,1,5,100));
		assertEquals(100000,funds.get()); verify(store).finish(op,100,40,"ACTIVE"); verify(store,never()).review(anyInt());
		verify(inventory,times(1)).addItem(eq(com.l2journey.gameserver.model.item.enums.ItemProcessType.REFUND),eq(57),eq(500L),eq(buyer),isNull());
	}

	@Test void successfulPartialSalePaysOnceAndRevalidatesRemainingStock() throws Exception
	{
		AuctionEscrow escrow=mockEscrow();
		AtomicLong stock=new AtomicLong(40); when(item.getCount()).thenAnswer(i -> stock.get()); when(item.isStackable()).thenReturn(true);
		Item delivered=mock(Item.class); when(delivered.getObjectId()).thenReturn(200); when(delivered.getId()).thenReturn(57001);
		when(delivered.getOwnerId()).thenReturn(20); when(delivered.getCount()).thenReturn(30L); when(delivered.getItemLocation()).thenReturn(ItemLocation.INVENTORY);
		when(escrow.transferItem(any(),eq(100),eq(30L),eq(inventory),eq(buyer),isNull())).thenAnswer(i -> {stock.set(10); return delivered;});
		var mail=mock(com.l2journey.gameserver.model.itemcontainer.Mail.class);
		Item payment=mock(Item.class); when(payment.getCount()).thenReturn(3000L); when(payment.getItemLocation()).thenReturn(ItemLocation.MAIL);
		when(payment.getLocationSlot()).thenReturn(900); when(payment.getOwnerId()).thenReturn(-1); when(payment.getId()).thenReturn(57);
		when(mail.addItem(any(),eq(57),eq(3000L),isNull(),isNull())).thenReturn(payment);
		try(var messages=mockConstruction(com.l2journey.gameserver.model.Message.class,(msg,ctx) -> { assertEquals(com.l2journey.gameserver.network.enums.MessageSenderType.AUCTION_MANAGER,ctx.arguments().get(3)); when(msg.getId()).thenReturn(900); when(msg.createAttachments()).thenReturn(mail); });
			var mailStatic=mockStatic(com.l2journey.gameserver.managers.MailManager.class))
		{
			var mailManager=mock(com.l2journey.gameserver.managers.MailManager.class);
			mailStatic.when(com.l2journey.gameserver.managers.MailManager::getInstance).thenReturn(mailManager);
			manager.buy(buyer,1,30,100);
			assertEquals(10,manager.listing(1).quantity()); assertEquals(97000,funds.get());
			verify(buyer).sendPacket(org.mockito.ArgumentMatchers.<com.l2journey.gameserver.network.serverpackets.ServerPacket>argThat(p -> p instanceof com.l2journey.gameserver.network.serverpackets.SystemMessage sm && sm.getSystemMessageId() == com.l2journey.gameserver.network.SystemMessageId.YOU_HAVE_OBTAINED_S2_S1));
			assertThrows(IllegalArgumentException.class, () -> manager.buy(buyer,1,30,100));
			verify(buyer,times(1)).reduceAdena(any(),anyLong(),any(),anyBoolean()); verify(mailManager,times(1)).sendMessage(any());
			var order=inOrder(store,buyer,escrow,mailManager);
			order.verify(store).reserve(eq(1),eq("BUY"),eq(20),eq(item),eq(30L),eq(3000L),eq(100000L));
			order.verify(buyer).reduceAdena(any(),eq(3000L),isNull(),eq(true));
			order.verify(escrow).transferItem(any(),eq(100),eq(30L),eq(inventory),eq(buyer),isNull());
			order.verify(mailManager).sendMessage(any()); order.verify(store).finish(op,100,10,"ACTIVE");
		}
	}

	@Test void twoConcurrentRequestsCannotRepeatAnInterruptedCharge() throws Exception
	{
		doThrow(new SQLException("Checkpoint failure")).when(store).checkpoint(op,"CHARGED",0,0);
		try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2))
		{
			var gate=new java.util.concurrent.CountDownLatch(1);
			java.util.concurrent.Callable<Boolean> action=() -> { gate.await(); try { manager.buy(buyer,1,5,100); return false; } catch(IllegalArgumentException e) {return true;} };
			var a=executor.submit(action); var b=executor.submit(action); gate.countDown();
			assertTrue(a.get(5,java.util.concurrent.TimeUnit.SECONDS)); assertTrue(b.get(5,java.util.concurrent.TimeUnit.SECONDS));
		}
		verify(buyer,times(1)).reduceAdena(any(),anyLong(),any(),anyBoolean()); verify(store,times(1)).review(1);
	}

	private Item saleSource()
	{
		Item source=mock(Item.class);
		when(source.getObjectId()).thenReturn(300); when(source.getOwnerId()).thenReturn(20); when(source.getId()).thenReturn(57002);
		when(source.getCount()).thenReturn(100L); when(source.getItemLocation()).thenReturn(ItemLocation.INVENTORY);
		when(source.isAvailable(buyer,false,false)).thenReturn(true); when(source.isTradeable()).thenReturn(true); when(source.isStackable()).thenReturn(true);
		when(buyer.checkItemManipulation(eq(300),anyLong(),eq("auction"))).thenReturn(source);
		return source;
	}
	@Test void creationPublishesOnlyAfterDurableEscrowAndFee() throws Exception
	{
		Item source=saleSource(); Reservation creation=new Reservation(2,"create-test");
		when(store.create(eq(20),eq(source),eq(5L),eq(100L),anyLong(),eq(10000L),eq(100000L))).thenReturn(creation);
		Item held=mock(Item.class); when(held.getObjectId()).thenReturn(301); when(held.getOwnerId()).thenReturn(20); when(held.getId()).thenReturn(57002);
		when(held.getCount()).thenReturn(5L); when(held.getItemLocation()).thenReturn(ItemLocation.AUCTION); when(held.getLocationSlot()).thenReturn(2);
		ItemTemplate template=mock(ItemTemplate.class); when(held.getTemplate()).thenReturn(template); when(template.getName()).thenReturn("Crystal"); when(template.getCrystalType()).thenReturn(CrystalType.NONE);
		when(inventory.transferItem(any(),eq(300),eq(5L),any(),eq(buyer),isNull())).thenAnswer(i -> { ((AuctionEscrow)i.getArgument(3)).restoreItem(held); return held; });
		manager.create(buyer,300,5,100,1,10000);
		assertEquals(5,manager.listing(2).quantity()); assertEquals(90000,funds.get());
		var order=inOrder(store,buyer,inventory);
		order.verify(store).create(eq(20),eq(source),eq(5L),eq(100L),anyLong(),eq(10000L),eq(100000L));
		order.verify(buyer).reduceAdena(any(),eq(10000L),isNull(),eq(true));
		order.verify(inventory).transferItem(any(),eq(300),eq(5L),any(),eq(buyer),isNull());
		order.verify(store).finish(creation,301,5,"ACTIVE");
	}
	@Test void creationTransferFailureReturnsFeeAndNeverPublishes() throws Exception
	{
		Item source=saleSource(); Reservation creation=new Reservation(2,"create-test");
		when(store.create(eq(20),eq(source),eq(5L),eq(100L),anyLong(),eq(10000L),eq(100000L))).thenReturn(creation);
		when(inventory.addItem(eq(com.l2journey.gameserver.model.item.enums.ItemProcessType.REFUND),eq(57),eq(10000L),eq(buyer),isNull()))
			.thenAnswer(i -> {funds.addAndGet(10000); return adena;});
		assertThrows(IllegalArgumentException.class, () -> manager.create(buyer,300,5,100,1,10000));
		assertEquals(100000,funds.get()); verify(store).finish(creation,300,0,"CANCELLED"); verify(store,never()).review(anyInt());
		assertThrows(IllegalArgumentException.class, () -> manager.listing(2));
	}
	@Test void expiryRunsWithoutOpeningBoardAndReturnsOnlyOnce() throws Exception
	{
		AuctionEscrow escrow=mockEscrow();
		var entriesField=AuctionHouseManager.class.getDeclaredField("entries"); entriesField.setAccessible(true);
		Object entry=((java.util.Map<?,?>)entriesField.get(manager)).get(1);
		var expiry=entry.getClass().getDeclaredField("expiresAt"); expiry.setAccessible(true); expiry.setLong(entry,1);
		Item returned=mock(Item.class); when(returned.getCount()).thenReturn(40L); when(returned.getItemLocation()).thenReturn(ItemLocation.MAIL); when(returned.getLocationSlot()).thenReturn(901); when(returned.getId()).thenReturn(57001);
		var mail=mock(com.l2journey.gameserver.model.itemcontainer.Mail.class);
		when(escrow.transferItem(any(),eq(100),eq(40L),eq(mail),isNull(),isNull())).thenReturn(returned);
		try(var messages=mockConstruction(com.l2journey.gameserver.model.Message.class,(msg,ctx) -> { assertEquals(com.l2journey.gameserver.network.enums.MessageSenderType.AUCTION_MANAGER,ctx.arguments().get(3)); when(msg.getId()).thenReturn(901); when(msg.createAttachments()).thenReturn(mail); });
			var mailStatic=mockStatic(com.l2journey.gameserver.managers.MailManager.class))
		{
			var mailManager=mock(com.l2journey.gameserver.managers.MailManager.class);
			mailStatic.when(com.l2journey.gameserver.managers.MailManager::getInstance).thenReturn(mailManager);
			var expire=AuctionHouseManager.class.getDeclaredMethod("expire"); expire.setAccessible(true); expire.invoke(manager); expire.invoke(manager);
			verify(mailManager,times(1)).sendMessage(any()); verify(store,times(1)).finish(op,0,0,"EXPIRED");
			assertTrue(manager.listings(0).isEmpty());
		}
	}

	@Test void blockedInventoryPreventsMarketTransactions() throws Exception
	{
		when(inventory.hasInventoryBlock()).thenReturn(true);
		assertThrows(IllegalArgumentException.class, () -> manager.buy(buyer,1,1,100));
		verify(store,never()).reserve(anyInt(),anyString(),anyInt(),any(),anyLong(),anyLong(),anyLong());
	}
	@Test void itemsWhoseDataWouldBeLostAreNotEligible()
	{
		Item source=saleSource(); assertTrue(manager.canList(buyer,source));
		when(source.isAugmented()).thenReturn(true); assertFalse(manager.canList(buyer,source));
		when(source.isAugmented()).thenReturn(false); when(source.getVisualItemId()).thenReturn(1); assertFalse(manager.canList(buyer,source));
		when(source.getVisualItemId()).thenReturn(0); when(source.getCustomType1()).thenReturn(1); assertFalse(manager.canList(buyer,source));
		when(source.getCustomType1()).thenReturn(0); when(source.isTimeLimitedItem()).thenReturn(true); assertFalse(manager.canList(buyer,source));
	}

}

