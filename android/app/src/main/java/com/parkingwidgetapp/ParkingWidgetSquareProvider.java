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
import android.app.AlarmManager;
import android.os.SystemClock;

public class ParkingWidgetSquareProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateWidget(context, appWidgetManager, appWidgetIds);
        scheduleNextUpdate(context);
    }
    
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        scheduleNextUpdate(context);
    }
    
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        cancelScheduledUpdates(context);
    }

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
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
                android.util.Log.d("ParkingWidgetSquare", "Square widget location: " + parkingLocation);
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
                android.util.Log.d("ParkingWidgetSquare", "Square widget timestamp: " + savedTimestamp);
            }
            timestampCursor.close();
            
            db.close();
            
        } catch (Exception e) {
            android.util.Log.e("ParkingWidgetSquare", "Error reading from AsyncStorage: " + e.getMessage());
            // Fallback to SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences("RCTAsyncLocalStorage", Context.MODE_PRIVATE);
            parkingLocation = prefs.getString("parkingLocation", null);
        }

        for (int widgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_parking_square);
            
            // Display appropriate message based on data availability
            String displayText = (parkingLocation != null && !parkingLocation.trim().isEmpty()) 
                ? parkingLocation 
                : "위치 정보 없음";
            views.setTextViewText(R.id.parking_text, displayText);

            // Show/hide timestamp
            boolean hasLocation = parkingLocation != null && !parkingLocation.trim().isEmpty() && savedTimestamp > 0;
            if (hasLocation) {
                String timeText = getRelativeTimeString(savedTimestamp);
                views.setTextViewText(R.id.saved_time_text, timeText);
                views.setViewVisibility(R.id.saved_time_text, android.view.View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.saved_time_text, android.view.View.GONE);
            }

            // Add click functionality to open the dialog
            Intent intent = new Intent(context, ParkingInputDialogActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
    
    private void scheduleNextUpdate(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ParkingWidgetSquareProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        
        ComponentName componentName = new ComponentName(context, ParkingWidgetSquareProvider.class);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, 
            1, // Different ID from medium widget
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Update every 15 minutes
        long interval = 15 * 60 * 1000; // 15 minutes in milliseconds
        alarmManager.setRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + interval,
            interval,
            pendingIntent
        );
    }
    
    private void cancelScheduledUpdates(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ParkingWidgetSquareProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, 
            1, // Same ID as in scheduleNextUpdate
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.cancel(pendingIntent);
    }

    private static String getRelativeTimeString(long savedTimestamp) {
        long currentTime = System.currentTimeMillis();
        
        // Check if saved timestamp is from today
        java.util.Calendar savedCal = java.util.Calendar.getInstance();
        savedCal.setTimeInMillis(savedTimestamp);
        
        java.util.Calendar currentCal = java.util.Calendar.getInstance();
        currentCal.setTimeInMillis(currentTime);
        
        // Same day - show absolute time
        if (savedCal.get(java.util.Calendar.YEAR) == currentCal.get(java.util.Calendar.YEAR) &&
            savedCal.get(java.util.Calendar.DAY_OF_YEAR) == currentCal.get(java.util.Calendar.DAY_OF_YEAR)) {
            
            int hour = savedCal.get(java.util.Calendar.HOUR_OF_DAY);
            int minute = savedCal.get(java.util.Calendar.MINUTE);
            
            String amPm = hour < 12 ? "오전" : "오후";
            int displayHour = hour == 0 ? 12 : (hour > 12 ? hour - 12 : hour);
            
            return String.format("%s %d:%02d 저장", amPm, displayHour, minute);
        }
        
        // Different day - show days ago
        long timeDiff = currentTime - savedTimestamp;
        long days = timeDiff / (24 * 60 * 60 * 1000);
        
        if (days == 0) {
            days = 1; // If less than 24 hours but different day
        }
        
        return days + "일 전";
    }
} 