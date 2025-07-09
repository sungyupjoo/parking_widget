package com.parkingwidgetapp;

import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.ComponentName;

public class ParkingWidgetMediumProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ParkingWidgetUtil.updateWidget(context, appWidgetManager, appWidgetIds, 
                                      R.layout.widget_parking_medium, "ParkingWidgetMedium");
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, ParkingWidgetMediumProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
        ParkingWidgetUtil.updateWidget(context, appWidgetManager, appWidgetIds, 
                                      R.layout.widget_parking_medium, "ParkingWidgetMedium");
    }
} 