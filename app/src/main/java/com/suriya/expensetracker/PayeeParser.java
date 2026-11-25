package com.suriya.expensetracker;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts the payee only from the explicitly supported bank SMS format. */
public final class PayeeParser {
    private static final Pattern PAYEE = Pattern.compile("(?is)\\bdebited\\s+for\\s+payee\\s+(.{1,100}?)\\s+for\\s+(?:rs\\.?|inr|₹)\\s*[0-9]");
    private static final Pattern KOTAK_PAYEE = Pattern.compile("(?is)^\\s*sent\\s+(?:rs\\.?|inr|₹)\\s*[0-9,.]+\\s+from\\s+kotak\\s+bank\\s+a\\s*/\\s*c\\s+\\S+\\s+to\\s+(.{1,80}?)\\s+on\\s+\\d{2}-\\d{2}-\\d{2,4}\\b");
    private PayeeParser() {}
    public static String extract(String message) {
        if (message == null) return null;
        Matcher m = PAYEE.matcher(message);
        if (!m.find()) {m=KOTAK_PAYEE.matcher(message);if(!m.find())return null;}
        String name = m.group(1).replaceAll("\\s+", " ").trim();
        return name.isEmpty() || name.length()>80 ? null : name;
    }
    public static String normalize(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
