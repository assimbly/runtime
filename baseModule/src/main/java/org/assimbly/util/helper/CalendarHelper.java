package org.assimbly.util.helper;

import java.util.Calendar;

public class CalendarHelper {

    private static String[] months = { "January", "February", "March", "April", "May", "June", "July",
            "August", "September", "October", "November", "December" };

    public static String getCurrentMonth(){
        int currentIndex = Calendar.getInstance().get(Calendar.MONTH);
        return getMonthString(currentIndex);
    }

    public static String getMonthString(int monthIndex){
        return months[monthIndex];
    }
}
