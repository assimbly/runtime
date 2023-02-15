package org.assimbly.dil.event.util;

import org.assimbly.dil.event.domain.Filter;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EventUtil {

    public static boolean isFiltered(final List<Filter> filters, final String text){
        return filters.stream().filter(o -> text.matches(o.getFilter())).findFirst().isPresent();
    }

    public static String getTimestamp(){

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC());

        return formatter.format(now);

    }


}