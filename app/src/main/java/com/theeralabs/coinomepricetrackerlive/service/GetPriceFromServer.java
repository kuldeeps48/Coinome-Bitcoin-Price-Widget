package com.theeralabs.coinomepricetrackerlive.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.theeralabs.coinomepricetrackerlive.CoinomeAppWidgetProvider;
import com.theeralabs.coinomepricetrackerlive.R;
import com.theeralabs.coinomepricetrackerlive.api.ApiClient;
import com.theeralabs.coinomepricetrackerlive.api.ApiInterface;
import com.theeralabs.coinomepricetrackerlive.model.BTCINR;
import com.theeralabs.coinomepricetrackerlive.model.PriceData;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.ContentValues.TAG;


public class GetPriceFromServer extends Service {
    private static final long FUTURE_UPDATE_TIME_IN_MILLISEC = 300_000;
    private static final String PREF_NAME = "LastPrice";

    private void setFutureUpdate(Context context) {

        AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
        assert alarmManager != null;
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + FUTURE_UPDATE_TIME_IN_MILLISEC,
                getPendingButtonClickIntent(context));
        Log.d(TAG, "setFutureUpdate: Alarm set for " +
                SystemClock.elapsedRealtime() + FUTURE_UPDATE_TIME_IN_MILLISEC);
    }

    private PendingIntent getPendingButtonClickIntent(Context context) {
        Intent intent = new Intent(context, CoinomeAppWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context, CoinomeAppWidgetProvider.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

        return PendingIntent.getBroadcast(context, 7, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void cancelAlarmSetToUpdateInFuture(Context context) {
        AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
        assert alarmManager != null;
        alarmManager.cancel(getPendingButtonClickIntent(context));
        Log.d(TAG, "cancelAlarmSetToUpdateInFuture: Cancelled Alarm...");
    }

    @Override
    public void onCreate() {
        startForeground(776, new Notification());

        //Make progressbar visible
        RemoteViews remoteViews = new RemoteViews(getApplication().getPackageName(), R.layout.coinome_appwidget);
        remoteViews.setViewVisibility(R.id.progressbar, View.VISIBLE);
        //Show on widget
        AppWidgetManager mgr = AppWidgetManager.getInstance(getApplicationContext());
        ComponentName cn = new ComponentName(getApplicationContext(), CoinomeAppWidgetProvider.class);
        mgr.updateAppWidget(cn, remoteViews);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service Started...");
        cancelAlarmSetToUpdateInFuture(getApplicationContext());

        new CheckIfOnline().execute();

        setFutureUpdate(getApplicationContext());
        return START_STICKY;
    }

    class CheckIfOnline extends AsyncTask<String, String, Boolean> {

        @Override
        protected Boolean doInBackground(String... strings) {
            try {
                int timeoutMs = 1500;
                Socket sock = new Socket();
                SocketAddress sockaddr = new InetSocketAddress("8.8.8.8", 53);

                sock.connect(sockaddr, timeoutMs);
                sock.close();
                Log.d(TAG, "doInBackground: Check dns");

                return true;
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean) {
                Log.d(TAG, "onPostExecute: StartRetrofit");
                startRetrofitRequest();
            }
            else {
                Log.d(TAG, "onPostExecute: no connection");
                noConnection();
            }
        }
    }

    private void noConnection() {
        RemoteViews remoteViews = new RemoteViews(getApplication().getPackageName(), R.layout.coinome_appwidget);
        //Remove progressbar
        remoteViews.setViewVisibility(R.id.progressbar, View.GONE);
        //Update widget
        AppWidgetManager mgr = AppWidgetManager.getInstance(getApplicationContext());
        ComponentName cn = new ComponentName(getApplicationContext(), CoinomeAppWidgetProvider.class);
        mgr.updateAppWidget(cn, remoteViews);

        cancelAlarmSetToUpdateInFuture(getApplicationContext());
        setFutureUpdate(getApplicationContext());

        stopSelf();
    }

    private void startRetrofitRequest() {
        final ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<PriceData> call = apiInterface.getPriceData();
        call.enqueue(new Callback<PriceData>() {
            @Override
            public void onResponse(@NonNull Call<PriceData> call, @NonNull Response<PriceData> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Got Response...");

                    BTCINR btcinr = response.body().getBTCINR();

                    float curBuy = Float.parseFloat(btcinr.getHighestBid());
                    float curSell = Float.parseFloat(btcinr.getLowestAsk());


                    NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
                    format.setCurrency(Currency.getInstance("IND"));

                    RemoteViews remoteViews = new RemoteViews(getApplication().getPackageName(), R.layout.coinome_appwidget);
                    remoteViews.setTextViewText(R.id.txt_buy_price,
                            "₹ " + format.format(curBuy).substring(4));
                    remoteViews.setTextViewText(R.id.txt_sell_price,
                            "₹ " + format.format(curSell).substring(4));
                    //Remove progressbar
                    remoteViews.setViewVisibility(R.id.progressbar, View.GONE);

                    SharedPreferences lastPrices = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                    float lastBuy = lastPrices.getFloat("buy", 0);
                    float lastSell = lastPrices.getFloat("sell", 0);

                    if (curBuy - lastBuy > 0) {
                        remoteViews.setImageViewResource(R.id.img_buy_status, R.drawable.up_green_arrow);
                    } else
                        remoteViews.setImageViewResource(R.id.img_buy_status, R.drawable.down_red_arrow);

                    if (curSell - lastSell > 0) {
                        remoteViews.setImageViewResource(R.id.img_sell_status, R.drawable.up_green_arrow);
                    } else
                        remoteViews.setImageViewResource(R.id.img_sell_status, R.drawable.down_red_arrow);


                    saveCurrentPrice(btcinr);
                    //Update widget
                    AppWidgetManager mgr = AppWidgetManager.getInstance(getApplicationContext());
                    ComponentName cn = new ComponentName(getApplicationContext(), CoinomeAppWidgetProvider.class);
                    mgr.updateAppWidget(cn, remoteViews);
                    Log.d(TAG, "Finished Response...");

                    stopSelf();
                } else {
                    RemoteViews remoteViews = new RemoteViews(getApplication().getPackageName(), R.layout.coinome_appwidget);
                    remoteViews.setTextViewText(R.id.txt_buy_price, "No Response");
                    remoteViews.setTextViewText(R.id.txt_sell_price, "No Response");
                    //Remove progressbar
                    remoteViews.setViewVisibility(R.id.progressbar, View.GONE);

                    //Update widget
                    AppWidgetManager mgr = AppWidgetManager.getInstance(getApplicationContext());
                    ComponentName cn = new ComponentName(getApplicationContext(), CoinomeAppWidgetProvider.class);
                    mgr.updateAppWidget(cn, remoteViews);

                    stopSelf();
                }

            }

            private void saveCurrentPrice(BTCINR btcinr) {
                SharedPreferences prices = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = prices.edit();
                editor.clear()
                        .putFloat("buy", Float.parseFloat(btcinr.getHighestBid()))
                        .putFloat("sell", Float.parseFloat(btcinr.getLowestAsk()))
                        .apply();
            }

            @Override
            public void onFailure(@NonNull Call<PriceData> call, @NonNull Throwable t) {
                Log.d(TAG, "Failed Response...");
                RemoteViews remoteViews = new RemoteViews(getApplication().getPackageName(), R.layout.coinome_appwidget);
                remoteViews.setTextViewText(R.id.txt_buy_price, "No Response");
                remoteViews.setTextViewText(R.id.txt_sell_price, "No Response");
                //Remove progressbar
                remoteViews.setViewVisibility(R.id.progressbar, View.GONE);

                //Update widget
                AppWidgetManager mgr = AppWidgetManager.getInstance(getApplicationContext());
                ComponentName cn = new ComponentName(getApplicationContext(), CoinomeAppWidgetProvider.class);
                mgr.updateAppWidget(cn, remoteViews);

                stopSelf();
            }
        });
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
