package com.parkingwidgetapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.util.Log;

public class MidnightUpdateReceiver extends BroadcastReceiver {
    private static final String TAG = "MidnightUpdateReceiver";
    private static final String ACTION_MIDNIGHT_UPDATE = "com.parkingwidgetapp.MIDNIGHT_UPDATE";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Midnight update triggered");
        
        if (ACTION_MIDNIGHT_UPDATE.equals(intent.getAction())) {
            updateAllWidgets(context);
            // Schedule next midnight update
            MidnightScheduler.scheduleMidnightUpdate(context);
        }
    }
    
    private void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        
        // Update Wide widgets
        int[] wideWidgetIds = appWidgetManager.getAppWidgetIds(
            new ComponentName(context, ParkingWidgetWideProvider.class));
        if (wideWidgetIds.length > 0) {
            ParkingWidgetUtil.updateWidget(context, appWidgetManager, wideWidgetIds, 
                R.layout.widget_parking_wide, "WideWidget");
        }
        
        // Update Medium widgets
        int[] mediumWidgetIds = appWidgetManager.getAppWidgetIds(
            new ComponentName(context, ParkingWidgetMediumProvider.class));
        if (mediumWidgetIds.length > 0) {
            ParkingWidgetUtil.updateWidget(context, appWidgetManager, mediumWidgetIds, 
                R.layout.widget_parking_medium, "MediumWidget");
        }
        
        // Update Square widgets
        int[] squareWidgetIds = appWidgetManager.getAppWidgetIds(
            new ComponentName(context, ParkingWidgetSquareProvider.class));
        if (squareWidgetIds.length > 0) {
            ParkingWidgetUtil.updateWidget(context, appWidgetManager, squareWidgetIds, 
                R.layout.widget_parking_square, "SquareWidget");
        }
        
        Log.d(TAG, "All widgets updated at midnight");
    }
    
    public static String getMidnightUpdateAction() {
        return ACTION_MIDNIGHT_UPDATE;
    }
}
