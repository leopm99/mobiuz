package com.l2journey.gameserver.donation;

import java.util.UUID;

import java.util.logging.Logger;

import com.l2journey.gameserver.donation.purchase.Purchase;

public interface IPaymentHandler
{
	final Logger LOGGER = Logger.getLogger(IPaymentHandler.class.getName());
	
	void reload();
	void checkPurchase(Purchase purchase);
	default void checkPurchase(Purchase purchase, boolean interactive) { checkPurchase(purchase); }
	void sendPurchase(Purchase purchase);
	void closePurchase(Purchase purchase);
	void refund(Purchase purchase);
	void resendEmail(Purchase purchase);
	
	/**
	 * Gera uma string aleatória a partir do UUID
	 * @param lenght : tamanho (máximo 32)
	 * @return
	 */
	public default String randomString(int lenght)
	{
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, lenght);
    }
}
