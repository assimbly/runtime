package org.assimbly.dil.listener;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TriggerMisfireLoggingListener implements TriggerListener {
    private static final Logger log = LoggerFactory.getLogger(TriggerMisfireLoggingListener.class);

    @Override
    public String getName() {
        return "TriggerMisfireLoggingListener";
    }

    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext context) {
        // do nothing
    }

    @Override
    public void triggerMisfired(Trigger trigger) {
        log.warn("""
                > Trigger misfire detected
                  - Group: {}
                  - Timer: {}
                  - Next Fire Time: {}
                """,
                trigger.getKey().getGroup(),
                trigger.getKey().getName(),
                trigger.getNextFireTime());
    }

    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext jobExecutionContext, Trigger.CompletedExecutionInstruction completedExecutionInstruction) {
        // do nothing
    }

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        return false;
    }
}