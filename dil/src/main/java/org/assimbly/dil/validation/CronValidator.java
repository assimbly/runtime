package org.assimbly.dil.validation;

import org.assimbly.util.error.ValidationErrorMessage;
import org.quartz.CronExpression;

public class CronValidator {

    private static final ValidationErrorMessage EMPTY_CRON_ERROR = new ValidationErrorMessage("Empty crons aren't allowed!");

    public ValidationErrorMessage validate(String cronExpression) {

        if (cronExpression.isEmpty())
            return EMPTY_CRON_ERROR;

        try {
            new CronExpression(cronExpression);
        } catch (Exception e) {
            return new ValidationErrorMessage("Cron Validation error: " + e.getMessage());
        }

        return null;
    }
}
