package com.suriya.expensetracker;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.math.BigDecimal;

/** Conservative recognition of bank transaction alerts, never proof of sender authenticity. */
final class SmsRules {
    private static final Pattern NON_TRANSACTION=Pattern.compile("(?i)\\b(?:loan\\s+application|account\\s+statement|coupon|voucher|discount|promo(?:tion|tional)?|offer|reward\\s+points?|otp|verification\\s+code|one.time.password|unsuccessful|failed|declined|revers(?:ed|al)|not\\s+(?:debited|credited)|will\\s+be\\s+(?:debited|credited)|payment\\s+due|due\\s+on)\\b");
    private static final Pattern ACCOUNT=Pattern.compile("(?i)(?:\\ba\\s*/\\s*c\\b|\\bac(?:count|ct)?\\b)");
    private static final Pattern KOTAK_SENT=Pattern.compile("(?i)^\\s*sent\\s+(?:₹|rs\\.?|inr)\\s*[0-9]");
    private SmsRules(){}
    static boolean accepts(String sender,String body){
        if(body==null||body.length()>2000||BankNames.fromSms(sender,body).isEmpty())return false;
        return !NON_TRANSACTION.matcher(body).find()&&ACCOUNT.matcher(body).find();
    }
    static final class Result {final long amount;final String kind;Result(long amount,String kind){this.amount=amount;this.kind=kind;}}
    static Result parse(String sender,String body){
        if(!accepts(sender,body))return null;String lower=body.toLowerCase(Locale.ROOT);
        boolean debit=Pattern.compile("\\b(debited|debit|spent|paid|purchase|withdrawn)\\b").matcher(lower).find()||bankSent(sender,body);
        boolean credit=Pattern.compile("\\b(credited|credit|received|deposited|refund(?:ed)?)\\b").matcher(lower).find();if(debit==credit)return null;
        Matcher m=Pattern.compile("(?i)(?:₹|\\b(?:rs\\.?|inr)\\s*)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)").matcher(body);long amount=0;int count=0;
        while(m.find()){String before=lower.substring(Math.max(0,m.start()-32),m.start());if(before.matches("(?s).*(?:bal(?:ance)?|available|avl)\\s*[:=-]?\\s*$"))continue;try{long n=new BigDecimal(m.group(1).replace(",","")).movePointRight(2).longValueExact();if(n>0&&n<100000000000L){amount=n;count++;}}catch(Exception ignored){}}
        return count==1?new Result(amount,credit?"Income":"Expense"):null;
    }
    static boolean bankSent(String sender,String body){return body!=null&&!BankNames.fromSms(sender,body).isEmpty()&&KOTAK_SENT.matcher(body).find()&&ACCOUNT.matcher(body).find();}
}
