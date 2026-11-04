package com.l2journey.gameserver.donation.paymenthandlers.paypal;

import java.util.logging.Level;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.Base64;

import com.l2journey.gameserver.donation.DonationExecutor;

import com.l2journey.gameserver.donation.DonationManager;
import com.l2journey.gameserver.donation.IPaymentHandler;
import com.l2journey.gameserver.donation.paymenthandlers.paypal.Invoice.BillingInfo;
import com.l2journey.gameserver.donation.paymenthandlers.paypal.Invoice.Detail;
import com.l2journey.gameserver.donation.paymenthandlers.paypal.Invoice.Invoicer;
import com.l2journey.gameserver.donation.paymenthandlers.paypal.Invoice.Item;
import com.l2journey.gameserver.donation.paymenthandlers.paypal.Invoice.PaymentTerm;
import com.l2journey.gameserver.donation.paymenthandlers.paypal.Invoice.Phone;
import com.l2journey.gameserver.donation.paymenthandlers.paypal.Invoice.Recipient;
import com.l2journey.gameserver.donation.paymenthandlers.paypal.Invoice.UnitAmount;
import com.l2journey.gameserver.donation.purchase.Purchase;
import com.l2journey.gameserver.donation.purchase.PurchaseStatus;
import com.l2journey.gameserver.donation.DonationConfig;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;

public class PayPal implements IPaymentHandler
{
	private String _accessToken;
	private long _accessTokenExpire;
	
	private final HttpClient _httpClient = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
	
	@Override
	public void reload()
	{
	}

	@Override
	public void checkPurchase(Purchase purchase)
	{
		DonationExecutor.submit(purchase, () -> getInvoice(purchase), false);
	}

	@Override
	public void sendPurchase(Purchase purchase)
	{
		DonationExecutor.submit(purchase, () -> createInvoice(purchase), true);
	}

	@Override
	public void closePurchase(Purchase purchase)
	{
		if (purchase.getPaypalInvoiceId() == null)
			return;

		DonationExecutor.submit(purchase, () -> cancelInvoice(purchase), false);
	}
	
	@Override
	public void refund(Purchase purchase)
	{
	}

	@Override
	public void resendEmail(Purchase purchase)
	{
		purchase.logEmailSending();
		DonationExecutor.submit(purchase, () -> sendInvoiceReminder(purchase), false);
	}

	private static void handleSendResponse(Purchase p)
	{
		final Player player = World.getInstance().getPlayer(p.getPlayerId());
		p.changeStatus(PurchaseStatus.WAITING);
		DonationManager.getInstance().showCheckoutWindow(player, p);
		
		if (p.wantQrCode())
			DonationManager.getInstance().showQrCodeWindow(player, p);
		else
		{
			p.logEmailSending();
		}
	}
	
	private void handlePaypalCheck(Purchase p, String status, String paymentId)
	{
		switch (status)
		{
			case "SENT":
			case "UNPAID":
			case "PAYMENT_PENDING":
				if (p.getStatus() == PurchaseStatus.FINISHING)
				{
					closePurchase(p);

					DonationManager.getInstance().inactivePurchase(p);
					return;
				}

				DonationManager.getInstance().onPaymentNotFound(p);
				break;

			case "CANCELLED":
			case "REFUNDED":
			case "MARKED_AS_REFUNDED":
				p.changeStatus(PurchaseStatus.FAILED);
				DonationManager.getInstance().onPaymentFailed(p);
				break;

			case "MARKED_AS_PAID":
                DonationManager.getInstance().onPaymentNotFound(p);
                break;
			case "PAID":
				if (p.getStatus() == PurchaseStatus.COMPLETED)
					return;

				p.setPaymentId(paymentId);

				DonationManager.getInstance().onCompletedPayment(p);
				break;
		}
	}

	/*
	* https://developer.paypal.com/reference/production/
	*/
	private static String getUrl(String path)
	{
		return (DonationConfig.DONATION_PAYPAL_SANDBOX_ENABLED ? "https://api-m.sandbox.paypal.com/" : "https://api-m.paypal.com/") + path;
	}

	/*
	* https://developer.paypal.com/api/rest/#link-getaccesstoken
	*/
	private String getAccessToken(Purchase purchase)
	{
		if (_accessToken != null && _accessTokenExpire - 60000 > System.currentTimeMillis())
			return _accessToken;
		
		synchronized(this)
		{
			if (_accessToken != null && _accessTokenExpire - 60000 > System.currentTimeMillis())
				return _accessToken;
			
			try
			{
				final String credentials = DonationConfig.DONATION_PAYPAL_CLIENT_ID + ":" + DonationConfig.DONATION_PAYPAL_CLIENT_SECRET;
				final String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
				final HttpRequest request = HttpRequest.newBuilder().timeout(java.time.Duration.ofSeconds(20))
						.uri(URI.create(getUrl("v1/oauth2/token")))
						.header("Content-Type", "application/x-www-form-urlencoded")
						.header("Authorization", "Basic " + encodedCredentials)
						.POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
						.build();

				final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body().length() > 1048576) throw new IllegalStateException("Payment provider HTTP " + response.statusCode());
				final JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
				_accessToken = "Bearer " + jsonObject.get("access_token").getAsString();
				_accessTokenExpire = System.currentTimeMillis() + jsonObject.get("expires_in").getAsLong() * 1000;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "PayPal: Falha ao obter o access_token do PayPal para a purchase #" + (purchase.getId()) + ".", e);
				DonationManager.getInstance().handleException(purchase, true);
			}

			return _accessToken;
		}
	}

	/*
	* https://developer.paypal.com/docs/api/invoicing/v2/#invoices_create
	*/
	private void createInvoice(Purchase purchase)
	{
		try
		{

			final String accessToken = getAccessToken(purchase);
			if (accessToken == null)
				return;

			final Invoice invoiceDetails = new Invoice();
			invoiceDetails.detail = new Detail();
            invoiceDetails.detail.reference = purchase.getReference();
			invoiceDetails.detail.invoice_number = purchase.getReference();
			invoiceDetails.detail.invoice_date = new SimpleDateFormat("yyyy-MM-dd").format(purchase.getDate());
			invoiceDetails.detail.currency_code = purchase.getCurrency();
			invoiceDetails.detail.payment_term = new PaymentTerm();
			invoiceDetails.detail.payment_term.term_type = "DUE_ON_DATE_SPECIFIED";
			invoiceDetails.detail.memo = "Compra realizada através do DonationManager.";

			if (!DonationConfig.DONATION_PAYPAL_NOTE_MSG.isEmpty())
				invoiceDetails.detail.note = DonationConfig.DONATION_PAYPAL_NOTE_MSG;

			invoiceDetails.invoicer = new Invoicer();
			invoiceDetails.invoicer.business_name = DonationConfig.DONATION_SERVER_NAME;
			invoiceDetails.invoicer.email_address = DonationConfig.DONATION_PAYPAL_ACCOUNT_EMAIL;

			if (!DonationConfig.DONATION_PAYPAL_WEBSITE.isEmpty())
				invoiceDetails.invoicer.website = DonationConfig.DONATION_PAYPAL_WEBSITE;

			if (!DonationConfig.DONATION_PAYPAL_LOGO_IMAGE.isEmpty())
				invoiceDetails.invoicer.logo_url = DonationConfig.DONATION_PAYPAL_LOGO_IMAGE;

			if (!DonationConfig.DONATION_PAYPAL_PHONE_CODE.isEmpty() && !DonationConfig.DONATION_PAYPAL_PHONE_NUMBER.isEmpty())
			{
				final Phone phone = new Phone();
				phone.country_code = DonationConfig.DONATION_PAYPAL_PHONE_CODE;
				phone.national_number = DonationConfig.DONATION_PAYPAL_PHONE_NUMBER;
				phone.phone_type = "MOBILE";
				invoiceDetails.invoicer.phones = new Phone[] { phone };
			}

			final Recipient recipient = new Recipient();
			recipient.billing_info = new BillingInfo();
			recipient.billing_info.business_name = purchase.getPlayerName();
			recipient.billing_info.email_address = purchase.getPlayerEmail();
			invoiceDetails.primary_recipients = new Recipient[] { recipient };

			final Item item1 = new Item();
			item1.name = purchase.getProductName();
			item1.quantity = String.valueOf(purchase.getQuantity());
			item1.unit_amount = new UnitAmount();
			item1.unit_amount.currency_code = purchase.getCurrency();
			item1.unit_amount.value = String.valueOf(purchase.getUnitPrice());
			invoiceDetails.items = new Item[] { item1 };

			final Gson gson = new GsonBuilder().create();
			final String jsonInputString = gson.toJson(invoiceDetails);

			final HttpRequest request = HttpRequest.newBuilder().timeout(java.time.Duration.ofSeconds(20))
				.uri(URI.create(getUrl("v2/invoicing/invoices")))
				.header("Authorization", accessToken)
				.header("Content-Type", "application/json")
				.header("Prefer", "return=representation")
                .header("PayPal-Request-Id", purchase.getReference() + "create")
				.POST(HttpRequest.BodyPublishers.ofString(jsonInputString))
				.build();

			final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body().length() > 1048576) throw new IllegalStateException("Payment provider HTTP " + response.statusCode());
			final JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);

			purchase.setPaypalInvoiceId(jsonObject.get("id").getAsString());
            com.l2journey.gameserver.donation.DonationData.getInstance().update(purchase);
			sendInvoice(purchase);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "PayPal: Falha ao CRIAR a Invoice para a purchase #" + (purchase.getId()) + ".", e);
			DonationManager.getInstance().handleException(purchase, true);
		}
	}

	/*
	* Quando um Invoice é criado, seu status padrão é DRAFT, para que ele seja pagável, é necessário "enviá-lo".
	* https://developer.paypal.com/docs/api/invoicing/v2/#invoices_send
	*/
	private void sendInvoice(Purchase purchase)
	{
		try
		{
			final String accessToken = getAccessToken(purchase);
			if (accessToken == null)
				return;

			final HttpRequest request = HttpRequest.newBuilder().timeout(java.time.Duration.ofSeconds(20))
				.uri(URI.create(getUrl("v2/invoicing/invoices/" + purchase.getPaypalInvoiceId() + "/send")))
				.header("Authorization", accessToken)
				.header("Content-Type", "application/json")
				.header("PayPal-Request-Id", "journey-pix-" + purchase.getId())
				.POST(HttpRequest.BodyPublishers.ofString(String.format("{\"send_to_recipient\": %s}", !purchase.wantQrCode())))
				.build();

			final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body().length() > 1048576) throw new IllegalStateException("Payment provider HTTP " + response.statusCode());
			if (response.statusCode() != 200)
			{
				LOGGER.warning("PayPal: Não foi possível enviar a purchase #" + (purchase.getId()) + " para o PayPal. Verifique sua configuração, em especial o e-mail especificado.");
				DonationManager.getInstance().handleException(purchase, true);
			}
			else
				handleSendResponse(purchase);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "PayPal: Falha ao ENVIAR a Invoice para a purchase #" + (purchase.getId()) + ".", e);
			DonationManager.getInstance().handleException(purchase, true);
		}
	}

	/*
	* https://developer.paypal.com/docs/api/invoicing/v2/#invoices_get
	*/
	private void getInvoice(Purchase purchase)
	{
		try
		{

			final String accessToken = getAccessToken(purchase);
			if (accessToken == null)
				return;

			final HttpRequest request = HttpRequest.newBuilder().timeout(java.time.Duration.ofSeconds(20))
				.uri(URI.create(getUrl("v2/invoicing/invoices/" + purchase.getPaypalInvoiceId())))
				.header("Authorization", accessToken)
				.header("Content-Type", "application/json")
				.GET()
				.build();

			final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body().length() > 1048576) throw new IllegalStateException("Payment provider HTTP " + response.statusCode());
			final Gson gson = new Gson();
			final JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
			final String status = jsonObject.get("status").getAsString();

			if (status.equals("PAID"))
            {
                if (!com.l2journey.gameserver.donation.PaymentValidation.paypal(purchase,jsonObject)) { LOGGER.warning("PayPal: Rejected PayPal invoice for purchase " + (purchase.getId())); return; }
            }

            if (status.equals("PAID"))
			{
				final String paymentId = jsonObject.getAsJsonObject("payments").getAsJsonArray("transactions").get(0).getAsJsonObject().get("payment_id").getAsString();
				handlePaypalCheck(purchase, status, paymentId);
			}
			else
				handlePaypalCheck(purchase, status, null);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "PayPal: Falha ao obter o Invoice da purchase #" + (purchase.getId()) + ".", e);
			DonationManager.getInstance().handleException(purchase, false);
		}
	}

	/*
	* https://developer.paypal.com/docs/api/invoicing/v2/#invoices_remind
	*/
	private void sendInvoiceReminder(Purchase purchase)
	{
		try
		{

			final String accessToken = getAccessToken(purchase);
			if (accessToken == null)
				return;

			final HttpRequest request = HttpRequest.newBuilder().timeout(java.time.Duration.ofSeconds(20))
				.uri(URI.create(getUrl("v2/invoicing/invoices/" + purchase.getPaypalInvoiceId() + "/remind")))
				.header("Authorization", accessToken)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();

			final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body().length() > 1048576) throw new IllegalStateException("Payment provider HTTP " + response.statusCode());
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "PayPal: Falha ao enviar ao enviar o Invoice Reminder da purchase #" + (purchase.getId()) + ".", e);
			DonationManager.getInstance().handleException(purchase, false);
		}
	}

	/*
	* https://developer.paypal.com/docs/api/invoicing/v2/#invoices_cancel
	*/
	private void cancelInvoice(Purchase purchase)
	{
		try
		{
			final String accessToken = getAccessToken(purchase);
			if (accessToken == null)
				return;

			if (purchase.getPaypalInvoiceId() == null)
				return;

			final HttpRequest request = HttpRequest.newBuilder().timeout(java.time.Duration.ofSeconds(20))
				.uri(URI.create(getUrl("v2/invoicing/invoices/" + purchase.getPaypalInvoiceId() + "/cancel")))
				.header("Authorization", accessToken)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{\"send_to_recipient\": false}"))
				.build();

			final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body().length() > 1048576) throw new IllegalStateException("Payment provider HTTP " + response.statusCode());
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "PayPal: Falha ao cancelar o Invoice da purchase #" + (purchase.getId()) + ".", e);
		}
	}
}