package com.l2journey.gameserver.donation;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.*;
import com.l2journey.gameserver.donation.purchase.*;
class DonationReconciliationTest
{
 private boolean sandbox; private String merchant; private Purchase purchase;
 private final long now=1700000000000L;
 @BeforeEach void setup() {
  sandbox=DonationConfig.DONATION_MP_SANDBOX; merchant=DonationConfig.DONATION_MP_MERCHANT_ID;
  DonationConfig.DONATION_MP_SANDBOX=false; DonationConfig.DONATION_MP_MERCHANT_ID="77";
  purchase=mock(Purchase.class); when(purchase.usesMercadoPagoOrders()).thenReturn(true); when(purchase.isMpSandbox()).thenReturn(false); when(purchase.getMpMerchantId()).thenReturn("77"); when(purchase.wasRequested()).thenReturn(true); when(purchase.getStatus()).thenReturn(PurchaseStatus.WAITING); when(purchase.getDate()).thenReturn(now-1000);
 }
 @AfterEach void cleanup() { DonationConfig.DONATION_MP_SANDBOX=sandbox; DonationConfig.DONATION_MP_MERCHANT_ID=merchant; }
 @Test void testOrdersAreSkippedInProductionAndResumeInTheirOriginalEnvironment() {
  when(purchase.isMpSandbox()).thenReturn(true); assertFalse(DonationReconciliation.shouldCheck(purchase,now));
  DonationConfig.DONATION_MP_SANDBOX=true; assertTrue(DonationReconciliation.shouldCheck(purchase,now)); verify(purchase,never()).changeStatus(any());
 }
 @Test void anotherMerchantOrMissingSavedMerchantIsSkipped() {
  when(purchase.getMpMerchantId()).thenReturn("88"); assertFalse(DonationReconciliation.shouldCheck(purchase,now));
  when(purchase.getMpMerchantId()).thenReturn(null); assertFalse(DonationReconciliation.shouldCheck(purchase,now));
 }
 @Test void currentOrdersStillReconcileLateApprovalsAfterCancellationOrExpiration() {
  for(var status : new PurchaseStatus[] {PurchaseStatus.WAITING,PurchaseStatus.FINISHING,PurchaseStatus.CANCELED,PurchaseStatus.EXPIRED,PurchaseStatus.FAILED}) {
   when(purchase.getStatus()).thenReturn(status); assertTrue(DonationReconciliation.shouldCheck(purchase,now));
  }
 }
 @Test void deliveredAndUnrequestedOrOldPurchasesAreNotPolled() {
  for(var status : new PurchaseStatus[] {PurchaseStatus.COMPLETED,PurchaseStatus.OFFLINE,PurchaseStatus.HIDDEN}) {
   when(purchase.getStatus()).thenReturn(status); assertFalse(DonationReconciliation.shouldCheck(purchase,now));
  }
  when(purchase.getStatus()).thenReturn(PurchaseStatus.WAITING); when(purchase.wasRequested()).thenReturn(false); assertFalse(DonationReconciliation.shouldCheck(purchase,now));
  when(purchase.wasRequested()).thenReturn(true); when(purchase.getDate()).thenReturn(now-java.util.concurrent.TimeUnit.DAYS.toMillis(8)); assertFalse(DonationReconciliation.shouldCheck(purchase,now));
 }
 @Test void hidingAHistoryEntryDoesNotDiscardFinancialReconciliation() {
  when(purchase.getStatus()).thenReturn(PurchaseStatus.CANCELED); when(purchase.isHidden()).thenReturn(true);
  assertTrue(DonationReconciliation.shouldCheck(purchase,now));
 }
}
