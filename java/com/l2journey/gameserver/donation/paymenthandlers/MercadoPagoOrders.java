package com.l2journey.gameserver.donation.paymenthandlers;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.logging.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.l2journey.gameserver.donation.DonationConfig;
import com.l2journey.gameserver.donation.DonationData;
import com.l2journey.gameserver.donation.DonationManager;
import com.l2journey.gameserver.donation.DonationReconciliation;
import com.l2journey.gameserver.donation.Mailersend;
import com.l2journey.gameserver.donation.PaymentValidation;
import com.l2journey.gameserver.donation.purchase.Purchase;
import com.l2journey.gameserver.donation.purchase.PurchaseStatus;
import com.l2journey.gameserver.model.World;

/** PIX Orders API. No SDK upgrade or changes to the game's HTML generation are required. */
final class MercadoPagoOrders
{
	private static final Logger LOGGER = Logger.getLogger(MercadoPagoOrders.class.getName());
	private final Transport _transport;
	private final LongSupplier _clock;
	private final ConcurrentHashMap<Integer, RetryState> _retries = new ConcurrentHashMap<>();
	
	private record RetryState(int attempts, long nextCheck, long nextWarning, long lastFailure, String reason)
	{
	}
	
	static final class OrdersHttpException extends IOException
	{
		private static final long serialVersionUID = 1L;
		final int status;
		
		OrdersHttpException(int status)
		{
			super("Orders request failed (HTTP " + status + ")");
			this.status = status;
		}
	}
	
	@FunctionalInterface
	interface Transport
	{
		JsonObject request(String method, String path, String key, JsonObject payload, boolean sandbox) throws IOException, InterruptedException;
	}
	
	MercadoPagoOrders()
	{
		this(new HttpTransport());
	}
	
	MercadoPagoOrders(Transport transport)
	{
		this(transport, System::currentTimeMillis);
	}
	
	MercadoPagoOrders(Transport transport, LongSupplier clock)
	{
		_transport = transport;
		_clock = clock;
	}
	
	static String idempotencyKey(Purchase p, String operation)
	{
		return UUID.nameUUIDFromBytes((p.getReference() + ":orders:" + operation).getBytes(StandardCharsets.UTF_8)).toString();
	}
	
	/**
	 * The same purchase always produces the same payload, even after a restart.
	 * @param p
	 * @return
	 */
	static JsonObject createPayload(Purchase p)
	{
		if (!"BRL".equals(p.getCurrency()) || (p.getTotalPrice().signum() <= 0))
		{
			throw new IllegalArgumentException("PIX Orders requires a positive BRL amount");
		}
		final long seconds = (p.getExpiration() - p.getDate()) / 1000;
		if ((seconds < 1800) || (seconds > Duration.ofDays(30).toSeconds()))
		{
			throw new IllegalArgumentException("PIX expiration must be between 30 minutes and 30 days");
		}
		final JsonObject method = new JsonObject();
		method.addProperty("id", "pix");
		method.addProperty("type", "bank_transfer");
		// Optional explicit descriptor; provider acceptance for PIX must be verified in sandbox.
		if ((p.getMpRequestVersion() >= 4) && (p.getMpStatementDescriptor() != null) && !p.getMpStatementDescriptor().isBlank())
		{
			method.addProperty("statement_descriptor", p.getMpStatementDescriptor());
		}
		final JsonObject payment = new JsonObject();
		payment.addProperty("amount", p.getTotalPrice().toPlainString());
		payment.addProperty("expiration_time", Duration.ofSeconds(seconds).toString());
		payment.add("payment_method", method);
		final JsonArray payments = new JsonArray();
		payments.add(payment);
		final JsonObject transactions = new JsonObject();
		transactions.add("payments", payments);
		final JsonObject payer = new JsonObject();
		payer.addProperty("email", p.getPlayerEmail());
		if (p.getMpRequestVersion() >= 5)
		{
			final com.l2journey.gameserver.donation.BuyerDetails details = p.getBuyerDetails();
			if (details.firstName() != null)
			{
				payer.addProperty("first_name", details.firstName());
			}
			if (details.lastName() != null)
			{
				payer.addProperty("last_name", details.lastName());
			}
			if (details.cpf() != null)
			{
				final JsonObject identification = new JsonObject();
				identification.addProperty("type", "CPF");
				identification.addProperty("number", details.cpf());
				payer.add("identification", identification);
			}
		}
		if (p.isMpSandbox() && p.isMpSandboxAutoApprove())
		{
			// Official sandbox PIX scenario; never add this simulation field in production.
			payer.addProperty("first_name", "APRO");
		}
		final JsonObject order = new JsonObject();
		order.addProperty("type", "online");
		order.addProperty("processing_mode", "automatic");
		order.addProperty("total_amount", p.getTotalPrice().toPlainString());
		order.addProperty("external_reference", p.getReference());
		order.add("payer", payer);
		order.add("transactions", transactions);
		// Keep the original body for already-requested orders when retrying the same key.
		if (p.getMpRequestVersion() >= 2)
		{
			final BigDecimal unitPrice = p.getUnitPrice().setScale(2, java.math.RoundingMode.UNNECESSARY);
			if ((p.getQuantity() <= 0) || (unitPrice.signum() <= 0) || (unitPrice.multiply(BigDecimal.valueOf(p.getQuantity())).compareTo(p.getTotalPrice()) != 0) || (p.getProductName() == null) || p.getProductName().isBlank())
			{
				throw new IllegalArgumentException("Invalid order product snapshot");
			}
			final JsonObject item = new JsonObject();
			item.addProperty("title", p.getProductName());
			item.addProperty("external_code", Integer.toString(p.getProductId()));
			item.addProperty("quantity", p.getQuantity());
			item.addProperty("unit_price", unitPrice.toPlainString());
			if ((p.getMpRequestVersion() >= 3) && (p.getMpProductCategory() != null) && !p.getMpProductCategory().isBlank())
			{
				item.addProperty("category_id", p.getMpProductCategory());
			}
			final JsonArray items = new JsonArray();
			items.add(item);
			order.add("items", items);
		}
		return order;
	}
	
	void create(Purchase p)
	{
		try
		{
			if (!checkAccount(p, true))
			{
				return;
			}
			if (p.getPaymentId() == null)
			{
				final JsonObject order = _transport.request("POST", "/v1/orders", idempotencyKey(p, "create"), createPayload(p), p.isMpSandbox());
				if (!creationMatches(p, order))
				{
					throw new IOException("Unexpected order creation response");
				}
				p.setPaymentId(text(order, "id"));
				// Persist ORD before the follow-up request, including asynchronous creation.
				p.changeStatus(PurchaseStatus.WAITING);
				LOGGER.info("MercadoPagoOrders: Mercado Pago Orders creation saved for purchase " + p.getId());
			}
			handle(p, fetch(p), true, true);
			_retries.remove(p.getId());
		}
		catch (IOException | RuntimeException e)
		{
			failed(p, true, "create", e);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			failed(p, true, "create", e);
		}
	}
	
	void check(Purchase p, boolean interactive)
	{
		if (!checkAccount(p, interactive))
		{
			return;
		}
		final RetryState retry = _retries.get(p.getId());
		if (!interactive && (retry != null) && (_clock.getAsLong() < retry.nextCheck()))
		{
			return;
		}
		try
		{
			final JsonObject order = fetch(p);
			if (order != null)
			{
				handle(p, order, interactive, false);
			}
			else if (interactive)
			{
				p.setMessagePaymentNotFound();
				show(p);
			}
			_retries.remove(p.getId());
		}
		catch (IOException | RuntimeException e)
		{
			failed(p, interactive, "check", e);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			failed(p, interactive, "check", e);
		}
	}
	
	void cancel(Purchase p)
	{
		try
		{
			if (!checkAccount(p, false))
			{
				return;
			}
			final JsonObject order = fetch(p);
			if ((order == null) || !matches(p, order))
			{
				return;
			}
			if (approved(p, order))
			{
				handle(p, order, false, false);
				return;
			}
			final String status = text(order, "status");
			if ("action_required".equals(status) || "created".equals(status))
			{
				_transport.request("POST", "/v1/orders/" + text(order, "id") + "/cancel", idempotencyKey(p, "cancel"), null, p.isMpSandbox());
				// An HTTP success is not proof that no payment completed in the meantime.
				handle(p, fetch(p), false, false);
			}
		}
		catch (IOException | RuntimeException e)
		{
			failed(p, false, "cancel", e);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			failed(p, false, "cancel", e);
		}
	}
	
	private static boolean checkAccount(Purchase p, boolean interactive)
	{
		if (p.usesMercadoPagoOrders() && DonationReconciliation.matchesCurrentAccount(p))
		{
			return true;
		}
		if (interactive)
		{
			p.setMessage("<br>This purchase belongs to another payment account or test/production environment. Its saved details were preserved; please contact a GM for reconciliation.");
			show(p);
		}
		return false;
	}
	
	private JsonObject fetch(Purchase p) throws IOException, InterruptedException
	{
		String id = p.getPaymentId();
		if (id == null)
		{
			// A timeout can happen after MP creates the order. Search by the durable reference.
			final String path = "/v1/orders?type=online&page=1&page_size=2&external_reference=" + encode(p.getReference()) + "&begin_date=" + encode(Instant.ofEpochMilli(p.getDate() - 60000).toString()) + "&end_date=" + encode(Instant.now().toString());
			final JsonObject search = _transport.request("GET", path, null, null, p.isMpSandbox());
			final JsonArray data = search.getAsJsonArray("data");
			if (data == null)
			{
				throw new IOException("Missing Orders search data");
			}
			if (data.isEmpty())
			{
				return null;
			}
			if ((data.size() != 1) || (search.has("paging") && search.getAsJsonObject("paging").has("total") && (search.getAsJsonObject("paging").get("total").getAsInt() != 1)))
			{
				throw new IOException("Ambiguous Orders reference; manual reconciliation required");
			}
			id = text(data.get(0).getAsJsonObject(), "id");
		}
		if (!validId(id, "ORD"))
		{
			throw new IOException("Invalid Mercado Pago order id");
		}
		final JsonObject order = _transport.request("GET", "/v1/orders/" + id, null, null, p.isMpSandbox());
		if (!id.equals(text(order, "id")))
		{
			throw new IOException("Order id does not match requested resource");
		}
		return order;
	}
	
	private void handle(Purchase p, JsonObject order, boolean interactive, boolean checkout) throws IOException
	{
		if ((p.getStatus() == PurchaseStatus.COMPLETED) || (p.getStatus() == PurchaseStatus.OFFLINE) || (p.getStatus() == PurchaseStatus.HIDDEN))
		{
			return;
		}
		if (!matches(p, order))
		{
			throw new IOException("Order identity, amount or PIX transaction mismatch");
		}
		p.setPaymentId(text(order, "id"));
		final JsonArray payments = order.getAsJsonObject("transactions").getAsJsonArray("payments");
		if ((payments != null) && (payments.size() == 1))
		{
			final JsonObject payment = payments.get(0).getAsJsonObject();
			p.setMpTransactionId(text(payment, "id"));
			final JsonObject method = payment.getAsJsonObject("payment_method");
			if (text(method, "qr_code") != null)
			{
				p.setQrCode(text(method, "qr_code"));
			}
			if (text(method, "ticket_url") != null)
			{
				p.setLink(text(method, "ticket_url"));
			}
		}
		DonationData.getInstance().update(p);
		if (approved(p, order))
		{
			DonationManager.getInstance().onCompletedPayment(p);
			return;
		}
		final PurchaseStatus terminal = terminalStatus(order);
		if (terminal != null)
		{
			p.changeStatus(terminal);
		}
		else if ((p.getStatus() == PurchaseStatus.CREATED) || (p.getStatus() == PurchaseStatus.PENDING))
		{
			p.changeStatus(PurchaseStatus.WAITING);
		}
		if (interactive)
		{
			if (checkout && (p.getStatus() == PurchaseStatus.WAITING) && ((p.getQrCode() != null) || (p.getLink() != null)))
			{
				DonationManager.getInstance().showCheckoutWindow(p);
				if (p.wantQrCode() && (p.getQrCode() != null))
				{
					DonationManager.getInstance().showQrCodeWindow(World.getInstance().getPlayer(p.getPlayerId()), p);
				}
				else if (!p.wantQrCode() && (p.getLink() != null))
				{
					p.logEmailSending();
					Mailersend.sendPurchaseMail(p);
				}
			}
			else
			{
				show(p);
			}
		}
	}
	
	/**
	 * A timed-out PIX may be reported as canceled/failed with detail=expired.
	 * @param order
	 * @return
	 */
	static PurchaseStatus terminalStatus(JsonObject order)
	{
		final String status = text(order, "status");
		if ("expired".equals(status))
		{
			return PurchaseStatus.EXPIRED;
		}
		if ("canceled".equals(status) || "failed".equals(status))
		{
			boolean expired = "expired".equals(text(order, "status_detail"));
			final JsonObject transactions = order.getAsJsonObject("transactions");
			final JsonArray payments = transactions == null ? null : transactions.getAsJsonArray("payments");
			if ((payments != null) && (payments.size() == 1))
			{
				final JsonObject payment = payments.get(0).getAsJsonObject();
				expired |= "expired".equals(text(payment, "status")) || "expired".equals(text(payment, "status_detail"));
			}
			if (expired)
			{
				return PurchaseStatus.EXPIRED;
			}
			return "canceled".equals(status) ? PurchaseStatus.CANCELED : PurchaseStatus.FAILED;
		}
		return "refunded".equals(status) ? PurchaseStatus.FAILED : null;
	}
	
	static boolean creationMatches(Purchase p, JsonObject order)
	{
		try
		{
			return validId(text(order, "id"), "ORD") && "online".equals(text(order, "type")) && p.getReference().equals(text(order, "external_reference")) && (p.getTotalPrice().compareTo(decimal(order, "total_amount")) == 0);
		}
		catch (RuntimeException e)
		{
			return false;
		}
	}
	
	/**
	 * Orders PIX is BRL in Brazil; currency/live_mode are checked when the API supplies them.
	 * @param p
	 * @param order
	 * @return
	 */
	static boolean matches(Purchase p, JsonObject order)
	{
		try
		{
			if (!creationMatches(p, order) || !"BRL".equals(p.getCurrency()) || !"BRA".equals(text(order, "country_code")) || !Objects.equals(p.getMpMerchantId(), text(order, "user_id")) || (p.getMpMerchantId() == null))
			{
				return false;
			}
			if (((p.getPaymentId() != null) && !p.getPaymentId().equals(text(order, "id"))) || (order.has("currency") && !"BRL".equals(text(order, "currency"))) || (order.has("live_mode") && (order.get("live_mode").getAsBoolean() == p.isMpSandbox())))
			{
				return false;
			}
			final JsonArray payments = order.getAsJsonObject("transactions").getAsJsonArray("payments");
			if ((payments == null) || payments.isEmpty())
			{
				return "processing".equals(text(order, "status")) || "created".equals(text(order, "status"));
			}
			if (payments.size() != 1)
			{
				return false;
			}
			final JsonObject payment = payments.get(0).getAsJsonObject();
			final JsonObject method = payment.getAsJsonObject("payment_method");
			return validId(text(payment, "id"), "PAY") && ((p.getMpTransactionId() == null) || p.getMpTransactionId().equals(text(payment, "id"))) && (p.getTotalPrice().compareTo(decimal(payment, "amount")) == 0) && "pix".equals(text(method, "id")) && "bank_transfer".equals(text(method, "type"));
		}
		catch (RuntimeException e)
		{
			return false;
		}
	}
	
	static boolean approved(Purchase p, JsonObject order)
	{
		try
		{
			if (!matches(p, order) || !"processed".equals(text(order, "status")) || !"accredited".equals(text(order, "status_detail")))
			{
				return false;
			}
			final JsonObject transactions = order.getAsJsonObject("transactions");
			if ((transactions.has("refunds") && !transactions.getAsJsonArray("refunds").isEmpty()) || (transactions.has("chargebacks") && !transactions.getAsJsonArray("chargebacks").isEmpty()))
			{
				return false;
			}
			final JsonObject payment = transactions.getAsJsonArray("payments").get(0).getAsJsonObject();
			return "processed".equals(text(payment, "status")) && "accredited".equals(text(payment, "status_detail")) && (p.getTotalPrice().compareTo(decimal(order, "total_paid_amount")) == 0) && (p.getTotalPrice().compareTo(decimal(payment, "paid_amount")) == 0);
		}
		catch (RuntimeException e)
		{
			return false;
		}
	}
	
	private static boolean validId(String value, String prefix)
	{
		return (value != null) && value.matches(prefix + "[A-Za-z0-9]{6,64}");
	}
	
	private static BigDecimal decimal(JsonObject value, String key)
	{
		return new BigDecimal(text(value, key));
	}
	
	private static String text(JsonObject value, String key)
	{
		return PaymentValidation.text(value, key);
	}
	
	private static String encode(String value)
	{
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
	
	private static void show(Purchase p)
	{
		DonationManager.getInstance().showPurchaseStatusWindow(World.getInstance().getPlayer(p.getPlayerId()), p);
	}
	
	private void failed(Purchase p, boolean interactive, String operation, Exception failure)
	{
		final long now = _clock.getAsLong();
		final int status = failure instanceof OrdersHttpException http ? http.status : 0;
		final String reason = failure.getClass().getSimpleName() + ":" + status;
		// Expire inactive retry records without retaining purchase objects or private provider data.
		_retries.entrySet().removeIf(entry -> (now - entry.getValue().lastFailure()) > Duration.ofDays(7).toMillis());
		_retries.compute(p.getId(), (id, previous) ->
		{
			final int attempts = previous == null ? 1 : Math.min(previous.attempts() + 1, 8);
			final long base = status == 404 ? Duration.ofMinutes(5).toMillis() : Duration.ofSeconds(30).toMillis();
			final long cap = Duration.ofMinutes(status == 404 ? 60 : 15).toMillis();
			final long delay = Math.min(cap, base * (1L << (attempts - 1)));
			final boolean warn = (previous == null) || (now >= previous.nextWarning()) || !reason.equals(previous.reason());
			if (warn)
			{
				// Do not log raw exception messages, response bodies, tokens or buyer details.
				LOGGER.warning("MercadoPagoOrders: Mercado Pago Orders " + operation + " failed for purchase " + id + " (" + failure.getClass().getSimpleName() + "; HTTP " + (status == 0 ? "unavailable" : Integer.toString(status)) + "); automatic retry in " + (delay / 1000) + " seconds; saved references were retained");
			}
			return new RetryState(attempts, now + delay, warn ? now + Duration.ofMinutes(5).toMillis() : previous.nextWarning(), now, reason);
		});
		if (interactive)
		{
			p.setMessage("<br>Could not check the payment service. Your purchase is saved; please try again later.");
			show(p);
		}
	}
	
	static final class HttpTransport implements Transport
	{
		private final HttpClient _client;
		
		HttpTransport()
		{
			this(newClient());
		}
		
		static HttpClient newClient()
		{
			final javax.net.ssl.SSLParameters tls = new javax.net.ssl.SSLParameters();
			tls.setProtocols(new String[]
			{
				"TLSv1.3",
				"TLSv1.2"
			});
			tls.setEndpointIdentificationAlgorithm("HTTPS");
			return HttpClient.newBuilder().sslParameters(tls).connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
		}
		
		HttpTransport(HttpClient client)
		{
			_client = client;
		}
		
		private final AtomicLong _blockedUntil = new AtomicLong();
		
		@Override
		public JsonObject request(String method, String path, String key, JsonObject payload, boolean sandbox) throws IOException, InterruptedException
		{
			if (System.currentTimeMillis() < _blockedUntil.get())
			{
				throw new IOException("Orders provider backoff is active");
			}
			final HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("https://api.mercadopago.com" + path)).timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + DonationConfig.DONATION_MP_TOKEN).header("Content-Type", "application/json").header("Accept", "application/json");
			if (sandbox)
			{
				builder.header("x-test-token", "true");
			}
			if (key != null)
			{
				builder.header("X-Idempotency-Key", key);
			}
			builder.method(method, payload == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8));
			final HttpResponse<String> response = _client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if ((response.statusCode() == 429) || (response.statusCode() >= 500))
			{
				long delay = 30000;
				final String retry = response.headers().firstValue("Retry-After").orElse("");
				try
				{
					delay = Math.max(delay, Math.multiplyExact(Long.parseLong(retry), 1000));
				}
				catch (RuntimeException e)
				{
					try
					{
						delay = Math.max(delay, ZonedDateTime.parse(retry, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() - System.currentTimeMillis());
					}
					catch (RuntimeException ignored)
					{
						// Keep the default backoff when the provider omits a valid value.
					}
				}
				_blockedUntil.accumulateAndGet(System.currentTimeMillis() + Math.min(delay, Duration.ofDays(1).toMillis()), Math::max);
			}
			if ((response.statusCode() < 200) || (response.statusCode() >= 300) || (response.body().length() > 1048576))
			{
				throw new OrdersHttpException(response.statusCode());
			}
			return response.body().isBlank() ? new JsonObject() : JsonParser.parseString(response.body()).getAsJsonObject();
		}
	}
}
