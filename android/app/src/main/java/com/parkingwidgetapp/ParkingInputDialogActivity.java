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
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextWatcher;
import android.text.Editable;
import android.util.Log;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

public class ParkingInputDialogActivity extends Activity {
    private static final String TAG = "ParkingInputDialog";
    
    private TextView currentLocationText;
    private Button floorTypeUnderground;
    private Button floorTypeAboveground;
    private EditText floorNumberInput;
    private EditText areaSectionInput;
    private Button saveButton;
    private Button cancelButton;
    private Button editButton;
    private Button deleteButton;
    
    private String currentSavedLocation = "없음";
    private boolean isUndergroundSelected = true;
    private boolean isEditingMode = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Remove title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.activity_parking_input_dialog);
        
        // Make dialog wider and adjust size
        setupDialogWindow();
        
        // Initialize views
        initializeViews();
        
        // Load current saved location
        loadSavedLocation();
        
        // Set up listeners
        setupListeners();
        
        // Update UI based on saved location
        updateUI();
        
        // Apply enter animation
        overridePendingTransition(R.anim.dialog_enter, 0);
    }
    
    @Override
    public void finish() {
        super.finish();
        // Apply exit animation
        overridePendingTransition(0, R.anim.dialog_exit);
    }
    
    @Override
    public void onBackPressed() {
        setEditingMode(false);
        super.onBackPressed();
        // Apply exit animation on back press
        overridePendingTransition(0, R.anim.dialog_exit);
    }
    
    private void setupDialogWindow() {
        Window window = getWindow();
        if (window != null) {
            // Make background transparent
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            
            WindowManager.LayoutParams params = window.getAttributes();
            
            // Make dialog wider - use 98% of screen width
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.98);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            
            window.setAttributes(params);
        }
    }
    
    private void initializeViews() {
        currentLocationText = findViewById(R.id.current_location_text);
        floorTypeUnderground = findViewById(R.id.floor_type_underground);
        floorTypeAboveground = findViewById(R.id.floor_type_aboveground);
        floorNumberInput = findViewById(R.id.floor_number_input);
        areaSectionInput = findViewById(R.id.area_section_input);
        saveButton = findViewById(R.id.save_button);
        cancelButton = findViewById(R.id.cancel_button);
        editButton = findViewById(R.id.edit_button);
        deleteButton = findViewById(R.id.delete_button);
        
        // Set up toggle buttons
        setupToggleButtons();
        
        setupFloorNumberFormatting();
    }
    
    private void setupToggleButtons() {
        // Default selection: underground
        selectFloorType(true);
        
        floorTypeUnderground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFloorType(true);
            }
        });
        
        floorTypeAboveground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFloorType(false);
            }
        });
    }
    
    private void selectFloorType(boolean isUnderground) {
        isUndergroundSelected = isUnderground;
        
        if (isUnderground) {
            floorTypeUnderground.setBackgroundResource(R.drawable.toggle_selected);
            floorTypeUnderground.setTextColor(getResources().getColor(android.R.color.black));
            floorTypeAboveground.setBackgroundResource(android.R.color.transparent);
            floorTypeAboveground.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else {
            floorTypeAboveground.setBackgroundResource(R.drawable.toggle_selected);
            floorTypeAboveground.setTextColor(getResources().getColor(android.R.color.black));
            floorTypeUnderground.setBackgroundResource(android.R.color.transparent);
            floorTypeUnderground.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }
    
    private void setupFloorNumberFormatting() {
        floorNumberInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                
                // Remove any existing "층" first
                if (input.endsWith("층")) {
                    input = input.substring(0, input.length() - 1);
                }
                
                // Check if input contains only numbers
                if (!input.isEmpty() && input.matches("\\d+")) {
                    // Add "층" suffix
                    String formatted = input + "층";
                    
                    // Only update if it's different to avoid infinite loop
                    if (!formatted.equals(s.toString())) {
                        floorNumberInput.removeTextChangedListener(this);
                        floorNumberInput.setText(formatted);
                        floorNumberInput.setSelection(formatted.length() - 1); // Position cursor before "층"
                        floorNumberInput.addTextChangedListener(this);
                    }
                }
            }
        });
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
                setEditingMode(false);
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
        
        // Ensure we're not in editing mode initially
        setEditingMode(false);
    }
    
    private void editCurrentLocation() {
        if (!currentSavedLocation.equals("없음")) {
            // Parse the saved location
            String[] parts = currentSavedLocation.split(" ");
            if (parts.length >= 2) {
                // Set floor type
                String floorType = parts[0];
                selectFloorType(floorType.equals("지하"));
                
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
        
        // Enter editing mode
        setEditingMode(true);
    }
    
    private void setEditingMode(boolean isEditing) {
        isEditingMode = isEditing;
        
        if (isEditing) {
            // Remove highlight from saved location
            currentLocationText.setBackgroundResource(android.R.color.transparent);
            
            // Highlight input fields
            floorNumberInput.setBackgroundResource(R.drawable.input_editing_highlight);
            areaSectionInput.setBackgroundResource(R.drawable.input_editing_highlight);
        } else {
            // Add highlight back to saved location
            currentLocationText.setBackgroundResource(R.drawable.location_highlight);
            
            // Remove highlight from input fields
            floorNumberInput.setBackgroundResource(R.drawable.input_underline);
            areaSectionInput.setBackgroundResource(R.drawable.input_underline);
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
            setEditingMode(false);
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting location: " + e.getMessage());
            Toast.makeText(this, "삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveLocation() {
        // Get floor type
        String floorType = isUndergroundSelected ? "지하" : "지상";
        
        // Get floor number (remove "층" suffix if present)
        String floorNumber = floorNumberInput.getText().toString().trim();
        if (floorNumber.endsWith("층")) {
            floorNumber = floorNumber.substring(0, floorNumber.length() - 1);
        }
        
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
            
            Toast.makeText(this, combinedLocation + "으로 저장되었습니다.", Toast.LENGTH_SHORT).show();
            setEditingMode(false);
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving location: " + e.getMessage());
            Toast.makeText(this, "저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }
} 