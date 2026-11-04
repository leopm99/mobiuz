package com.l2journey.gameserver.donation.paymenthandlers.binance;

import java.util.logging.Level;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.l2journey.gameserver.donation.DonationExecutor;

import com.l2journey.gameserver.donation.DonationManager;
import com.l2journey.gameserver.donation.IPaymentHandler;
import com.l2journey.gameserver.donation.Mailersend;
import com.l2journey.gameserver.donation.paymenthandlers.binance.Order.Env;
import com.l2journey.gameserver.donation.paymenthandlers.binance.Order.GoodsDetail;
import com.l2journey.gameserver.donation.paymenthandlers.binance.Order.GoodsUnitAmount;
import com.l2journey.gameserver.donation.purchase.Purchase;
import com.l2journey.gameserver.donation.purchase.PurchaseStatus;
import com.l2journey.gameserver.donation.DonationConfig;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;

public class Binance implements IPaymentHandler
{
	private static final String BINANCE_PAY_URL = "https://bpay.binanceapi.com/binancepay/openapi";

	private final HttpClient _httpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();

	@Override
	public void reload()
	{
	}

	@Override
	public void checkPurchase(Purchase purchase)
	{
		DonationExecutor.submit(purchase, () -> queryOrder(purchase), false);
	}

	@Override
	public void sendPurchase(Purchase purchase)
	{
		DonationExecutor.submit(purchase, () -> createOrder(purchase), true);
	}

	@Override
	public void closePurchase(Purchase purchase)
	{
		DonationExecutor.submit(purchase, () -> closeOrder(purchase), false);
	}
	
	@Override
	public void refund(Purchase purchase)
	{
	}

	@Override
	public void resendEmail(Purchase purchase)
	{
		purchase.logEmailSending();
		Mailersend.sendPurchaseMail(purchase);
	}

	/*
	* https://merchant.binance.com/en/docs/getting-started
	* https://developers.binance.com/docs/binance-pay/api-common
	* https://developers.binance.com/docs/derivatives/option/error-code
	*/
	private static void handleCheckResponse(Purchase p, JsonObject response)
	{
		final String status = response.get("status").getAsString();
        if ("PAID".equals(status) && !com.l2journey.gameserver.donation.PaymentValidation.binance(p,response)) { LOGGER.warning("Binance: Rejected Binance payment for purchase " + (p.getId())); return; }
		switch (status)
		{
			case "EXPIRED":
			case "CANCELED":
			case "REFUNDING":
			case "REFUNDED":
			case "FULL_REFUNDED":
			case "ERROR":
				DonationManager.getInstance().onPaymentFailed(p);
				break;

			case "PENDING":
			case "INITIAL":
				if (p.getStatus() == PurchaseStatus.FINISHING)
				{
					DonationManager.getInstance().inactivePurchase(p);
					return;
				}

				DonationManager.getInstance().onPaymentNotFound(p);
				break;

			case "PAID":
				if (p.getStatus() == PurchaseStatus.COMPLETED)
					return;

				DonationManager.getInstance().onCompletedPayment(p);
				break;
		}
	}

	private void handleCreateResponse(Purchase p, JsonObject response)
	{
		final Player player = World.getInstance().getPlayer(p.getPlayerId());
		p.setPaymentId(response.get("prepayId").getAsString());
		p.setQrCode(response.get("qrContent").getAsString());
		p.setLink(response.get("checkoutUrl").getAsString());
		p.changeStatus(PurchaseStatus.WAITING);
		DonationManager.getInstance().showCheckoutWindow(player, p);
		
		if (p.wantQrCode())
			DonationManager.getInstance().showQrCodeWindow(player, p);			
		else
			resendEmail(p);
	}

	/*
	* https://github.com/binance/binance-connector-java
	* src/main/java/com/binance/connector/client/utils/signaturegenerator/HmacSignatureGenerator.java
	*/
	private static String hashing(String data, String nonce, String timestamp) throws NoSuchAlgorithmException, InvalidKeyException
	{
		byte[] hmacSha256;
		final SecretKeySpec secretKeySpec = new SecretKeySpec(DonationConfig.DONATION_BINANCE_SECRET_KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA512");
		final Mac mac = Mac.getInstance("HmacSHA512");
		final String payload = timestamp + "\n" + nonce + "\n" + data + "\n";
		mac.init(secretKeySpec);
		hmacSha256 = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		return java.util.HexFormat.of().formatHex(hmacSha256).toUpperCase();
	}

	private void createOrder(Purchase purchase)
	{
		try
		{
			final Order order = new Order();
			order.env = new Env();
			order.env.terminalType = "APP";
			order.merchantTradeNo = purchase.getReference();
            order.orderAmount = purchase.getTotalPrice().toString();
			order.currency = purchase.getCurrency();
			order.description = "Compra no " + DonationConfig.DONATION_SERVER_NAME;
			order.orderExpireTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(DonationConfig.DONATION_BINANCE_EXPIRATION_TIME);
			order.supportPayCurrency = purchase.getCurrency();

			final GoodsDetail goods = new GoodsDetail();
			goods.goodsType = "02";
			goods.goodsCategory = "6000";
			goods.referenceGoodsId = String.valueOf(purchase.getProductId());
			goods.goodsName = purchase.getProductName();
			final GoodsUnitAmount goodsUnit = new GoodsUnitAmount();
			goodsUnit.amount = purchase.getUnitPrice().doubleValue();
			goodsUnit.currency = purchase.getCurrency();
			goods.goodsUnitAmount = goodsUnit;
			order.goodsDetails = new GoodsDetail[]{goods};

			final Gson gson = new GsonBuilder().create();
			final String payload = gson.toJson(order);

			final String nonce = randomString(32);
			final String timestamp = String.valueOf(System.currentTimeMillis());
			final String signature = hashing(payload, nonce, timestamp);

			final HttpRequest request = HttpRequest.newBuilder().timeout(java.time.Duration.ofSeconds(20))
				.uri(URI.create(BINANCE_PAY_URL + "/v3/order"))
				.header("Content-Type", "application/json")
				.header("BinancePay-Timestamp", timestamp)
				.header("BinancePay-Nonce", nonce)
				.header("BinancePay-Certificate-SN", DonationConfig.DONATION_BINANCE_API_KEY)
				.header("BinancePay-Signature", signature)
				.POST(HttpRequest.BodyPublishers.ofString(payload))
				.build();

			final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body().length() > 1048576) throw new IllegalStateException("Payment provider HTTP " + response.statusCode());
			final JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
			final String queryResponse = jsonObject.get("status").getAsString();
			if (queryResponse.equals("SUCCESS"))
				handleCreateResponse(purchase, jsonObject.getAsJsonObject("data"));
			else
				DonationManager.getInstance().handleException(purchase, true);
		}
		catch (Exception e)
		{
			DonationManager.getInstance().handleException(purchase, true);
			LOGGER.log(Level.WARNING, "Binance: Falha ao criar a Order (Binance) para a purchase #" + (purchase.getId()) + ".", e);
		}
	}

	private void queryOrder(Purchase purchase)
	{
		try
		{

			final String payload = String.format("{\"prepayId\": %s}", purchase.getPaymentId());
			final String nonce = randomString(32);
			final String timestamp = String.valueOf(System.currentTimeMillis());
			final String signature = hashing(payload, nonce, timestamp);

			final HttpRequest request = HttpRequest.newBuilder().timeout(java.time.Duration.ofSeconds(20))
				.uri(URI.create(BINANCE_PAY_URL + "/v2/order/query"))
				.header("Content-Type", "application/json")
				.header("BinancePay-Timestamp", timestamp)
				.header("BinancePay-Nonce", nonce)
				.header("BinancePay-Certificate-SN", DonationConfig.DONATION_BINANCE_API_KEY)
				.header("BinancePay-Signature", signature)
				.POST(HttpRequest.BodyPublishers.ofString(payload))
				.build();

			final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body().length() > 1048576) throw new IllegalStateException("Payment provider HTTP " + response.statusCode());
			final Gson gson = new Gson();
			final JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
			final String queryResponse = jsonObject.get("status").getAsString();
			if (queryResponse.equals("SUCCESS"))
				handleCheckResponse(purchase, jsonObject.getAsJsonObject("data"));
			else
				DonationManager.getInstance().handleException(purchase, false);
		}
		catch (Exception e)
		{
			DonationManager.getInstance().handleException(purchase, false);
			LOGGER.log(Level.WARNING, "Binance: Falha ao obter a Order (Binance) da purchase #" + (purchase.getId()) + ".", e);
		}
	}

	private void closeOrder(Purchase purchase)
	{
		try
		{

			final String payload = String.format("{\"prepayId\": %s}", purchase.getPaymentId());
			final String nonce = randomString(32);
			final String timestamp = String.valueOf(System.currentTimeMillis());
			final String signature = hashing(payload, nonce, timestamp);

			final HttpRequest request = HttpRequest.newBuilder().timeout(java.time.Duration.ofSeconds(20))
				.uri(URI.create(BINANCE_PAY_URL + "/v2/order/close"))
				.header("Content-Type", "application/json")
				.header("BinancePay-Timestamp", timestamp)
				.header("BinancePay-Nonce", nonce)
				.header("BinancePay-Certificate-SN", DonationConfig.DONATION_BINANCE_API_KEY)
				.header("BinancePay-Signature", signature)
				.POST(HttpRequest.BodyPublishers.ofString(payload))
				.build();

			final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body().length() > 1048576) throw new IllegalStateException("Payment provider HTTP " + response.statusCode());
		}
		catch (Exception e)
		{
		}
	}
}
