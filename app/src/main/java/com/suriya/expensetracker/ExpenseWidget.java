package com.suriya.expensetracker;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A current-month summary; no widget configuration or data leaves the device. */
public final class ExpenseWidget extends AppWidgetProvider {
    static final String ACTION_ADD="com.suriya.expensetracker.action.ADD_FROM_WIDGET";
    static final String ACTION_HOME="com.suriya.expensetracker.action.HOME_FROM_WIDGET";
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
    private static int id(Context context,String type,String name){return context.getResources().getIdentifier(name,type,context.getPackageName());}
    private static String money(long paise){return "₹"+String.format(Locale.US,"%,d.%02d",paise/100,Math.abs(paise%100));}
    private static Intent destination(Context context,String action){return new Intent(context,MainActivity.class).setAction(action).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);}

    @Override public void onReceive(Context context,Intent intent){
        if(AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())){
            final PendingResult pending=goAsync();final Context app=context.getApplicationContext();
            WORK.execute(()->{try{updateAllNow(app);}finally{pending.finish();}});
        }else super.onReceive(context,intent);
    }
    @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){requestUpdate(context);}
    static void execute(Runnable task){WORK.execute(task);}
    static void requestUpdate(Context context){
        final Context app=context.getApplicationContext();AppWidgetManager manager=AppWidgetManager.getInstance(app);
        if(manager.getAppWidgetIds(new ComponentName(app,ExpenseWidget.class)).length==0)return;
        WORK.execute(()->updateAllNow(app));
    }
    static void updateAllNow(Context context){
        AppWidgetManager manager=AppWidgetManager.getInstance(context);int[] widgets=manager.getAppWidgetIds(new ComponentName(context,ExpenseWidget.class));if(widgets.length==0)return;
        Calendar start=Calendar.getInstance();start.set(Calendar.DAY_OF_MONTH,1);start.set(Calendar.HOUR_OF_DAY,0);start.set(Calendar.MINUTE,0);start.set(Calendar.SECOND,0);start.set(Calendar.MILLISECOND,0);Calendar end=(Calendar)start.clone();end.add(Calendar.MONTH,1);
        long credit=0,debit=0,personal=0;boolean loaded=false;
        try(Store store=new Store(context);Cursor c=store.getReadableDatabase().rawQuery("SELECT kind,COALESCE(SUM(amount),0) FROM transactions WHERE date>=? AND date<? GROUP BY kind",new String[]{""+start.getTimeInMillis(),""+end.getTimeInMillis()})){
            while(c.moveToNext()){if("Income".equals(c.getString(0)))credit=c.getLong(1);else if("Expense".equals(c.getString(0)))debit=c.getLong(1);}personal=store.personalSpent(start.getTimeInMillis(),end.getTimeInMillis(),new SimpleDateFormat("yyyy-MM",Locale.US).format(start.getTime()));credit+=new SalarySettings(context,store).extraCredit(SalarySettings.period(start));loaded=true;
        }catch(Exception ignored){/* Display an unavailable state rather than misleading zeroes. */}
        android.content.SharedPreferences prefs=context.getSharedPreferences("profile",Context.MODE_PRIVATE);
        String monthKey=new SimpleDateFormat("yyyy-MM",Locale.US).format(start.getTime());long limit=prefs.getLong("limit_"+monthKey,0);long net=credit-debit;
        for(int widget:widgets){
            RemoteViews views=new RemoteViews(context.getPackageName(),id(context,"layout","expense_widget"));
            views.setTextViewText(id(context,"id","widget_month"),new SimpleDateFormat("MMMM yyyy",Locale.ENGLISH).format(start.getTime()));
            views.setTextViewText(id(context,"id","widget_credit"),loaded?money(credit):"—");views.setTextViewText(id(context,"id","widget_debit"),loaded?money(debit):"—");views.setTextViewText(id(context,"id","widget_net"),loaded?(net<0?"−":"")+money(Math.abs(net)):"—");
            String budget=!loaded?"Unable to load data":limit<=0?"No limit set":personal>=limit?(personal==limit?"Limit reached":"Over limit by "+money(personal-limit)):money(limit-personal)+" left of "+money(limit);
            views.setTextViewText(id(context,"id","widget_limit"),budget);views.setProgressBar(id(context,"id","widget_progress"),100,loaded&&limit>0?(int)Math.min(100,(double)personal*100/limit):0,false);
            PendingIntent home=PendingIntent.getActivity(context,0,destination(context,ACTION_HOME),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            PendingIntent add=PendingIntent.getActivity(context,1,destination(context,ACTION_ADD),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(id(context,"id","widget_root"),home);views.setOnClickPendingIntent(id(context,"id","widget_add"),add);manager.updateAppWidget(widget,views);
        }
    }
}
