package com.suriya.expensetracker;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.provider.Telephony;
import java.util.*;
import java.util.regex.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;

public final class Store extends SQLiteOpenHelper {
    public Store(Context c) { super(c, "expenses.db", null, 7); }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE transactions (_id INTEGER PRIMARY KEY, amount INTEGER NOT NULL, kind TEXT NOT NULL, category TEXT NOT NULL, title TEXT NOT NULL, date INTEGER NOT NULL, review INTEGER NOT NULL DEFAULT 0, bank TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE processed (fingerprint TEXT PRIMARY KEY)");
        db.execSQL("CREATE TABLE payee_rules (normalized TEXT PRIMARY KEY, name TEXT NOT NULL, category TEXT NOT NULL)");
        createFuelTable(db);
        createEmiTables(db);
        createCommitments(db);
        createBackupTables(db);
    }
    static void createFuelTable(SQLiteDatabase db){db.execSQL("CREATE TABLE fuel_cycles (_id INTEGER PRIMARY KEY, start_date INTEGER NOT NULL, start_m INTEGER NOT NULL, litres_ml INTEGER NOT NULL, price INTEGER NOT NULL, end_date INTEGER, end_m INTEGER, expense_id INTEGER UNIQUE)");}
    static void createEmiTables(SQLiteDatabase db){
        db.execSQL("CREATE TABLE emi_plans (_id INTEGER PRIMARY KEY, product TEXT NOT NULL, amount INTEGER NOT NULL, start_date INTEGER NOT NULL, months INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE emi_payments (_id INTEGER PRIMARY KEY, plan_id INTEGER NOT NULL, installment INTEGER NOT NULL, paid_at INTEGER NOT NULL, expense_id INTEGER UNIQUE, UNIQUE(plan_id, installment))");
    }
    static void createCommitments(SQLiteDatabase db){
        db.execSQL("CREATE TABLE commitments (_id INTEGER PRIMARY KEY, name TEXT NOT NULL, expected_amount INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE commitment_links (commitment_id INTEGER NOT NULL, month TEXT NOT NULL, expense_id INTEGER NOT NULL UNIQUE, PRIMARY KEY(commitment_id,month))");
        db.execSQL("CREATE TRIGGER commitment_remove_transaction AFTER DELETE ON transactions BEGIN DELETE FROM commitment_links WHERE expense_id=OLD._id; END");
    }
    static void createBackupTables(SQLiteDatabase db){
        db.execSQL("CREATE TABLE backup_ids (kind TEXT NOT NULL, local_id INTEGER NOT NULL, uid TEXT PRIMARY KEY)");
        db.execSQL("CREATE TABLE sms_links (fingerprint TEXT PRIMARY KEY, transaction_id INTEGER NOT NULL UNIQUE)");
        db.execSQL("CREATE TRIGGER backup_transaction_delete AFTER DELETE ON transactions BEGIN DELETE FROM sms_links WHERE transaction_id=OLD._id; DELETE FROM backup_ids WHERE kind='transactions' AND local_id=OLD._id; END");
        for(String table:new String[]{"fuel_cycles","emi_plans","emi_payments","commitments","commitment_links"})db.execSQL("CREATE TRIGGER backup_"+table+"_delete AFTER DELETE ON "+table+" BEGIN DELETE FROM backup_ids WHERE kind='"+table+"' AND local_id=OLD.rowid; END");
    }
    long personalSpent(long start,long end,String month){
        try(Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(t.amount),0) FROM transactions t WHERE t.kind='Expense' AND t.date>=? AND t.date<? AND NOT EXISTS (SELECT 1 FROM commitment_links l WHERE l.expense_id=t._id AND l.month=?)",new String[]{""+start,""+end,month})){return c.moveToFirst()?c.getLong(0):0;}
    }
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) { if(oldV<2) db.execSQL("CREATE TABLE payee_rules (normalized TEXT PRIMARY KEY, name TEXT NOT NULL, category TEXT NOT NULL)"); if(oldV<3)createFuelTable(db); if(oldV<4)createEmiTables(db); if(oldV<5)createCommitments(db); if(oldV<6)db.execSQL("ALTER TABLE transactions ADD COLUMN bank TEXT NOT NULL DEFAULT ''"); if(oldV<7)createBackupTables(db); }
    public String categoryFor(String name) { String key=PayeeParser.normalize(name); if(key.isEmpty())return null; try(Cursor c=getReadableDatabase().rawQuery("SELECT category FROM payee_rules WHERE normalized=?",new String[]{key})){return c.moveToFirst()?c.getString(0):null;} }
    public void saveRule(String name, String category){String key=PayeeParser.normalize(name);if(key.isEmpty())throw new IllegalArgumentException("Payee name is required");ContentValues v=new ContentValues();v.put("normalized",key);v.put("name",name.trim().replaceAll("\\s+"," "));v.put("category",category);getWritableDatabase().insertWithOnConflict("payee_rules",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    public void removeRule(String name){getWritableDatabase().delete("payee_rules","normalized=?",new String[]{PayeeParser.normalize(name)});}
    public int categorizeUncategorized(String name,String category){SQLiteDatabase db=getWritableDatabase();String key=PayeeParser.normalize(name);ArrayList<Long> ids=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT _id,title FROM transactions WHERE kind='Expense' AND category='Uncategorized'",null)){while(c.moveToNext())if(key.equals(PayeeParser.normalize(c.getString(1))))ids.add(c.getLong(0));}int count=0;ContentValues v=new ContentValues();v.put("category",category);db.beginTransaction();try{for(Long id:ids)count+=db.update("transactions",v,"_id=? AND category='Uncategorized'",new String[]{id.toString()});db.setTransactionSuccessful();}finally{db.endTransaction();}return count;}
    static String fingerprint(String sender, String body, long date) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(((sender == null ? "" : sender.toLowerCase(Locale.ROOT)) + "|" + body.trim() + "|" + new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(date))).getBytes(StandardCharsets.UTF_8));
            StringBuilder b = new StringBuilder(); for (byte x:bytes) b.append(String.format(Locale.US, "%02x", x & 255)); return b.toString();
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    static ContentValues parse(String sender,String body,long date){
        SmsRules.Result parsed=SmsRules.parse(sender,body);if(parsed==null)return null;
        ContentValues v=new ContentValues();v.put("amount",parsed.amount);v.put("kind",parsed.kind);v.put("category","Uncategorized");v.put("title","SMS transaction");v.put("date",date);v.put("review",1);return v;
    }
    public synchronized boolean importSms(String sender, String body, long date) {
        ContentValues values = parse(sender,body,date); if(values == null) return false; values.put("bank",BankNames.fromSms(sender,body));
        if("Expense".equals(values.getAsString("kind"))){String payee=PayeeParser.extract(body);if(payee!=null){values.put("title",payee);String category=categoryFor(payee);if(category!=null)values.put("category",category);}}
        String key = fingerprint(sender,body,date); SQLiteDatabase db=getWritableDatabase();
        db.beginTransaction(); try {
            ContentValues seen=new ContentValues(); seen.put("fingerprint",key);
            if(db.insertWithOnConflict("processed",null,seen,SQLiteDatabase.CONFLICT_IGNORE)==-1) { db.setTransactionSuccessful(); return false; }
            long id=db.insertOrThrow("transactions",null,values);ContentValues link=new ContentValues();link.put("fingerprint",key);link.put("transaction_id",id);db.insertOrThrow("sms_links",null,link); db.setTransactionSuccessful(); return true;
        } finally { db.endTransaction(); }
    }
    /** Backfill only an unambiguous, previously imported SMS with its original timestamp. */
    public int recoverBanks(Context context){
        SQLiteDatabase db=getWritableDatabase();Map<Long,String> candidates=new LinkedHashMap<>();Map<Long,Integer> appearances=new HashMap<>();Set<String> seenMessages=new HashSet<>();
        try(Cursor sms=context.getContentResolver().query(Telephony.Sms.Inbox.CONTENT_URI,new String[]{"address","body","date"},null,null,null)){
            if(sms==null)throw new IllegalStateException("SMS inbox unavailable");
            while(sms.moveToNext()){
                String sender=sms.getString(0),body=sms.getString(1),bank=BankNames.fromSms(sender,body);long date=sms.getLong(2);if(bank.isEmpty()||body==null)continue;
                ContentValues parsed=parse(sender,body,date);if(parsed==null)continue;String fingerprint=fingerprint(sender,body,date);if(!seenMessages.add(fingerprint))continue;
                try(Cursor recorded=db.rawQuery("SELECT 1 FROM processed WHERE fingerprint=?",new String[]{fingerprint})){if(!recorded.moveToFirst())continue;}
                ArrayList<Long> matches=new ArrayList<>();
                try(Cursor rows=db.rawQuery("SELECT _id FROM transactions WHERE amount=? AND kind=? AND date=?",new String[]{parsed.getAsLong("amount").toString(),parsed.getAsString("kind"),Long.toString(date)})){while(rows.moveToNext())matches.add(rows.getLong(0));}
                for(Long id:matches)appearances.put(id,appearances.getOrDefault(id,0)+1);
                if(matches.size()==1)candidates.put(matches.get(0),bank);
            }
        }
        int updated=0;db.beginTransaction();try{
            for(Map.Entry<Long,String> entry:candidates.entrySet()){
                if(appearances.getOrDefault(entry.getKey(),0)!=1)continue;
                ContentValues values=new ContentValues();values.put("bank",entry.getValue());
                updated+=db.update("transactions",values,"_id=? AND (bank IS NULL OR bank='')",new String[]{entry.getKey().toString()});
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        return updated;
    }
    /** Link older imports to their SMS only when both message and stored row match uniquely. */
    synchronized int recoverSmsLinks(Context context){
        SQLiteDatabase db=getWritableDatabase();Map<Long,String> candidates=new LinkedHashMap<>();Map<Long,Integer> counts=new HashMap<>();Set<String> fingerprints=new HashSet<>();
        try(Cursor sms=context.getContentResolver().query(Telephony.Sms.Inbox.CONTENT_URI,new String[]{"address","body","date"},null,null,null)){
            if(sms==null)throw new IllegalStateException("SMS inbox unavailable");while(sms.moveToNext()){
                String sender=sms.getString(0),body=sms.getString(1);long date=sms.getLong(2);ContentValues parsed=parse(sender,body,date);if(parsed==null)continue;String key=fingerprint(sender,body,date);if(!fingerprints.add(key))continue;
                try(Cursor c=db.rawQuery("SELECT 1 FROM processed WHERE fingerprint=?",new String[]{key})){if(!c.moveToFirst())continue;}
                ArrayList<Long> found=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT _id FROM transactions WHERE amount=? AND kind=? AND date=?",new String[]{parsed.getAsLong("amount").toString(),parsed.getAsString("kind"),Long.toString(date)})){while(c.moveToNext())found.add(c.getLong(0));}
                for(Long id:found)counts.put(id,counts.getOrDefault(id,0)+1);if(found.size()==1)candidates.put(found.get(0),key);
            }
        }
        int linked=0;db.beginTransaction();try{for(Map.Entry<Long,String> e:candidates.entrySet()){if(counts.getOrDefault(e.getKey(),0)!=1)continue;ContentValues v=new ContentValues();v.put("fingerprint",e.getValue());v.put("transaction_id",e.getKey());if(db.insertWithOnConflict("sms_links",null,v,SQLiteDatabase.CONFLICT_IGNORE)!=-1)linked++;}db.setTransactionSuccessful();}finally{db.endTransaction();}return linked;
    }
    public int recoverPayees(Context context) {
        SQLiteDatabase db=getWritableDatabase();
        Map<Long, String> uniqueCandidates=new LinkedHashMap<>(); Map<Long, Integer> appearances=new HashMap<>(); Set<String> messages=new HashSet<>();
        long since=System.currentTimeMillis()-90L*24*60*60*1000;
        try(Cursor sms=context.getContentResolver().query(Telephony.Sms.Inbox.CONTENT_URI,new String[]{"address","body","date"},"date >= ?",new String[]{Long.toString(since)},null)){
            if(sms==null)throw new IllegalStateException("SMS inbox unavailable");
            while(sms.moveToNext()){
                String sender=sms.getString(0),body=sms.getString(1);long date=sms.getLong(2);String payee=PayeeParser.extract(body);if(payee==null)continue;
                ContentValues parsed=parse(sender,body,date);if(parsed==null||!"Expense".equals(parsed.getAsString("kind")))continue;
                String fingerprint=fingerprint(sender,body,date);if(!messages.add(fingerprint))continue;
                try(Cursor seen=db.rawQuery("SELECT 1 FROM processed WHERE fingerprint=?",new String[]{fingerprint})){if(!seen.moveToFirst())continue;}
                ArrayList<Long> candidates=new ArrayList<>();
                try(Cursor c=db.rawQuery("SELECT _id FROM transactions WHERE title='SMS transaction' AND category='Uncategorized' AND review=1 AND kind='Expense' AND amount=? AND date BETWEEN ? AND ?",new String[]{parsed.getAsLong("amount").toString(),Long.toString(date-300000),Long.toString(date+300000)})){while(c.moveToNext())candidates.add(c.getLong(0));}
                for(Long id:candidates)appearances.put(id,appearances.getOrDefault(id,0)+1);
                if(candidates.size()==1)uniqueCandidates.put(candidates.get(0),payee);
            }
        }
        int updated=0;db.beginTransaction();try{
            for(Map.Entry<Long,String> candidate:uniqueCandidates.entrySet()){
                if(appearances.getOrDefault(candidate.getKey(),0)!=1)continue;
                ContentValues values=new ContentValues();values.put("title",candidate.getValue());String category=categoryFor(candidate.getValue());if(category!=null)values.put("category",category);
                updated+=db.update("transactions",values,"_id=? AND title='SMS transaction' AND category='Uncategorized' AND review=1",new String[]{candidate.getKey().toString()});
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        return updated;
    }
}
