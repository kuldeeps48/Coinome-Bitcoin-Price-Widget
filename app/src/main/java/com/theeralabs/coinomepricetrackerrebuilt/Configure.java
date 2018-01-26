package com.theeralabs.coinomepricetrackerrebuilt;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import static android.content.ContentValues.TAG;

public class Configure extends AppCompatActivity {

    private LinearLayout layoutBitcoin, layoutBitcoinCash, layoutLitecoin, layoutDash;
    private TextView txtRate;
    private int mAppWidgetId;
    private AppWidgetManager appWidgetManager;
    private RemoteViews remoteViews;
    String c;
    private static final long FUTURE_UPDATE_TIME_IN_MILLISEC = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_CANCELED, resultValue);

        appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        remoteViews = new RemoteViews(getPackageName(), R.layout.coinome_appwidget);

        layoutBitcoin = findViewById(R.id.item_bitcoin);
        layoutBitcoinCash = findViewById(R.id.item_bitcoin_cash);
        layoutLitecoin = findViewById(R.id.item_litecoin);
        layoutDash = findViewById(R.id.item_dash);

        layoutBitcoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setLayoutAndExit(R.drawable.bitcoin_icon, "BTC/INR", "bitcoin");
            }
        });

        layoutBitcoinCash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setLayoutAndExit(R.drawable.bitcoin_cash_icon, "BCH/INR", "bitcoinCash");
            }
        });

        layoutLitecoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setLayoutAndExit(R.drawable.litecoin_icon, "LTC/INR", "litecoin");
            }
        });

        layoutDash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setLayoutAndExit(R.drawable.dash_icon, "DSH/INR", "dash");
            }
        });

        txtRate = findViewById(R.id.btn_rate_review);
        txtRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openPlayStore();
            }
        });

    }

    private void openPlayStore() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    private void setLayoutAndExit(int icon, String title, String coin) {
        c = coin;
        saveIDandCoin(mAppWidgetId, coin);
        updateWidgetAndFinish();
    }

    private void saveIDandCoin(int mAppWidgetId, String coin) {
        SharedPreferences prices = getSharedPreferences("" + mAppWidgetId, MODE_PRIVATE);
        SharedPreferences.Editor editor = prices.edit();
        editor.clear()
                .putString("" + mAppWidgetId, coin)
                .commit();
    }

    private void updateWidgetAndFinish() {
        setFutureUpdate(getApplicationContext());

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);

        setFutureUpdate(getApplicationContext());
        finish();
    }


    private void setFutureUpdate(Context context) {
        AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
        assert alarmManager != null;
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + FUTURE_UPDATE_TIME_IN_MILLISEC,
                getPendingButtonClickIntent(context, new int[]{mAppWidgetId}));
        Log.d(TAG, "setFutureUpdate: Alarm set for " +
                SystemClock.elapsedRealtime() + FUTURE_UPDATE_TIME_IN_MILLISEC);
    }

    private PendingIntent getPendingButtonClickIntent(Context context, int[] appWidgetId) {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null,
                context, CoinomeAppWidgetProvider.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetId);

        return PendingIntent.getBroadcast(context, appWidgetId[0], intent, 0);
    }


}
