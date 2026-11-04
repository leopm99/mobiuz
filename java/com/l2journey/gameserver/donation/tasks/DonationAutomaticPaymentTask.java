package com.l2journey.gameserver.donation.tasks;
import java.util.List;
import com.l2journey.gameserver.donation.*;
import com.l2journey.gameserver.donation.purchase.*;
/** Bounded rotating reconciliation, including purchases older than five minutes. */
public final class DonationAutomaticPaymentTask implements Runnable
{
 private int cursor;
 private DonationAutomaticPaymentTask() { DonationTasks.schedule(this,30000,30000); }
 @Override public void run()
 {
  final long now = System.currentTimeMillis();
  final List<Purchase> pending = DonationData.getInstance().getPurchases().values().stream().flatMap(List::stream).filter(p -> DonationReconciliation.shouldCheck(p, now)).toList();
  if (pending.isEmpty()) return;
  for (int i=0; i<Math.min(5,pending.size()); i++)
  {
   final Purchase p=pending.get(Math.floorMod(cursor++,pending.size()));
   if (!p.isBusy() && p.getPaymentMethod().isEnabled() && (p.getPaymentMethod().getId().equals("MP") || p.getPaymentId() != null || p.getPaypalInvoiceId() != null)) p.getPaymentHandler().checkPurchase(p);
  }
 }
 public static DonationAutomaticPaymentTask getInstance() { return Holder.INSTANCE; }
 private static class Holder { private static final DonationAutomaticPaymentTask INSTANCE=new DonationAutomaticPaymentTask(); }
}
