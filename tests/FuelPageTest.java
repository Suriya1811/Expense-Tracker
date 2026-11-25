package com.suriya.expensetracker;
import java.util.*;
public class FuelPageTest {
    static void check(boolean condition){if(!condition)throw new AssertionError();}
    public static void main(String[] args){
        check(FuelPage.thousandths("2.5")==2500);
        check(FuelPage.thousandths("12345.125")==12345125);
        check(FuelPage.average(10000000,10125000,2500).equals("50.00 km/L"));
        check(FuelPage.average(100,99,2000).equals("—"));
        try {FuelPage.thousandths("0");throw new AssertionError();} catch(IllegalArgumentException expected){}
        try {FuelPage.thousandths("2.0001");throw new AssertionError();} catch(ArithmeticException expected){}
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));Calendar c=Calendar.getInstance();c.clear();c.set(2026,Calendar.OCTOBER,10,23,59);long t=c.getTimeInMillis();check(FuelPage.dayStart(t)<=t&&t<FuelPage.dayEnd(t));c.set(2026,Calendar.OCTOBER,11,0,0);check(FuelPage.dayEnd(t)==c.getTimeInMillis());
        System.out.println("Fuel calculation and date tests passed");
    }
}
