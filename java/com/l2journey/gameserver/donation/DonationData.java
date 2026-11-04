package com.l2journey.gameserver.donation;

import java.util.logging.Level;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import com.l2journey.commons.database.DatabaseFactory;
import com.l2journey.gameserver.donation.purchase.*;
import com.l2journey.gameserver.donation.tasks.DonationTaskManager;

/** Durable purchase records. Financial history is retained independently of character deletion. */
public final class DonationData
{
 private static final Logger LOGGER = Logger.getLogger(DonationData.class.getName());
 private final Map<Integer, List<Purchase>> purchases = new ConcurrentHashMap<>();
 private volatile boolean ready;
 public boolean isReady() { return ready; }
 public Map<Integer, List<Purchase>> getPurchases() { return purchases; }
 public List<Purchase> getPurchases(int playerId) { return purchases.computeIfAbsent(playerId, k -> new CopyOnWriteArrayList<>()); }
 public Purchase getPurchaseById(int id) { return purchases.values().stream().flatMap(List::stream).filter(p -> p.getId() == id).findFirst().orElse(null); }
 public int allocateId()
 {
  if (!ready) throw new IllegalStateException("Donation database is unavailable");
  try (Connection con = DatabaseFactory.getConnection(); PreparedStatement ps = con.prepareStatement("INSERT INTO donation_ids VALUES ()", Statement.RETURN_GENERATED_KEYS))
  {
   ps.executeUpdate();
   try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) return Math.toIntExact(rs.getLong(1)); }
   throw new SQLException("Missing purchase id");
  }
  catch (SQLException e) { throw new IllegalStateException("Could not allocate donation id", e); }
 }
 public void restore()
 {
  ready = false;
  final Map<Integer, Purchase> loaded = new HashMap<>();
  try (Connection con = DatabaseFactory.getConnection())
  {
   for (String table : List.of("donations", "donations_payments", "donation_ids", "donation_deliveries"))
   {
    try (PreparedStatement ps = con.prepareStatement("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?"))
    {
     ps.setString(1, table);
     try (ResultSet rs = ps.executeQuery()) { if (!rs.next() || !"InnoDB".equalsIgnoreCase(rs.getString(1))) throw new SQLException("Missing InnoDB table: " + table); }
    }
   }
   try (PreparedStatement ps = con.prepareStatement("SELECT * FROM donations"); ResultSet rs = ps.executeQuery())
   {
    while (rs.next()) { final Purchase p = new Purchase(rs); loaded.put(p.getId(), p); }
   }
   try (PreparedStatement ps = con.prepareStatement("SELECT * FROM donations_payments"); ResultSet rs = ps.executeQuery())
   {
    while (rs.next())
    {
     final Purchase p = loaded.get(rs.getInt("purchase_id"));
     if (p == null) { LOGGER.warning("DonationData: Orphan donation payment record " + rs.getInt("purchase_id")); continue; }
     p.setMpPreferenceId(rs.getString("mp_preference_id")); p.setPaypalInvoiceId(rs.getString("paypal_invoice_id")); p.setQrCode(rs.getString("qrcode")); p.setLink(rs.getString("link")); p.setMpTransactionId(rs.getString("mp_transaction_id"));
    }
   }
   try (PreparedStatement ps = con.prepareStatement("SELECT DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='donations' AND COLUMN_NAME='unit_price'"); ResultSet rs=ps.executeQuery())
   { if (!rs.next() || !"decimal".equalsIgnoreCase(rs.getString(1))) throw new SQLException("Legacy donation schema requires migration"); }
   purchases.clear();
   for (Purchase p : loaded.values())
   {
    getPurchases(p.getPlayerId()).add(p);
    if (p.wasRequested() && p.getPaymentMethod() == PaymentMethod.PAYPAL && p.getPaypalInvoiceId() == null && p.getStatus() != PurchaseStatus.COMPLETED && p.getStatus() != PurchaseStatus.HIDDEN)
     LOGGER.warning("DonationData: Donation " + p.getId() + " requires PayPal creation reconciliation before another payment is requested");
    if (p.getStatus() == PurchaseStatus.WAITING || p.getStatus() == PurchaseStatus.CREATED || p.getStatus() == PurchaseStatus.PENDING || p.getStatus() == PurchaseStatus.FINISHING) DonationTaskManager.getInstance().add(p);
   }
   ready = true;
   LOGGER.info("DonationData: Loaded " + loaded.size() + " donation purchases");
   try (PreparedStatement ps = con.prepareStatement("SELECT purchase_id FROM donation_deliveries WHERE state='RESERVED'"); ResultSet rs = ps.executeQuery())
   { while (rs.next()) LOGGER.severe("DonationData: Donation " + rs.getInt(1) + " requires manual delivery reconciliation; automatic redelivery is blocked"); }
  }
  catch (Exception e) { LOGGER.log(Level.SEVERE, "DonationData: Donation service disabled: could not restore purchases", e); }
 }
 public void store(Purchase p)
 {
  try (Connection con = DatabaseFactory.getConnection(); PreparedStatement ps = con.prepareStatement("INSERT INTO donations (purchase_id,player_id,email,product_id,quantity,unit_price,date,status,payment_method,currency,terms,expires_at,reference,requested,mp_api,mp_sandbox,mp_merchant_id,mp_sandbox_auto_approve,product_name,mp_request_version,mp_product_category,mp_statement_descriptor,buyer_first_name,buyer_last_name,buyer_cpf) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"))
  {
   ps.setInt(1,p.getId()); ps.setInt(2,p.getPlayerId()); ps.setString(3,p.getPlayerEmail()); ps.setInt(4,p.getProductId()); ps.setInt(5,p.getQuantity()); ps.setBigDecimal(6,p.getUnitPrice()); ps.setLong(7,p.getDate()); ps.setString(8,p.getStatus().name()); ps.setString(9,p.getPaymentMethod().name()); ps.setString(10,p.getCurrency()); ps.setBoolean(11,p.agreedTerms()); ps.setLong(12,p.getExpiration()); ps.setString(13,p.getReference()); ps.setBoolean(14,p.wasRequested()); ps.setString(15,p.getMpApi()); ps.setBoolean(16,p.isMpSandbox()); ps.setString(17,p.getMpMerchantId()); ps.setBoolean(18,p.isMpSandboxAutoApprove()); ps.setString(19,p.getStoredProductName()); ps.setInt(20,p.getMpRequestVersion()); ps.setString(21,p.getMpProductCategory()); ps.setString(22,p.getMpStatementDescriptor()); ps.setString(23,p.getBuyerDetails().firstName()); ps.setString(24,p.getBuyerDetails().lastName()); ps.setString(25,p.getBuyerDetails().cpf()); ps.executeUpdate();
   LOGGER.info("DonationData: Donation purchase " + p.getId() + " stored: item " + p.getProductId() + ", quantity " + p.getQuantity() + ", total " + p.getTotalPrice() + " " + p.getCurrency());
   getPurchases(p.getPlayerId()).add(p); DonationTaskManager.getInstance().add(p);
   DonationManager.getInstance().getWaitingPurchases().remove(p.getPlayerId(), p);
  }
  catch (SQLException e) { throw new IllegalStateException("Could not persist donation purchase " + p.getId(), e); }
 }
 public void update(Purchase p)
 {
  try (Connection con = DatabaseFactory.getConnection())
  {
   con.setAutoCommit(false);
   try
   {
    try (PreparedStatement ps = con.prepareStatement("UPDATE donations SET payment_id=?,email=?,status=?,terms=?,requested=?,hidden=?,mp_api=?,mp_sandbox=?,mp_merchant_id=?,mp_sandbox_auto_approve=?,product_name=?,mp_request_version=?,mp_product_category=?,mp_statement_descriptor=?,buyer_first_name=?,buyer_last_name=?,buyer_cpf=? WHERE purchase_id=?"))
    {
     ps.setString(1,p.getPaymentId()); ps.setString(2,p.getPlayerEmail()); ps.setString(3,p.getStatus().name()); ps.setBoolean(4,p.agreedTerms()); ps.setBoolean(5,p.wasRequested()); ps.setBoolean(6,p.isHidden()); ps.setString(7,p.getMpApi()); ps.setBoolean(8,p.isMpSandbox()); ps.setString(9,p.getMpMerchantId()); ps.setBoolean(10,p.isMpSandboxAutoApprove()); ps.setString(11,p.getStoredProductName()); ps.setInt(12,p.getMpRequestVersion()); ps.setString(13,p.getMpProductCategory()); ps.setString(14,p.getMpStatementDescriptor()); ps.setString(15,p.getBuyerDetails().firstName()); ps.setString(16,p.getBuyerDetails().lastName()); ps.setString(17,p.getBuyerDetails().cpf()); ps.setInt(18,p.getId());
     if (ps.executeUpdate() != 1) throw new SQLException("Missing purchase " + p.getId());
    }
    try (PreparedStatement ps = con.prepareStatement("INSERT INTO donations_payments (purchase_id,mp_preference_id,paypal_invoice_id,qrcode,link,mp_transaction_id) VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE mp_preference_id=VALUES(mp_preference_id),paypal_invoice_id=VALUES(paypal_invoice_id),qrcode=VALUES(qrcode),link=VALUES(link),mp_transaction_id=VALUES(mp_transaction_id)"))
    {
     ps.setInt(1,p.getId()); ps.setString(2,p.getMpPreferenceId()); ps.setString(3,p.getPaypalInvoiceId()); ps.setString(4,p.getQrCode(true)); ps.setString(5,p.getLink(true)); ps.setString(6,p.getMpTransactionId()); ps.executeUpdate();
    }
    if (p.getStatus() == PurchaseStatus.OFFLINE)
    {
     try (PreparedStatement ps = con.prepareStatement("INSERT INTO donation_deliveries (purchase_id,state) VALUES (?,'PENDING') ON DUPLICATE KEY UPDATE purchase_id=VALUES(purchase_id)")) { ps.setInt(1,p.getId()); ps.executeUpdate(); }
    }
    con.commit();
   }
   catch (SQLException | RuntimeException e) { con.rollback(); throw e; }
  }
  catch (SQLException e) { throw new IllegalStateException("Could not update donation purchase " + p.getId(), e); }
 }
 public boolean reserveDelivery(Purchase p)
 {
  try (Connection con = DatabaseFactory.getConnection(); PreparedStatement ps = con.prepareStatement("UPDATE donation_deliveries SET state='RESERVED',updated_at=CURRENT_TIMESTAMP WHERE purchase_id=? AND state='PENDING'"))
  { ps.setInt(1,p.getId()); return ps.executeUpdate() == 1; }
  catch (SQLException e) { throw new IllegalStateException("Could not reserve donation delivery", e); }
 }
 public boolean verifyItem(Purchase p, int objectId, long expectedCount)
 {
  try (Connection con = DatabaseFactory.getConnection(); PreparedStatement ps = con.prepareStatement("SELECT count FROM items WHERE object_id=? AND owner_id=? AND item_id=? AND loc='INVENTORY'"))
  { ps.setInt(1,objectId); ps.setInt(2,p.getPlayerId()); ps.setInt(3,p.getProductId()); try (ResultSet rs=ps.executeQuery()) { return rs.next() && rs.getLong(1)>=expectedCount; } }
  catch (SQLException e) { throw new IllegalStateException("Could not verify donation item persistence",e); }
 }
 public void finishDelivery(Purchase p)
 {
  try (Connection con = DatabaseFactory.getConnection())
  {
   con.setAutoCommit(false);
   try
   {
    try (PreparedStatement ps = con.prepareStatement("UPDATE donation_deliveries SET state='DELIVERED',updated_at=CURRENT_TIMESTAMP WHERE purchase_id=? AND state='RESERVED'"))
    { ps.setInt(1,p.getId()); if (ps.executeUpdate() != 1) throw new SQLException("Missing delivery reservation"); }
    try (PreparedStatement ps = con.prepareStatement("UPDATE donations SET status='COMPLETED' WHERE purchase_id=? AND status='OFFLINE'"))
    { ps.setInt(1,p.getId()); if (ps.executeUpdate() != 1) throw new SQLException("Invalid purchase delivery status"); }
    con.commit();
    LOGGER.info("DonationData: Donation purchase " + p.getId() + " delivery committed");
   }
   catch (SQLException e) { con.rollback(); throw e; }
  }
  catch (SQLException e) { throw new IllegalStateException("Donation delivery needs manual reconciliation: " + p.getId(), e); }
 }
 public void delete(Purchase p, boolean purchase, boolean payment)
 {
  if (purchase && p.getStatus() != PurchaseStatus.COMPLETED && p.getStatus() != PurchaseStatus.OFFLINE) p.changeStatus(PurchaseStatus.EXPIRED);
 }
 public static DonationData getInstance() { return Holder.INSTANCE; }
 private static class Holder { private static final DonationData INSTANCE = new DonationData(); }
}
