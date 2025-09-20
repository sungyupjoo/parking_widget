package com.parkingwidgetapp;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import android.util.Log;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;

public class ParkingWidgetModule extends ReactContextBaseJavaModule {
    
    private static final String TAG = "ParkingWidgetModule";
    
    public ParkingWidgetModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }
    
    @Override
    public String getName() {
        return "ParkingWidgetModule";
    }
    
    @ReactMethod
    public void updateWidgets(Promise promise) {
        try {
            ReactApplicationContext context = getReactApplicationContext();
            
            // Update all widgets
            ParkingWidgetMediumProvider.updateAllWidgets(context);
            ParkingWidgetSquareProvider.updateAllWidgets(context);
            ParkingWidgetWideProvider.updateAllWidgets(context);
            
            Log.d(TAG, "Widgets updated successfully from React Native");
            promise.resolve("Widgets updated successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating widgets: " + e.getMessage());
            promise.reject("ERROR", "Failed to update widgets: " + e.getMessage());
        }
    }
    
    @ReactMethod
    public void getCurrentParkingLocation(Promise promise) {
        try {
            ReactApplicationContext context = getReactApplicationContext();
            String parkingLocation = null;
            long savedTimestamp = 0;
            
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
            }
            timestampCursor.close();
            
            db.close();
            
            WritableMap result = Arguments.createMap();
            result.putString("location", parkingLocation);
            result.putDouble("timestamp", savedTimestamp);
            
            promise.resolve(result);
            
        } catch (Exception e) {
            Log.e(TAG, "Error reading parking location: " + e.getMessage());
            promise.reject("ERROR", "Failed to read parking location: " + e.getMessage());
        }
    }
    
    // Method to emit events to React Native when data changes
    public static void emitDataChangedEvent(ReactApplicationContext context) {
        try {
            if (context != null && context.hasActiveCatalystInstance()) {
                context
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit("parkingDataChanged", null);
                Log.d("ParkingWidgetModule", "Emitted parkingDataChanged event");
            }
        } catch (Exception e) {
            Log.e("ParkingWidgetModule", "Error emitting data changed event: " + e.getMessage());
        }
    }
} 