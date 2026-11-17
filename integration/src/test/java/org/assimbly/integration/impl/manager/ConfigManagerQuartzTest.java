package org.assimbly.integration.impl.manager;

import org.apache.camel.component.quartz.QuartzComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigManagerQuartzTest {

    @Test
    void defaultsWhenEnvIsEmpty() {
        Map<String, String> properties = ConfigManager.quartzProperties(Map.of());

        assertEquals("10", properties.get("org.quartz.threadPool.threadCount"));
        assertEquals("60000", properties.get("org.quartz.jobStore.misfireThreshold"));
    }

    @Test
    void envOverridesDefaults() {
        Map<String, String> properties = ConfigManager.quartzProperties(Map.of(
                "QUARTZ_THREAD_COUNT", "25",
                "QUARTZ_MISFIRE_THRESHOLD", "120000"));

        assertEquals("25", properties.get("org.quartz.threadPool.threadCount"));
        assertEquals("120000", properties.get("org.quartz.jobStore.misfireThreshold"));
    }

    @Test
    void quartzComponentStartsSchedulerWithConfiguredThreadCount() throws Exception {
        try (DefaultCamelContext context = new DefaultCamelContext()) {
            QuartzComponent quartz = context.getComponent("quartz", QuartzComponent.class);
            quartz.setProperties(ConfigManager.quartzProperties(Map.of("QUARTZ_THREAD_COUNT", "7")));

            context.start();

            Scheduler scheduler = quartz.getScheduler();
            assertEquals(7, scheduler.getMetaData().getThreadPoolSize());
        }
    }

}
