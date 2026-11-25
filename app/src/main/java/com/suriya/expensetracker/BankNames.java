package com.suriya.expensetracker;
import java.util.Locale;
import java.util.regex.Pattern;
/** Sender and message based bank labels. SMS sender IDs are not authentication. */
final class BankNames {
    static final String[] CHOICES={"Unassigned","IOB","Kotak"};
    private static final Pattern IOB=Pattern.compile("^(?:[A-Z]{2}-)?IOBCHN(?:-[A-Z])?$");
    private static final Pattern KOTAK=Pattern.compile("^(?:[A-Z]{2}-)?KOTAK[BD](?:-[A-Z])?$");
    static String fromSender(String sender){if(sender==null)return "";String s=sender.trim().toUpperCase(Locale.ROOT);if(IOB.matcher(s).matches())return "IOB";if(KOTAK.matcher(s).matches())return "Kotak";
        if(!s.matches("(?:[A-Z]{2}-)?[A-Z]{6}(?:-[A-Z])?"))return "";
        String id=s.replaceFirst("^[A-Z]{2}-","").replaceFirst("-[A-Z]$","");
        if(id.equals("HDFCBK"))return "Hdfc Bank";if(id.equals("SBIINB")||id.equals("SBIBNK"))return "SBI";if(id.equals("ICICIB"))return "ICICI Bank";if(id.equals("AXISBK"))return "Axis Bank";if(id.equals("CANBNK"))return "Canara Bank";return "";}
    // For other banks, use the name only when it appears in the message and agrees with the sender ID.
    static String fromSms(String sender,String body){
        String known=fromSender(sender);if(!known.isEmpty())return known;
        if(sender==null||body==null)return "";String id=sender.trim().toUpperCase(Locale.ROOT).replaceFirst("^[A-Z]{2}-","").replaceFirst("-[A-Z]$","");
        if(!id.matches("[A-Z]{6}"))return "";
        java.util.regex.Matcher m=Pattern.compile("(?i)\\b([a-z]{3,20})\\s+bank\\b").matcher(body);
        while(m.find()){String word=m.group(1),upper=word.toUpperCase(Locale.ROOT);if(id.startsWith(upper)){return word.substring(0,1).toUpperCase(Locale.ROOT)+word.substring(1).toLowerCase(Locale.ROOT)+" Bank";}}
        return "";
    }
    static int choice(String bank){return "IOB".equals(bank)?1:"Kotak".equals(bank)?2:0;}
    static String value(int index){return index==1?"IOB":index==2?"Kotak":"";}
}
