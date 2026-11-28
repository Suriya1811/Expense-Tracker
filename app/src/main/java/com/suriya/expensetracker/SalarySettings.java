package com.suriya.expensetracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.text.SimpleDateFormat;
import java.util.*;

/** Monthly salary choice; linked credits are already included in transaction totals. */
final class SalarySettings {
    static final String MANUAL="manual", TRACKER="tracker";
    final SharedPreferences prefs; final Store store; final Context context;
    SalarySettings(Context c,Store s){context=c.getApplicationContext();prefs=c.getSharedPreferences("profile",Context.MODE_PRIVATE);store=s;}
    static String period(Calendar month){return new SimpleDateFormat("yyyy-MM",Locale.US).format(month.getTime());}
    String source(String month){return SalaryRules.mostRecentPeriod(prefs.getAll().keySet(),month);}
    String mode(String month){String source=source(month);return source==null?MANUAL:prefs.getString(SalaryRules.MODE_PREFIX+source,MANUAL);}
    long manualAmount(String month){String source=source(month);return source==null?prefs.getLong("salary",0):prefs.getLong("salary_value_"+source,0);}
    long extraCredit(String month){return TRACKER.equals(mode(month))?0:manualAmount(month);}
    boolean hasAutomaticPattern(String month){String src=source(month);return src!=null&&!prefs.getString("salary_pattern_"+src,"").isEmpty()&&!prefs.getString("salary_bank_"+src,"").isEmpty();}
    String linkedUid(String month){return prefs.getString("salary_link_"+month,"");}
    long linkedId(String month){String uid=linkedUid(month);SQLiteDatabase db=store.getReadableDatabase();Calendar start=Calendar.getInstance();start.clear();try{start.setTime(new SimpleDateFormat("yyyy-MM",Locale.US).parse(month));}catch(Exception e){return -1;}Calendar end=(Calendar)start.clone();end.add(Calendar.MONTH,1);
        if(!uid.isEmpty()){try(Cursor c=db.rawQuery("SELECT t._id FROM backup_ids i JOIN transactions t ON t._id=i.local_id WHERE i.kind='transactions' AND i.uid=? AND t.kind='Income' AND t.date>=? AND t.date<?",new String[]{uid,""+start.getTimeInMillis(),""+end.getTimeInMillis()})){return c.moveToFirst()?c.getLong(0):-1;}}
        String source=source(month);if(source==null||!TRACKER.equals(mode(month)))return -1;String pattern=prefs.getString("salary_pattern_"+source,""),bank=prefs.getString("salary_bank_"+source,"");long amount=prefs.getLong("salary_amount_"+source,0);if(pattern.isEmpty()||bank.isEmpty()||amount<=0)return -1;
        long match=-1;try(Cursor c=db.rawQuery("SELECT t._id,l.fingerprint FROM transactions t JOIN sms_links l ON l.transaction_id=t._id WHERE t.kind='Income' AND t.bank=? AND t.amount=? AND t.date>=? AND t.date<?",new String[]{bank,""+amount,""+start.getTimeInMillis(),""+end.getTimeInMillis()})){while(c.moveToNext()){if(!pattern.equals(prefs.getString("salary_sms_signature_"+c.getString(1),"")))continue;if(match!=-1)return -1;match=c.getLong(0);}}return match;
    }
    long linkedAmount(String month){long id=linkedId(month);if(id<0)return 0;try(Cursor c=store.getReadableDatabase().rawQuery("SELECT amount FROM transactions WHERE _id=?",new String[]{""+id})){return c.moveToFirst()?c.getLong(0):0;}}
    void saveManual(String month,long amount){prefs.edit().putString(SalaryRules.MODE_PREFIX+month,MANUAL).putLong("salary_value_"+month,amount).remove("salary_link_"+month).apply();}
    void saveTracker(String month,long id){SQLiteDatabase db=store.getWritableDatabase();String uid=BackupSnapshot.uid(db,"transactions",id),bank="",fingerprint="",pattern="";long amount=0,date=0;try(Cursor c=db.rawQuery("SELECT t.bank,t.amount,t.date,l.fingerprint FROM transactions t LEFT JOIN sms_links l ON l.transaction_id=t._id WHERE t._id=? AND t.kind='Income'",new String[]{""+id})){if(!c.moveToFirst())throw new IllegalArgumentException("Credit no longer exists");bank=c.getString(0);amount=c.getLong(1);date=c.getLong(2);if(!c.isNull(3))fingerprint=c.getString(3);}
        if(!fingerprint.isEmpty()){pattern=prefs.getString("salary_sms_signature_"+fingerprint,"");if(pattern.isEmpty()&&context.checkSelfPermission(android.Manifest.permission.READ_SMS)==android.content.pm.PackageManager.PERMISSION_GRANTED){try(Cursor sms=context.getContentResolver().query(android.provider.Telephony.Sms.Inbox.CONTENT_URI,new String[]{"address","body","date"},"date=?",new String[]{""+date},null)){if(sms!=null)while(sms.moveToNext()){String sender=sms.getString(0),body=sms.getString(1);if(body!=null&&fingerprint.equals(Store.fingerprint(sender,body,sms.getLong(2)))){pattern=SalaryPattern.signature(sender,body);break;}}}catch(Exception ignored){}}}
        prefs.edit().putString(SalaryRules.MODE_PREFIX+month,TRACKER).putString("salary_link_"+month,uid).putString("salary_pattern_"+month,pattern).putString("salary_bank_"+month,bank==null?"":bank).putLong("salary_amount_"+month,amount).apply();}

}
