package org.assimbly.dil.listener;

import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Misfire logging with dedupe so the same missed fire time is not reported twice,
 * and a per-trigger consecutive counter that resets only after a real execution.
 */
final class TriggerMisfireLog {
    private static final Logger log = LoggerFactory.getLogger(TriggerMisfireLog.class);
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();
    private static final Map<String, AtomicInteger> COUNTERS = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_MISSED_MILLIS = new ConcurrentHashMap<>();

    private TriggerMisfireLog() {
    }

    static void logIfNew(Trigger trigger, Date missedFireTime) {
        if (trigger == null || missedFireTime == null) {
            return;
        }
        TriggerKey key = trigger.getKey();
        String triggerId = triggerId(key);
        String dedupeKey = triggerId + "@" + missedFireTime.getTime();
        if (!LOGGED.add(dedupeKey)) {
            return;
        }
        int occurrence = COUNTERS.computeIfAbsent(triggerId, id -> new AtomicInteger()).incrementAndGet();
        LAST_MISSED_MILLIS.merge(triggerId, missedFireTime.getTime(), Math::max);
        log.warn("""
                > Trigger misfire detected
                  - Group: {}
                  - Timer: {}
                  - Missed Fire Time: {}
                  - Occurrence: {}
                """,
                key.getGroup(),
                key.getName(),
                missedFireTime,
                occurrence);
    }

    /**
     * Reset streak only when Quartz has actually executed the job at or after the
     * last missed fire time. Advancing {@code nextFireTime} alone is not enough —
     * misfire recovery does that without running the job, which previously kept
     * Occurrence stuck at 1.
     */
    static void clearIfRecovered(Trigger trigger) {
        if (trigger == null) {
            return;
        }
        TriggerKey key = trigger.getKey();
        String triggerId = triggerId(key);
        Long lastMissed = LAST_MISSED_MILLIS.get(triggerId);
        if (lastMissed == null) {
            return;
        }
        Date previousFireTime = trigger.getPreviousFireTime();
        if (previousFireTime != null && previousFireTime.getTime() >= lastMissed) {
            clearTrigger(key);
        }
    }

    static void clearTrigger(TriggerKey key) {
        if (key == null) {
            return;
        }
        String triggerId = triggerId(key);
        COUNTERS.remove(triggerId);
        LAST_MISSED_MILLIS.remove(triggerId);
        String prefix = triggerId + "@";
        LOGGED.removeIf(entry -> entry.startsWith(prefix));
    }

    /**
     * Drop dedupe entries older than {@code olderThan} so the set does not grow forever
     * for triggers that disappear without a recovery clear. Active streaks are kept.
     */
    static void pruneOlderThan(Date olderThan) {
        if (olderThan == null) {
            return;
        }
        long cutoff = olderThan.getTime();
        Iterator<String> it = LOGGED.iterator();
        while (it.hasNext()) {
            String entry = it.next();
            int at = entry.lastIndexOf('@');
            if (at < 0) {
                it.remove();
                continue;
            }
            String triggerId = entry.substring(0, at);
            if (COUNTERS.containsKey(triggerId)) {
                continue;
            }
            try {
                long fireTime = Long.parseLong(entry.substring(at + 1));
                if (fireTime < cutoff) {
                    it.remove();
                }
            } catch (NumberFormatException e) {
                it.remove();
            }
        }
    }

    private static String triggerId(TriggerKey key) {
        return key.getGroup() + "/" + key.getName();
    }
}
