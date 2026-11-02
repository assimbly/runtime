package org.assimbly.dil.listener;

import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Polls Quartz triggers on its own thread so misfires are logged in near real time,
 * even when the Quartz worker pool is exhausted.
 */
public class TriggerMisfireWatchdog {
    private static final Logger log = LoggerFactory.getLogger(TriggerMisfireWatchdog.class);
    private static final int MAX_MISSES_PER_TRIGGER = 100;

    private final Scheduler scheduler;
    private final long misfireThresholdMs;
    private final long intervalMs;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService executor;

    public TriggerMisfireWatchdog(Scheduler scheduler, long misfireThresholdMs, long intervalMs) {
        this.scheduler = scheduler;
        this.misfireThresholdMs = Math.max(1L, misfireThresholdMs);
        this.intervalMs = Math.max(1L, intervalMs);
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "quartz-misfire-watchdog");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleWithFixedDelay(this::safeCheck, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        log.info("Quartz misfire watchdog started (interval={}ms, threshold={}ms)", intervalMs, misfireThresholdMs);
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        log.info("Quartz misfire watchdog stopped");
    }

    private void safeCheck() {
        try {
            check();
        } catch (Exception e) {
            log.debug("Quartz misfire watchdog check failed: {}", e.toString());
        }
    }

    void check() throws Exception {
        if (scheduler == null || !scheduler.isStarted() || scheduler.isShutdown()) {
            return;
        }

        Date now = new Date();
        Date lateCutoff = new Date(now.getTime() - misfireThresholdMs);
        Set<TriggerKey> keys = scheduler.getTriggerKeys(GroupMatcher.anyTriggerGroup());

        for (TriggerKey key : keys) {
            inspectTrigger(key, lateCutoff);
        }

        TriggerMisfireLog.pruneOlderThan(new Date(now.getTime() - (misfireThresholdMs * 10)));
    }

    private void inspectTrigger(TriggerKey key, Date lateCutoff) throws Exception {
        Trigger.TriggerState state = scheduler.getTriggerState(key);
        if (state != Trigger.TriggerState.NORMAL && state != Trigger.TriggerState.BLOCKED) {
            return;
        }

        Trigger trigger = scheduler.getTrigger(key);
        if (trigger == null) {
            return;
        }

        Date nextFireTime = trigger.getNextFireTime();
        if (nextFireTime == null) {
            TriggerMisfireLog.clearTrigger(key);
            return;
        }
        if (nextFireTime.after(lateCutoff)) {
            // On schedule again — reset only if the job actually ran after the last miss.
            TriggerMisfireLog.clearIfRecovered(trigger);
            return;
        }

        logMissedFires(trigger, key, nextFireTime, lateCutoff);
    }

    private void logMissedFires(Trigger trigger, TriggerKey key, Date nextFireTime, Date lateCutoff) {
        Date missed = nextFireTime;
        int count = 0;
        while (missed != null && !missed.after(lateCutoff) && count < MAX_MISSES_PER_TRIGGER) {
            TriggerMisfireLog.logIfNew(trigger, missed);
            Date after = trigger.getFireTimeAfter(missed);
            if (after == null || !after.after(missed)) {
                break;
            }
            missed = after;
            count++;
        }

        if (count >= MAX_MISSES_PER_TRIGGER && missed != null && !missed.after(lateCutoff)) {
            log.warn("Trigger {}/{} has additional missed fires beyond the log limit of {}",
                    key.getGroup(), key.getName(), MAX_MISSES_PER_TRIGGER);
        }
    }
}
