package org.assimbly.dil.event.collect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.NamedNode;
import org.apache.camel.Route;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicy;
import org.apache.camel.component.micrometer.routepolicy.MicrometerRoutePolicyFactory;
import org.apache.camel.spi.RoutePolicy;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom MicrometerRoutePolicyFactory that tracks last exchange failure and completion timestamps
 */
public class MicrometerTimestampRoutePolicyFactory extends MicrometerRoutePolicyFactory {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, AtomicLong> lastFailureTimestamps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> lastCompletionTimestamps = new ConcurrentHashMap<>();

    public MicrometerTimestampRoutePolicyFactory(MeterRegistry meterRegistry) {
        super();
        this.meterRegistry = meterRegistry;
    }

    @Override
    public RoutePolicy createRoutePolicy(CamelContext camelContext, String routeId, NamedNode routeDefinition) {
        // Create the default micrometer route policy
        MicrometerRoutePolicy basePolicy = (MicrometerRoutePolicy) super.createRoutePolicy(camelContext, routeId, routeDefinition);

        // Initialize timestamp holders for this route
        AtomicLong lastFailureTimestamp = new AtomicLong(0L);
        AtomicLong lastCompletionTimestamp = new AtomicLong(0L);

        lastFailureTimestamps.put(routeId, lastFailureTimestamp);
        lastCompletionTimestamps.put(routeId, lastCompletionTimestamp);

        // Register gauges for the timestamps
        Tags routeTags = Tags.of("routeId", routeId,"kind","CamelRoute");

        meterRegistry.gauge("camel.exchanges.lastfailure.timestamp",
                routeTags,
                lastFailureTimestamp,
                AtomicLong::get);

        meterRegistry.gauge("camel.exchanges.lastcompleted.timestamp",
                routeTags,
                lastCompletionTimestamp,
                AtomicLong::get);

        // Return a custom route policy that wraps the base policy
        return new TimestampTrackingRoutePolicy(basePolicy, routeId, lastFailureTimestamp, lastCompletionTimestamp);
    }

    /**
     * Get the last exchange failure timestamp for a specific route
     */
    public long getLastExchangeFailureTimestamp(String routeId) {
        AtomicLong timestamp = lastFailureTimestamps.get(routeId);
        return timestamp != null ? timestamp.get() : 0L;
    }

    /**
     * Get the last exchange completion timestamp for a specific route
     */
    public long getLastExchangeCompletedTimestamp(String routeId) {
        AtomicLong timestamp = lastCompletionTimestamps.get(routeId);
        return timestamp != null ? timestamp.get() : 0L;
    }

    /**
     * Custom RoutePolicy that tracks timestamps and delegates to the base MicrometerRoutePolicy
     */
    private static class TimestampTrackingRoutePolicy implements RoutePolicy {

        private final MicrometerRoutePolicy basePolicy;
        private final String routeId;
        private final AtomicLong lastFailureTimestamp;
        private final AtomicLong lastCompletionTimestamp;

        public TimestampTrackingRoutePolicy(MicrometerRoutePolicy basePolicy,
                                            String routeId,
                                            AtomicLong lastFailureTimestamp,
                                            AtomicLong lastCompletionTimestamp) {
            this.basePolicy = basePolicy;
            this.routeId = routeId;
            this.lastFailureTimestamp = lastFailureTimestamp;
            this.lastCompletionTimestamp = lastCompletionTimestamp;
        }

        @Override
        public void onInit(Route route) {
            basePolicy.onInit(route);
        }

        @Override
        public void onStart(Route route) {
            basePolicy.onStart(route);
        }

        @Override
        public void onStop(Route route) {
            basePolicy.onStop(route);
        }

        @Override
        public void onSuspend(Route route) {
            basePolicy.onSuspend(route);
        }

        @Override
        public void onResume(Route route) {
            basePolicy.onResume(route);
        }

        @Override
        public void onRemove(Route route) {
            basePolicy.onRemove(route);
        }

        @Override
        public void onExchangeBegin(Route route, Exchange exchange) {
            basePolicy.onExchangeBegin(route, exchange);
        }

        @Override
        public void onExchangeDone(Route route, Exchange exchange) {
            basePolicy.onExchangeDone(route, exchange);

            long currentTime = Instant.now().toEpochMilli();

            // Check if exchange failed
            if (exchange.isFailed() || exchange.getException() != null) {
                lastFailureTimestamp.set(currentTime);
            } else {
                // Exchange completed successfully
                lastCompletionTimestamp.set(currentTime);
            }
        }
    }
}