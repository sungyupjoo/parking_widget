package com.parkingwidgetapp;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.widget.RemoteViews;
import android.content.Intent;
import android.app.PendingIntent;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.content.SharedPreferences;

public class ParkingWidgetUtil {
    
    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, 
                                   int[] appWidgetIds, int layoutId, String logTag) {
        String parkingLocation = null;
        long savedTimestamp = 0;
        
        try {
            // Read from AsyncStorage SQLite database
            SQLiteDatabase db = SQLiteDatabase.openDatabase(
                context.getDatabasePath("RKStorage").getPath(),
                null,
                SQLiteDatabase.OPEN_READONLY
            );
            
            // Load parking location
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
                android.util.Log.d(logTag, logTag + " widget location: " + parkingLocation);
            }
            cursor.close();
            
            // Load timestamp
            Cursor timestampCursor = db.query(
                "catalystLocalStorage",
                new String[]{"value"},
                "key = ?",
                new String[]{"parkingLocationTimestamp"},
                null,
                null,
                null
            );
            
            if (timestampCursor.moveToFirst()) {
                savedTimestamp = Long.parseLong(timestampCursor.getString(0));
                android.util.Log.d(logTag, logTag + " widget timestamp: " + savedTimestamp);
            }
            timestampCursor.close();
            
            db.close();
            
        } catch (Exception e) {
            android.util.Log.e(logTag, "Error reading from AsyncStorage: " + e.getMessage());
            // Fallback to SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences("RCTAsyncLocalStorage", Context.MODE_PRIVATE);
            parkingLocation = prefs.getString("parkingLocation", null);
        }

        for (int widgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
            
            // Display appropriate message based on data availability
            String displayText = (parkingLocation != null && !parkingLocation.trim().isEmpty()) 
                ? parkingLocation 
                : "위치정보 없음";
            views.setTextViewText(R.id.parking_text, displayText);

            // Show timestamp or fallback text
            boolean hasLocation = parkingLocation != null && !parkingLocation.trim().isEmpty() && savedTimestamp > 0;
            if (hasLocation) {
                String timeText = getRelativeTimeString(savedTimestamp);
                views.setTextViewText(R.id.saved_time_text, timeText);
            } else {
                views.setTextViewText(R.id.saved_time_text, "터치하여 위치 입력");
            }
            views.setViewVisibility(R.id.saved_time_text, android.view.View.VISIBLE);

            // Add click functionality to open the dialog
            Intent intent = new Intent(context, ParkingInputDialogActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            appWidgetManager.updateAppWidget(widgetId, views);
        }
    }
    
    public static String getRelativeTimeString(long savedTimestamp) {
        java.util.Calendar savedCal = java.util.Calendar.getInstance();
        savedCal.setTimeInMillis(savedTimestamp);
        
        int hour = savedCal.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = savedCal.get(java.util.Calendar.MINUTE);
        
        String amPm = hour < 12 ? "오전" : "오후";
        int displayHour = hour == 0 ? 12 : (hour > 12 ? hour - 12 : hour);
        
        return String.format("%s %d:%02d 저장", amPm, displayHour, minute);
    }
} 