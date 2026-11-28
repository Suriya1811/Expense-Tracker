package com.suriya.expensetracker;

import java.util.Set;

/** Resolves the most recent salary preference without changing earlier months. */
final class SalaryRules {
    static final String MODE_PREFIX = "salary_mode_";
    static String mostRecentPeriod(Set<String> keys, String month) {
        if (month == null || !month.matches("[0-9]{4}-(0[1-9]|1[0-2])"))
            throw new IllegalArgumentException("Invalid salary month");
        String result = null;
        for (String key : keys) {
            if (!key.startsWith(MODE_PREFIX)) continue;
            String period = key.substring(MODE_PREFIX.length());
            if (!period.matches("[0-9]{4}-(0[1-9]|1[0-2])") || period.compareTo(month) > 0) continue;
            if (result == null || period.compareTo(result) > 0) result = period;
        }
        return result;
    }
}
