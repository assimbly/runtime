package org.assimbly.util.mail;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultHeaderFilterStrategy;

public class ExtendedHeaderFilterStrategy extends DefaultHeaderFilterStrategy {

    @Override
    protected boolean extendedFilter(Direction direction, String key, Object value, Exchange exchange) {
        if (direction == Direction.IN) {
            return false;
        }

        return exchange.getContext().getTypeConverter().tryConvertTo(String.class, value).contains("\n");
    }
}
