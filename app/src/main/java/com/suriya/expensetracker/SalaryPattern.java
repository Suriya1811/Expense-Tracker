package com.suriya.expensetracker;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
/** Hash a conservative SMS pattern after removing dates and long reference numbers. */
final class SalaryPattern {
    static String signature(String sender,String body){
        if(sender==null||body==null)return "";
        String address=sender.trim().toUpperCase(Locale.ROOT).replaceFirst("^[A-Z]{2}-","").replaceFirst("-[A-Z]$","");
        String text=body.toLowerCase(Locale.ROOT).replaceAll("\\b\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}\\b","<date>")
            .replaceAll("\\b\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}\\b","<date>")
            .replaceAll("\\b\\d{6,}\\b","<reference>").replaceAll("\\s+"," ").trim();
        try{byte[] digest=MessageDigest.getInstance("SHA-256").digest((address+"|"+text).getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:digest)out.append(String.format(Locale.US,"%02x",b&255));return out.toString();}catch(Exception e){throw new IllegalStateException(e);}
    }
}
