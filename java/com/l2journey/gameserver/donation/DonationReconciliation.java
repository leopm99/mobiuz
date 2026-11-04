package com.l2journey.gameserver.donation;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import com.l2journey.gameserver.donation.purchase.Purchase;
import com.l2journey.gameserver.donation.purchase.PurchaseStatus;

/** Selects purchases without changing their saved payment context or financial state. */
public final class DonationReconciliation
{
	private DonationReconciliation() { }

	public static boolean matchesCurrentAccount(Purchase purchase)
	{
		return !purchase.usesMercadoPagoOrders() || (purchase.isMpSandbox() == DonationConfig.DONATION_MP_SANDBOX
			&& purchase.getMpMerchantId() != null && !purchase.getMpMerchantId().isBlank()
			&& Objects.equals(purchase.getMpMerchantId(), DonationConfig.DONATION_MP_MERCHANT_ID));
	}

	public static boolean shouldCheck(Purchase purchase, long now)
	{
		final PurchaseStatus status = purchase.getStatus();
		return purchase.wasRequested() && status != PurchaseStatus.COMPLETED && status != PurchaseStatus.OFFLINE
			&& status != PurchaseStatus.HIDDEN && purchase.getDate() > now - TimeUnit.DAYS.toMillis(7)
			&& matchesCurrentAccount(purchase);
	}
}
