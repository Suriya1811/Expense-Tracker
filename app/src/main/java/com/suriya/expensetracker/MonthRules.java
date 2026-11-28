package com.suriya.expensetracker;

import java.util.Calendar;
import java.util.TreeMap;
import java.util.TreeSet;

/** Helpers for showing the current calendar month alongside recorded history. */
final class MonthRules {
    static void includeCurrent(TreeMap<Integer,TreeSet<Integer>> months,Calendar now){
        int year=now.get(Calendar.YEAR),month=now.get(Calendar.MONTH);
        if(!months.containsKey(year))months.put(year,new TreeSet<Integer>());
        months.get(year).add(month);
    }
    static boolean isCurrent(Calendar selected,Calendar now){
        return selected.get(Calendar.YEAR)==now.get(Calendar.YEAR)&&selected.get(Calendar.MONTH)==now.get(Calendar.MONTH);
    }
    static boolean changed(Calendar previous,Calendar now){return !isCurrent(previous,now);}
}
