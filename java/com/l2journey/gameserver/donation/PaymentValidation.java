package com.l2journey.gameserver.donation;
import java.math.BigDecimal;
import java.util.Objects;
import com.google.gson.JsonObject;
import com.l2journey.gameserver.donation.purchase.Purchase;
/** Fail closed on incomplete, mismatched or manually recorded payments. */
public final class PaymentValidation
{
 public static boolean matches(String expectedReference, String actualReference, String expectedCurrency, String actualCurrency, BigDecimal expectedAmount, BigDecimal actualAmount)
 {
  return expectedReference != null && !expectedReference.isBlank() && expectedReference.equals(actualReference) && expectedCurrency != null && expectedCurrency.equals(actualCurrency) && expectedAmount != null && expectedAmount.signum()>0 && actualAmount != null && expectedAmount.compareTo(actualAmount)==0;
 }
 public static String text(JsonObject o,String key) { return o != null && o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null; }
 public static boolean paypal(Purchase p, JsonObject invoice)
 {
  try
  {
   final JsonObject amount=invoice.getAsJsonObject("amount");
   final JsonObject detail=invoice.getAsJsonObject("detail");
   if (!"PAID".equals(text(invoice,"status")) || !Objects.equals(p.getPaypalInvoiceId(),text(invoice,"id")) || !DonationConfig.DONATION_PAYPAL_ACCOUNT_EMAIL.equalsIgnoreCase(text(invoice.getAsJsonObject("invoicer"),"email_address"))) return false;
   if (!matches(p.getReference(),text(detail,"reference"),p.getCurrency(),text(amount,"currency_code"),p.getTotalPrice(),new BigDecimal(text(amount,"value")))) return false;
   final var transactions=invoice.getAsJsonObject("payments").getAsJsonArray("transactions");
   if (transactions == null || transactions.isEmpty()) return false;
   BigDecimal total=BigDecimal.ZERO;
   for (var entry : transactions)
   {
    final JsonObject tx=entry.getAsJsonObject();
    if (!"PAYPAL".equals(text(tx,"type")) || text(tx,"payment_id")==null) return false;
    final JsonObject paid=tx.getAsJsonObject("amount");
    if (!p.getCurrency().equals(text(paid,"currency_code"))) return false;
    total=total.add(new BigDecimal(text(paid,"value")));
   }
   return total.compareTo(p.getTotalPrice())==0;
  }
  catch (RuntimeException e) { return false; }
 }
 public static boolean binance(Purchase p,JsonObject order)
 {
  try
  {
   return "PAID".equals(text(order,"status")) && Objects.equals(p.getPaymentId(),text(order,"prepayId")) && DonationConfig.DONATION_BINANCE_MERCHANT_ID.equals(text(order,"merchantId")) && matches(p.getReference(),text(order,"merchantTradeNo"),p.getCurrency(),text(order,"currency"),p.getTotalPrice(),new BigDecimal(text(order,"orderAmount")));
  }
  catch (RuntimeException e) { return false; }
 }
}
