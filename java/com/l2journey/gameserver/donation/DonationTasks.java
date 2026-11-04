package com.l2journey.gameserver.donation;

import java.util.logging.Level;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import com.l2journey.commons.threads.ThreadPool;
/** Tracks periodic jobs and coalesces slow executions instead of queuing them repeatedly. */
public final class DonationTasks
{
 private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(DonationTasks.class.getName());
 private static final List<ScheduledFuture<?>> TASKS = new CopyOnWriteArrayList<>();
 public static void schedule(Runnable job, long initial, long interval)
 {
  final AtomicBoolean running = new AtomicBoolean();
  TASKS.add(ThreadPool.scheduleAtFixedRate(() ->
  {
   if (!running.compareAndSet(false,true)) return;
   try { DonationExecutor.execute(() -> { try { job.run(); } catch (RuntimeException e) { LOGGER.log(Level.WARNING, "DonationTasks: Donation scheduled job failed", e); } finally { running.set(false); } }); }
   catch (RuntimeException e) { running.set(false); }
  },initial,interval));
 }
 public static void shutdown() { for (var task : TASKS) task.cancel(false); TASKS.clear(); DonationExecutor.shutdown(); }
}
