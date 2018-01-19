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
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import com.theeralabs.coinomepricetrackerlive.service.GetPriceFromServer;

import java.util.Objects;

import static android.content.Context.ALARM_SERVICE;

public class CoinomeAppWidgetProvider extends AppWidgetProvider {

    private static final String TAG = CoinomeAppWidgetProvider.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Bundle extras = intent.getExtras();
        if(extras!=null) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = new ComponentName(context.getPackageName(),
                    CoinomeAppWidgetProvider.class.getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        Log.d(TAG, "Updating widget...");

        for (int appWidgetId : appWidgetIds) {
            //Add Refresh button listener
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.coinome_appwidget);
            remoteViews.setOnClickPendingIntent(R.id.btn_refresh,
                    getPendingButtonClickIntent(context));

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

            //Start a service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.getApplicationContext().startForegroundService(new Intent(context, GetPriceFromServer.class));
            } else
                context.getApplicationContext().startService(new Intent(context, GetPriceFromServer.class));
        }
    }


    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        context.getApplicationContext().registerReceiver(screenOnReceiver,filter);
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
        try {
            context.getApplicationContext().unregisterReceiver(screenOnReceiver);
        } catch (Exception e) {
            Log.d(TAG, "onDeleted: ScreenOn Broadcast Receiver not found");
        }
        Log.d(TAG, "onDeleted: Cancelled Future Alarm...");
        super.onDeleted(context, appWidgetIds);
    }

    BroadcastReceiver screenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), "android.intent.action.SCREEN_ON")) {
                cancelJobSetToUpdateInFuture(context);

                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName thisAppWidget = new ComponentName(context.getPackageName(),
                        CoinomeAppWidgetProvider.class.getName());
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

                onUpdate(context, appWidgetManager, appWidgetIds);
            }
        }
    };


    private PendingIntent getPendingButtonClickIntent(Context context) {
        Intent intent = new Intent(context, CoinomeAppWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context, CoinomeAppWidgetProvider.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

        return PendingIntent.getBroadcast(context, 7, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void cancelJobSetToUpdateInFuture(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getApplicationContext().getSystemService(ALARM_SERVICE);
        assert alarmManager != null;
        alarmManager.cancel(getPendingButtonClickIntent(context));

        Log.d(TAG, "cancelJobSetToUpdateInFuture: Cancelled Alarm...");
    }

}