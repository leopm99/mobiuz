package com.l2journey.gameserver.donation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.l2journey.gameserver.donation.purchase.PaymentMethod;
import com.l2journey.gameserver.model.actor.Player;

public class CheckoutChoice
{
	private String _gateway;
	private String _currency;
	private List<String> _customCryptos;
	
	public void changeCurrencyChoice(Player player, String currency)
	{
		final List<String> currencies = Arrays.stream(PaymentMethod.values()).flatMap(p -> p.getAllCurrencies().stream()).toList();
		if (!currencies.contains(currency) && !CurrencyManager.getInstance().isCryptoAvailable(currency))
			return;
		
		if (getCurrency() != null)
		{
			if (getCurrency().equals(currency))
			{
				setCurrency(null);
			}
			else
			{
				setCurrency(currency);
			}
		}
		else
		{
			setCurrency(currency);
		}
		
		if (getGateway() != null)
		{
			if (getGateway().equals("BINANCE"))
			{
				if (!PaymentMethod.valueOf(getGateway()).getAllCurrencies().contains(currency))
					getCustomCryptos().add(currency);
			}
			
			if (getCurrency() == null)
			{
				if (PaymentMethod.valueOf(getGateway()).getAllCurrencies().size() == 1)
				{
					setGateway(null);
				}
			}
		}
		else
		{
			final List<PaymentMethod> pmList = Arrays.stream(PaymentMethod.values()).filter(p -> p.getAllCurrencies().contains(getCurrency())).toList();
			
			if (pmList.size() == 1)
			{
				setGateway(pmList.iterator().next().name());
			}
		}
			
		DonationManager.getInstance().showIndexWindow(player);
	}
	
	public void changeGatewayChoice(Player player, String gateway)
	{
		try
		{
			final PaymentMethod pm = PaymentMethod.valueOf(gateway);
			boolean deactivating = false;
			
			if (getGateway() != null)
			{
				if (getGateway().equals(gateway))
				{
					deactivating = true;
					
					setGateway(null);
					setCurrency(null);
				}
				else
				{
					setGateway(gateway);
				}
			}
			else
			{
				setGateway(gateway);
			}
			
			if (getCurrency() != null)
			{
				if (!pm.getAllCurrencies().contains(getCurrency()))
					setCurrency(null);
				
			}
			else if (!deactivating)
			{
				final List<PaymentMethod> pmList = Arrays.stream(PaymentMethod.values()).filter(p -> p.name().equals(gateway) && p.getAllCurrencies().size() == 1).toList();
				
				if (pmList.size() == 1)
				{
					setCurrency(pmList.iterator().next().getMainCurrency());
				}
			}
			
			DonationManager.getInstance().showIndexWindow(player);
		}
		catch (IllegalArgumentException e)
		{
		}
	}
	
	public void changeCustom(Player player, String currency)
	{
		if (!CurrencyManager.getInstance().isCryptoAvailable(currency))
			return;
		
		if (getCustomCryptos().contains(currency))
			getCustomCryptos().remove(currency);
		else
		{
			if (getCustomCryptos().size() >= 8)
			{
				DonationManager.getInstance().showSearchWindow(player, "Você atingiu o número máximo.", null);
				return;
			}
			
			getCustomCryptos().add(currency);
		}

		DonationManager.getInstance().showSearchWindow(player, null, null);
	}
	
	public String getGateway()
	{
		if (_gateway == null && _currency == null)
		{
			final List<PaymentMethod> pmList = Arrays.stream(PaymentMethod.values()).filter(p -> p.isEnabled()).toList();
			if (pmList.size() == 1)
			{
				final PaymentMethod pm = pmList.iterator().next();
				_gateway = pm.name();
				
				if (pm.getAllCurrencies().size() == 1)
					_currency = pm.getMainCurrency();
			}
		}
		
		return _gateway;
	}
	
	public void setGateway(String gateway)
	{
		_gateway = gateway;
	}

	public String getCurrency()
	{
		return _currency;
	}

	public void setCurrency(String currency)
	{
		_currency = currency;
	}
	
	public String getGatewayName()
	{
		return PaymentMethod.valueOf(_gateway).getDisplayName();
	}
	
	public BigDecimal getGatewayPrice()
	{
		return getGatewayPrice(1);
	}
	
	public BigDecimal getGatewayPrice(int qnt)
	{
		return PaymentMethod.valueOf(_gateway).getPrice(_currency, qnt);
	}
	
	/**
	 * @return : Lista de moedas padrão e "adicionadas" por este player através da função de pesquisa. Pode acontecer caso DONATION_BINANCE_PAY_CURRENCY esteja vazio
	 */
	public final List<String> getCustomCryptos()
	{
		if (_customCryptos == null)
			_customCryptos = new ArrayList<>();
		
		return _customCryptos;
	}
}
