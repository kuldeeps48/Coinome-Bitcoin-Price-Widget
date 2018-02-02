package com.theeralabs.coinomepricetrackerrebuilt.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.theeralabs.coinomepricetrackerrebuilt.CoinomeAppWidgetProvider;
import com.theeralabs.coinomepricetrackerrebuilt.R;
import com.theeralabs.coinomepricetrackerrebuilt.api.ApiClient;
import com.theeralabs.coinomepricetrackerrebuilt.api.ApiInterface;
import com.theeralabs.coinomepricetrackerrebuilt.model.Coin;
import com.theeralabs.coinomepricetrackerrebuilt.model.PriceData;

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
    private static final long FUTURE_UPDATE_TIME_IN_MILLISEC = 60_000;
    private static final String PREF_NAME = "LastPrice";
    private static final int ON_GOING_NOTIF = 776;
    public static final String CHANNEL_ID = String.valueOf(127);
    private static AppWidgetManager mgr;
    private static RemoteViews remoteViews;

    private void setFutureUpdate(Context context, String coin, int appWidgetID) {
        AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
        assert alarmManager != null;
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + FUTURE_UPDATE_TIME_IN_MILLISEC,
                getPendingButtonClickIntent(context, coin, appWidgetID));
        Log.d(TAG, "setFutureUpdate: Alarm set for " +
                SystemClock.elapsedRealtime() + FUTURE_UPDATE_TIME_IN_MILLISEC);
    }

    private PendingIntent getPendingButtonClickIntent(Context context, String coin, int appWidgetID) {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null,
                context, CoinomeAppWidgetProvider.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetID});

        return PendingIntent.getBroadcast(context, appWidgetID, intent, 0);
    }

    private void cancelAlarmSetToUpdateInFuture(Context context, String coin, int appWidgetID) {
        AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
        assert alarmManager != null;
        alarmManager.cancel(getPendingButtonClickIntent(context, coin, appWidgetID));
        Log.d(TAG, "cancelAlarmSetToUpdateInFuture: Cancelled Alarm...");
    }

    private String getCoin(Context context, int mAppWidgetId) {
        SharedPreferences coinFile = context.getSharedPreferences("" + mAppWidgetId, MODE_PRIVATE);
        return coinFile.getString("" + mAppWidgetId, "");

    }

    void createNotificationStartForeground() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            startForeground(ON_GOING_NOTIF, new Notification());

        } else {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            String channelName = "Network Channel";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID,
                    channelName, importance);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);
            Notification notification = new Notification.Builder(getApplicationContext(),
                    CHANNEL_ID)
                    .setVisibility(Notification.VISIBILITY_SECRET)
                    .setSubText("")
                    .setSmallIcon(R.drawable.notification_small_icon)
                    .setOngoing(true)
                    .build();
            startForeground(ON_GOING_NOTIF, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand...");
        createNotificationStartForeground();

        remoteViews = new RemoteViews(getPackageName(), R.layout.coinome_appwidget);
        mgr = AppWidgetManager.getInstance(getApplicationContext());

        String coin = "";
        int appWidgetID = 0;

        if (intent != null) {
            int[] appWidgetIDS = intent.getExtras().getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS);
            if (appWidgetIDS != null) {
                appWidgetID = appWidgetIDS[0];
                coin = getCoin(getApplicationContext(), appWidgetID);
            }

            if (!coin.isEmpty()) {
                cancelAlarmSetToUpdateInFuture(getApplicationContext(), coin, appWidgetID);
                new CheckIfOnline(coin, appWidgetID).execute();
                setFutureUpdate(getApplicationContext(), coin, appWidgetID);
            }
        }
        return START_REDELIVER_INTENT;
    }

    class CheckIfOnline extends AsyncTask<String, String, Boolean> {
        String coin;
        int appWidgetID;

        CheckIfOnline(String coin, int appWidgetID) {
            super();
            this.coin = coin;
            this.appWidgetID = appWidgetID;
        }

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
        protected void onPostExecute(Boolean online) {
            super.onPostExecute(online);
            if (online) {
                Log.d(TAG, "onPostExecute: Online, Fetch data");
                //Make progressbar visible
                remoteViews.setViewVisibility(R.id.progressbar, View.VISIBLE);
                //Show on widget
                mgr.updateAppWidget(appWidgetID, remoteViews);
                startRetrofitRequest(coin, appWidgetID);
            } else {
                Log.d(TAG, "onPostExecute: Offline");
                noConnection(coin, appWidgetID);
            }
        }
    }

    private void noConnection(String coin, int appWidgetID) {
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.coinome_appwidget);
        //Remove progressbar
        remoteViews.setViewVisibility(R.id.progressbar, View.INVISIBLE);
        //Update widget
        AppWidgetManager mgr = AppWidgetManager.getInstance(this);
        ComponentName cn = new ComponentName(this, CoinomeAppWidgetProvider.class);
        mgr.updateAppWidget(cn, remoteViews);

        cancelAlarmSetToUpdateInFuture(getApplicationContext(), coin, appWidgetID);
        setFutureUpdate(getApplicationContext(), coin, appWidgetID);
    }

    private void startRetrofitRequest(final String coin, final int appWidgetID) {
        final ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
        Call<PriceData> call = apiInterface.getPriceData();
        call.enqueue(new Callback<PriceData>() {
            @Override
            public void onResponse(@NonNull Call<PriceData> call, @NonNull Response<PriceData> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Got Response for " + coin);


                    PriceData priceData = response.body();
                    if (priceData == null) {
                        onFailure(call, new Throwable("No priceData received"));
                        return;
                    }

                    Coin coinINR;
                    switch (coin) {
                        case "bitcoin":
                            coinINR = priceData.getBTCINR();
                            break;
                        case "bitcoinCash":
                            coinINR = priceData.getBCHINR();
                            break;
                        case "litecoin":
                            coinINR = priceData.getLTCINR();
                            break;
                        case "dash":
                            coinINR = priceData.getDASHINR();
                            break;
                        default:
                            onFailure(call, new Throwable("Coin not found"));
                            return;
                    }

                    float curBuy;
                    float curSell;
                    try {
                        curBuy = Float.parseFloat(coinINR.getHighestBid());
                        curSell = Float.parseFloat(coinINR.getLowestAsk());
                    } catch (NullPointerException e) {
                        onFailure(call, new Throwable("No Price found"));
                        return;
                    }


                    NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
                    try {
                        format.setCurrency(Currency.getInstance("IND"));
                    } catch (IllegalArgumentException e) {
                        format.setCurrency(Currency.getInstance(Locale.US));
                    }

                    remoteViews.setTextViewText(R.id.txt_buy_price,
                            "₹ " + format.format(curBuy).substring(3));
                    remoteViews.setTextViewText(R.id.txt_sell_price,
                            "₹ " + format.format(curSell).substring(3));


                    SharedPreferences lastPrices = getSharedPreferences("" + coin + PREF_NAME, MODE_PRIVATE);
                    float lastBuy = lastPrices.getFloat("buy", 0.0f);
                    float lastSell = lastPrices.getFloat("sell", 0.0f);

                    if (curBuy - lastBuy > 0) {
                        remoteViews.setImageViewResource(R.id.img_buy_status, R.drawable.up_green_arrow);
                    } else
                        remoteViews.setImageViewResource(R.id.img_buy_status, R.drawable.down_red_arrow);

                    if (curSell - lastSell > 0) {
                        remoteViews.setImageViewResource(R.id.img_sell_status, R.drawable.up_green_arrow);
                    } else
                        remoteViews.setImageViewResource(R.id.img_sell_status, R.drawable.down_red_arrow);


                    saveCurrentPrice(coinINR);

                    //Update widget
                    //Remove progressbar
                    remoteViews.setViewVisibility(R.id.progressbar, View.INVISIBLE);
                    mgr.updateAppWidget(appWidgetID, remoteViews);

                    Log.d(TAG, "Finished Response...");

                } else {
                    remoteViews.setTextViewText(R.id.txt_buy_price, "No Response");
                    remoteViews.setTextViewText(R.id.txt_sell_price, "No Response");
                    //Remove progressbar
                    remoteViews.setViewVisibility(R.id.progressbar, View.INVISIBLE);

                    //Update widget
                    mgr.updateAppWidget(appWidgetID, remoteViews);
                }

            }

            private void saveCurrentPrice(Coin coinINR) {
                SharedPreferences prices = getSharedPreferences(coin + PREF_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = prices.edit();
                editor.clear()
                        .putFloat("buy", Float.parseFloat(coinINR.getHighestBid()))
                        .putFloat("sell", Float.parseFloat(coinINR.getLowestAsk()))
                        .apply();
            }

            @Override
            public void onFailure(@NonNull Call<PriceData> call, @NonNull Throwable t) {
                Log.d(TAG, "Failed Response...");
                remoteViews.setTextViewText(R.id.txt_buy_price, "No Response");
                remoteViews.setTextViewText(R.id.txt_sell_price, "No Response");
                //Remove progressbar
                remoteViews.setViewVisibility(R.id.progressbar, View.INVISIBLE);

                //Update widget
                mgr.updateAppWidget(appWidgetID, remoteViews);

            }
        });
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
