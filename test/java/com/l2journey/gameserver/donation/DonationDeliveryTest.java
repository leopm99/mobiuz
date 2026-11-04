package com.l2journey.gameserver.donation;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import com.l2journey.gameserver.donation.purchase.*;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;
import com.l2journey.gameserver.model.item.instance.Item;
import com.l2journey.gameserver.model.itemcontainer.PlayerInventory;
class DonationDeliveryTest
{
 private MockedStatic<DonationData> dataStatic;
 private MockedStatic<World> worldStatic;
 private MockedStatic<com.l2journey.gameserver.data.xml.ItemData> itemStatic;
 private DonationData data;
 private DonationManager manager;
 private Purchase purchase;
 private Player player;
 private PlayerInventory inventory;
 private Item item;
 @BeforeEach void setup()
 {
  DonationConfig.DONATION_ENABLED=false; manager=new DonationManager();
  data=mock(DonationData.class); dataStatic=mockStatic(DonationData.class); dataStatic.when(DonationData::getInstance).thenReturn(data);
  World world=mock(World.class); worldStatic=mockStatic(World.class); worldStatic.when(World::getInstance).thenReturn(world);
  itemStatic=mockStatic(com.l2journey.gameserver.data.xml.ItemData.class);
  itemStatic.when(com.l2journey.gameserver.data.xml.ItemData::getInstance).thenReturn(mock(com.l2journey.gameserver.data.xml.ItemData.class));
  player=mock(Player.class); inventory=mock(PlayerInventory.class); item=mock(Item.class); purchase=mock(Purchase.class);
  when(purchase.getId()).thenReturn(42); when(purchase.getPlayerId()).thenReturn(7); when(purchase.getProductId()).thenReturn(3470); when(purchase.getQuantity()).thenReturn(10); when(purchase.getPaymentId()).thenReturn("123");
  var state=new AtomicReference<>(PurchaseStatus.WAITING); when(purchase.getStatus()).thenAnswer(i -> state.get()); doAnswer(i -> { state.set(i.getArgument(0)); return null; }).when(purchase).changeStatus(any());
  when(player.getObjectId()).thenReturn(7); when(world.getPlayer(7)).thenReturn(player); when(player.isOnline()).thenReturn(true); when(player.getInventory()).thenReturn(inventory);
  when(inventory.validateCapacityByItemId(3470,10)).thenReturn(true); when(inventory.validateWeightByItemId(3470,10)).thenReturn(true);
  when(data.reserveDelivery(purchase)).thenReturn(true,false); when(data.verifyItem(eq(purchase),anyInt(),anyLong())).thenReturn(true);
  when(player.addItem(any(),eq(3470),eq(10L),eq(player),eq(true))).thenReturn(item);
  when(item.getCount()).thenReturn(10L); when(item.getObjectId()).thenReturn(111);
 }
 @AfterEach void cleanup() { dataStatic.close(); worldStatic.close(); if(itemStatic!=null) itemStatic.close(); }
 @Test void duplicateApprovalCannotGiveSecondReward() { manager.onCompletedPayment(purchase); manager.onCompletedPayment(purchase); verify(player,times(1)).addItem(any(),eq(3470),eq(10L),eq(player),eq(true)); verify(data).finishDelivery(purchase); }
 @Test void offlinePaymentRemainsPendingDelivery() { when(player.isOnline()).thenReturn(false); manager.onCompletedPayment(purchase); verify(purchase).changeStatus(PurchaseStatus.OFFLINE); verify(data,never()).reserveDelivery(any()); }
 @Test void fullInventoryDoesNotConsumeDeliveryClaim() { when(inventory.validateCapacityByItemId(3470,10)).thenReturn(false); manager.onCompletedPayment(purchase); verify(data,never()).reserveDelivery(any()); verify(player,never()).addItem(any(),anyInt(),anyLong(),any(),anyBoolean()); }
 @Test void reservedDeliveryIsNotRepeated() { when(data.reserveDelivery(purchase)).thenReturn(false); manager.onCompletedPayment(purchase); verify(player,never()).addItem(any(),anyInt(),anyLong(),any(),anyBoolean()); }
 @Test void persistenceFailureNeedsReconciliation() { when(data.verifyItem(eq(purchase),anyInt(),anyLong())).thenReturn(false); manager.onCompletedPayment(purchase); verify(data,never()).finishDelivery(any()); verify(purchase,never()).changeStatus(PurchaseStatus.COMPLETED); }
 @Test void missingPaymentCannotGrantItems() { when(purchase.getPaymentId()).thenReturn(null); manager.onCompletedPayment(purchase); verify(purchase,never()).changeStatus(any()); }
 @Test void arbitraryPurchaseIdCannotExposeAnotherPlayersPurchase()
 {
  DonationConfig.DONATION_ENABLED=true; when(data.isReady()).thenReturn(true); when(data.getPurchases(7)).thenReturn(java.util.List.of());
  manager.handleBypass(player,"status 999"); verify(data).getPurchases(7); verify(data,never()).getPurchaseById(999); verify(player,never()).sendPacket(any(com.l2journey.gameserver.network.serverpackets.ServerPacket.class));
 }

}
