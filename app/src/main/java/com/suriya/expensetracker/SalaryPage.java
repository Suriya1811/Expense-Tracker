package com.suriya.expensetracker;

import android.app.AlertDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Allows each month to inherit a salary method while linking only that month's credit. */
final class SalaryPage {
    final MainActivity a; final Store store; final SalarySettings settings; final Calendar month=Calendar.getInstance();
    SalaryPage(MainActivity a,Store store){this.a=a;this.store=store;settings=new SalarySettings(a,store);month.set(Calendar.DAY_OF_MONTH,1);month.set(Calendar.HOUR_OF_DAY,0);month.set(Calendar.MINUTE,0);month.set(Calendar.SECOND,0);month.set(Calendar.MILLISECOND,0);}
    void edit(Runnable afterSave){
        this.afterSave=afterSave;String key=SalarySettings.period(month);int selected=SalarySettings.TRACKER.equals(settings.mode(key))?0:1;
        a.aShowChoice("Select from Tracker?",new String[]{"Yes","No"},selected,which->{if(which==0)pick();else manual();});
    }
    Runnable afterSave=()->{};
    boolean monthIsCurrent(){if(MonthRules.isCurrent(month,Calendar.getInstance()))return true;Toast.makeText(a,"The month changed. Open salary edit again.",Toast.LENGTH_LONG).show();return false;}
    void manual(){if(!monthIsCurrent())return;String key=SalarySettings.period(month);LinearLayout form=a.column();form.setPadding(a.dp(22),a.dp(8),a.dp(22),a.dp(8));form.addView(a.formLabel("SALARY AMOUNT · ₹"));EditText amount=new EditText(a);amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);amount.setHint("0.00");a.styleField(amount);long prior=SalarySettings.MANUAL.equals(settings.mode(key))?settings.manualAmount(key):0;if(prior>0)amount.setText(a.amt(prior));form.addView(amount);a.space(form,8);form.addView(a.text("Added to credit from this month onward. If the salary is already in Tracker, select it instead to avoid counting it twice.",12,Color.WHITE));AlertDialog d=new AlertDialog.Builder(a).setTitle("Enter salary manually").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();d.setOnShowListener(v->d.getButton(-1).setOnClickListener(w->{try{if(!monthIsCurrent()){d.dismiss();return;}long value=a.parseAmount(amount.getText().toString());if(value<=0){amount.setError("Enter an amount greater than zero");return;}settings.saveManual(key,value);ExpenseWidget.requestUpdate(a);d.dismiss();afterSave.run();}catch(Exception e){amount.setError("Enter a valid amount");}}));a.showStyled(d);}
    void pick(){if(!monthIsCurrent())return;String key=SalarySettings.period(month);Calendar end=(Calendar)month.clone();end.add(Calendar.MONTH,1);ArrayList<Long> ids=new ArrayList<>();ArrayList<String> labels=new ArrayList<>();try(Cursor c=store.getReadableDatabase().rawQuery("SELECT _id,title,amount,date,bank FROM transactions WHERE kind='Income' AND date>=? AND date<? ORDER BY date DESC,_id DESC",new String[]{""+month.getTimeInMillis(),""+end.getTimeInMillis()})){while(c.moveToNext()){ids.add(c.getLong(0));labels.add(c.getString(1)+" · "+a.money(c.getLong(2))+"\n"+new SimpleDateFormat("d MMM yyyy",Locale.ENGLISH).format(new Date(c.getLong(3)))+" · "+(c.getString(4).isEmpty()?"Bank not set":c.getString(4)));}}
        if(ids.isEmpty()){a.showStyled(new AlertDialog.Builder(a).setTitle("No credits found").setMessage("Import or add the salary credit to Tracker for this month, then select it here.").setPositiveButton("OK",null));return;}
        LinearLayout box=a.column();box.setPadding(a.dp(20),a.dp(8),a.dp(20),a.dp(8));ScrollView scroll=new ScrollView(a);LinearLayout items=a.column();scroll.addView(items);box.addView(scroll,new LinearLayout.LayoutParams(-1,a.dp(380)));AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Select salary from Tracker").setView(box).setNegativeButton("Cancel",null).create();for(int i=0;i<ids.size();i++){long id=ids.get(i);TextView item=a.text(labels.get(i)+(id==settings.linkedId(key)?"  ✓":""),14,Color.WHITE);item.setPadding(a.dp(12),a.dp(14),a.dp(12),a.dp(14));items.addView(item);item.setOnClickListener(v->{if(!monthIsCurrent()){dialog.dismiss();return;}dialog.dismiss();Toast.makeText(a,"Saving salary…",Toast.LENGTH_SHORT).show();new Thread(()->{try{if(a.checkSelfPermission(android.Manifest.permission.READ_SMS)==android.content.pm.PackageManager.PERMISSION_GRANTED)try{store.recoverSmsLinks(a);}catch(Exception ignored){}settings.saveTracker(key,id);ExpenseWidget.requestUpdate(a);a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed()){afterSave.run();if(!settings.hasAutomaticPattern(key))a.showStyled(new AlertDialog.Builder(a).setTitle("Salary selected").setMessage("The original bank SMS or bank name could not be matched. This month is linked, but future months will need to be selected manually.").setPositiveButton("OK",null));}});}catch(Exception error){a.runOnUiThread(()->Toast.makeText(a,"Could not save salary selection",Toast.LENGTH_LONG).show());}}).start();});}a.showStyled(dialog);
    }
}
