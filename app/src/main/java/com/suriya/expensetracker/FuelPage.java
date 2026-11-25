package com.suriya.expensetracker;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

/** On-device fuel log. Odometer and litres are stored as integer thousandths. */
final class FuelPage {
    final MainActivity a; final Store store;
    FuelPage(MainActivity activity, Store store){a=activity;this.store=store;}
    static final class Match {long id,amount,date;String label;Match(long i,long n,long d,String l){id=i;amount=n;date=d;label=l;}}
    static long thousandths(String raw){long n=new BigDecimal(raw.trim()).movePointRight(3).longValueExact();if(n<=0||n>1_000_000_000_000L)throw new IllegalArgumentException();return n;}
    static String decimal(long n){return BigDecimal.valueOf(n,3).stripTrailingZeros().toPlainString();}
    static String average(long start,long end,long ml){if(end<start||ml<=0)return "—";return BigDecimal.valueOf(end-start).divide(BigDecimal.valueOf(ml),2,RoundingMode.HALF_UP).toPlainString()+" km/L";}
    static long dayStart(long time){Calendar c=Calendar.getInstance();c.setTimeInMillis(time);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTimeInMillis();}
    static long dayEnd(long time){Calendar c=Calendar.getInstance();c.setTimeInMillis(dayStart(time));c.add(Calendar.DAY_OF_MONTH,1);return c.getTimeInMillis();}
    String date(long d){return new SimpleDateFormat("dd MMM yyyy",Locale.ENGLISH).format(new Date(d));}
    TextView label(String text){TextView v=a.text(text,12,0xff78859a);v.setPadding(0,a.dp(8),0,a.dp(5));return v;}
    EditText numeric(String hint){EditText e=new EditText(a);e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);e.setHint(hint);a.styleField(e);return e;}
    Calendar dateInput(LinearLayout form,String title,long initial){form.addView(label(title));Calendar date=Calendar.getInstance();date.setTimeInMillis(initial);date.set(Calendar.HOUR_OF_DAY,12);date.set(Calendar.MINUTE,0);date.set(Calendar.SECOND,0);date.set(Calendar.MILLISECOND,0);Button b=a.button("");b.setBackground(a.background(0xff383838,12));b.setStateListAnimator(null);b.setTextColor(a.isLight()?0xff171717:Color.WHITE);Runnable update=()->b.setText(date(date.getTimeInMillis()));update.run();b.setOnClickListener(v->a.showStyled(new DatePickerDialog(a,(view,y,m,d)->{date.set(y,m,d);update.run();},date.get(Calendar.YEAR),date.get(Calendar.MONTH),date.get(Calendar.DAY_OF_MONTH))));form.addView(b);return date;}
    void render(){
        ExpenseWidget.requestUpdate(a);
        a.showingHome=true;a.showingPayees=false;a.trackerPage=false;a.fuelPage=true;a.emiPage=false;a.updateSystemBack();a.root=a.column();a.root.setBackgroundColor(a.bg(0xfff6f7fb));a.root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(0,i.getSystemWindowInsetTop(),0,i.getSystemWindowInsetBottom());return i;});a.setContentView(a.root);
        LinearLayout content=a.column();content.setPadding(a.dp(20),a.dp(18),a.dp(20),0);a.root.addView(content,new LinearLayout.LayoutParams(-1,0,1));content.addView(a.text("YOUR VEHICLE",11,0xff7f8ba1));a.space(content,4);content.addView(a.bold("Fuel tracker",24,0xff182137));a.space(content,5);content.addView(a.text("Track each refuel and its fuel cycle",13,0xff8490a4));a.space(content,14);
        LinearLayout overview=a.card(Color.WHITE,15);overview.addView(a.bold("Current cycle",15,0xff202b40));a.space(overview,10);long currentStart=0,currentLitres=0,currentPrice=0;boolean active=false;try(Cursor c=store.getReadableDatabase().rawQuery("SELECT start_m,litres_ml,price FROM fuel_cycles WHERE end_date IS NULL ORDER BY start_date DESC,_id DESC LIMIT 1",null)){if(c.moveToFirst()){active=true;currentStart=c.getLong(0);currentLitres=c.getLong(1);currentPrice=c.getLong(2);}}LinearLayout stats=new LinearLayout(a);addStat(stats,"START KM",active?decimal(currentStart)+" km":"—",false);addStat(stats,"FUEL",active?decimal(currentLitres)+" L":"—",true);addStat(stats,"PRICE",active?a.money(currentPrice):"—",true);overview.addView(stats);a.space(overview,9);android.content.SharedPreferences prefs=a.getSharedPreferences("profile",Context.MODE_PRIVATE);long min=prefs.getLong("bike_mileage_min",40000),max=prefs.getLong("bike_mileage_max",45000);overview.addView(a.text(min>0&&max>=min?"Expected mileage: "+decimal(min)+"–"+decimal(max)+" km/L":"Set your expected mileage in Profile",12,0xff8490a4));content.addView(overview);a.space(content,14);
        TextView add=a.accentAction("+ Add fuel",0xff65bceb);content.addView(add,new LinearLayout.LayoutParams(-1,a.dp(50)));add.setOnClickListener(v->addFuel());a.space(content,15);
        TextView guidance=a.text("A new refuel closes the previous open cycle using the new date and odometer reading.",12,0xff77859a);content.addView(guidance);a.space(content,14);
        ScrollView scroll=new ScrollView(a);content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));a.list=a.column();scroll.addView(a.list);
        try(Cursor c=store.getReadableDatabase().rawQuery("SELECT _id,start_date,start_m,litres_ml,price,end_date,end_m,expense_id FROM fuel_cycles ORDER BY start_date DESC,_id DESC",null)){
            while(c.moveToNext()){
                long id=c.getLong(0),startDate=c.getLong(1),startM=c.getLong(2),litres=c.getLong(3),price=c.getLong(4);boolean ended=!c.isNull(5)&&!c.isNull(6);long endDate=ended?c.getLong(5):0,endM=ended?c.getLong(6):0;boolean linked=!c.isNull(7);
                LinearLayout card=a.card(Color.WHITE,17);LinearLayout heading=new LinearLayout(a);heading.setGravity(Gravity.CENTER_VERTICAL);heading.addView(a.bold(date(startDate),16,0xff202b40),new LinearLayout.LayoutParams(0,-2,1));heading.addView(a.accentText(a.money(price),16,0xff65bceb,true));card.addView(heading);a.space(card,8);
                card.addView(a.text("Fuel: "+decimal(litres)+" L   •   Start: "+decimal(startM)+" km",13,0xff53617a));a.space(card,5);card.addView(a.text(ended?"End: "+decimal(endM)+" km  •  "+date(endDate):"Cycle in progress • end reading pending",13,0xff53617a));a.space(card,7);card.addView(a.bold("Average: "+(ended?average(startM,endM,litres):"—"),15,0xff202b40));a.space(card,4);card.addView(a.text(linked?"Added to expense tracker":"Not added to expense tracker",12,0xff8490a4));
                a.space(card,12);TextView edit=a.secondaryAction("Edit cycle");card.addView(edit);edit.setOnClickListener(v->editCycle(id));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=a.dp(10);a.list.addView(card,lp);
            }
        }
        if(a.list.getChildCount()==0){LinearLayout empty=a.card(Color.WHITE,20);empty.addView(a.bold("Start your first cycle",16,0xff27334a));a.space(empty,6);empty.addView(a.text("Add the date, odometer, litres and total amount paid.",13,0xff8490a4));a.list.addView(empty);}
        a.addNavigation(false);
    }
    void addStat(LinearLayout row,String label,String value,boolean spacing){LinearLayout cell=a.column();cell.setPadding(a.dp(9),a.dp(10),a.dp(9),a.dp(10));cell.setBackground(a.background(0xfff3f1ff,13));cell.addView(a.text(label,10,0xff7f8ba1));a.space(cell,5);TextView amount=label.equals("FUEL")?a.accentText(value,14,0xff65bceb,true):a.bold(value,14,0xff322c70);amount.setMaxLines(2);cell.addView(amount);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);if(spacing)lp.leftMargin=a.dp(7);row.addView(cell,lp);}
    /** Payment can either reference a same-day debit or be entered only in the fuel log. */
    final class PriceChooser {
        final Calendar date;final long cycleId;final Match[] chosen={null};final RadioButton yes,no;final LinearLayout linked,manual;final EditText price;final TextView select,amountLabel;
        PriceChooser(LinearLayout form,Calendar start,long cycleId,long oldExpense,long oldPrice){
            this.date=start;this.cycleId=cycleId;a.space(form,12);form.addView(a.bold("Select from Tracker?",15,0xff202b40));
            RadioGroup options=new RadioGroup(a);options.setOrientation(RadioGroup.HORIZONTAL);yes=new RadioButton(a);no=new RadioButton(a);yes.setId(View.generateViewId());no.setId(View.generateViewId());yes.setText("Yes");no.setText("No");yes.setTextColor(a.isLight()?0xff171717:Color.WHITE);no.setTextColor(a.isLight()?0xff171717:Color.WHITE);options.addView(yes);options.addView(no);form.addView(options);
            linked=a.card(0xfff4f2ff,14);linked.addView(a.bold("Choose a Tracker debit",15,0xff322c70));a.space(linked,4);linked.addView(a.text("Only debits on the fuel date are shown.",12,0xff69758e));a.space(linked,9);
            select=a.action("Choose from Tracker  →",Color.WHITE,0xff534acb);select.setBackground(a.background(0xff383838,12));select.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);linked.addView(select);a.space(linked,8);amountLabel=a.text("Amount will appear here",12,0xff69758e);linked.addView(amountLabel);form.addView(linked);
            manual=a.column();manual.addView(label("TOTAL AMOUNT PAID  ·  ₹"));price=numeric("e.g. 400.20");if(oldPrice>0)price.setText(a.amt(oldPrice));manual.addView(price);a.space(manual,5);manual.addView(a.text(oldExpense!=0?"The existing Tracker transaction stays in Tracker; only the link is removed.":"This amount is saved only in the fuel tracker.",12,0xff78859a));form.addView(manual);
            if(oldExpense!=0){try(Cursor c=store.getReadableDatabase().rawQuery("SELECT title,date,amount,category FROM transactions WHERE _id=? AND kind='Expense'",new String[]{""+oldExpense})){
                if(c.moveToFirst()&&dayStart(c.getLong(1))==dayStart(start.getTimeInMillis())){String desc=c.getString(0)+"  •  "+date(c.getLong(1))+"\n"+a.money(c.getLong(2))+"  •  "+c.getString(3);chosen[0]=new Match(oldExpense,c.getLong(2),c.getLong(1),desc);select.setText(desc);amountLabel.setText("Amount: "+a.money(c.getLong(2)));}
            }}
            select.setOnClickListener(v->selectTransaction(chosen,select,amountLabel,date.getTimeInMillis(),cycleId));
            options.setOnCheckedChangeListener((group,checked)->{boolean useTracker=checked==yes.getId();linked.setVisibility(useTracker?View.VISIBLE:View.GONE);manual.setVisibility(useTracker?View.GONE:View.VISIBLE);});
            options.check((cycleId==0||oldExpense!=0)?yes.getId():no.getId());
        }
        long amount(){
            if(yes.isChecked()){if(chosen[0]==null||dayStart(chosen[0].date)!=dayStart(date.getTimeInMillis()))throw new IllegalArgumentException("Select a debit from the chosen fuel date.");return chosen[0].amount;}
            try{long value=a.parseAmount(price.getText().toString());if(value<=0)throw new IllegalArgumentException();return value;}catch(Exception invalid){throw new IllegalArgumentException("Enter a valid fuel amount greater than zero, with up to 2 decimals.");}
        }
        long expenseId(){return yes.isChecked()?chosen[0].id:0;}
    }
    void addFuel(){
        LinearLayout form=a.column();form.setPadding(a.dp(22),a.dp(22),a.dp(22),a.dp(5));
        form.addView(a.text("FUEL TRACKER",11,0xff756bd2));a.space(form,4);form.addView(a.bold("Add fuel cycle",22,0xff182137));a.space(form,4);
        form.addView(a.text("Record your refuel and payment.",13,0xff7b879d));a.space(form,16);
        Calendar start=dateInput(form,"REFUEL DATE",System.currentTimeMillis());a.space(form,10);
        form.addView(label("ODOMETER  ·  KM"));EditText km=numeric("e.g. 16189");form.addView(km);a.space(form,10);
        form.addView(label("FUEL QUANTITY  ·  LITRES"));EditText litres=numeric("e.g. 5.4");form.addView(litres);a.space(form,4);
        PriceChooser payment=new PriceChooser(form,start,0,0,0);ScrollView scroller=new ScrollView(a);scroller.addView(form);
        AlertDialog d=new AlertDialog.Builder(a).setView(scroller).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{try{
            long odometer=thousandths(km.getText().toString()),qty=thousandths(litres.getText().toString()),amount=payment.amount();save(d,start.getTimeInMillis(),odometer,qty,amount,payment.expenseId());
        }catch(IllegalArgumentException e){Toast.makeText(a,e.getMessage()!=null?e.getMessage():"Enter valid positive odometer and litres.",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(a,"Enter valid positive odometer and litres.",Toast.LENGTH_LONG).show();}}));a.showStyled(d);
    }
    void selectTransaction(Match[] selected,TextView select,TextView amountLabel,long fuelDate,long cycleId){
        ArrayList<Match> choices=new ArrayList<>();
        try(Cursor c=store.getReadableDatabase().rawQuery("SELECT t._id,t.title,t.date,t.amount,t.category FROM transactions t WHERE t.kind='Expense' AND t.amount>0 AND t.date>=? AND t.date<? AND NOT EXISTS (SELECT 1 FROM fuel_cycles f WHERE f.expense_id=t._id AND f._id!=?) ORDER BY t.date DESC,t._id DESC",new String[]{""+dayStart(fuelDate),""+dayEnd(fuelDate),""+cycleId})){
            while(c.moveToNext()){long amount=c.getLong(3);String description=c.getString(1)+"  •  "+date(c.getLong(2))+"\n"+a.money(amount)+"  •  "+c.getString(4);choices.add(new Match(c.getLong(0),amount,c.getLong(2),description));}
        }
        if(choices.isEmpty()){a.showStyled(new AlertDialog.Builder(a).setTitle("No available debits").setMessage("No available debits for "+date(fuelDate)+". Import or add one in Tracker. Debits linked to other fuel cycles are not shown.").setPositiveButton("OK",null));return;}
        LinearLayout picker=a.column();picker.setPadding(a.dp(22),a.dp(22),a.dp(22),a.dp(8));
        picker.addView(a.text("FUEL PAYMENT",11,0xff756bd2));a.space(picker,4);picker.addView(a.bold("Select a debit",22,0xff182137));a.space(picker,4);picker.addView(a.text("Debits for "+date(fuelDate)+" • Search by payee or amount",12,0xff78859a));a.space(picker,10);
        EditText search=new EditText(a);search.setHint("Search transactions");search.setSingleLine(true);a.styleField(search);picker.addView(search);a.space(picker,12);
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(false);LinearLayout results=a.column();scroll.addView(results);picker.addView(scroll,new LinearLayout.LayoutParams(-1,a.dp(380)));
        AlertDialog dialog=new AlertDialog.Builder(a).setView(picker).setNegativeButton("Cancel",null).create();
        Runnable refresh=()->{results.removeAllViews();String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);int shown=0;
            for(Match item:choices){if(!item.label.toLowerCase(Locale.ROOT).contains(query))continue;shown++;
                String[] lines=item.label.split("\\n",2);LinearLayout card=a.card(0xfff5f5fb,14);LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);
                TextView description=a.bold(lines[0],14,0xff202b40);description.setMaxLines(2);row.addView(description,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=a.bold("›",22,0xff6656df);row.addView(arrow);card.addView(row);
                if(lines.length>1){a.space(card,4);card.addView(a.text(lines[1],13,0xff65738a));}
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=a.dp(8);results.addView(card,lp);
                card.setOnClickListener(v->{selected[0]=item;select.setText(item.label);amountLabel.setText("Amount: "+a.money(item.amount));dialog.dismiss();});
            }
            if(shown==0){TextView empty=a.text("No matching transactions found.",14,0xff78859a);empty.setPadding(a.dp(4),a.dp(18),0,0);results.addView(empty);}
        };
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){refresh.run();}public void afterTextChanged(android.text.Editable e){}});
        refresh.run();a.showStyled(dialog);if(dialog.getWindow()!=null)dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);picker.setFocusableInTouchMode(true);picker.requestFocus();
    }
    void save(AlertDialog dialog,long date,long km,long qty,long amount,long expenseId){
        SQLiteDatabase db=store.getWritableDatabase();try{db.beginTransaction();
            try(Cursor c=db.rawQuery("SELECT _id,start_date,start_m,end_date,end_m FROM fuel_cycles ORDER BY start_date DESC,_id DESC LIMIT 1",null)){if(c.moveToFirst()){long priorId=c.getLong(0),priorDate=c.getLong(1),priorKm=c.getLong(2);if(dayStart(date)<dayStart(priorDate)||km<=priorKm)throw new IllegalArgumentException("New refuel must be after the latest cycle and have a higher odometer reading.");if(c.isNull(3)){ContentValues close=new ContentValues();close.put("end_date",date);close.put("end_m",km);db.update("fuel_cycles",close,"_id=? AND end_date IS NULL",new String[]{Long.toString(priorId)});}else if(dayStart(date)<dayStart(c.getLong(3))||km<c.getLong(4))throw new IllegalArgumentException("New refuel cannot be before the previous cycle ended.");}}
            if(amount<=0)throw new IllegalArgumentException("Enter a valid fuel amount.");
            if(expenseId!=0){ContentValues cat=new ContentValues();cat.put("category","Fuel");int changed=db.update("transactions",cat,"_id=? AND kind='Expense' AND amount=? AND date>=? AND date<? AND NOT EXISTS (SELECT 1 FROM fuel_cycles WHERE expense_id=?)",new String[]{Long.toString(expenseId),Long.toString(amount),Long.toString(dayStart(date)),Long.toString(dayEnd(date)),Long.toString(expenseId)});if(changed!=1)throw new IllegalStateException("The selected debit changed or is already linked. Please select it again.");}
            ContentValues f=new ContentValues();f.put("start_date",date);f.put("start_m",km);f.put("litres_ml",qty);f.put("price",amount);if(expenseId!=0)f.put("expense_id",expenseId);db.insertOrThrow("fuel_cycles",null,f);db.setTransactionSuccessful();
        }catch(Exception e){Toast.makeText(a,e instanceof IllegalArgumentException||e instanceof IllegalStateException?e.getMessage():"Could not save fuel cycle.",Toast.LENGTH_LONG).show();return;}finally{if(db.inTransaction())db.endTransaction();}
        dialog.dismiss();render();Toast.makeText(a,"Fuel cycle saved",Toast.LENGTH_LONG).show();
    }
    void editCycle(long id){
        long oldDate,oldKm,oldLitres,oldPrice,oldExpense;
        try(Cursor c=store.getReadableDatabase().rawQuery("SELECT start_date,start_m,litres_ml,price,expense_id FROM fuel_cycles WHERE _id=?",new String[]{Long.toString(id)})){
            if(!c.moveToFirst())return;oldDate=c.getLong(0);oldKm=c.getLong(1);oldLitres=c.getLong(2);oldPrice=c.getLong(3);oldExpense=c.isNull(4)?0:c.getLong(4);
        }
        LinearLayout form=a.column();form.setPadding(a.dp(22),0,a.dp(22),a.dp(5));Calendar start=dateInput(form,"Fuel date",oldDate);
        form.addView(label("Odometer when refuelling (km)"));EditText km=numeric("Odometer");km.setText(decimal(oldKm));form.addView(km);
        form.addView(label("Fuel quantity (litres)"));EditText litres=numeric("Litres");litres.setText(decimal(oldLitres));form.addView(litres);
        PriceChooser payment=new PriceChooser(form,start,id,oldExpense,oldPrice);
        ScrollView scroll=new ScrollView(a);scroll.addView(form);
        AlertDialog d=new AlertDialog.Builder(a).setTitle("Edit fuel cycle").setView(scroll).setNeutralButton("Delete",(ignored,which)->confirmDeleteCycle(id)).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create();
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{try{
            long newKm=thousandths(km.getText().toString()),newLitres=thousandths(litres.getText().toString()),newDate=start.getTimeInMillis();
            long newPrice=payment.amount(),expense=payment.expenseId();
            updateCycle(id,oldDate,oldKm,newDate,newKm,newLitres,newPrice,expense);
            d.dismiss();render();Toast.makeText(a,"Fuel cycle updated",Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(a,e instanceof IllegalArgumentException&&e.getMessage()!=null?e.getMessage():"Enter valid readings and select a debit.",Toast.LENGTH_LONG).show();}}));a.showStyled(d);
    }
    void confirmDeleteCycle(long id){
        a.showStyled(new AlertDialog.Builder(a).setTitle("Delete fuel cycle?").setMessage("The fuel cycle will be removed. Its linked Tracker transaction will stay in your expenses.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(dialog,which)->deleteCycle(id)));
    }
    void deleteCycle(long id){
        SQLiteDatabase db=store.getWritableDatabase();db.beginTransaction();
        try{
            long start,km;Long end=null,endKm=null;
            try(Cursor c=db.rawQuery("SELECT start_date,start_m,end_date,end_m FROM fuel_cycles WHERE _id=?",new String[]{""+id})){
                if(!c.moveToFirst())throw new IllegalArgumentException("Fuel cycle no longer exists.");start=c.getLong(0);km=c.getLong(1);if(!c.isNull(2)&&!c.isNull(3)){end=c.getLong(2);endKm=c.getLong(3);}
            }
            long prior=0;boolean linked=false;
            try(Cursor c=db.rawQuery("SELECT _id,end_date,end_m FROM fuel_cycles WHERE start_date<? OR (start_date=? AND _id<?) ORDER BY start_date DESC,_id DESC LIMIT 1",new String[]{""+start,""+start,""+id})){
                if(c.moveToFirst()){prior=c.getLong(0);linked=!c.isNull(1)&&!c.isNull(2)&&c.getLong(1)==start&&c.getLong(2)==km;}
            }
            long nextDate=0,nextKm=0;boolean hasNext=false;
            try(Cursor c=db.rawQuery("SELECT start_date,start_m FROM fuel_cycles WHERE start_date>? OR (start_date=? AND _id>?) ORDER BY start_date,_id LIMIT 1",new String[]{""+start,""+start,""+id})){
                if(c.moveToFirst()){hasNext=true;nextDate=c.getLong(0);nextKm=c.getLong(1);}
            }
            if(linked){
                if(hasNext&&(end==null||end.longValue()!=nextDate||endKm.longValue()!=nextKm))throw new IllegalArgumentException("This cycle has a manually set end. Delete the latest cycle first.");
                ContentValues v=new ContentValues();if(hasNext){v.put("end_date",nextDate);v.put("end_m",nextKm);}else{v.putNull("end_date");v.putNull("end_m");}db.update("fuel_cycles",v,"_id=?",new String[]{""+prior});
            }
            if(db.delete("fuel_cycles","_id=?",new String[]{""+id})!=1)throw new IllegalStateException();db.setTransactionSuccessful();
        }catch(Exception e){Toast.makeText(a,e instanceof IllegalArgumentException?e.getMessage():"Could not delete the cycle.",Toast.LENGTH_LONG).show();return;}finally{db.endTransaction();}
        render();Toast.makeText(a,"Fuel cycle deleted. Tracker transaction kept.",Toast.LENGTH_LONG).show();
    }
    void updateCycle(long id,long oldDate,long oldKm,long newDate,long newKm,long litres,long price,long expenseId){
        SQLiteDatabase db=store.getWritableDatabase();db.beginTransaction();
        try{
            long previousId=0,previousDate=0,previousKm=0,previousEndDate=0,previousEndKm=0;boolean previousEnded=false;
            try(Cursor c=db.rawQuery("SELECT _id,start_date,start_m,end_date,end_m FROM fuel_cycles WHERE start_date<? OR (start_date=? AND _id<?) ORDER BY start_date DESC,_id DESC LIMIT 1",new String[]{""+oldDate,""+oldDate,""+id})){
                if(c.moveToFirst()){previousId=c.getLong(0);previousDate=c.getLong(1);previousKm=c.getLong(2);previousEnded=!c.isNull(3)&&!c.isNull(4);if(previousEnded){previousEndDate=c.getLong(3);previousEndKm=c.getLong(4);}}
            }
            long nextId=0,nextDate=0,nextKm=0;
            try(Cursor c=db.rawQuery("SELECT _id,start_date,start_m FROM fuel_cycles WHERE start_date>? OR (start_date=? AND _id>?) ORDER BY start_date,_id LIMIT 1",new String[]{""+oldDate,""+oldDate,""+id})){
                if(c.moveToFirst()){nextId=c.getLong(0);nextDate=c.getLong(1);nextKm=c.getLong(2);}
            }
            if(previousId!=0&&(newDate<previousDate||(newDate==previousDate&&id<previousId)||newKm<=previousKm))throw new IllegalArgumentException("Date and odometer must be after the previous refuel.");
            if(nextId!=0&&(newDate>nextDate||(newDate==nextDate&&id>nextId)||newKm>=nextKm))throw new IllegalArgumentException("Date and odometer must be before the next refuel.");
            boolean previousAuto=previousEnded&&previousEndDate==oldDate&&previousEndKm==oldKm;
            if(previousEnded&&!previousAuto&&(dayStart(newDate)<dayStart(previousEndDate)||newKm<previousEndKm))throw new IllegalArgumentException("This refuel must be after the previous cycle ended.");
            try(Cursor c=db.rawQuery("SELECT end_date,end_m FROM fuel_cycles WHERE _id=?",new String[]{""+id})){
                if(!c.moveToFirst())throw new IllegalArgumentException("This fuel cycle no longer exists.");
                if(!c.isNull(0)&&!c.isNull(1)&&(dayStart(newDate)>dayStart(c.getLong(0))||newKm>c.getLong(1)))throw new IllegalArgumentException("The start must be before this cycle's ending date and odometer.");
            }
            if(expenseId!=0){ContentValues cat=new ContentValues();cat.put("category","Fuel");int changed=db.update("transactions",cat,"_id=? AND kind='Expense' AND amount=? AND date>=? AND date<? AND NOT EXISTS (SELECT 1 FROM fuel_cycles WHERE expense_id=? AND _id!=?)",new String[]{""+expenseId,""+price,""+dayStart(newDate),""+dayEnd(newDate),""+expenseId,""+id});if(changed!=1)throw new IllegalArgumentException("The selected debit changed or is unavailable. Select it again.");}
            ContentValues values=new ContentValues();values.put("start_date",newDate);values.put("start_m",newKm);values.put("litres_ml",litres);values.put("price",price);if(expenseId!=0)values.put("expense_id",expenseId);else values.putNull("expense_id");db.update("fuel_cycles",values,"_id=?",new String[]{""+id});
            if(previousAuto){ContentValues end=new ContentValues();end.put("end_date",newDate);end.put("end_m",newKm);db.update("fuel_cycles",end,"_id=?",new String[]{""+previousId});}
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
}
