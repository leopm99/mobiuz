package com.l2journey.gameserver.donation;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.sql.*;
import org.junit.jupiter.api.Test;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.donation.purchase.*;
class DonationDataTest
{
 @Test void failedInsertDoesNotPublishPurchase() throws Exception
 {
  var data=new DonationData(); var connection=mock(Connection.class); var statement=mock(PreparedStatement.class); var p=mock(Purchase.class); when(p.getBuyerDetails()).thenReturn(BuyerDetails.EMPTY);
  when(p.getPlayerId()).thenReturn(7); when(p.getStatus()).thenReturn(PurchaseStatus.CREATED); when(p.getPaymentMethod()).thenReturn(PaymentMethod.MP_PIX);
  when(connection.prepareStatement(anyString())).thenReturn(statement); when(statement.executeUpdate()).thenThrow(new SQLException("unavailable"));
  try (var db=mockStatic(DatabaseFactory.class)) { db.when(DatabaseFactory::getConnection).thenReturn(connection); assertThrows(IllegalStateException.class,() -> data.store(p)); }
  assertTrue(data.getPurchases(7).isEmpty());
 }
 @Test void metadataFailureRollsBackPurchaseUpdate() throws Exception
 {
  var data=new DonationData(); var connection=mock(Connection.class); var statement=mock(PreparedStatement.class); var p=mock(Purchase.class); when(p.getBuyerDetails()).thenReturn(BuyerDetails.EMPTY);
  when(p.getStatus()).thenReturn(PurchaseStatus.WAITING); when(connection.prepareStatement(anyString())).thenReturn(statement).thenThrow(new SQLException("metadata unavailable")); when(statement.executeUpdate()).thenReturn(1);
  try (var db=mockStatic(DatabaseFactory.class)) { db.when(DatabaseFactory::getConnection).thenReturn(connection); assertThrows(IllegalStateException.class,() -> data.update(p)); }
  verify(connection).rollback(); verify(connection,never()).commit();
 }
 @Test void claimRequiresPendingRow() throws Exception
 {
  var data=new DonationData(); var connection=mock(Connection.class); var statement=mock(PreparedStatement.class); var p=mock(Purchase.class); when(p.getBuyerDetails()).thenReturn(BuyerDetails.EMPTY);
  when(connection.prepareStatement(anyString())).thenReturn(statement); when(statement.executeUpdate()).thenReturn(1,0);
  try (var db=mockStatic(DatabaseFactory.class)) { db.when(DatabaseFactory::getConnection).thenReturn(connection); assertTrue(data.reserveDelivery(p)); assertFalse(data.reserveDelivery(p)); }
  verify(connection,times(2)).prepareStatement(contains("state='PENDING'"));
 }
 @Test void missingDatabaseDisablesService() throws Exception
 {
  var data=new DonationData(); try(var db=mockStatic(DatabaseFactory.class)) { db.when(DatabaseFactory::getConnection).thenThrow(new IllegalStateException("unavailable")); data.restore(); }
  assertFalse(data.isReady()); assertThrows(IllegalStateException.class,data::allocateId);
 }
 @Test void completedPurchasePaymentMetadataRestoresWithoutNullDereference() throws Exception
 {
  var data=new DonationData(); var con=mock(Connection.class); var engine=mock(PreparedStatement.class); var engineRs=mock(ResultSet.class); var purchases=mock(PreparedStatement.class); var purchaseRs=mock(ResultSet.class); var payments=mock(PreparedStatement.class); var paymentRs=mock(ResultSet.class); var type=mock(PreparedStatement.class); var typeRs=mock(ResultSet.class); var reserved=mock(PreparedStatement.class); var reservedRs=mock(ResultSet.class);
  when(con.prepareStatement(contains("SELECT ENGINE"))).thenReturn(engine); when(engine.executeQuery()).thenReturn(engineRs); when(engineRs.next()).thenReturn(true); when(engineRs.getString(1)).thenReturn("InnoDB");
  when(con.prepareStatement("SELECT * FROM donations")).thenReturn(purchases); when(purchases.executeQuery()).thenReturn(purchaseRs); when(purchaseRs.next()).thenReturn(true,false);
  when(purchaseRs.getString("payment_method")).thenReturn("MP_PIX"); when(purchaseRs.getString("status")).thenReturn("COMPLETED"); when(purchaseRs.getInt("purchase_id")).thenReturn(42); when(purchaseRs.getInt("player_id")).thenReturn(7); when(purchaseRs.getBigDecimal("unit_price")).thenReturn(java.math.BigDecimal.ONE);
  when(con.prepareStatement("SELECT * FROM donations_payments")).thenReturn(payments); when(payments.executeQuery()).thenReturn(paymentRs); when(paymentRs.next()).thenReturn(true,true,false); when(paymentRs.getInt("purchase_id")).thenReturn(42,999); when(paymentRs.getString("qrcode")).thenReturn("pix-payload"); when(paymentRs.getString("mp_transaction_id")).thenReturn("PAY123456789");
  when(con.prepareStatement(contains("SELECT DATA_TYPE"))).thenReturn(type); when(type.executeQuery()).thenReturn(typeRs); when(typeRs.next()).thenReturn(true); when(typeRs.getString(1)).thenReturn("decimal");
  when(con.prepareStatement(contains("state='RESERVED'"))).thenReturn(reserved); when(reserved.executeQuery()).thenReturn(reservedRs);
  try(var db=mockStatic(DatabaseFactory.class)) { db.when(DatabaseFactory::getConnection).thenReturn(con); data.restore(); }
  assertTrue(data.isReady()); assertEquals("pix-payload",data.getPurchaseById(42).getQrCode(true)); assertNull(data.getPurchaseById(999)); assertEquals("PAY123456789",data.getPurchaseById(42).getMpTransactionId());
 }

 @Test void ordersApiAndTransactionIdentifiersAreSavedInOneTransaction() throws Exception
 {
  var data=new DonationData(); var con=mock(Connection.class); var primary=mock(PreparedStatement.class); var metadata=mock(PreparedStatement.class); var p=mock(Purchase.class); when(p.getBuyerDetails()).thenReturn(BuyerDetails.EMPTY);
  when(p.getId()).thenReturn(42); when(p.getStatus()).thenReturn(PurchaseStatus.WAITING); when(p.getMpApi()).thenReturn("ORDERS"); when(p.isMpSandbox()).thenReturn(true); when(p.getMpMerchantId()).thenReturn("77"); when(p.getPaymentId()).thenReturn("ORD123456789"); when(p.getMpTransactionId()).thenReturn("PAY123456789"); when(p.isMpSandboxAutoApprove()).thenReturn(true); when(p.getStoredProductName()).thenReturn("Journey Coin"); when(p.getMpRequestVersion()).thenReturn(5); when(p.getBuyerDetails()).thenReturn(new BuyerDetails("Ana", "Silva", null)); when(p.getMpStatementDescriptor()).thenReturn("L2JOURNEY"); when(p.getMpProductCategory()).thenReturn("games");
  when(con.prepareStatement(anyString())).thenReturn(primary,metadata); when(primary.executeUpdate()).thenReturn(1);
  try(var db=mockStatic(DatabaseFactory.class)) { db.when(DatabaseFactory::getConnection).thenReturn(con); data.update(p); }
  verify(primary).setString(1,"ORD123456789"); verify(primary).setString(7,"ORDERS"); verify(primary).setBoolean(8,true); verify(primary).setString(9,"77"); verify(primary).setBoolean(10,true); verify(primary).setString(11,"Journey Coin"); verify(primary).setInt(12,5); verify(primary).setString(13,"games"); verify(primary).setString(14,"L2JOURNEY"); verify(primary).setString(15,"Ana"); verify(primary).setString(16,"Silva"); verify(primary).setString(17,null); verify(primary).setInt(18,42); verify(metadata).setString(6,"PAY123456789"); verify(con).commit(); verify(con,never()).rollback();
 }
 @Test void onlyUnrequestedPixCanChangeProtocol() throws Exception
 {
  var rs=mock(ResultSet.class); when(rs.getString("payment_method")).thenReturn("MP_PIX"); when(rs.getString("status")).thenReturn("CREATED"); when(rs.getString("mp_api")).thenReturn("PAYMENTS");
  when(rs.getString("product_name")).thenReturn("Gold Bar"); var p=new Purchase(rs); assertFalse(p.usesMercadoPagoOrders());
  String previous=DonationConfig.DONATION_MP_MERCHANT_ID; boolean sandbox=DonationConfig.DONATION_MP_SANDBOX;
  try
  {
   DonationConfig.DONATION_MP_MERCHANT_ID="77"; DonationConfig.DONATION_MP_SANDBOX=true; p.useMercadoPagoOrders();
   assertTrue(p.usesMercadoPagoOrders()); assertEquals("77",p.getMpMerchantId()); assertTrue(p.isMpSandbox());
   when(rs.getBoolean("requested")).thenReturn(true); var old=new Purchase(rs);
   assertThrows(IllegalStateException.class,old::useMercadoPagoOrders); assertFalse(old.usesMercadoPagoOrders());
  }
  finally { DonationConfig.DONATION_MP_MERCHANT_ID=previous; DonationConfig.DONATION_MP_SANDBOX=sandbox; }
 }

 @Test void sandboxScenarioIsFrozenAndRestoredAcrossConfigChanges() throws Exception
 {
  var rs=mock(ResultSet.class); when(rs.getString("payment_method")).thenReturn("MP_PIX"); when(rs.getString("status")).thenReturn("CREATED"); when(rs.getString("mp_api")).thenReturn("PAYMENTS");
  when(rs.getString("product_name")).thenReturn("Gold Bar"); boolean previous=DonationConfig.DONATION_MP_SANDBOX_AUTO_APPROVE;
  try
  {
   DonationConfig.DONATION_MP_SANDBOX_AUTO_APPROVE=false; var pending=new Purchase(rs); pending.useMercadoPagoOrders();
   DonationConfig.DONATION_MP_SANDBOX_AUTO_APPROVE=true;
   assertFalse(pending.isMpSandboxAutoApprove());
   when(rs.getString("mp_api")).thenReturn("ORDERS"); when(rs.getBoolean("requested")).thenReturn(true); when(rs.getBoolean("mp_sandbox_auto_approve")).thenReturn(false);
   var restored=new Purchase(rs); assertTrue(restored.usesMercadoPagoOrders()); assertFalse(restored.isMpSandboxAutoApprove());
   assertThrows(IllegalStateException.class,restored::useMercadoPagoOrders);
   when(rs.getBoolean("mp_sandbox_auto_approve")).thenReturn(true);
   assertTrue(new Purchase(rs).isMpSandboxAutoApprove());
  }
  finally { DonationConfig.DONATION_MP_SANDBOX_AUTO_APPROVE=previous; }
 }

 @Test void productSnapshotSurvivesConfigChangesAndRestart() throws Exception
 {
  var rs=mock(ResultSet.class); when(rs.getString("payment_method")).thenReturn("MP_PIX"); when(rs.getString("status")).thenReturn("WAITING"); when(rs.getString("product_name")).thenReturn("Journey Coin"); when(rs.getInt("mp_request_version")).thenReturn(2); when(rs.getBoolean("requested")).thenReturn(true);
  String previous=DonationConfig.DONATION_PRODUCT_NAME;
  try
  {
   DonationConfig.DONATION_PRODUCT_NAME="Different Coin";
   var p=new Purchase(rs);
   assertEquals("Journey Coin",p.getProductName()); assertEquals(2,p.getMpRequestVersion());
   assertThrows(IllegalStateException.class,p::useMercadoPagoOrders);
  }
  finally { DonationConfig.DONATION_PRODUCT_NAME=previous; }
 }
 @Test void categoryIsFrozenBeforeSendingAndRestoredWithoutUsingCurrentConfig() throws Exception
 {
  var rs=mock(ResultSet.class); when(rs.getString("payment_method")).thenReturn("MP_PIX"); when(rs.getString("status")).thenReturn("CREATED"); when(rs.getString("product_name")).thenReturn("Journey Coin");
  String previous=DonationConfig.DONATION_MP_PRODUCT_CATEGORY;
  try
  {
   DonationConfig.DONATION_MP_PRODUCT_CATEGORY="games";
   var p=new Purchase(rs); p.useMercadoPagoOrders();
   DonationConfig.DONATION_MP_PRODUCT_CATEGORY="other";
   assertEquals("games",p.getMpProductCategory()); assertEquals(5,p.getMpRequestVersion());
   when(rs.getString("mp_product_category")).thenReturn("games"); when(rs.getInt("mp_request_version")).thenReturn(3); when(rs.getBoolean("requested")).thenReturn(true);
   var restored=new Purchase(rs);
   assertEquals("games",restored.getMpProductCategory()); assertEquals(3,restored.getMpRequestVersion());
   assertThrows(IllegalStateException.class,restored::useMercadoPagoOrders);
  }
  finally { DonationConfig.DONATION_MP_PRODUCT_CATEGORY=previous; }
 }
 @Test void statementDescriptorIsFrozenAndRestored() throws Exception
 {
  var rs=mock(ResultSet.class); when(rs.getString("payment_method")).thenReturn("MP_PIX"); when(rs.getString("status")).thenReturn("CREATED"); when(rs.getString("product_name")).thenReturn("Journey Coin");
  String previous=DonationConfig.DONATION_MP_STATEMENT_DESCRIPTOR;
  try
  {
   DonationConfig.DONATION_MP_STATEMENT_DESCRIPTOR="L2JOURNEY";
   var p=new Purchase(rs); p.useMercadoPagoOrders();
   DonationConfig.DONATION_MP_STATEMENT_DESCRIPTOR="DIFFERENT";
   assertEquals("L2JOURNEY",p.getMpStatementDescriptor()); assertEquals(5,p.getMpRequestVersion());
   when(rs.getString("mp_statement_descriptor")).thenReturn("L2JOURNEY"); when(rs.getInt("mp_request_version")).thenReturn(4); when(rs.getBoolean("requested")).thenReturn(true);
   var restored=new Purchase(rs); assertEquals("L2JOURNEY",restored.getMpStatementDescriptor()); assertEquals(4,restored.getMpRequestVersion());
   assertThrows(IllegalStateException.class,restored::useMercadoPagoOrders);
  }
  finally { DonationConfig.DONATION_MP_STATEMENT_DESCRIPTOR=previous; }
 }
 @Test void statementDescriptorConfigRejectsInvalidValues()
 {
  assertEquals("",DonationConfig.normalizeStatementDescriptor("   "));
  assertEquals("L2JOURNEY",DonationConfig.normalizeStatementDescriptor(" L2JOURNEY "));
  assertEquals("A".repeat(50),DonationConfig.normalizeStatementDescriptor("A".repeat(50)));
  assertThrows(IllegalArgumentException.class,() -> DonationConfig.normalizeStatementDescriptor("A".repeat(51)));
  assertThrows(IllegalArgumentException.class,() -> DonationConfig.normalizeStatementDescriptor("NAME\nSTORE"));
  assertThrows(IllegalArgumentException.class,() -> DonationConfig.normalizeStatementDescriptor("<STORE>"));
 }
 @Test void buyerDetailsRestoreAndCannotChangeAfterRequest() throws Exception
 {
  var rs=mock(ResultSet.class); when(rs.getString("payment_method")).thenReturn("MP_PIX"); when(rs.getString("status")).thenReturn("WAITING"); when(rs.getString("buyer_first_name")).thenReturn("Ana Maria"); when(rs.getString("buyer_last_name")).thenReturn("Silva"); when(rs.getString("buyer_cpf")).thenReturn("52998224725"); when(rs.getBoolean("requested")).thenReturn(true); when(rs.getInt("mp_request_version")).thenReturn(5);
  var p=new Purchase(rs);
  assertEquals(new BuyerDetails("Ana Maria","Silva","52998224725"),p.getBuyerDetails());
  assertThrows(IllegalStateException.class,() -> p.setBuyerDetails(BuyerDetails.EMPTY));
 }
}
