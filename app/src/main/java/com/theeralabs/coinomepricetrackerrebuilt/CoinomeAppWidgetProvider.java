package com.theeralabs.coinomepricetrackerrebuilt;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import com.theeralabs.coinomepricetrackerrebuilt.service.GetPriceFromServer;

import java.util.Objects;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;

public class CoinomeAppWidgetProvider extends AppWidgetProvider {

    private static final String TAG = CoinomeAppWidgetProvider.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive...");
        super.onReceive(context, intent);

    }

    private String getCoin(Context context, int mAppWidgetId) {
        SharedPreferences coinFile = context.getSharedPreferences("" + mAppWidgetId, MODE_PRIVATE);
        return coinFile.getString("" + mAppWidgetId, "");

    }


    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        Log.d(TAG, "onUpdate...");
        for (int appWidgetId : appWidgetIds) {
            String coin = getCoin(context, appWidgetId);
            //Add Refresh button listener
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.coinome_appwidget);

            switch (coin) {
                case "bitcoin":
                    Log.d(TAG, "coin => " + coin);
                    setTextAndIcon(R.drawable.bitcoin_icon, "BTC/INR", remoteViews);
                    break;
                case "litecoin":
                    Log.d(TAG, "coin => " + coin);
                    setTextAndIcon(R.drawable.litecoin_icon, "LTC/INR", remoteViews);
                    break;
                case "bitcoinCash":
                    Log.d(TAG, "coin => " + coin);
                    setTextAndIcon(R.drawable.bitcoin_cash_icon, "BCH/INR", remoteViews);
                    break;
                case "dash":
                    Log.d(TAG, "coin => " + coin);
                    setTextAndIcon(R.drawable.dash_icon, "DSH/INR", remoteViews);
                    break;
                default:
                    Log.d(TAG, "coin => NO COIN!");
                    return;
            }

            remoteViews.setOnClickPendingIntent(R.id.btn_refresh,
                    getPendingButtonClickIntent(context, new int[]{appWidgetId}));
            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

            //Start a service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent intent = new Intent(context, GetPriceFromServer.class);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
                context.getApplicationContext().startForegroundService(intent);
            } else {
                Intent intent = new Intent(context, GetPriceFromServer.class);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
                context.getApplicationContext().startService(intent);
            }
        }
    }

    private void setTextAndIcon(int icon, String title, RemoteViews views) {
        views.setImageViewResource(R.id.img_cointype, icon);
        views.setTextViewText(R.id.txt_cointype, title);
    }


    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "onEnable...");
        super.onEnabled(context);
        //IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        //context.getApplicationContext().registerReceiver(screenOnReceiver, filter);
    }

    @Override
    public void onDisabled(Context context) {
        Log.d(TAG, "onDisable...");
        context.getApplicationContext().stopService(new Intent(context, GetPriceFromServer.class));
        super.onDisabled(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d(TAG, "onDelete...");
        cancelJobSetToUpdateInFuture(context, appWidgetIds);
        /*
        try {
            context.getApplicationContext().unregisterReceiver(screenOnReceiver);
        } catch (Exception e) {
            Log.d(TAG, "onDeleted: ScreenOn Broadcast Receiver not found");
        }
        */
        Log.d(TAG, "onDeleted: Cancelled Future Alarm...");
        super.onDeleted(context, appWidgetIds);
    }

    BroadcastReceiver screenOnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), "android.intent.action.SCREEN_ON")) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName thisAppWidget = new ComponentName(context.getPackageName(),
                        CoinomeAppWidgetProvider.class.getName());
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

                cancelJobSetToUpdateInFuture(context, appWidgetIds);

                onUpdate(context, appWidgetManager, appWidgetIds);
            }
        }
    };


    private PendingIntent getPendingButtonClickIntent(Context context, int[] appWidgetId) {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null,
                context, CoinomeAppWidgetProvider.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetId);

        return PendingIntent.getBroadcast(context, appWidgetId[0], intent, 0);
    }

    private void cancelJobSetToUpdateInFuture(Context context, int[] appWidgetIds) {
        AlarmManager alarmManager = (AlarmManager) context.getApplicationContext().getSystemService(ALARM_SERVICE);
        assert alarmManager != null;
        alarmManager.cancel(getPendingButtonClickIntent(context, appWidgetIds));

        Log.d(TAG, "cancelJobSetToUpdateInFuture: Cancelled Alarm...");
    }

}