package com.theeralabs.coinomepricetrackerlive;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import com.theeralabs.coinomepricetrackerlive.service.GetPriceFromServer;

import java.util.Objects;

import static android.content.Context.ALARM_SERVICE;

public class CoinomeAppWidgetProvider extends AppWidgetProvider {

    private static final String TAG = CoinomeAppWidgetProvider.class.getSimpleName();
    private static final String UPDATE = "android.appwidget.action.APPWIDGET_UPDATE";

    @Override
    public void onReceive(Context context, Intent intent) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        context.getApplicationContext().registerReceiver(screenOnReceiver,filter);

        if (Objects.equals(intent.getAction(), UPDATE)) {
            setButton(context);
            cancelJobSetToUpdateInFuture(context);
            scheduleNow(context);
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        Log.d(TAG, "Updating widget...");
        //Refresh button listener
        setButton(context);
    }


    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getApplicationContext().startForegroundService(new Intent(context, GetPriceFromServer.class));
        } else
            context.getApplicationContext().startService(new Intent(context, GetPriceFromServer.class));
    }


    @Override
    public void onDisabled(Context context) {
        cancelJobSetToUpdateInFuture(context);
        Log.d(TAG, "onDeleted: Cancelled Future Alarm...");
        super.onDisabled(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        cancelJobSetToUpdateInFuture(context);
        context.getApplicationContext().unregisterReceiver(screenOnReceiver);
        Log.d(TAG, "onDeleted: Cancelled Future Alarm...");
        super.onDeleted(context, appWidgetIds);
    }

    BroadcastReceiver screenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), "android.intent.action.SCREEN_ON")) {
                setButton(context);
                cancelJobSetToUpdateInFuture(context);
                scheduleNow(context);
            }
        }
    };

    private void setButton(Context context) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.coinome_appwidget);
        remoteViews.setOnClickPendingIntent(R.id.btn_refresh, getPendingButtonClickIntent(context, UPDATE));

        //Update widget
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        ComponentName cn = new ComponentName(context, CoinomeAppWidgetProvider.class);
        mgr.updateAppWidget(cn, remoteViews);
    }

    private void scheduleNow(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getApplicationContext().startForegroundService(new Intent(context, GetPriceFromServer.class));
        } else
            context.getApplicationContext().startService(new Intent(context, GetPriceFromServer.class));
    }

    private PendingIntent getPendingButtonClickIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 7, intent, 0);
    }

    private void cancelJobSetToUpdateInFuture(Context context) {
        Intent intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                1, intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getApplicationContext().getSystemService(ALARM_SERVICE);
        assert alarmManager != null;
        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "cancelJobSetToUpdateInFuture: Cancelled Alarm...");
    }

}