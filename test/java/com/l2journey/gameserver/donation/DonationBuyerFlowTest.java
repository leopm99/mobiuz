package com.l2journey.gameserver.donation;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import com.l2journey.gameserver.donation.purchase.*;
import com.l2journey.gameserver.model.actor.Player;
class DonationBuyerFlowTest
{
 private DonationManager manager; private DonationData data; private Purchase purchase; private Player player;
 private MockedStatic<DonationData> dataStatic; private boolean enabled;
 @BeforeEach void setup() {
  enabled=DonationConfig.DONATION_ENABLED; DonationConfig.DONATION_ENABLED=false;
  manager=spy(new DonationManager()); data=mock(DonationData.class); player=mock(Player.class); purchase=mock(Purchase.class);
  dataStatic=mockStatic(DonationData.class); dataStatic.when(DonationData::getInstance).thenReturn(data);
  when(data.isReady()).thenReturn(true); when(player.isOnline()).thenReturn(true); when(player.getObjectId()).thenReturn(7);
  when(purchase.getPlayerId()).thenReturn(7); when(purchase.getPaymentMethod()).thenReturn(PaymentMethod.MP_PIX); when(purchase.getExpiration()).thenReturn(Long.MAX_VALUE); when(purchase.getPlayerEmail()).thenReturn("buyer@example.com");
  doNothing().when(manager).showPurchaseStatusWindow(player,purchase); doNothing().when(manager).showCustomWindow(player,"buyer.htm");
  doAnswer(i -> { manager.getWaitingPurchases().remove(7,purchase); return null; }).when(data).store(purchase);
  manager.getWaitingPurchases().put(7,purchase); DonationConfig.DONATION_ENABLED=true;
 }
 @AfterEach void cleanup() { manager.getWaitingPurchases().remove(7); dataStatic.close(); DonationConfig.DONATION_ENABLED=enabled; }
 @Test void optionalDataContinuesThePurchaseOnce() {
  manager.handleBypass(player,"buyer set|Ana Maria|Silva|52998224725|");
  verify(purchase).setBuyerDetails(new BuyerDetails("Ana Maria","Silva","52998224725")); verify(data).store(purchase); verify(manager).showPurchaseStatusWindow(player,purchase);
  manager.handleBypass(player,"buyer set|Ana Maria|Silva|52998224725|"); verify(data,times(1)).store(purchase);
 }
 @Test void skipDoesNotRequireNamesOrCpf() {
  manager.handleBypass(player,"buyer skip"); verify(purchase).setBuyerDetails(BuyerDetails.EMPTY); verify(data).store(purchase);
 }
 @Test void invalidCpfDoesNotPersistOrAdvancePurchase() {
  manager.handleBypass(player,"buyer set|Ana|Silva|52998224724|");
  verify(data,never()).store(any()); verify(purchase,never()).setBuyerDetails(any()); verify(manager).showCustomWindow(player,"buyer.htm"); assertSame(purchase,manager.getWaitingPurchases().get(7));
 }
 @Test void anotherPlayersDraftCannotBeEdited() {
  when(purchase.getPlayerId()).thenReturn(8); manager.handleBypass(player,"buyer skip"); verify(data,never()).store(any()); verify(purchase,never()).setBuyerDetails(any());
 }
 @Test void requestedPurchaseCannotBeEdited() {
  when(purchase.wasRequested()).thenReturn(true); manager.handleBypass(player,"buyer skip"); verify(data,never()).store(any()); verify(purchase,never()).setBuyerDetails(any());
 }
}
