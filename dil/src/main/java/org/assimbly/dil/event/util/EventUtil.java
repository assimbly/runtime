package org.assimbly.dil.event.util;

import org.assimbly.dil.event.domain.Filter;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

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

    public static String getCreatedTimestamp(){
        return getCreatedTimestamp(Instant.now());
    }

    public static String getCreatedTimestamp(Instant time){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnZ");
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(time, ZoneOffset.UTC);
        return formatter.format(zonedDateTime);
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

    public static long calcMapLength(Map<String, Object> map) {
        long totalLength = 0;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            totalLength += key.length();
            if (value != null) {
                totalLength += value.toString().length();
            }
        }

        return totalLength;
    }

}