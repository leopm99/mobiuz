package com.l2journey.gameserver.donation.tasks;

import java.util.logging.Level;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import java.util.logging.Logger;
import com.l2journey.gameserver.donation.DonationTasks;

import com.l2journey.gameserver.donation.CurrencyManager;
import com.l2journey.gameserver.donation.DonationManager;
import com.l2journey.gameserver.donation.purchase.PaymentMethod;
import com.l2journey.gameserver.donation.DonationConfig;

public class CryptoCurrencyTask implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(CryptoCurrencyTask.class.getName());
	
	protected CryptoCurrencyTask()
	{
		final long delay = Math.max(TimeUnit.MINUTES.toMillis(1), Math.min(TimeUnit.MINUTES.toMillis(DonationConfig.DONATION_BINANCE_CURRENCY_TASK_INTERVAL), TimeUnit.MINUTES.toMillis(60)));
		DonationTasks.schedule(this, 5000, delay);
	}
	
	@Override
	public void run()
	{
		if (!DonationConfig.DONATION_ENABLED || !DonationConfig.DONATION_BINANCE_PAY) return;
		update(PaymentMethod.valueOf("BINANCE").getAllCurrencies(), null, 0);
	}
	
	public void search(int playerId, String search)
	{
		update(null, search, playerId);
	}
	
	private static void update(List<String> pmCurrencies, String playerSearch, int playerId)
	{

		try
		{
			final List<String> currencies = playerSearch == null ? pmCurrencies : List.of(playerSearch);
			final String conversions = currencies.stream().map(c -> "\"" + c + DonationConfig.DONATION_BINANCE_FIAT_CURRENCY + "\"").collect(Collectors.joining(",", "[", "]"));
			final HttpRequest request = HttpRequest.newBuilder().timeout(java.time.Duration.ofSeconds(20))
				.uri(URI.create("https://api.binance.com/api/v3/ticker/price?symbols=" + URLEncoder.encode(conversions, StandardCharsets.UTF_8)))
				.build();

			final HttpResponse<String> response = CurrencyManager.getInstance().getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
			if (!response.body().contains("Invalid symbol"))
			{
				final JsonArray jsonArray = JsonParser.parseString(response.body()).getAsJsonArray();
				for (JsonElement element : jsonArray)
				{
					final JsonObject jsonObject = element.getAsJsonObject();
					CurrencyManager.getInstance().setExchangeRate(jsonObject.get("symbol").getAsString(), jsonObject.get("price").getAsBigDecimal(), true);
				}
			}
		}
		catch (Exception e)
		{
			if (playerSearch == null)
				LOGGER.log(Level.WARNING, "CryptoCurrencyTask: Falha ao consultar a cotação de moedas através da Binance. Por favor, verique se as moedas utilizadas são compatíveis. Os últimos valores serão utilizados (caso existam).", e);
		}
		finally
		{
			if (playerId != 0 && playerSearch != null)
				DonationManager.getInstance().onSearchCrypto(playerId, playerSearch);
		}
	}
	
	public static CryptoCurrencyTask getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final CryptoCurrencyTask INSTANCE = new CryptoCurrencyTask();
	}
}
