package com.parkingwidgetapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.content.ContentValues;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

public class ParkingInputDialogActivity extends Activity {
    private static final String TAG = "ParkingInputDialog";
    
    private TextView currentLocationText;
    private RadioGroup floorTypeRadioGroup;
    private RadioButton floorTypeUnderground;
    private RadioButton floorTypeAboveground;
    private EditText floorNumberInput;
    private EditText areaSectionInput;
    private Button saveButton;
    private Button cancelButton;
    private Button editButton;
    private Button deleteButton;
    
    private String currentSavedLocation = "없음";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking_input_dialog);
        
        // Initialize views
        initializeViews();
        
        // Load current saved location
        loadSavedLocation();
        
        // Set up listeners
        setupListeners();
        
        // Update UI based on saved location
        updateUI();
    }
    
    private void initializeViews() {
        currentLocationText = findViewById(R.id.current_location_text);
        floorTypeRadioGroup = findViewById(R.id.floor_type_radio_group);
        floorTypeUnderground = findViewById(R.id.floor_type_underground);
        floorTypeAboveground = findViewById(R.id.floor_type_aboveground);
        floorNumberInput = findViewById(R.id.floor_number_input);
        areaSectionInput = findViewById(R.id.area_section_input);
        saveButton = findViewById(R.id.save_button);
        cancelButton = findViewById(R.id.cancel_button);
        editButton = findViewById(R.id.edit_button);
        deleteButton = findViewById(R.id.delete_button);
    }
    
    private void setupListeners() {
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveLocation();
            }
        });
        
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editCurrentLocation();
            }
        });
        
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteCurrentLocation();
            }
        });
    }
    
    private void loadSavedLocation() {
        try {
            // Read from AsyncStorage SQLite database
            SQLiteDatabase db = SQLiteDatabase.openDatabase(
                getDatabasePath("RKStorage").getPath(),
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
                currentSavedLocation = cursor.getString(0);
                Log.d(TAG, "Loaded location: " + currentSavedLocation);
            }
            
            cursor.close();
            db.close();
            
        } catch (Exception e) {
            Log.e(TAG, "Error reading from AsyncStorage: " + e.getMessage());
            currentSavedLocation = "없음";
        }
    }
    
    private void updateUI() {
        currentLocationText.setText(currentSavedLocation);
        
        // Show/hide buttons based on whether there's saved data
        boolean hasSavedData = !currentSavedLocation.equals("없음");
        editButton.setVisibility(hasSavedData ? View.VISIBLE : View.GONE);
        deleteButton.setVisibility(hasSavedData ? View.VISIBLE : View.GONE);
    }
    
    private void editCurrentLocation() {
        if (!currentSavedLocation.equals("없음")) {
            // Parse the saved location
            String[] parts = currentSavedLocation.split(" ");
            if (parts.length >= 2) {
                // Set floor type
                String floorType = parts[0];
                if (floorType.equals("지하")) {
                    floorTypeUnderground.setChecked(true);
                } else {
                    floorTypeAboveground.setChecked(true);
                }
                
                // Set floor number
                String floorNumber = parts[1].replace("층", "");
                floorNumberInput.setText(floorNumber);
                
                // Set area section
                if (parts.length > 2) {
                    StringBuilder areaBuilder = new StringBuilder();
                    for (int i = 2; i < parts.length; i++) {
                        if (i > 2) areaBuilder.append(" ");
                        areaBuilder.append(parts[i]);
                    }
                    areaSectionInput.setText(areaBuilder.toString());
                }
            }
        }
    }
    
    private void deleteCurrentLocation() {
        new AlertDialog.Builder(this)
            .setTitle("위치 삭제")
            .setMessage("저장된 주차 위치를 삭제하시겠습니까?")
            .setPositiveButton("삭제", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    performDelete();
                }
            })
            .setNegativeButton("취소", null)
            .show();
    }
    
    private void performDelete() {
        try {
            // Delete from AsyncStorage SQLite database
            SQLiteDatabase db = SQLiteDatabase.openDatabase(
                getDatabasePath("RKStorage").getPath(),
                null,
                SQLiteDatabase.OPEN_READWRITE
            );
            
            db.delete("catalystLocalStorage", "key = ?", new String[]{"parkingLocation"});
            db.close();
            
            // Update widgets
            ParkingWidgetMediumProvider.updateAllWidgets(this);
            ParkingWidgetSquareProvider.updateAllWidgets(this);
            
            Toast.makeText(this, "저장된 주차 위치가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting location: " + e.getMessage());
            Toast.makeText(this, "삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveLocation() {
        // Get floor type
        String floorType = "";
        int selectedId = floorTypeRadioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.floor_type_underground) {
            floorType = "지하";
        } else if (selectedId == R.id.floor_type_aboveground) {
            floorType = "지상";
        }
        
        // Get floor number
        String floorNumber = floorNumberInput.getText().toString().trim();
        
        // Get area section
        String areaSection = areaSectionInput.getText().toString().trim();
        
        // Validate input
        if (floorType.isEmpty() || floorNumber.isEmpty()) {
            Toast.makeText(this, "지하/지상과 층수를 모두 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Combine location
        String combinedLocation = floorType + " " + floorNumber + "층";
        if (!areaSection.isEmpty()) {
            combinedLocation += " " + areaSection;
        }
        
        try {
            // Save to AsyncStorage SQLite database
            SQLiteDatabase db = SQLiteDatabase.openDatabase(
                getDatabasePath("RKStorage").getPath(),
                null,
                SQLiteDatabase.OPEN_READWRITE
            );
            
            ContentValues values = new ContentValues();
            values.put("key", "parkingLocation");
            values.put("value", combinedLocation);
            
            // Insert or replace
            db.insertWithOnConflict("catalystLocalStorage", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            db.close();
            
            // Update widgets
            ParkingWidgetMediumProvider.updateAllWidgets(this);
            ParkingWidgetSquareProvider.updateAllWidgets(this);
            
            Toast.makeText(this, combinedLocation + "로 저장되었습니다.", Toast.LENGTH_SHORT).show();
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving location: " + e.getMessage());
            Toast.makeText(this, "저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }
} 