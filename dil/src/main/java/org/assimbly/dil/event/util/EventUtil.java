package org.assimbly.dil.event.util;

import com.google.common.collect.FluentIterable;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.event.domain.Filter;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class EventUtil {

    public static boolean isFiltered(final List<Filter> filters, final String text){
        return filters.stream().anyMatch(o -> text.contains(o.getFilter()));
    }

    public static boolean isFilteredEquals(final List<Filter> filters, final String text){
        return filters.stream().anyMatch(o -> text.equals(o.getFilter()));
    }

    public static String getTimestamp(){

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC());

        return formatter.format(now);

    }

    public static String getExpiryTimestamp(String expiryInHours){

        long hours = 1;
        if(expiryInHours!=null){
            hours = Long.parseLong(expiryInHours);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC()).plusHours(hours);

        return formatter.format(now);

    }

}