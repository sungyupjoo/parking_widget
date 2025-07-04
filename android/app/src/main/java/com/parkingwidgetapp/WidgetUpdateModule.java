package com.parkingwidgetapp;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;

public class WidgetUpdateModule extends ReactContextBaseJavaModule {

    private static final String MODULE_NAME = "WidgetUpdateModule";

    public WidgetUpdateModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @ReactMethod
    public void updateWidget() {
        android.util.Log.d("WidgetUpdateModule", "updateWidget() called from React Native");
        ReactApplicationContext context = getReactApplicationContext();
        if (context != null) {
            android.util.Log.d("WidgetUpdateModule", "Context is not null, calling ParkingWidgetProvider.updateAllWidgets");
            
            // Debug: Check AsyncStorage data directly from SQLite
            try {
                SQLiteDatabase db = SQLiteDatabase.openDatabase(
                    context.getDatabasePath("RKStorage").getPath(),
                    null,
                    SQLiteDatabase.OPEN_READONLY
                );
                
                Cursor cursor = db.query(
                    "catalystLocalStorage",
                    new String[]{"key", "value"},
                    null,
                    null,
                    null,
                    null,
                    null
                );
                
                android.util.Log.d("WidgetUpdateModule", "AsyncStorage SQLite contents:");
                while (cursor.moveToNext()) {
                    String key = cursor.getString(0);
                    String value = cursor.getString(1);
                    android.util.Log.d("WidgetUpdateModule", "  " + key + " = " + value);
                }
                
                cursor.close();
                db.close();
                
            } catch (Exception e) {
                android.util.Log.e("WidgetUpdateModule", "Error reading AsyncStorage SQLite: " + e.getMessage());
            }
            
            ParkingWidgetProvider.updateAllWidgets(context);
        } else {
            android.util.Log.e("WidgetUpdateModule", "Context is null!");
        }
    }
} 