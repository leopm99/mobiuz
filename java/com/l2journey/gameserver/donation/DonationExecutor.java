package com.l2journey.gameserver.donation;

import java.util.logging.Level;
import java.util.concurrent.*;
import com.l2journey.gameserver.donation.purchase.Purchase;
import com.l2journey.gameserver.donation.purchase.PurchaseStatus;
/** Network operations never occupy packet threads or the shared game executor. */
public final class DonationExecutor
{
 private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(DonationExecutor.class.getName());
 private static final ThreadPoolExecutor WORKERS = new ThreadPoolExecutor(2,2,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(128),r -> { Thread t=new Thread(r,"DonationNetwork"); t.setDaemon(true); return t; });
 public static void execute(Runnable action) { WORKERS.execute(action); }
 public static void submit(Purchase p, Runnable action, boolean creating)
 {
  if (!p.tryStart()) return;
  try
  {
   execute(() ->
   {
    try
    {
     if (creating && ((p.getStatus() != PurchaseStatus.CREATED && p.getStatus() != PurchaseStatus.PENDING) || !p.agreedTerms())) return;
     if (creating)
     {
      if (p.timeExpired()) return;
      if (p.wasRequested() && p.getPaymentMethod() != com.l2journey.gameserver.donation.purchase.PaymentMethod.MP_PIX)
      { p.setMessage("<br>This request requires payment reconciliation. Please contact a GM before creating another payment."); return; }
      p.markRequested();
     }
     action.run();
    }
    catch (RuntimeException e) { LOGGER.log(Level.WARNING, "DonationExecutor: Donation operation failed for purchase " + p.getId(), e); }
    finally { p.setBusy(false); }
   });
  }
  catch (RuntimeException e) { p.setBusy(false); throw e; }
 }
 public static void shutdown() { WORKERS.shutdown(); }
}
