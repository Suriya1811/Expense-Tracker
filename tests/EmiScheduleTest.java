package com.suriya.expensetracker;
import java.util.*;
public final class EmiScheduleTest {
    static long date(int year,int month,int day){Calendar c=Calendar.getInstance();c.clear();c.set(year,month-1,day,12,0,0);return c.getTimeInMillis();}
    static void equal(String label,long actual,long expected){if(actual!=expected)throw new AssertionError(label+": "+new Date(actual)+" != "+new Date(expected));}
    public static void main(String[] args){
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        long start=date(2026,1,31);
        equal("first",EmiSchedule.due(start,0),date(2026,1,31));
        equal("short February",EmiSchedule.due(start,1),date(2026,2,28));
        equal("returns to 31",EmiSchedule.due(start,2),date(2026,3,31));
        equal("leap year",EmiSchedule.due(date(2028,1,31),1),date(2028,2,29));
        equal("year change",EmiSchedule.due(date(2026,12,5),1),date(2027,1,5));
        equal("remind two days before",EmiSchedule.reminderStart(date(2026,12,5)),EmiSchedule.day(date(2026,12,3)));
        equal("remind across months",EmiSchedule.reminderStart(date(2027,1,1)),EmiSchedule.day(date(2026,12,30)));
        System.out.println("EMI calendar schedule and reminder dates: 7 passed");
    }
}
