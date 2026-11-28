package com.suriya.expensetracker;
public final class SalaryPatternTest {
 public static void main(String[] args){String a=SalaryPattern.signature("VM-BANKXX-S","Rs. 10000.00 credited to account XX1234 on 25-11-2026 ref 111111111111");String b=SalaryPattern.signature("BZ-BANKXX-S","Rs. 10000.00 credited to account XX1234 on 25-12-2026 ref 222222222222");String different=SalaryPattern.signature("VM-BANKXX-S","Rs. 10000.00 credited to account XX5678 on 25-12-2026 ref 222222222222");if(!a.equals(b)||a.equals(different))throw new AssertionError("Message patterns differ unexpectedly");System.out.println("Salary SMS pattern checks passed");}
}
