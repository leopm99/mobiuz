package com.l2journey.gameserver.donation;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import com.google.gson.*;
import com.l2journey.gameserver.donation.purchase.Purchase;
class PaymentValidationTest
{
 private Purchase purchase()
 {
  Purchase p=mock(Purchase.class); when(p.getReference()).thenReturn("journey1p42"); when(p.getCurrency()).thenReturn("BRL"); when(p.getTotalPrice()).thenReturn(new BigDecimal("10.00")); when(p.getPaypalInvoiceId()).thenReturn("INV-42"); when(p.getPaymentId()).thenReturn("123"); return p;
 }
 private JsonObject invoice()
 {
  DonationConfig.DONATION_PAYPAL_ACCOUNT_EMAIL="merchant@example.com";
  return JsonParser.parseString("""
   {"id":"INV-42","status":"PAID","detail":{"reference":"journey1p42"},"invoicer":{"email_address":"merchant@example.com"},"amount":{"currency_code":"BRL","value":"10.00"},"payments":{"transactions":[{"type":"PAYPAL","payment_id":"CAPTURE-1","amount":{"currency_code":"BRL","value":"10.00"}}]}}
   """).getAsJsonObject();
 }
 @Test void acceptsEquivalentDecimalAmounts() { assertTrue(PaymentValidation.matches("x","x","BRL","BRL",new BigDecimal("10.0"),new BigDecimal("10.00"))); }
 @Test void rejectsWrongReference() { assertFalse(PaymentValidation.matches("x","y","BRL","BRL",BigDecimal.ONE,BigDecimal.ONE)); }
 @Test void rejectsWrongCurrency() { assertFalse(PaymentValidation.matches("x","x","BRL","USD",BigDecimal.ONE,BigDecimal.ONE)); }
 @Test void rejectsUnderpayment() { assertFalse(PaymentValidation.matches("x","x","BRL","BRL",BigDecimal.TEN,BigDecimal.ONE)); }
 @Test void rejectsMissingAmount() { assertFalse(PaymentValidation.matches("x","x","BRL","BRL",BigDecimal.ONE,null)); }
 @Test void acceptsVerifiedPaypalPayment() { assertTrue(PaymentValidation.paypal(purchase(),invoice())); }
 @Test void rejectsManuallyMarkedPaypalPayment() { var i=invoice(); i.addProperty("status","MARKED_AS_PAID"); assertFalse(PaymentValidation.paypal(purchase(),i)); }
 @Test void rejectsManualTransactionInsidePaidInvoice() { var i=invoice(); i.getAsJsonObject("payments").getAsJsonArray("transactions").get(0).getAsJsonObject().addProperty("type","EXTERNAL"); assertFalse(PaymentValidation.paypal(purchase(),i)); }
 @Test void rejectsMissingPaypalTransactions() { var i=invoice(); i.remove("payments"); assertFalse(PaymentValidation.paypal(purchase(),i)); }
 @Test void rejectsOtherMerchant() { var i=invoice(); i.getAsJsonObject("invoicer").addProperty("email_address","other@example.com"); assertFalse(PaymentValidation.paypal(purchase(),i)); }
 @Test void rejectsDifferentInvoice() { var i=invoice(); i.addProperty("id","INV-99"); assertFalse(PaymentValidation.paypal(purchase(),i)); }
 @Test void checksBinanceOrderAndMerchant()
 {
  DonationConfig.DONATION_BINANCE_MERCHANT_ID="77";
  var o=JsonParser.parseString("{\"status\":\"PAID\",\"merchantId\":\"77\",\"prepayId\":\"123\",\"merchantTradeNo\":\"journey1p42\",\"currency\":\"BRL\",\"orderAmount\":\"10\"}").getAsJsonObject();
  assertTrue(PaymentValidation.binance(purchase(),o)); o.addProperty("merchantId","88"); assertFalse(PaymentValidation.binance(purchase(),o));
 }
}
