package com.parkingwidgetapp;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import android.util.Log;

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
} 