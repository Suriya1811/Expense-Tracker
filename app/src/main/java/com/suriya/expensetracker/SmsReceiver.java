package com.suriya.expensetracker;
import android.content.*;
import android.provider.Telephony;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        SmsMessage[] parts=Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if(parts == null || parts.length == 0) return;
        StringBuilder text=new StringBuilder(); for(SmsMessage p:parts) text.append(p.getMessageBody());
        final String sender=parts[0].getOriginatingAddress(),body=text.toString();final long received=parts[0].getTimestampMillis();final Context app=context.getApplicationContext();final PendingResult pending=goAsync();
        ExpenseWidget.execute(()->{try(Store store=new Store(app)){if(store.importSms(sender,body,received))ExpenseWidget.updateAllNow(app);}finally{pending.finish();}});
    }
}
