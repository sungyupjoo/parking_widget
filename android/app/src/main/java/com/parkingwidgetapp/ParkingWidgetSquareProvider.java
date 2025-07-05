package com.parkingwidgetapp;

import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.widget.RemoteViews;
import android.content.Intent;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.content.SharedPreferences;

public class ParkingWidgetSquareProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateWidget(context, appWidgetManager, appWidgetIds);
    }

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        String parkingLocation = "위치 없음";
        
        try {
            // Read from AsyncStorage SQLite database
            SQLiteDatabase db = SQLiteDatabase.openDatabase(
                context.getDatabasePath("RKStorage").getPath(),
                null,
                SQLiteDatabase.OPEN_READONLY
            );
            
            Cursor cursor = db.query(
                "catalystLocalStorage",
                new String[]{"value"},
                "key = ?",
                new String[]{"parkingLocation"},
                null,
                null,
                null
            );
            
            if (cursor.moveToFirst()) {
                parkingLocation = cursor.getString(0);
                android.util.Log.d("ParkingWidgetSquare", "Square widget location: " + parkingLocation);
            }
            
            cursor.close();
            db.close();
            
        } catch (Exception e) {
            android.util.Log.e("ParkingWidgetSquare", "Error reading from AsyncStorage: " + e.getMessage());
            // Fallback to SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences("RCTAsyncLocalStorage", Context.MODE_PRIVATE);
            parkingLocation = prefs.getString("parkingLocation", "위치 없음");
        }

        for (int widgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_parking_square);
            views.setTextViewText(R.id.parking_text, parkingLocation);

            // Add click functionality to open the app
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.parking_text, pendingIntent);

            appWidgetManager.updateAppWidget(widgetId, views);
        }
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, ParkingWidgetSquareProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
        updateWidget(context, appWidgetManager, appWidgetIds);
    }
} 