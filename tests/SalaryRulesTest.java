package com.suriya.expensetracker;
import java.util.*;
public class SalaryRulesTest {
    static void expect(String expected,String actual){if(!Objects.equals(expected,actual))throw new AssertionError(expected+" != "+actual);}
    public static void main(String[] args){Set<String> keys=new HashSet<>(Arrays.asList("salary_mode_2026-11","salary_mode_2027-01","salary_mode_invalid","salary_mode_2026-00"));expect(null,SalaryRules.mostRecentPeriod(keys,"2026-10"));expect("2026-11",SalaryRules.mostRecentPeriod(keys,"2026-11"));expect("2026-11",SalaryRules.mostRecentPeriod(keys,"2026-12"));expect("2027-01",SalaryRules.mostRecentPeriod(keys,"2027-02"));System.out.println("Salary preference inheritance checks passed");}
}
