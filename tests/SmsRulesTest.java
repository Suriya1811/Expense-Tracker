package com.suriya.expensetracker;
public class SmsRulesTest {
 static void accepted(String sender,String message,long amount,String kind){SmsRules.Result r=SmsRules.parse(sender,message);if(r==null||r.amount!=amount||!kind.equals(r.kind))throw new AssertionError("Expected "+kind+" "+amount+" for "+sender);}
 static void rejected(String sender,String message){if(SmsRules.parse(sender,message)!=null)throw new AssertionError("Unexpected transaction for "+sender);}
 public static void main(String[] args){
  String kotak="Sent Rs.22.00 from Kotak Bank A/c X1234 to Demo Person on 25-11-26. UPI Ref 123456789012. Not done by you? Tap https://kotak.bank.in/KBANKT/Fraud";
  accepted("VM-KOTAKB-S",kotak,2200,"Expense");
  if(!"Demo Person".equals(PayeeParser.extract(kotak)))throw new AssertionError("Kotak payee");
  accepted("BZ-IOBCHN-S","Your a/c XXXXX99 debited for payee Demo Person for Rs. 22.00 on 2026-11-23, ref 123456789012.If not you, report to your bank immediately-IOB.",2200,"Expense");
  accepted("VM-KOTAKB-S","Received Rs.100.09 in your Kotak Bank AC 1234 from A PERSON on 25-11-26.UPI Ref:123456789012",10009,"Income");
  rejected("VM-atTIRA-S","Up to Rs.1,100 off coupon received in your Tira account! Your code is DEALRUSH");
  rejected("VM-KOTAKB-S","Up to Rs.1,100 off coupon received in your bank account!");
  rejected("VM-KOTAKB-S","Your OTP is 123456 for Rs.22.00 debited from account");
  rejected("VM-KOTAKB-S","Rs.22.00 will be debited from your account");
  rejected("VM-KOTAKB-S","Transaction for Rs.22.00 from your account failed");
  rejected("VM-KOTAKB-S","Your account statement is ready and loan application received Rs.22.00");
  rejected("VM-SPOOFX-S",kotak);
  accepted("VM-HDFCBK-S","Rs.100.00 debited from your HDFC Bank account XX1234 on 25-11-26",10000,"Expense");
  accepted("VM-SBIINB-S","Your SBI account XX1234 credited with INR 300.00",30000,"Income");
  accepted("VM-ACMEBK-S","Your Acme Bank account XX1234 debited for Rs. 44.00",4400,"Expense");
  rejected("VM-ACMEBK-S","Your Acme Bank account received coupon of Rs. 100.00");
  rejected("VM-ACMEBK-S","Your Acme Bank account credited INR 50.00 after failed transaction");
  System.out.println("Bank SMS recognition checks passed");
 }
}
