package com.suriya.expensetracker;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Monthly commitment links affect the budget, never the underlying transactions. */
final class CommitmentsPage {
    final MainActivity a; final Store store; final Calendar month=Calendar.getInstance();
    CommitmentsPage(MainActivity activity,Store db){a=activity;store=db;month.set(Calendar.DAY_OF_MONTH,1);month.set(Calendar.HOUR_OF_DAY,0);month.set(Calendar.MINUTE,0);month.set(Calendar.SECOND,0);month.set(Calendar.MILLISECOND,0);}
    long end(){Calendar e=(Calendar)month.clone();e.add(Calendar.MONTH,1);return e.getTimeInMillis();}
    void show(){
        a.showingHome=false;a.showingPayees=true;a.trackerPage=false;a.fuelPage=false;a.emiPage=false;a.updateSystemBack();a.list=null;
        a.root=a.column();a.root.setBackgroundColor(a.bg(0xfff6f7fb));a.root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(0,i.getSystemWindowInsetTop(),0,i.getSystemWindowInsetBottom());return i;});a.setContentView(a.root);
        ScrollView scroll=new ScrollView(a);a.root.addView(scroll);LinearLayout page=a.column();page.setPadding(a.dp(20),a.dp(18),a.dp(20),a.dp(32));scroll.addView(page);
        TextView back=a.backButton("Back to profile");page.addView(back,new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));back.setOnClickListener(v->a.showProfile());a.space(page,18);
        page.addView(a.bold("Fixed commitments",25,Color.WHITE));a.space(page,6);page.addView(a.text("Link each month's payment to exclude it from your personal spending limit.",13,Color.WHITE));a.space(page,16);
        LinearLayout nav=new LinearLayout(a);nav.setGravity(Gravity.CENTER_VERTICAL);TextView prev=a.action("‹",0,0),next=a.action("›",0,0),label=a.bold(new SimpleDateFormat("MMMM yyyy",Locale.ENGLISH).format(month.getTime()),16,Color.WHITE);label.setGravity(Gravity.CENTER);nav.addView(prev,new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));nav.addView(label,new LinearLayout.LayoutParams(0,-2,1));nav.addView(next,new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));page.addView(nav);prev.setOnClickListener(v->{month.add(Calendar.MONTH,-1);show();});next.setOnClickListener(v->{month.add(Calendar.MONTH,1);show();});a.space(page,12);
        TextView add=a.accentAction("+ Add commitment",0xffe8b76b);page.addView(add);add.setOnClickListener(v->edit(0,"",0));a.space(page,16);
        boolean any=false;
        try(Cursor c=store.getReadableDatabase().rawQuery("SELECT c._id,c.name,c.expected_amount,t._id,t.title,t.amount,t.date FROM commitments c LEFT JOIN commitment_links l ON l.commitment_id=c._id AND l.month=? LEFT JOIN transactions t ON t._id=l.expense_id AND t.kind='Expense' AND t.date>=? AND t.date<? ORDER BY c.name COLLATE NOCASE,c._id",new String[]{a.key(month),""+month.getTimeInMillis(),""+end()})){
            while(c.moveToNext()){
                any=true;long id=c.getLong(0),expected=c.getLong(2);String name=c.getString(1);boolean linked=!c.isNull(3);long tx=linked?c.getLong(3):0;
                LinearLayout card=a.card(Color.WHITE,16);LinearLayout header=new LinearLayout(a);header.setGravity(Gravity.CENTER_VERTICAL);header.addView(a.bold(name,16,Color.WHITE),new LinearLayout.LayoutParams(0,-2,1));TextView edit=a.secondaryAction("Edit");header.addView(edit);card.addView(header);edit.setOnClickListener(v->edit(id,name,expected));a.space(card,8);
                card.addView(a.text(expected>0?"Usual amount  ·  "+a.money(expected):"Usual amount not set",12,Color.WHITE));a.space(card,8);
                if(linked){String date=new SimpleDateFormat("d MMM yyyy",Locale.ENGLISH).format(new Date(c.getLong(6)));card.addView(a.accentText("Linked: "+c.getString(4)+"  ·  "+date+"  ·  "+a.money(c.getLong(5)),13,0xffe8b76b,false));a.space(card,9);}
                TextView choose=a.secondaryAction(linked?"Change Tracker payment":"Select from Tracker");card.addView(choose);choose.setOnClickListener(v->pick(id,tx));
                if(linked){a.space(card,6);TextView unlink=a.text("Remove link",13,Color.WHITE);unlink.setPadding(a.dp(4),a.dp(8),0,0);card.addView(unlink);unlink.setOnClickListener(v->{store.getWritableDatabase().delete("commitment_links","commitment_id=? AND month=?",new String[]{""+id,a.key(month)});ExpenseWidget.requestUpdate(a);show();});}
                page.addView(card);a.space(page,10);
            }
        }
        if(!any)page.addView(a.text("Add commitments such as hostel rent or money sent home.",14,Color.WHITE));
    }
    void edit(long id,String name,long expected){
        LinearLayout form=a.column();form.setPadding(a.dp(22),a.dp(4),a.dp(22),0);form.addView(a.formLabel("NAME"));EditText title=new EditText(a);title.setHint("e.g. Hostel rent");a.styleField(title);title.setText(name);form.addView(title);a.space(form,10);form.addView(a.formLabel("USUAL AMOUNT · OPTIONAL"));EditText amount=new EditText(a);amount.setInputType(8194);amount.setHint("e.g. 9300");a.styleField(amount);if(expected>0)amount.setText(a.amt(expected));form.addView(amount);a.space(form,8);form.addView(a.text("The linked Tracker amount is used for each month. Edit that transaction if the amount changes.",12,Color.WHITE));
        AlertDialog.Builder builder=new AlertDialog.Builder(a).setTitle(id==0?"Add commitment":"Edit commitment").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Save",null);
        if(id!=0)builder.setNeutralButton("Delete",(d,w)->a.showStyled(new AlertDialog.Builder(a).setTitle("Delete commitment?").setMessage("Its links will be removed. Tracker transactions will stay, and count towards the limit again.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(x,y)->{SQLiteDatabase db=store.getWritableDatabase();db.beginTransaction();try{db.delete("commitment_links","commitment_id=?",new String[]{""+id});db.delete("commitments","_id=?",new String[]{""+id});db.setTransactionSuccessful();}finally{db.endTransaction();}ExpenseWidget.requestUpdate(a);show();})));
        AlertDialog dialog=builder.create();dialog.setOnShowListener(x->dialog.getButton(-1).setOnClickListener(v->{try{String n=title.getText().toString().trim();if(n.isEmpty()){title.setError("Enter a name");return;}long value=a.parseAmount(amount.getText().toString());ContentValues cv=new ContentValues();cv.put("name",n);cv.put("expected_amount",value);if(id==0)store.getWritableDatabase().insertOrThrow("commitments",null,cv);else store.getWritableDatabase().update("commitments",cv,"_id=?",new String[]{""+id});dialog.dismiss();show();}catch(Exception e){amount.setError("Enter a valid amount with up to 2 decimals");}}));a.showStyled(dialog);
    }
    void pick(long commitment,long current){
        ArrayList<Long> ids=new ArrayList<>();ArrayList<String> labels=new ArrayList<>();String period=a.key(month);
        try(Cursor c=store.getReadableDatabase().rawQuery("SELECT t._id,t.title,t.amount,t.date FROM transactions t WHERE t.kind='Expense' AND t.date>=? AND t.date<? AND NOT EXISTS (SELECT 1 FROM commitment_links l WHERE l.expense_id=t._id AND NOT (l.commitment_id=? AND l.month=?)) ORDER BY t.date DESC,t._id DESC",new String[]{""+month.getTimeInMillis(),""+end(),""+commitment,period})){
            while(c.moveToNext()){ids.add(c.getLong(0));labels.add(c.getString(1)+"  ·  "+a.money(c.getLong(2))+"\n"+new SimpleDateFormat("d MMM yyyy",Locale.ENGLISH).format(new Date(c.getLong(3))));}
        }
        if(ids.isEmpty()){a.showStyled(new AlertDialog.Builder(a).setTitle("No available debits").setMessage("Add or import a debit for this month in Tracker first. Payments already linked to another commitment are not shown.").setPositiveButton("OK",null));return;}
        LinearLayout box=a.column();box.setPadding(a.dp(20),a.dp(8),a.dp(20),a.dp(8));ScrollView scroller=new ScrollView(a);LinearLayout items=a.column();scroller.addView(items);box.addView(scroller,new LinearLayout.LayoutParams(-1,a.dp(380)));AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Select a payment").setView(box).setNegativeButton("Cancel",null).create();
        for(int i=0;i<ids.size();i++){final long id=ids.get(i);TextView item=a.text(labels.get(i)+(id==current?"  ✓":""),14,Color.WHITE);item.setPadding(a.dp(12),a.dp(14),a.dp(12),a.dp(14));items.addView(item);item.setOnClickListener(v->{SQLiteDatabase db=store.getWritableDatabase();try{db.beginTransaction();try(Cursor c=db.rawQuery("SELECT 1 FROM transactions WHERE _id=? AND kind='Expense' AND date>=? AND date<?",new String[]{""+id,""+month.getTimeInMillis(),""+end()})){if(!c.moveToFirst())throw new IllegalStateException();}db.delete("commitment_links","commitment_id=? AND month=?",new String[]{""+commitment,period});ContentValues cv=new ContentValues();cv.put("commitment_id",commitment);cv.put("month",period);cv.put("expense_id",id);db.insertOrThrow("commitment_links",null,cv);db.setTransactionSuccessful();}catch(Exception e){Toast.makeText(a,"This payment is no longer available. Try again.",Toast.LENGTH_LONG).show();return;}finally{db.endTransaction();}dialog.dismiss();ExpenseWidget.requestUpdate(a);show();});}
        a.showStyled(dialog);
    }
}
