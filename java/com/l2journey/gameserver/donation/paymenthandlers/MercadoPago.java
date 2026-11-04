package com.l2journey.gameserver.donation.paymenthandlers;

import com.l2journey.gameserver.donation.DonationExecutor;
import com.l2journey.gameserver.donation.IPaymentHandler;
import com.l2journey.gameserver.donation.purchase.PaymentMethod;
import com.l2journey.gameserver.donation.purchase.Purchase;

/** Routes each purchase to the API that originally created it. */
public final class MercadoPago implements IPaymentHandler
{
	private final MercadoPagoLegacy _legacy = new MercadoPagoLegacy();
	private final MercadoPagoOrders _orders = new MercadoPagoOrders();

	@Override
	public void reload()
	{
		_legacy.reload();
	}

	@Override
	public void sendPurchase(Purchase purchase)
	{
		if (purchase.getPaymentMethod() == PaymentMethod.MP_PIX)
		{
			synchronized (purchase)
			{
				if (purchase.isBusy())
				{
					return;
				}
				if (!purchase.wasRequested())
				{
					purchase.useMercadoPagoOrders();
				}
			}
			if (purchase.usesMercadoPagoOrders())
			{
				DonationExecutor.submit(purchase, () -> _orders.create(purchase), true);
				return;
			}
		}
		_legacy.sendPurchase(purchase);
	}

	@Override
	public void checkPurchase(Purchase purchase)
	{
		checkPurchase(purchase, false);
	}

	@Override
	public void checkPurchase(Purchase purchase, boolean interactive)
	{
		if (purchase.usesMercadoPagoOrders())
		{
			DonationExecutor.submit(purchase, () -> _orders.check(purchase, interactive), false);
		}
		else
		{
			_legacy.checkPurchase(purchase);
		}
	}

	@Override
	public void closePurchase(Purchase purchase)
	{
		if (purchase.usesMercadoPagoOrders())
		{
			DonationExecutor.submit(purchase, () -> _orders.cancel(purchase), false);
		}
		else
		{
			_legacy.closePurchase(purchase);
		}
	}

	@Override
	public void refund(Purchase purchase)
	{
		if (purchase.usesMercadoPagoOrders())
		{
			LOGGER.warning("MercadoPago: Refund order " + (purchase.getId()) + " through the Mercado Pago dashboard and reconcile delivered items manually");
		}
		else
		{
			_legacy.refund(purchase);
		}
	}

	@Override
	public void resendEmail(Purchase purchase)
	{
		_legacy.resendEmail(purchase);
	}
}
