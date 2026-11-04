package com.l2journey.gameserver.donation;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import com.mercadopago.client.payment.*;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.serialization.Serializer;
import com.l2journey.gameserver.donation.util.DonationQrCode;
class DonationCompatibilityTest
{
 @Test void sdkSerializesPixDatesOnJava25WithoutOpeningJdkModules()
 {
  var request=PaymentCreateRequest.builder().transactionAmount(new BigDecimal("10.00")).paymentMethodId("pix").externalReference("journey1p42").dateOfExpiration(OffsetDateTime.parse("2026-11-02T12:00:00-03:00")).payer(PaymentPayerRequest.builder().email("test@example.com").build()).build();
  var json=Serializer.serializeToJson(request);
  assertEquals(OffsetDateTime.parse("2026-11-02T12:00:00-03:00"),OffsetDateTime.parse(json.get("date_of_expiration").getAsString()));
 }
 @Test void sdkDeserializesPaymentDatesOnJava25() throws Exception
 {
  Payment payment=Serializer.deserializeFromJson(Payment.class,"""
   {"id":123,"status":"approved","external_reference":"journey1p42","currency_id":"BRL","transaction_amount":10.00,"collector_id":77,"live_mode":false,"date_approved":"2026-11-02T12:00:00.000-03:00"}
   """);
  assertEquals(77L,payment.getCollectorId()); assertEquals(new BigDecimal("10.00"),payment.getTransactionAmount()); assertNotNull(payment.getDateApproved());
 }
 @Test void qrUsesJourneysDdsEncoder() throws Exception
 {
  byte[] dds=DonationQrCode.create("https://example.com/payment/42");
  assertTrue(dds.length>128); assertEquals("DDS ",new String(dds,0,4,java.nio.charset.StandardCharsets.US_ASCII));
 }
}
