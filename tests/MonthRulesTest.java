package com.suriya.expensetracker;
import java.util.*;
public final class MonthRulesTest {
    static Calendar month(int y,int m){Calendar c=Calendar.getInstance();c.clear();c.set(y,m-1,1);return c;}
    public static void main(String[] args){TreeMap<Integer,TreeSet<Integer>> history=new TreeMap<>();history.put(2026,new TreeSet<>(Arrays.asList(Calendar.NOVEMBER)));Calendar december=month(2026,12);MonthRules.includeCurrent(history,december);if(!history.get(2026).contains(Calendar.DECEMBER)||!history.get(2026).contains(Calendar.NOVEMBER))throw new AssertionError("Include empty current month and retain history");Calendar january=month(2027,1);if(!MonthRules.changed(december,january)||MonthRules.changed(january,month(2027,1)))throw new AssertionError("Calendar rollover");MonthRules.includeCurrent(history,january);if(!history.get(2027).contains(Calendar.JANUARY))throw new AssertionError("Year rollover");System.out.println("Calendar month checks passed");}
}
