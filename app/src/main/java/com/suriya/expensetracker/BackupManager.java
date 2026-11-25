package com.suriya.expensetracker;

import android.app.*;
import android.app.job.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Folder access, verified spreadsheet writing and safe replacement. */
final class BackupManager {
    static final String FILE="All-Transaction-Expense-Tracker.xlsx", MIME="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static final String PREF="backup_control", TREE="tree", ARMED="armed", LAST="last_day";static final Object LOCK=new Object();static final int JOB=8417;
    static String currentDay(){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());}
    static SharedPreferences prefs(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
    static Uri tree(Context c){String s=prefs(c).getString(TREE,"");return s.isEmpty()?null:Uri.parse(s);}
    static Uri find(Context c,Uri tree,String name)throws Exception{
        String parent=DocumentsContract.getTreeDocumentId(tree);Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,parent);
        try(Cursor rows=c.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){
            if(rows==null)throw new IOException("Cannot read the backup folder");Uri found=null;while(rows.moveToNext()){if(name.equals(rows.getString(1))){if(found!=null)throw new IOException("More than one backup has this name");found=DocumentsContract.buildDocumentUriUsingTree(tree,rows.getString(0));}}return found;
        }
    }
    static void schedule(Context c){JobScheduler scheduler=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);if(scheduler!=null)scheduler.cancel(JOB);}
    static void write(Context context,boolean manual)throws Exception{synchronized(LOCK){
        Uri tree=tree(context);if(tree==null)throw new IOException("Choose a backup folder first");if(!manual&&!prefs(context).getBoolean(ARMED,false))return;
        if(!manual&&currentDay().equals(prefs(context).getString(LAST,"")))return;
        File local=File.createTempFile("expense-workbook-", ".xlsx",context.getCacheDir());Uri temp=null,old=null;String oldName=null;boolean completed=false;
        try(Store store=new Store(context)){
            BackupSnapshot snapshot=new BackupSnapshot(context,store);try(OutputStream out=new FileOutputStream(local)){WorkbookIO.write(out,snapshot.capture());}
            BackupSnapshot.validate(WorkbookIO.readMeta(local));
            ContentResolver r=context.getContentResolver();Uri root=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));String tempName=".expense-tracker-"+UUID.randomUUID()+".xlsx";temp=DocumentsContract.createDocument(r,root,MIME,tempName);if(temp==null)throw new IOException("Could not create backup in selected folder");
            try(OutputStream out=r.openOutputStream(temp,"w");InputStream in=new FileInputStream(local)){if(out==null)throw new IOException("Could not open backup file");byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1)out.write(b,0,n);out.flush();}
            // Verify the provider actually stored the complete workbook before touching the previous one.
            File check=File.createTempFile("expense-check-", ".xlsx",context.getCacheDir());try{copyToFile(r,temp,check);if(!java.util.Arrays.equals(digest(local),digest(check)))throw new IOException("Backup verification failed");BackupSnapshot.validate(WorkbookIO.readMeta(check));}finally{check.delete();}
            Uri existing=find(context,tree,FILE);if(existing!=null){oldName=".expense-tracker-previous-"+UUID.randomUUID()+".xlsx";old=DocumentsContract.renameDocument(r,existing,oldName);if(old==null)throw new IOException("Folder does not support safe backup replacement");}
            Uri result=DocumentsContract.renameDocument(r,temp,FILE);if(result==null)throw new IOException("Could not finish the backup");temp=null;completed=true;
            if(old!=null){if(!DocumentsContract.deleteDocument(r,old)){/* New backup is valid; keep the previous copy if the provider refuses deletion. */}old=null;}
            prefs(context).edit().putString(LAST,currentDay()).putLong("last_success",System.currentTimeMillis()).putString("last_error","").apply();
        }catch(Exception failure){
            if(old!=null&&!completed){try{DocumentsContract.renameDocument(context.getContentResolver(),old,FILE);}catch(Exception ignored){/* Original remains under previous name. */}}
            prefs(context).edit().putString("last_error","Backup failed. Check folder access and try again.").apply();throw failure;
        }finally{if(temp!=null){try{DocumentsContract.deleteDocument(context.getContentResolver(),temp);}catch(Exception ignored){}}local.delete();}
    }}
    static byte[] digest(File file)throws Exception{java.security.MessageDigest md=java.security.MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1)md.update(b,0,n);}return md.digest();}
    static void copyToFile(ContentResolver resolver,Uri uri,File out)throws IOException{try(InputStream in=resolver.openInputStream(uri);OutputStream dst=new FileOutputStream(out)){if(in==null)throw new IOException("Unable to open the Excel file");byte[] buffer=new byte[16384];int read;long total=0;while((read=in.read(buffer))!=-1){total+=read;if(total>80L*1024*1024)throw new IOException("Backup file is too large");dst.write(buffer,0,read);}}}
    static File copySelected(Context c,Uri uri)throws IOException{File f=File.createTempFile("expense-restore-", ".xlsx",c.getCacheDir());try{copyToFile(c.getContentResolver(),uri,f);return f;}catch(IOException e){f.delete();throw e;}}
    static void checkOnOpen(Context c){if(tree(c)==null||!prefs(c).getBoolean(ARMED,false)||currentDay().equals(prefs(c).getString(LAST,"")))return;Context app=c.getApplicationContext();new Thread(()->{try{write(app,false);}catch(Exception failure){new Handler(Looper.getMainLooper()).post(()->android.widget.Toast.makeText(app,"Could not update the Excel backup. Check the backup folder in Settings.",android.widget.Toast.LENGTH_LONG).show());}schedule(app);}).start();}
}
