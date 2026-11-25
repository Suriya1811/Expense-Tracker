package com.suriya.expensetracker;
import java.util.Calendar;

/** Calendar-month repayment dates; end-of-month dates are clamped without drifting. */
final class EmiSchedule {
    static long due(long startMillis,int installment){
        Calendar start=Calendar.getInstance();start.setTimeInMillis(startMillis);int originalDay=start.get(Calendar.DAY_OF_MONTH);
        Calendar result=(Calendar)start.clone();result.set(Calendar.DAY_OF_MONTH,1);result.add(Calendar.MONTH,installment);
        result.set(Calendar.DAY_OF_MONTH,Math.min(originalDay,result.getActualMaximum(Calendar.DAY_OF_MONTH)));
        result.set(Calendar.HOUR_OF_DAY,12);result.set(Calendar.MINUTE,0);result.set(Calendar.SECOND,0);result.set(Calendar.MILLISECOND,0);return result.getTimeInMillis();
    }
    static long day(long time){Calendar c=Calendar.getInstance();c.setTimeInMillis(time);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTimeInMillis();}
    static long reminderStart(long due){Calendar c=Calendar.getInstance();c.setTimeInMillis(day(due));c.add(Calendar.DAY_OF_MONTH,-2);return c.getTimeInMillis();}
}
