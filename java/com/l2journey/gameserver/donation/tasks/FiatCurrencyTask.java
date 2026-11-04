package com.l2journey.gameserver.donation.tasks;

import java.util.logging.Level;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import java.util.logging.Logger;
import com.l2journey.gameserver.donation.DonationTasks;

import com.l2journey.gameserver.donation.CurrencyManager;
import com.l2journey.gameserver.donation.purchase.PaymentMethod;
import com.l2journey.gameserver.donation.DonationConfig;

public class FiatCurrencyTask implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(FiatCurrencyTask.class.getName());

	protected FiatCurrencyTask()
	{
		final long delay = Math.max(TimeUnit.MINUTES.toMillis(20), Math.min(TimeUnit.MINUTES.toMillis(DonationConfig.DONATION_CURRENCY_TASK_INTERVAL), TimeUnit.HOURS.toMillis(2)));
		DonationTasks.schedule(this, 5000, delay);
	}

	@Override
	public void run()
	{
		for (PaymentMethod pm : PaymentMethod.values())
		{
			if (!DonationConfig.DONATION_ENABLED)
				break;

			if (!pm.isEnabled() || pm == PaymentMethod.BINANCE || pm.getAltCurrencies() == null)
				continue;

			final boolean beacon = !DonationConfig.DONATION_CURRENCY_CB_API_KEY.isEmpty();

			try
			{
				if (beacon)
					updateCurrencyBeacon(pm);
				else if (DonationConfig.DONATION_CURRENCY_AWESOMEAPI)
					updateAwesomeApi(pm);
				else
					LOGGER.warning("FiatCurrencyTask: Nenhum serviço de câmbio de moedas fiat foi ativado. Por favor, verifique seu arquivo de configuração.");
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "FiatCurrencyTask: Falha ao consultar a cotação de moedas através do " + (beacon ? "CurrencyBeacon" : "AwesomeAPI") + ".", e);

				try
				{
					if (beacon)
					{
						LOGGER.warning("FiatCurrencyTask: Tentando o AwesomeAPI como alternativa.");
						updateAwesomeApi(pm);
					}
				}
				catch (Exception e2)
				{
					LOGGER.log(Level.WARNING, "FiatCurrencyTask: Nenhuma API de currency exchange está funcionando. Os últimos valores serão utilizados (caso existam).", e2);
				}
			}
		}
	}

	private static void updateCurrencyBeacon(PaymentMethod pm) throws IOException, InterruptedException
	{
		final String url = String.format("https://api.currencybeacon.com/v1/latest?api_key=%s&base=%s&symbols=%s", DonationConfig.DONATION_CURRENCY_CB_API_KEY, pm.getMainCurrency(), String.join(",", pm.getAltCurrencies()));
		final HttpRequest request = HttpRequest.newBuilder().timeout(java.time.Duration.ofSeconds(20))
			.uri(URI.create(url))
			.GET()
			.build();

		final HttpResponse<String> response = CurrencyManager.getInstance().getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
		final Gson gson = new Gson();
		final JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
		final JsonObject rates = jsonObject.getAsJsonObject("response").getAsJsonObject("rates");

		for (String currency : pm.getAltCurrencies())
		{
			final BigDecimal rate = rates.get(currency).getAsBigDecimal();
			CurrencyManager.getInstance().setExchangeRate(pm.getMainCurrency() + currency, rate, false);
		}
	}

	private static void updateAwesomeApi(PaymentMethod pm) throws IOException, InterruptedException
	{
		final String conversions = Arrays.stream(pm.getAltCurrencies()).map(c -> pm.getMainCurrency() + "-" + c).collect(Collectors.joining(","));
		final String url = "https://economia.awesomeapi.com.br/last/" + conversions;
		final HttpRequest request = HttpRequest.newBuilder().timeout(java.time.Duration.ofSeconds(20))
			.uri(URI.create(url))
			.GET()
			.build();

		final HttpResponse<String> response = CurrencyManager.getInstance().getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
		final Gson gson = new Gson();
		final JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);

		for (String currency : conversions.replace("-", "").split(","))
		{
			final BigDecimal rate = jsonObject.getAsJsonObject(currency).get("high").getAsBigDecimal();
			CurrencyManager.getInstance().setExchangeRate(pm.getMainCurrency() + currency, rate, false);
		}
	}

	public static FiatCurrencyTask getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final FiatCurrencyTask INSTANCE = new FiatCurrencyTask();
	}
}
