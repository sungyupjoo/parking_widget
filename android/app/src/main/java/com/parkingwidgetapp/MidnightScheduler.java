package com.parkingwidgetapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.util.Calendar;

public class MidnightScheduler {
    private static final String TAG = "MidnightScheduler";
    private static final int MIDNIGHT_REQUEST_CODE = 1001;
    
    public static void scheduleMidnightUpdate(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null");
            return;
        }
        
        // Cancel any existing midnight alarm
        cancelMidnightUpdate(context);
        
        // Create intent for midnight update
        Intent intent = new Intent(context, MidnightUpdateReceiver.class);
        intent.setAction(MidnightUpdateReceiver.getMidnightUpdateAction());
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, 
            MIDNIGHT_REQUEST_CODE, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Calculate next midnight
        Calendar midnight = Calendar.getInstance();
        midnight.add(Calendar.DAY_OF_YEAR, 1); // Tomorrow
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);
        
        long triggerTime = midnight.getTimeInMillis();
        
        // Schedule the alarm
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            Log.d(TAG, "Midnight update scheduled for: " + midnight.getTime().toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule midnight update: " + e.getMessage());
        }
    }
    
    public static void cancelMidnightUpdate(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        
        Intent intent = new Intent(context, MidnightUpdateReceiver.class);
        intent.setAction(MidnightUpdateReceiver.getMidnightUpdateAction());
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context, 
            MIDNIGHT_REQUEST_CODE, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Midnight update cancelled");
    }
    
    public static void initializeMidnightScheduler(Context context) {
        // Schedule the first midnight update when the app starts or widget is added
        scheduleMidnightUpdate(context);
        Log.d(TAG, "Midnight scheduler initialized");
    }
}
