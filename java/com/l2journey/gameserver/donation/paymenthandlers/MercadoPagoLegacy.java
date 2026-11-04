package com.l2journey.gameserver.donation.paymenthandlers;

import java.util.logging.Level;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentAdditionalInfoRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.client.payment.PaymentRefundClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.net.MPResultsResourcesPage;
import com.mercadopago.net.MPSearchRequest;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.l2journey.gameserver.donation.DonationExecutor;

import com.l2journey.gameserver.donation.DonationManager;
import com.l2journey.gameserver.donation.PaymentValidation;
import com.l2journey.gameserver.donation.IPaymentHandler;
import com.l2journey.gameserver.donation.Mailersend;
import com.l2journey.gameserver.donation.purchase.Purchase;
import com.l2journey.gameserver.donation.purchase.PaymentMethod;
import com.l2journey.gameserver.donation.purchase.PurchaseStatus;
import com.l2journey.gameserver.donation.DonationConfig;
import com.l2journey.gameserver.model.World;
import com.l2journey.gameserver.model.actor.Player;

final class MercadoPagoLegacy implements IPaymentHandler
{
	public MercadoPagoLegacy()
	{
		reload();
	}
	
	@Override
	public void reload()
	{
		MercadoPagoConfig.setAccessToken(DonationConfig.DONATION_MP_TOKEN);
        MercadoPagoConfig.setConnectionTimeout(10000);
        MercadoPagoConfig.setSocketTimeout(20000);
	}
	
	@Override
	public void checkPurchase(Purchase purchase)
	{
		if (purchase.getPaymentMethod() == PaymentMethod.MP_PIX && purchase.getPaymentId() != null)
			DonationExecutor.submit(purchase, () -> getPayment(purchase), false);
		else
			DonationExecutor.submit(purchase, () -> searchPayment(purchase), false);
	}

	@Override
	public void sendPurchase(Purchase purchase)
	{
		if (purchase.getPaymentMethod() == PaymentMethod.MP_PIX)
			DonationExecutor.submit(purchase, () -> createPayment(purchase), true);
		else
			DonationExecutor.submit(purchase, () -> createPreference(purchase), true);
	}
	
	@Override
	public void refund(Purchase purchase)
	{
		if (purchase.getStatus() != PurchaseStatus.COMPLETED)
			return;
		
		DonationExecutor.execute(() -> 
		{
			try
			{
				final PaymentRefundClient client = new PaymentRefundClient();
				client.refund(Long.valueOf(purchase.getPaymentId()), purchase.getTotalPrice());
				purchase.changeStatus(PurchaseStatus.REFUNDED);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "MercadoPagoLegacy: Falha ao cancelar a Preference da purchase #" + (purchase.getId()) + ".", e);
			}
		});
	}
	
	@Override
	public void resendEmail(Purchase purchase)
	{
		purchase.logEmailSending();
		Mailersend.sendPurchaseMail(purchase);
	}

	/*
	 * Cancela o Payment ou Preference para evitar pagamentos de compras canceladas, expiradas ou já pagas
	 * Fazemos uma tentativa. Não lidamos com erros.
	 */
	@Override
	public void closePurchase(Purchase purchase)
	{
		if (purchase.getPaymentMethod() == PaymentMethod.MP_PIX)
		{
			if (purchase.getPaymentId() == null)
				return;
			
			DonationExecutor.submit(purchase, () -> closePayment(purchase), false);
		}
		else
		{
			if (purchase.getMpPreferenceId() == null)
				return;
			
			DonationExecutor.submit(purchase, () -> closePreference(purchase), false);
		}
	}
	
	/*
	 * A Preference (link) foi criada no mercado pago, salvamos junto com a Purchase
	 */
	private void handleMPPreferenceResponse(Purchase p, Preference preference)
	{
		final Player player = World.getInstance().getPlayer(p.getPlayerId());
		p.setMpPreferenceId(preference.getId());
        p.setLink(preference.getInitPoint());
		p.changeStatus(PurchaseStatus.WAITING);
		DonationManager.getInstance().showCheckoutWindow(player, p);

		if (p.wantQrCode())
			DonationManager.getInstance().showQrCodeWindow(player, p);
		else
			resendEmail(p);
	}

	/*
	 * O Payment foi criado no mercado pago, salvamos junto com a Purchase
	 */
	private void handleMPPaymentResponse(Purchase p, Payment payment)
	{
		final Player player = World.getInstance().getPlayer(p.getPlayerId());
		p.setPaymentId(String.valueOf(payment.getId()));
		p.setQrCode(payment.getPointOfInteraction().getTransactionData().getQrCode());
		p.setLink(payment.getPointOfInteraction().getTransactionData().getTicketUrl());
		p.changeStatus(PurchaseStatus.WAITING);
		DonationManager.getInstance().showCheckoutWindow(player, p);
		
		if (p.wantQrCode())
			DonationManager.getInstance().showQrCodeWindow(player, p);
		else
			resendEmail(p);
	}

	/*
	 * Lida com pagamentos não encontrados de uma Preference
	 */
	private static void handleMPCheck(Purchase p)
	{
		if (p.getStatus() == PurchaseStatus.FINISHING)
		{
			DonationManager.getInstance().inactivePurchase(p);
			return;
		}
		
		final Player player = World.getInstance().getPlayer(p.getPlayerId());
		if (player == null || !player.isOnline())
			return;

		if (p.getStatus() != PurchaseStatus.EXPIRED)
			p.setMessagePaymentNotFound();

		DonationManager.getInstance().showPurchaseStatusWindow(player, p);
	}

	/*
	 * Um Payment foi encontrado, vamos verificar seu status
	 */
	public void handleMPCheck(Purchase p, Payment payment)
	{
		if (payment.isLiveMode() == DonationConfig.DONATION_MP_SANDBOX || !PaymentValidation.matches(p.getReference(),payment.getExternalReference(),p.getCurrency(),payment.getCurrencyId(),p.getTotalPrice(),payment.getTransactionAmount()) || !DonationConfig.DONATION_MP_MERCHANT_ID.equals(String.valueOf(payment.getCollectorId())) || (p.getPaymentId() != null && !p.getPaymentId().equals(String.valueOf(payment.getId()))))
        { LOGGER.warning("MercadoPagoLegacy: Rejected mismatched Mercado Pago response for purchase " + (p.getId())); return; }
        switch (payment.getStatus())
		{
			case "in_mediation":
			case "cancelled":
			case "refunded":
			case "charged_back":
			case "rejected":
				DonationManager.getInstance().onPaymentFailed(p);
				break;

			case "pending":
			case "in_process":
			case "authorized":
				if (p.getStatus() == PurchaseStatus.FINISHING)
				{
					DonationManager.getInstance().inactivePurchase(p);
					return;
				}
				
				DonationManager.getInstance().onPaymentNotFound(p);
				break;
					
			case "approved":
                if (payment.getTransactionAmountRefunded() != null && payment.getTransactionAmountRefunded().signum() != 0) return;
                p.setPaymentId(String.valueOf(payment.getId()));
				if (p.getStatus() == PurchaseStatus.COMPLETED)
					return;
				
				if (p.getPaymentMethod() == PaymentMethod.MP_LINK)
				{
					p.setPaymentId(String.valueOf(payment.getId()));
					closePurchase(p);
				}

				DonationManager.getInstance().onCompletedPayment(p);
				break;
		}
	}
	
	/*
	 * Atualizar a data de vencimento da preference para evitar que ainda continue disponível para pagamento
	 */
	private static void closePreference(Purchase purchase)
	{
		if (purchase.timeExpired())
			return;
		
		try
		{
			final PreferenceClient client = new PreferenceClient();
			final PreferenceRequest updateRequest = PreferenceRequest.builder().dateOfExpiration(OffsetDateTime.now().plusSeconds(15)).build();
			client.update(purchase.getMpPreferenceId(), updateRequest);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "MercadoPagoLegacy: Falha ao cancelar a Preference da purchase #" + (purchase.getId()) + ".", e);
		}
	}
	
	/*
	 * https://www.mercadopago.com.br/developers/pt/reference/payments/_payments_id/put
	 */
	private static void closePayment(Purchase purchase)
	{
		if (purchase.timeExpired())
			return;
		
		try
		{
			final PaymentClient client = new PaymentClient();
			client.cancel(Long.valueOf(purchase.getPaymentId()));
		}
		catch (MPApiException e)
		{
			LOGGER.log(Level.WARNING, "MercadoPagoLegacy: Falha ao cancelar o Payment da purchase #" + (purchase.getId()) + ".", e);
		}
		catch (MPException e)
		{
		}
	}

	/*
	 * https://www.mercadopago.com.ar/developers/pt/docs/checkout-pro/integrate-preferences
	 * https://www.mercadopago.com.br/developers/pt/reference/preferences/_checkout_preferences/post
	 */
	private void createPreference(Purchase purchase)
	{
		try
		{

			final PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
				.id(String.valueOf(purchase.getId()))
				.title(purchase.getProductName())
				.quantity(purchase.getQuantity())
				.currencyId(purchase.getCurrency())
				.unitPrice(purchase.getUnitPrice())
				.build();

			final PreferencePayerRequest payerRequest = PreferencePayerRequest.builder()
				.name(purchase.getPlayerName())
				.email(purchase.getPlayerEmail())
				.build();

			final OffsetDateTime eol = OffsetDateTime.ofInstant(Instant.ofEpochMilli(purchase.getExpiration()), ZoneId.systemDefault());
			final PreferenceRequest request = PreferenceRequest.builder()
				.items(List.of(itemRequest))
				.payer(payerRequest)
				.dateOfExpiration(eol)
				.statementDescriptor(DonationConfig.DONATION_SERVER_NAME)
				.expires(true)
				.externalReference(purchase.getReference()) // Preference não é associada ao Payment, temos que fazer isso
				.build();

			final PreferenceClient client = new PreferenceClient();
			final Preference preference = client.create(request);
			handleMPPreferenceResponse(purchase, preference);
			
		}
		catch (MPApiException e)
		{
			logException(e, purchase);
		}
		catch (MPException e)
		{
			logException(e, purchase);
		}
	}

	/*
	 * https://www.mercadopago.com.br/developers/pt/docs/checkout-api/integration-configuration/integrate-with-pix
	 * MercadoPagoConfig.setSocketTimeout(20000); // default
	 */
	private void createPayment(Purchase purchase)
	{
		try
		{
			
			final MPRequestOptions requestOptions = MPRequestOptions.builder()
				.customHeaders(Map.of("x-idempotency-key", purchase.getReference() + "pix"))
				.build();

			final PaymentPayerRequest payerRequest = PaymentPayerRequest.builder()
				.firstName(purchase.getPlayerName())
				.email(purchase.getPlayerEmail())
				.build();

			final OffsetDateTime eol = OffsetDateTime.ofInstant(Instant.ofEpochMilli(purchase.getExpiration()), ZoneId.systemDefault());
			final PaymentCreateRequest paymentRequest = PaymentCreateRequest.builder()
				.transactionAmount(purchase.getTotalPrice())
				.description(purchase.getProductName())
				.paymentMethodId("pix")
				.dateOfExpiration(eol)
				.payer(payerRequest)
				.additionalInfo(PaymentAdditionalInfoRequest.builder().ipAddress(purchase.getIpAddress()).build())
				.externalReference(purchase.getReference())
				.build();

			final PaymentClient client = new PaymentClient();
			final Payment payment = client.create(paymentRequest, requestOptions);
			handleMPPaymentResponse(purchase, payment);
		}
		catch (MPApiException e)
		{
			logException(e, purchase);
		}
		catch (MPException e)
		{
			logException(e, purchase);
		}
	}

	/*
	 * https://www.mercadopago.com.br/developers/pt/reference/payments/_payments_search/get
	 */
	private void getPayment(Purchase purchase)
	{
		try
		{

			final PaymentClient paymentClient = new PaymentClient();
			final Payment payment = paymentClient.get(Long.valueOf(purchase.getPaymentId()));
			handleMPCheck(purchase, payment);
		}
		catch (MPApiException e)
		{
			logException(e, purchase);
		}
		catch (MPException e)
		{
			logException(e, purchase);
		}
	}

	/*
	 * https://www.mercadopago.com.br/developers/pt/reference/payments/_payments_search/get
	 */
	private void searchPayment(Purchase purchase)
	{
		try
		{

			final PaymentClient paymentClient = new PaymentClient();
			final Map<String, Object> filters = new HashMap<>();
			filters.put("sort", "date_created");
			filters.put("criteria", "desc");
			filters.put("external_reference", purchase.getReference());
			filters.put("range", "date_created");
			filters.put("begin_date", Instant.ofEpochMilli(purchase.getDate()).toString());
			filters.put("end_date", "NOW");

			filters.put("status", "approved");
            final MPSearchRequest searchRequest = MPSearchRequest.builder().offset(0).limit(1).filters(filters).build(); // limit 0?
			final MPResultsResourcesPage<Payment> search = paymentClient.search(searchRequest);

			if (search.getResults().isEmpty())
				handleMPCheck(purchase);
			else
				handleMPCheck(purchase, search.getResults().get(0));
		}
		catch (MPApiException e)
		{
			logException(e, purchase);
		}
		catch (MPException e)
		{
			logException(e, purchase);
		}
	}

	private static void logException(MPApiException e, Purchase p)
	{
		switch (e.getApiResponse().getStatusCode())
		{
			case 404:
				handleMPCheck(p);
				break;

			case 500:
				DonationManager.getInstance().handleException(p, false);
				LOGGER.warning("MercadoPagoLegacy: A API do Mercado Pago não está funcionando agora. É necessário aguardar.");
				break;

			default:
				DonationManager.getInstance().handleException(p, true);
				LOGGER.warning("MercadoPagoLegacy: Falha ao enviar requisição da purchase #" + (p.getId()) + " para à API do Mercado Pago.");
				LOGGER.warning("MercadoPagoLegacy: Mercado Pago request returned HTTP " + (e.getApiResponse().getStatusCode()) + " for purchase " + (p.getId()));
		}
	}

	private static void logException(MPException e, Purchase p)
	{
		LOGGER.warning("MercadoPagoLegacy: Falha ao enviar requisição da purchase #" + (p.getId()) + " para à API do Mercado Pago.");
		DonationManager.getInstance().handleException(p, true);
	}

	/*
	 * Sem utilidade no momento
	 * https://www.mercadopago.com.br/developers/pt/reference/preferences/_checkout_preferences_id/get
	 */
}