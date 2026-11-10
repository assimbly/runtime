package org.assimbly.dil.listener;

import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
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

        Instant now = Instant.now();
        Instant lateCutoff = now.minusMillis(misfireThresholdMs);
        Set<TriggerKey> keys = scheduler.getTriggerKeys(GroupMatcher.anyTriggerGroup());

        for (TriggerKey key : keys) {
            inspectTrigger(key, lateCutoff);
        }

        TriggerMisfireLog.pruneOlderThan(now.minusMillis(misfireThresholdMs * 10));
    }

    private void inspectTrigger(TriggerKey key, Instant lateCutoff) throws Exception {
        Trigger.TriggerState state = scheduler.getTriggerState(key);
        if (state != Trigger.TriggerState.NORMAL && state != Trigger.TriggerState.BLOCKED) {
            return;
        }

        Trigger trigger = scheduler.getTrigger(key);
        if (trigger == null) {
            return;
        }

        var nextFireTime = trigger.getNextFireTime();
        if (nextFireTime == null) {
            TriggerMisfireLog.clearTrigger(key);
            return;
        }
        if (nextFireTime.toInstant().isAfter(lateCutoff)) {
            // On schedule again — reset only if the job actually ran after the last miss.
            TriggerMisfireLog.clearIfRecovered(trigger);
            return;
        }

        logMissedFires(trigger, key, lateCutoff);
    }

    private void logMissedFires(Trigger trigger, TriggerKey key, Instant lateCutoff) {
        // Quartz's getFireTimeAfter only accepts java.util.Date, so keep the Quartz value alongside the Instant.
        var missedDate = trigger.getNextFireTime();
        Instant missed = missedDate.toInstant();
        int count = 0;
        while (!missed.isAfter(lateCutoff) && count < MAX_MISSES_PER_TRIGGER) {
            TriggerMisfireLog.logIfNew(trigger, missed);
            var afterDate = trigger.getFireTimeAfter(missedDate);
            if (afterDate == null || !afterDate.toInstant().isAfter(missed)) {
                break;
            }
            missedDate = afterDate;
            missed = afterDate.toInstant();
            count++;
        }

        if (count >= MAX_MISSES_PER_TRIGGER && !missed.isAfter(lateCutoff)) {
            log.warn("Trigger {}/{} has additional missed fires beyond the log limit of {}",
                    key.getGroup(), key.getName(), MAX_MISSES_PER_TRIGGER);
        }
    }
}
