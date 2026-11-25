package com.suriya.expensetracker;
public class BankNamesTest {
 public static void main(String[] args){
  String[][] cases={{"BZ-IOBCHN-S","IOB"},{"VA-IOBCHN-S","IOB"},{"BT-IOBCHN-S","IOB"},{"VM-KOTAKB-S","Kotak"},{"JK-KOTAKD-S","Kotak"},{"JX-KOTAKD-S","Kotak"},{"JM-KOTAKB-S","Kotak"},{"AD-IOBCHN-T","IOB"},{"evilIOBCHN",""},{"+919999999999",""},{"KOTAKX",""},{"",""}};
  for(String[] c:cases)if(!BankNames.fromSender(c[0]).equals(c[1]))throw new AssertionError(c[0]);
  if(!BankNames.fromSender(null).isEmpty()||!BankNames.value(0).isEmpty())throw new AssertionError();
  System.out.println("Bank sender and unassigned tests passed");
 }
}
