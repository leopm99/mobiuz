package com.l2journey.gameserver.donation.paymenthandlers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.l2journey.gameserver.donation.*;
import com.l2journey.gameserver.donation.purchase.*;
import com.l2journey.gameserver.model.World;

class MercadoPagoOrdersTest
{
	private Purchase _purchase;
	private DonationData _data;
	private DonationManager _manager;
	private MockedStatic<DonationData> _dataStatic;
	private MockedStatic<DonationManager> _managerStatic;
	private MockedStatic<World> _worldStatic;
	private String _merchant;
	private boolean _sandbox;
	private record Call(String method, String path, String key, JsonObject payload) { }

	@BeforeEach
	void setup()
	{
		_merchant = DonationConfig.DONATION_MP_MERCHANT_ID;
		_sandbox = DonationConfig.DONATION_MP_SANDBOX;
		DonationConfig.DONATION_MP_MERCHANT_ID = "77";
		DonationConfig.DONATION_MP_SANDBOX = true;
		_purchase = mock(Purchase.class);
		when(_purchase.getReference()).thenReturn("journey1p42");
		when(_purchase.getPaymentMethod()).thenReturn(PaymentMethod.MP_PIX);
		when(_purchase.getCurrency()).thenReturn("BRL");
		when(_purchase.getTotalPrice()).thenReturn(new BigDecimal("10.00"));
		when(_purchase.getMpRequestVersion()).thenReturn(2);
		when(_purchase.getProductName()).thenReturn("Journey Coin");
		when(_purchase.getProductId()).thenReturn(3470);
		when(_purchase.getQuantity()).thenReturn(5);
		when(_purchase.getUnitPrice()).thenReturn(new BigDecimal("2.00"));
		when(_purchase.getPlayerEmail()).thenReturn("buyer@example.com");
		when(_purchase.getDate()).thenReturn(1700000000000L);
		when(_purchase.getExpiration()).thenReturn(1700000000000L + Duration.ofMinutes(30).toMillis());
		when(_purchase.getMpMerchantId()).thenReturn("77");
		when(_purchase.isMpSandbox()).thenReturn(true);
		when(_purchase.isMpSandboxAutoApprove()).thenReturn(true);
		when(_purchase.usesMercadoPagoOrders()).thenReturn(true);
		AtomicReference<String> id = new AtomicReference<>("ORD123456789");
		when(_purchase.getPaymentId()).thenAnswer(i -> id.get());
		doAnswer(i -> { id.set(i.getArgument(0)); return null; }).when(_purchase).setPaymentId(nullable(String.class));
		AtomicReference<String> transaction = new AtomicReference<>();
		when(_purchase.getMpTransactionId()).thenAnswer(i -> transaction.get());
		doAnswer(i -> { transaction.set(i.getArgument(0)); return null; }).when(_purchase).setMpTransactionId(anyString());
		AtomicReference<PurchaseStatus> state = new AtomicReference<>(PurchaseStatus.WAITING);
		when(_purchase.getStatus()).thenAnswer(i -> state.get());
		doAnswer(i -> { state.set(i.getArgument(0)); return null; }).when(_purchase).changeStatus(any());
		_data = mock(DonationData.class);
		_dataStatic = mockStatic(DonationData.class);
		_dataStatic.when(DonationData::getInstance).thenReturn(_data);
		_manager = mock(DonationManager.class);
		_managerStatic = mockStatic(DonationManager.class);
		_managerStatic.when(DonationManager::getInstance).thenReturn(_manager);
		_worldStatic = mockStatic(World.class);
		_worldStatic.when(World::getInstance).thenReturn(mock(World.class));
	}

	@AfterEach
	void cleanup()
	{
		_dataStatic.close();
		_managerStatic.close();
		_worldStatic.close();
		DonationConfig.DONATION_MP_MERCHANT_ID = _merchant;
		DonationConfig.DONATION_MP_SANDBOX = _sandbox;
	}

	private JsonObject order()
	{
		return JsonParser.parseString("""
			{"id":"ORD123456789","type":"online","country_code":"BRA","user_id":"77","external_reference":"journey1p42","total_amount":"10.00","total_paid_amount":"10.00","status":"processed","status_detail":"accredited","transactions":{"payments":[{"id":"PAY123456789","amount":"10.00","paid_amount":"10.00","status":"processed","status_detail":"accredited","payment_method":{"id":"pix","type":"bank_transfer","qr_code":"pix-code"}}]}}
			""").getAsJsonObject();
	}

	private JsonObject payment(JsonObject o)
	{
		return o.getAsJsonObject("transactions").getAsJsonArray("payments").get(0).getAsJsonObject();
	}

	@Test void createsPixOrdersPayloadAndStableKey()
	{
		JsonObject payload = MercadoPagoOrders.createPayload(_purchase);
		assertEquals("online",payload.get("type").getAsString());
		assertEquals("automatic",payload.get("processing_mode").getAsString());
		assertFalse(payload.has("expiration_time"));
		assertEquals("PT30M",payment(payload).get("expiration_time").getAsString());
		assertEquals("APRO",payload.getAsJsonObject("payer").get("first_name").getAsString());
		assertEquals(payload,MercadoPagoOrders.createPayload(_purchase));
		assertEquals(MercadoPagoOrders.idempotencyKey(_purchase,"create"),MercadoPagoOrders.idempotencyKey(_purchase,"create"));
		assertNotEquals(MercadoPagoOrders.idempotencyKey(_purchase,"create"),MercadoPagoOrders.idempotencyKey(_purchase,"cancel"));
	}

	@Test void includesConfiguredProductAndConsistentAmounts()
	{
		JsonObject item=MercadoPagoOrders.createPayload(_purchase).getAsJsonArray("items").get(0).getAsJsonObject();
		assertEquals("Journey Coin",item.get("title").getAsString());
		assertEquals("3470",item.get("external_code").getAsString());
		assertEquals(5,item.get("quantity").getAsInt());
		assertEquals("2.00",item.get("unit_price").getAsString());
		when(_purchase.getQuantity()).thenReturn(6);
		assertThrows(IllegalArgumentException.class,() -> MercadoPagoOrders.createPayload(_purchase));
	}

	@Test void oldRequestsKeepOriginalPayloadWhenRetried()
	{
		when(_purchase.getMpRequestVersion()).thenReturn(1);
		assertFalse(MercadoPagoOrders.createPayload(_purchase).has("items"));
		verify(_purchase,never()).getProductName();
	}

	@Test void transportRequiresModernTlsAndCertificateHostnameValidation()
	{
		try (var client=MercadoPagoOrders.HttpTransport.newClient())
		{
			assertArrayEquals(new String[] { "TLSv1.3", "TLSv1.2" },client.sslParameters().getProtocols());
			assertEquals("HTTPS",client.sslParameters().getEndpointIdentificationAlgorithm());
			assertEquals(java.net.http.HttpClient.Redirect.NEVER,client.followRedirects());
		}
	}

	@Test void distinctPurchasesHaveDistinctOperationKeys()
	{
		String original=MercadoPagoOrders.idempotencyKey(_purchase,"create");
		when(_purchase.getReference()).thenReturn("journey1p43");
		assertNotEquals(original,MercadoPagoOrders.idempotencyKey(_purchase,"create"));
	}

	@Test void categoryIsOnlySentForNewPayloadVersion()
	{
		when(_purchase.getMpProductCategory()).thenReturn("games");
		// A version 2 retry must preserve the original body, even if a category is available.
		assertFalse(MercadoPagoOrders.createPayload(_purchase).getAsJsonArray("items").get(0).getAsJsonObject().has("category_id"));
		when(_purchase.getMpRequestVersion()).thenReturn(3);
		assertEquals("games",MercadoPagoOrders.createPayload(_purchase).getAsJsonArray("items").get(0).getAsJsonObject().get("category_id").getAsString());
		when(_purchase.getMpProductCategory()).thenReturn("");
		assertFalse(MercadoPagoOrders.createPayload(_purchase).getAsJsonArray("items").get(0).getAsJsonObject().has("category_id"));
		when(_purchase.getMpProductCategory()).thenReturn(null);
		assertFalse(MercadoPagoOrders.createPayload(_purchase).getAsJsonArray("items").get(0).getAsJsonObject().has("category_id"));
	}

	@Test void descriptorUsesTheDocumentedNestedFieldOnlyForNewRequests()
	{
		when(_purchase.getMpStatementDescriptor()).thenReturn("L2JOURNEY");
		for (int version : new int[] { 1, 2, 3 })
		{
			when(_purchase.getMpRequestVersion()).thenReturn(version);
			assertFalse(payment(MercadoPagoOrders.createPayload(_purchase)).getAsJsonObject("payment_method").has("statement_descriptor"));
		}
		when(_purchase.getMpRequestVersion()).thenReturn(4);
		JsonObject payload=MercadoPagoOrders.createPayload(_purchase);
		assertEquals("L2JOURNEY",payment(payload).getAsJsonObject("payment_method").get("statement_descriptor").getAsString());
		assertFalse(payload.has("statement_descriptor"));
		assertEquals("pix",payment(payload).getAsJsonObject("payment_method").get("id").getAsString());
		assertEquals("bank_transfer",payment(payload).getAsJsonObject("payment_method").get("type").getAsString());
		assertEquals(payload,MercadoPagoOrders.createPayload(_purchase));
	}

	@Test void blankDescriptorKeepsTheCurrentPixBody()
	{
		when(_purchase.getMpRequestVersion()).thenReturn(4);
		for (String descriptor : new String[] { null, "", " " })
		{
			when(_purchase.getMpStatementDescriptor()).thenReturn(descriptor);
			assertFalse(payment(MercadoPagoOrders.createPayload(_purchase)).getAsJsonObject("payment_method").has("statement_descriptor"));
		}
	}

	@Test void optionalBuyerDetailsAreOnlySentForNewRequests()
	{
		when(_purchase.getBuyerDetails()).thenReturn(new BuyerDetails("Ana Maria", "Silva", "52998224725"));
		when(_purchase.isMpSandboxAutoApprove()).thenReturn(false);
		for (int version : new int[] { 1, 2, 3, 4 })
		{
			when(_purchase.getMpRequestVersion()).thenReturn(version);
			var payer=MercadoPagoOrders.createPayload(_purchase).getAsJsonObject("payer");
			assertFalse(payer.has("first_name")); assertFalse(payer.has("last_name")); assertFalse(payer.has("identification"));
		}
		when(_purchase.getMpRequestVersion()).thenReturn(5);
		var payload=MercadoPagoOrders.createPayload(_purchase); var payer=payload.getAsJsonObject("payer");
		assertEquals("Ana Maria",payer.get("first_name").getAsString()); assertEquals("Silva",payer.get("last_name").getAsString());
		assertEquals("CPF",payer.getAsJsonObject("identification").get("type").getAsString()); assertEquals("52998224725",payer.getAsJsonObject("identification").get("number").getAsString());
		assertEquals(payload,MercadoPagoOrders.createPayload(_purchase));
	}

	@Test void blankBuyerDetailsDoNotCreateEmptyPayerFields()
	{
		when(_purchase.getMpRequestVersion()).thenReturn(5);
		when(_purchase.isMpSandboxAutoApprove()).thenReturn(false);
		when(_purchase.getBuyerDetails()).thenReturn(BuyerDetails.EMPTY);
		var payer=MercadoPagoOrders.createPayload(_purchase).getAsJsonObject("payer");
		assertFalse(payer.has("first_name")); assertFalse(payer.has("last_name")); assertFalse(payer.has("identification"));
	}

	@Test void sandboxAutoApprovalKeepsOfficialScenarioName()
	{
		when(_purchase.getMpRequestVersion()).thenReturn(5);
		when(_purchase.getBuyerDetails()).thenReturn(new BuyerDetails("Ana", null, null));
		assertEquals("APRO",MercadoPagoOrders.createPayload(_purchase).getAsJsonObject("payer").get("first_name").getAsString());
		when(_purchase.isMpSandbox()).thenReturn(false);
		assertEquals("Ana",MercadoPagoOrders.createPayload(_purchase).getAsJsonObject("payer").get("first_name").getAsString());
	}

	@Test void productionDoesNotSendSandboxApprovalName()
	{
		when(_purchase.isMpSandbox()).thenReturn(false);
		assertFalse(MercadoPagoOrders.createPayload(_purchase).getAsJsonObject("payer").has("first_name"));
	}

	@Test void rejectsUnsupportedCurrency()
	{
		when(_purchase.getCurrency()).thenReturn("USD");
		assertThrows(IllegalArgumentException.class,() -> MercadoPagoOrders.createPayload(_purchase));
	}

	@Test void rejectsOutOfRangeExpiration()
	{
		when(_purchase.getExpiration()).thenReturn(1700000001000L);
		assertThrows(IllegalArgumentException.class,() -> MercadoPagoOrders.createPayload(_purchase));
	}

	@Test void bothOrderAndTransactionMustBeAccredited()
	{
		JsonObject o=order(); assertTrue(MercadoPagoOrders.approved(_purchase,o));
		payment(o).addProperty("status","action_required"); assertFalse(MercadoPagoOrders.approved(_purchase,o));
	}

	@Test void processedWithoutAccreditationDoesNotGrant()
	{
		JsonObject o=order(); o.addProperty("status_detail","waiting_capture");
		assertFalse(MercadoPagoOrders.approved(_purchase,o));
	}

	@Test void mismatchedMerchantOrReferenceIsRejected()
	{
		JsonObject o=order(); o.addProperty("user_id","88"); assertFalse(MercadoPagoOrders.matches(_purchase,o));
		o=order(); o.addProperty("external_reference","another-purchase"); assertFalse(MercadoPagoOrders.matches(_purchase,o));
	}

	@Test void mismatchedAmountsOrCurrencyAreRejected()
	{
		JsonObject o=order(); payment(o).addProperty("amount","1.00"); assertFalse(MercadoPagoOrders.matches(_purchase,o));
		o=order(); o.addProperty("currency","USD"); assertFalse(MercadoPagoOrders.matches(_purchase,o));
		o=order(); o.addProperty("total_paid_amount","1.00"); assertFalse(MercadoPagoOrders.approved(_purchase,o));
	}

	@Test void changedTransactionOrOrderIdIsRejected()
	{
		when(_purchase.getMpTransactionId()).thenReturn("PAY999999999");
		assertFalse(MercadoPagoOrders.matches(_purchase,order()));
		when(_purchase.getMpTransactionId()).thenReturn(null);
		JsonObject o=order(); o.addProperty("id","ORD999999999"); assertFalse(MercadoPagoOrders.matches(_purchase,o));
	}

	@Test void additionalPaymentsOrWrongMethodAreRejected()
	{
		JsonObject o=order(); o.getAsJsonObject("transactions").getAsJsonArray("payments").add(payment(o).deepCopy());
		assertFalse(MercadoPagoOrders.matches(_purchase,o));
		o=order(); payment(o).getAsJsonObject("payment_method").addProperty("id","visa"); assertFalse(MercadoPagoOrders.matches(_purchase,o));
	}

	@Test void refundsAndChargebacksNeverGrant()
	{
		for (String name : List.of("refunds","chargebacks"))
		{
			JsonObject o=order(); JsonArray entries=new JsonArray(); entries.add(new JsonObject()); o.getAsJsonObject("transactions").add(name,entries);
			assertFalse(MercadoPagoOrders.approved(_purchase,o));
		}
	}

	@Test void pendingAndMalformedResponsesNeverGrant()
	{
		JsonObject o=order(); o.addProperty("status","action_required"); assertFalse(MercadoPagoOrders.approved(_purchase,o));
		assertFalse(MercadoPagoOrders.approved(_purchase,new JsonObject()));
	}

	@Test void processesApprovedCanonicalOrderWithoutPoppingBackgroundWindows()
	{
		new MercadoPagoOrders((method,path,key,body,sandbox) -> order()).check(_purchase,false);
		verify(_purchase).setMpTransactionId("PAY123456789");
		verify(_data).update(_purchase);
		verify(_manager).onCompletedPayment(_purchase);
		verify(_manager,never()).showPurchaseStatusWindow(any(),any());
	}

	@Test void timeoutDoesNotFailOrDiscardPurchase()
	{
		new MercadoPagoOrders((method,path,key,body,sandbox) -> { throw new IOException("timeout"); }).check(_purchase,false);
		verify(_purchase,never()).changeStatus(any());
		verify(_manager,never()).onCompletedPayment(any());
		verify(_data,never()).update(any());
	}

	@Test void canceledLocalOrderCanStillReconcileLateApproval()
	{
		_purchase.changeStatus(PurchaseStatus.CANCELED);
		new MercadoPagoOrders((method,path,key,body,sandbox) -> order()).check(_purchase,false);
		verify(_manager).onCompletedPayment(_purchase);
	}

	@Test void searchRecoversUnknownOrderIdAndThenRetrievesFullOrder()
	{
		_purchase.setPaymentId(null);
		List<Call> calls=new ArrayList<>();
		new MercadoPagoOrders((method,path,key,body,sandbox) ->
		{
			calls.add(new Call(method,path,key,body));
			if (path.startsWith("/v1/orders?"))
			{
				JsonObject result=new JsonObject(); JsonArray data=new JsonArray(); data.add(order()); result.add("data",data); return result;
			}
			return order();
		}).check(_purchase,false);
		assertEquals(2,calls.size()); assertTrue(calls.get(0).path().contains("external_reference=journey1p42"));
		assertEquals("/v1/orders/ORD123456789",calls.get(1).path());
		verify(_manager).onCompletedPayment(_purchase);
	}

	@Test void ambiguousSearchCannotGrant()
	{
		_purchase.setPaymentId(null);
		new MercadoPagoOrders((method,path,key,body,sandbox) -> { JsonObject r=new JsonObject(); JsonArray a=new JsonArray(); a.add(order()); a.add(order()); r.add("data",a); return r; }).check(_purchase,false);
		verify(_manager,never()).onCompletedPayment(any());
	}

	@Test void creationPersistsOrderBeforeFollowupQuery()
	{
		_purchase.setPaymentId(null);
		List<Call> calls=new ArrayList<>();
		new MercadoPagoOrders((method,path,key,body,sandbox) ->
		{
			calls.add(new Call(method,path,key,body));
			if ("GET".equals(method)) verify(_purchase).changeStatus(PurchaseStatus.WAITING);
			return order();
		}).create(_purchase);
		assertEquals("POST",calls.get(0).method()); assertEquals("/v1/orders",calls.get(0).path());
		assertNotNull(calls.get(0).key()); assertEquals("GET",calls.get(1).method());
		verify(_manager).onCompletedPayment(_purchase);
	}

	@Test void paidOrderIsNeverCanceled()
	{
		List<Call> calls=new ArrayList<>();
		new MercadoPagoOrders((method,path,key,body,sandbox) -> { calls.add(new Call(method,path,key,body)); return order(); }).cancel(_purchase);
		assertEquals(1,calls.size()); assertEquals("GET",calls.get(0).method());
		verify(_manager).onCompletedPayment(_purchase);
	}

	@Test void pendingCancellationUsesDedicatedEndpointAndKey()
	{
		List<Call> calls=new ArrayList<>();
		new MercadoPagoOrders((method,path,key,body,sandbox) ->
		{
			calls.add(new Call(method,path,key,body)); JsonObject o=order(); o.addProperty("status","action_required");
			if (calls.size()==3) o.addProperty("status","canceled"); return o;
		}).cancel(_purchase);
		assertEquals(3,calls.size()); assertEquals("POST",calls.get(1).method());
		assertEquals("/v1/orders/ORD123456789/cancel",calls.get(1).path());
		assertNotEquals(MercadoPagoOrders.idempotencyKey(_purchase,"create"),calls.get(1).key());
		verify(_purchase).changeStatus(PurchaseStatus.CANCELED);
	}

	@Test void changedAccountOrEnvironmentPreventsAnyRequest()
	{
		DonationConfig.DONATION_MP_SANDBOX=false;
		MercadoPagoOrders.Transport transport=mock(MercadoPagoOrders.Transport.class);
		new MercadoPagoOrders(transport).check(_purchase,false);
		verifyNoInteractions(transport);
	}
	@Test void mismatchedEnvironmentShowsExplanationOnlyForManualChecks()
	{
		DonationConfig.DONATION_MP_SANDBOX=false;
		var transport=mock(MercadoPagoOrders.Transport.class); var handler=new MercadoPagoOrders(transport);
		handler.check(_purchase,false);
		verifyNoInteractions(transport); verify(_data,never()).update(any()); verify(_manager,never()).showPurchaseStatusWindow(any(),any());
		handler.check(_purchase,true);
		verify(_purchase).setMessage(contains("another payment account"));
		verify(_manager).showPurchaseStatusWindow(any(),eq(_purchase));
		verifyNoInteractions(transport); verify(_manager,never()).onCompletedPayment(any());
	}

	@Test void notFoundRetriesUseIncreasingPerPurchaseIntervals()
	{
		var now=new java.util.concurrent.atomic.AtomicLong(1000000);
		var calls=new java.util.concurrent.atomic.AtomicInteger();
		var handler=new MercadoPagoOrders((method,path,key,body,sandbox) -> { calls.incrementAndGet(); throw new MercadoPagoOrders.OrdersHttpException(404); },now::get);
		handler.check(_purchase,false); handler.check(_purchase,false); handler.check(_purchase,false);
		assertEquals(1,calls.get());
		now.addAndGet(Duration.ofMinutes(5).toMillis()); handler.check(_purchase,false); assertEquals(2,calls.get());
		now.addAndGet(Duration.ofMinutes(5).toMillis()); handler.check(_purchase,false); assertEquals(2,calls.get());
		now.addAndGet(Duration.ofMinutes(5).toMillis()); handler.check(_purchase,false); assertEquals(3,calls.get());
		verify(_data,never()).update(any()); verify(_purchase,never()).changeStatus(any()); verify(_manager,never()).onCompletedPayment(any());
		assertEquals("ORD123456789",_purchase.getPaymentId());
	}

	@Test void networkFailuresAlsoBackOffWithoutChangingPaymentState()
	{
		var now=new java.util.concurrent.atomic.AtomicLong(1000000); var calls=new java.util.concurrent.atomic.AtomicInteger();
		var handler=new MercadoPagoOrders((method,path,key,body,sandbox) -> { calls.incrementAndGet(); throw new IOException("timeout"); },now::get);
		handler.check(_purchase,false); handler.check(_purchase,false); assertEquals(1,calls.get());
		now.addAndGet(30000); handler.check(_purchase,false); assertEquals(2,calls.get());
		now.addAndGet(30000); handler.check(_purchase,false); assertEquals(2,calls.get());
		now.addAndGet(30000); handler.check(_purchase,false); assertEquals(3,calls.get());
		verify(_purchase,never()).changeStatus(any()); verify(_manager,never()).onCompletedPayment(any());
	}

	@Test void oneMissingOrderDoesNotDelayOtherPurchases()
	{
		var calls=new java.util.concurrent.atomic.AtomicInteger();
		var handler=new MercadoPagoOrders((method,path,key,body,sandbox) -> { calls.incrementAndGet(); throw new MercadoPagoOrders.OrdersHttpException(404); });
		handler.check(_purchase,false);
		Purchase other=mock(Purchase.class); when(other.getId()).thenReturn(99); when(other.usesMercadoPagoOrders()).thenReturn(true); when(other.isMpSandbox()).thenReturn(true); when(other.getMpMerchantId()).thenReturn("77"); when(other.getPaymentId()).thenReturn("ORD987654321");
		handler.check(other,false); handler.check(_purchase,false); assertEquals(2,calls.get());
	}

	@Test void successfulManualCheckClearsTheAutomaticRetryDelay()
	{
		var now=new java.util.concurrent.atomic.AtomicLong(1000000); var calls=new java.util.concurrent.atomic.AtomicInteger();
		var handler=new MercadoPagoOrders((method,path,key,body,sandbox) ->
		{
			if (calls.incrementAndGet()==1) throw new MercadoPagoOrders.OrdersHttpException(404);
			var result=order(); result.addProperty("status","action_required"); result.addProperty("status_detail","waiting_transfer"); return result;
		},now::get);
		handler.check(_purchase,false); handler.check(_purchase,false); assertEquals(1,calls.get());
		handler.check(_purchase,true); assertEquals(2,calls.get());
		handler.check(_purchase,false); assertEquals(3,calls.get());
		verify(_manager,never()).onCompletedPayment(any());
	}

	@Test void httpNotFoundPreservesItsStatusForTheRetryPolicy() throws Exception
	{
		var client=mock(java.net.http.HttpClient.class);
		@SuppressWarnings("unchecked") java.net.http.HttpResponse<String> response=mock(java.net.http.HttpResponse.class);
		when(response.statusCode()).thenReturn(404); when(response.body()).thenReturn("{}");
		when(client.send(any(java.net.http.HttpRequest.class),org.mockito.ArgumentMatchers.<java.net.http.HttpResponse.BodyHandler<String>>any())).thenReturn(response);
		var error=assertThrows(MercadoPagoOrders.OrdersHttpException.class,() -> new MercadoPagoOrders.HttpTransport(client).request("GET","/v1/orders/ORD123456789",null,null,true));
		assertEquals(404,error.status);
	}

	@Test void httpTransportSendsOrdersHeadersAndSandboxFlag() throws Exception
	{
		String token=DonationConfig.DONATION_MP_TOKEN;
		try
		{
			DonationConfig.DONATION_MP_TOKEN="test-placeholder";
			var client=mock(java.net.http.HttpClient.class);
			@SuppressWarnings("unchecked") java.net.http.HttpResponse<String> response=mock(java.net.http.HttpResponse.class);
			when(response.statusCode()).thenReturn(200); when(response.body()).thenReturn("{}");
			when(client.send(any(java.net.http.HttpRequest.class),org.mockito.ArgumentMatchers.<java.net.http.HttpResponse.BodyHandler<String>>any())).thenReturn(response);
			var transport=new MercadoPagoOrders.HttpTransport(client);
			transport.request("POST","/v1/orders","stable-key",MercadoPagoOrders.createPayload(_purchase),true);
			var capture=org.mockito.ArgumentCaptor.forClass(java.net.http.HttpRequest.class);
			verify(client).send(capture.capture(),org.mockito.ArgumentMatchers.<java.net.http.HttpResponse.BodyHandler<String>>any());
			var request=capture.getValue(); assertEquals("https://api.mercadopago.com/v1/orders",request.uri().toString());
			assertEquals("stable-key",request.headers().firstValue("X-Idempotency-Key").orElseThrow());
			assertEquals("true",request.headers().firstValue("x-test-token").orElseThrow());
			assertEquals("Bearer test-placeholder",request.headers().firstValue("Authorization").orElseThrow());
			assertEquals(Duration.ofSeconds(20),request.timeout().orElseThrow());
		}
		finally { DonationConfig.DONATION_MP_TOKEN=token; }
	}

	@Test void rateLimitBackoffPreventsImmediateSecondRequest() throws Exception
	{
		var client=mock(java.net.http.HttpClient.class);
		@SuppressWarnings("unchecked") java.net.http.HttpResponse<String> response=mock(java.net.http.HttpResponse.class);
		when(response.statusCode()).thenReturn(429); when(response.body()).thenReturn("{}");
		when(response.headers()).thenReturn(java.net.http.HttpHeaders.of(java.util.Map.of("Retry-After",List.of("120")),(k,v) -> true));
		when(client.send(any(java.net.http.HttpRequest.class),org.mockito.ArgumentMatchers.<java.net.http.HttpResponse.BodyHandler<String>>any())).thenReturn(response);
		var transport=new MercadoPagoOrders.HttpTransport(client);
		assertThrows(IOException.class,() -> transport.request("GET","/v1/orders/ORD123456789",null,null,true));
		assertThrows(IOException.class,() -> transport.request("GET","/v1/orders/ORD123456789",null,null,true));
		verify(client,times(1)).send(any(java.net.http.HttpRequest.class),org.mockito.ArgumentMatchers.<java.net.http.HttpResponse.BodyHandler<String>>any());
	}

	@Test void manualStatusCheckDoesNotReopenQrOrResendCheckout()
	{
		when(_purchase.getQrCode()).thenReturn("pix-code"); when(_purchase.wantQrCode()).thenReturn(true);
		new MercadoPagoOrders((method,path,key,body,sandbox) -> { JsonObject o=order(); o.addProperty("status","action_required"); return o; }).check(_purchase,true);
		verify(_manager).showPurchaseStatusWindow(any(),eq(_purchase));
		verify(_manager,never()).showCheckoutWindow(any(Purchase.class));
		verify(_manager,never()).showQrCodeWindow(any(),any());
	}

	@Test void sandboxWithoutAutoApprovalOmitsAproAndKeepsPixPayload()
	{
		when(_purchase.isMpSandboxAutoApprove()).thenReturn(false);
		JsonObject payload=MercadoPagoOrders.createPayload(_purchase);
		assertFalse(payload.getAsJsonObject("payer").has("first_name"));
		assertEquals("pix",payment(payload).getAsJsonObject("payment_method").get("id").getAsString());
		assertEquals("PT30M",payment(payload).get("expiration_time").getAsString());
		assertEquals(payload,MercadoPagoOrders.createPayload(_purchase));
	}

	@Test void pendingOrderNeverGrantsItemsEvenWithPreviouslyPaidFields()
	{
		new MercadoPagoOrders((method,path,key,body,sandbox) ->
		{
			JsonObject o=order(); o.addProperty("status","action_required"); o.addProperty("status_detail","waiting_transfer");
			payment(o).addProperty("status","action_required"); payment(o).addProperty("status_detail","waiting_transfer"); return o;
		}).check(_purchase,false);
		verify(_manager,never()).onCompletedPayment(any());
	}

	@Test void expiredOrderDoesNotGrantAndRecordsExpiration()
	{
		new MercadoPagoOrders((method,path,key,body,sandbox) -> { JsonObject o=order(); o.addProperty("status","expired"); payment(o).addProperty("status","canceled"); return o; }).check(_purchase,false);
		verify(_purchase).changeStatus(PurchaseStatus.EXPIRED);
		verify(_manager,never()).onCompletedPayment(any());
	}

	@Test void canceledAndFailedOrdersWithExpiredDetailRecordExpiration()
	{
		for (String status : List.of("canceled","failed"))
		{
			JsonObject o=order(); o.addProperty("status",status); o.addProperty("status_detail","expired");
			assertEquals(PurchaseStatus.EXPIRED,MercadoPagoOrders.terminalStatus(o));
		}
	}

	@Test void transactionExpirationExplainsCanceledOrder()
	{
		JsonObject o=order(); o.addProperty("status","canceled"); o.addProperty("status_detail","canceled");
		payment(o).addProperty("status","canceled"); payment(o).addProperty("status_detail","expired");
		assertEquals(PurchaseStatus.EXPIRED,MercadoPagoOrders.terminalStatus(o));
	}

	@Test void canceledByApiStaysCanceledEvenAfterLocalDeadline()
	{
		when(_purchase.timeExpired()).thenReturn(true);
		JsonObject o=order(); o.addProperty("status","canceled"); o.addProperty("status_detail","canceled_by_api");
		payment(o).addProperty("status","canceled"); payment(o).addProperty("status_detail","canceled_by_api");
		new MercadoPagoOrders((method,path,key,body,sandbox) -> o).check(_purchase,false);
		verify(_purchase).changeStatus(PurchaseStatus.CANCELED);
		verify(_purchase,never()).changeStatus(PurchaseStatus.EXPIRED);
		verify(_manager,never()).onCompletedPayment(any());
	}

	@Test void pendingOrderIsNotExpiredFromItsLocalClock()
	{
		when(_purchase.timeExpired()).thenReturn(true);
		_purchase.changeStatus(PurchaseStatus.FINISHING);
		JsonObject o=order(); o.addProperty("status","action_required"); o.addProperty("status_detail","waiting_transfer");
		new MercadoPagoOrders((method,path,key,body,sandbox) -> o).check(_purchase,false);
		verify(_purchase,never()).changeStatus(PurchaseStatus.EXPIRED);
		verify(_manager,never()).onCompletedPayment(any());
	}

	@Test void previousMisclassifiedCancellationIsCorrectedByReconciliation()
	{
		_purchase.changeStatus(PurchaseStatus.CANCELED);
		JsonObject o=order(); o.addProperty("status","canceled"); o.addProperty("status_detail","expired");
		payment(o).addProperty("status","canceled"); payment(o).addProperty("status_detail","expired");
		new MercadoPagoOrders((method,path,key,body,sandbox) -> o).check(_purchase,false);
		verify(_purchase).changeStatus(PurchaseStatus.EXPIRED);
		verify(_manager,never()).onCompletedPayment(any());
	}

	@Test void expirationStillAllowsReconciliationOfAnActuallyAccreditedPayment()
	{
		_purchase.changeStatus(PurchaseStatus.EXPIRED);
		new MercadoPagoOrders((method,path,key,body,sandbox) -> order()).check(_purchase,false);
		verify(_manager).onCompletedPayment(_purchase);
	}

}
