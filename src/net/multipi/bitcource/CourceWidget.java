/*
 *
 */
package net.multipi.bitcource;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.IBinder;
import android.widget.RemoteViews;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.JSONObject;

/**
 *
 * @author marat
 */
public class CourceWidget extends AppWidgetProvider {

    public static String ACTION_WIDGET_RELOAD = "reload";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        context.startService(new Intent(context, UpdateService.class));
    }

    public static class UpdateService extends Service {

        @Override
        public void onStart(Intent intent, int startId) {
            Toast.makeText(this, R.string.loading, Toast.LENGTH_SHORT).show();
            RemoteViews updateViews = buildUpdate(this);
            AppWidgetManager.getInstance(this).updateAppWidget(new ComponentName(this, CourceWidget.class), updateViews);
        }

        /**
         * Build a widget update 
         */
        public RemoteViews buildUpdate(Context context) {

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.cource_message);

            // Create event
            Intent active = new Intent(context, CourceWidget.class);
            active.setAction(ACTION_WIDGET_RELOAD);
            PendingIntent actionPendingIntent = PendingIntent.getBroadcast(context, 0, active, 0);
            // Register event
            views.setOnClickPendingIntent(R.id.reload, actionPendingIntent);

            SharedPreferences sp = context.getSharedPreferences("course", 0);
            String buy = "";
            String sell = "";
            float avg = sp.getFloat("avg", 0);
            // get gata from JSON
            try {
                JSONObject resp = new JSONObject(Http.Request("https://mtgox.com/code/data/ticker.php"));
                buy = resp.getJSONObject("ticker").getString("buy");
                sell = resp.getJSONObject("ticker").getString("sell");
                avg = Float.parseFloat(resp.getJSONObject("ticker").getString("avg"));
            } catch (Exception e) {
                buy = "...";
                sell = "...";
                e.printStackTrace(System.err);
                Toast.makeText(this, R.string.err_connect, Toast.LENGTH_SHORT).show();
            }
            if (avg < sp.getFloat("avg", 0)) {
                views.setImageViewResource(R.id.state, android.R.drawable.arrow_down_float);
            } else if (avg == sp.getFloat("avg", 0)) {
                views.setImageViewResource(R.id.state, android.R.drawable.button_onoff_indicator_off);
            } else {
                views.setImageViewResource(R.id.state, android.R.drawable.arrow_up_float);
            }
            sp.edit().putFloat("avg", avg).commit();
            views.setTextViewText(R.id.message_b, "$"+buy);
            views.setTextViewText(R.id.message_s, "$"+sell);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            views.setTextViewText(R.id.time, "@ "+sdf.format(new Date()));
            return views;
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // Receive event
        final String action = intent.getAction();
        if (ACTION_WIDGET_RELOAD.equals(action)) {
            context.startService(new Intent(context, UpdateService.class));
        }
        super.onReceive(context, intent);
    }
}
