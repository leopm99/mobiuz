package com.l2journey.gameserver.donation.tasks;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.l2journey.gameserver.donation.DonationTasks;
import com.l2journey.gameserver.donation.DonationReconciliation;

import com.l2journey.gameserver.donation.DonationManager;
import com.l2journey.gameserver.donation.purchase.Purchase;
import com.l2journey.gameserver.donation.purchase.PurchaseStatus;

public class DonationTaskManager implements Runnable
{
	private final Set<Purchase> _list = ConcurrentHashMap.newKeySet();

	protected DonationTaskManager()
	{
		DonationTasks.schedule(this, 10000, 10000);
	}

	public void add(Purchase p)
	{
		_list.add(p);
	}

	public void remove(Purchase p)
	{
		_list.remove(p);
	}

	@Override
	public void run()
	{
		if (_list.isEmpty())
			return;

		for (Purchase purchase : _list)
		{
			if (purchase.isBusy())
				continue;
			
			if (purchase.getStatus() != PurchaseStatus.WAITING && purchase.getStatus() != PurchaseStatus.CREATED && purchase.getStatus() != PurchaseStatus.PENDING)
			{
				_list.remove(purchase);
				continue;
			}

			if (!DonationReconciliation.matchesCurrentAccount(purchase))
			{
				// Keep the saved state. The automatic reconciliation task can resume with the original account/environment.
				_list.remove(purchase);
				continue;
			}

			if (purchase.timeExpired())
			{
				if (purchase.getPaymentId() != null || purchase.getMpPreferenceId() != null || purchase.getPaypalInvoiceId() != null)
				{
					purchase.changeStatus(PurchaseStatus.FINISHING);
					purchase.getPaymentHandler().checkPurchase(purchase);
					continue;
				}

				DonationManager.getInstance().deleteOrExpire(purchase);
				_list.remove(purchase);
			}
		}
	}

	public static final DonationTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private static class SingletonHolder
	{
		protected static final DonationTaskManager INSTANCE = new DonationTaskManager();
	}
}
