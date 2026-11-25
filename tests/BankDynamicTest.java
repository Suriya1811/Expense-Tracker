package com.suriya.expensetracker;
public class BankDynamicTest {
 public static void main(String[] args){
  check("IOB",BankNames.fromSms("BZ-IOBCHN-S","Paid Rs 50"));
  check("Kotak",BankNames.fromSms("VM-KOTAKB-S","Received Rs 20"));
  check("Hdfc Bank",BankNames.fromSms("VM-HDFCBK-S","HDFC Bank account debited Rs 20"));
  check("",BankNames.fromSms("VM-OTHERX-S","HDFC Bank account debited Rs 20"));
  check("",BankNames.fromSms("+919999999999","HDFC Bank account debited Rs 20"));
  check("",BankNames.fromSms("VM-OTHERX-S","your bank account debited Rs 20"));
  System.out.println("Dynamic sender recognition checks passed");
 }
 static void check(String a,String b){if(!a.equals(b))throw new AssertionError(a+" != "+b);}
}
