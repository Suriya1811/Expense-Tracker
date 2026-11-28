package com.suriya.expensetracker;
import android.app.Activity;
import android.os.Bundle;
import android.database.Cursor;
import java.io.*;
import java.util.*;
public final class SalaryIntegrationActivity extends Activity {
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 static long day(int year,int month,int day){Calendar c=Calendar.getInstance();c.clear();c.set(year,month-1,day,12,0);return c.getTimeInMillis();}
 static long tx(Store s,long date){try(Cursor c=s.getReadableDatabase().rawQuery("SELECT _id FROM transactions WHERE date=?",new String[]{""+date})){if(!c.moveToFirst())throw new AssertionError("transaction missing");return c.getLong(0);}}
 @Override public void onCreate(Bundle state){super.onCreate(state);new Thread(()->{try{runTests();android.util.Log.i("SALARY_INTEGRATION","PASS current month, salary, backup restore and review flags");}catch(Throwable t){android.util.Log.e("SALARY_INTEGRATION","FAIL",t);}finally{runOnUiThread(()->finish());}}).start();}
 void runTests() throws Exception{
  check("com.suriya.expensetracker.salarytest20261128".equals(getPackageName()),"wrong package");
  getSharedPreferences("profile",MODE_PRIVATE).edit().clear().putLong("salary",3269200L).commit();
  File file=new File(getCacheDir(),"salary-roundtrip.xlsx");String sender="VM-KOTAKB-S";
  String nov="Received Rs.32692.00 in your Kotak Bank AC 1234 from PAYROLL on 25-11-26.UPI Ref:111111111111";
  String dec="Received Rs.32692.00 in your Kotak Bank AC 1234 from PAYROLL on 25-12-26.UPI Ref:222222222222";
  String dup="Received Rs.32692.00 in your Kotak Bank AC 1234 from PAYROLL on 26-12-26.UPI Ref:333333333333";
  long n=day(2026,11,25), d=day(2026,12,25), e=day(2026,12,26);
  try(Store store=new Store(this)){
   SalarySettings salary=new SalarySettings(this,store);check(salary.extraCredit("2026-10")==3269200L,"legacy salary");
   java.util.concurrent.atomic.AtomicReference<MainActivity> screenRef=new java.util.concurrent.atomic.AtomicReference<>();java.util.concurrent.CountDownLatch ready=new java.util.concurrent.CountDownLatch(1);runOnUiThread(()->{screenRef.set(new MainActivity());ready.countDown();});check(ready.await(5,java.util.concurrent.TimeUnit.SECONDS),"test activity setup");MainActivity screen=screenRef.get();screen.store=store;Calendar now=Calendar.getInstance();java.util.TreeMap<Integer,java.util.TreeSet<Integer>> periods=screen.monthsWithTransactions();check(periods.containsKey(now.get(Calendar.YEAR))&&periods.get(now.get(Calendar.YEAR)).contains(now.get(Calendar.MONTH)),"empty current month missing");
   java.lang.reflect.Field selectedField=MainActivity.class.getDeclaredField("month");selectedField.setAccessible(true);Calendar old=(Calendar)selectedField.get(screen);old.add(Calendar.MONTH,-1);screen.latestOverviewMonth();check(MonthRules.isCurrent((Calendar)selectedField.get(screen),now),"dashboard did not select current month");
   java.lang.reflect.Field seenField=MainActivity.class.getDeclaredField("calendarMonthSeen");seenField.setAccessible(true);Calendar previous=(Calendar)now.clone();previous.add(Calendar.MONTH,-1);seenField.set(screen,previous);old=(Calendar)selectedField.get(screen);old.add(Calendar.MONTH,-1);screen.syncCalendarMonth();check(MonthRules.isCurrent((Calendar)selectedField.get(screen),now),"calendar rollover did not update month");
   check(store.importSms(sender,nov,n),"November import");salary.saveTracker("2026-11",tx(store,n));check(salary.hasAutomaticPattern("2026-11"),"missing pattern");check(salary.extraCredit("2026-11")==0,"double counting");
   check(store.importSms(sender,dec,d),"December import");check(salary.linkedId("2026-12")==tx(store,d),"automatic matching");
   check(store.importSms(sender,dup,e),"second December import");check(salary.linkedId("2026-12")==-1,"ambiguous matches must not be guessed");
   salary.saveTracker("2026-12",tx(store,d));check(salary.linkedId("2026-12")==tx(store,d),"manual correction");
   salary.saveManual("2027-01",3000000L);check(salary.extraCredit("2027-02")==3000000L,"manual inheritance");check(salary.extraCredit("2026-10")==3269200L,"past changed");
   try(OutputStream out=new FileOutputStream(file)){WorkbookIO.write(out,new BackupSnapshot(this,store).capture());}
  }
  check(deleteDatabase("expenses.db"),"isolated test database removal");getSharedPreferences("profile",MODE_PRIVATE).edit().clear().commit();
  try(Store restored=new Store(this)){BackupSnapshot.Parsed data=BackupSnapshot.validate(WorkbookIO.readMeta(file));check(new BackupSnapshot(this,restored).merge(data)==3,"restore count");SalarySettings salary=new SalarySettings(this,restored);check(salary.linkedId("2026-11")>=0,"restored link");check(salary.linkedId("2026-12")>=0,"restored corrected link");check(salary.extraCredit("2027-02")==3000000L,"restored salary");check(!restored.importSms(sender,nov,n),"duplicate SMS after restore");
   restored.saveRule("DEMO CAFE","Snacks");long debitDay=day(2026,11,20);String debit="Your a/c XXXXX99 debited for payee DEMO CAFE for Rs. 22.00 on 2026-11-20, ref 123456789012. If not you, report to your bank immediately-IOB.";
   check(restored.importSms("BZ-IOBCHN-S",debit,debitDay),"regular payee import");try(Cursor c=restored.getReadableDatabase().rawQuery("SELECT category,review FROM transactions WHERE date=?",new String[]{""+debitDay})){check(c.moveToFirst()&&"Snacks".equals(c.getString(0))&&c.getInt(1)==0,"categorized payee review flag");}
   try(Cursor c=restored.getReadableDatabase().rawQuery("SELECT review FROM transactions WHERE date=?",new String[]{""+n})){check(c.moveToFirst()&&c.getInt(0)==0,"credit review flag");}
   check(ReviewRules.needsReview("Expense","Uncategorized"),"uncategorized display");check(!ReviewRules.needsReview("Expense","Snacks"),"categorized display");check(!ReviewRules.needsReview("Income","Uncategorized"),"income display");}
 }
}
