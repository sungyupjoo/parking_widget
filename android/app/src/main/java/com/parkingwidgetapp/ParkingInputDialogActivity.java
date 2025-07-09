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
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.inputmethod.InputMethodManager;

public class ParkingInputDialogActivity extends Activity {
    private static final String TAG = "ParkingInputDialog";
    
    private TextView currentLocationText;
    private TextView savedTimeText;
    private Button floorTypeUnderground;
    private Button floorTypeAboveground;
    private EditText floorNumberInput;
    private EditText areaSectionInput;
    private Button saveButton;
    private Button cancelButton;
    private Button editButton;
    private Button deleteButton;
    
    private String currentSavedLocation = null;
    private long savedTimestamp = 0;
    private boolean isUndergroundSelected = true;
    private boolean isEditingMode = false;
    
    // For real-time timestamp updates
    private Handler timestampHandler;
    private Runnable timestampUpdater;
    
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
        
        // Start real-time timestamp updates
        startTimestampUpdates();
        
        // Auto-focus on floor number input and show keyboard
        if (floorNumberInput != null) {
            floorNumberInput.requestFocus();
            
            // Show soft keyboard with slight delay to ensure UI is ready
            floorNumberInput.postDelayed(new Runnable() {
                @Override
                public void run() {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(floorNumberInput, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            }, 100); // 100ms delay to ensure dialog is fully shown
        }
    }
    
    @Override
    public void finish() {
        stopTimestampUpdates();
        super.finish();
        // Apply exit animation
        overridePendingTransition(0, R.anim.dialog_exit);
    }
    
    @Override
    protected void onDestroy() {
        stopTimestampUpdates();
        super.onDestroy();
    }
    
    @Override
    protected void onPause() {
        stopTimestampUpdates();
        super.onPause();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        startTimestampUpdates();
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
    
    private String getRelativeTimeString(long savedTimestamp) {
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
    
    private void startTimestampUpdates() {
        if (timestampHandler == null) {
            timestampHandler = new Handler(Looper.getMainLooper());
        }
        
        timestampUpdater = new Runnable() {
            @Override
            public void run() {
                updateTimestampDisplay();
                // Update every minute
                timestampHandler.postDelayed(this, 60000);
            }
        };
        
        // Start immediately
        timestampHandler.post(timestampUpdater);
    }
    
    private void stopTimestampUpdates() {
        if (timestampHandler != null && timestampUpdater != null) {
            timestampHandler.removeCallbacks(timestampUpdater);
        }
    }
    
    private void updateTimestampDisplay() {
        boolean hasSavedData = currentSavedLocation != null && !currentSavedLocation.trim().isEmpty();
        
        if (hasSavedData && savedTimestamp > 0) {
            savedTimeText.setText(getRelativeTimeString(savedTimestamp));
            savedTimeText.setVisibility(View.VISIBLE);
        } else {
            savedTimeText.setVisibility(View.GONE);
        }
    }
    
    private void initializeViews() {
        currentLocationText = findViewById(R.id.current_location_text);
        savedTimeText = findViewById(R.id.saved_time_text);
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
        setupAreaSectionValidation();
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
                    // Validate floor number range (0-99)
                    try {
                        int floorNum = Integer.parseInt(input);
                        if (floorNum > 99) {
                            // Truncate to 99
                            input = "99";
                            Toast.makeText(ParkingInputDialogActivity.this, "층수는 99층까지 입력 가능합니다.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        // Invalid number, clear input
                        floorNumberInput.removeTextChangedListener(this);
                        floorNumberInput.setText("");
                        floorNumberInput.addTextChangedListener(this);
                        return;
                    }
                    
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
    
    private void setupAreaSectionValidation() {
        areaSectionInput.addTextChangedListener(new TextWatcher() {
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
                
                // Check if input exceeds 30 characters
                if (input.length() > 30) {
                    // Truncate to 30 characters
                    String truncated = input.substring(0, 30);
                    areaSectionInput.removeTextChangedListener(this);
                    areaSectionInput.setText(truncated);
                    areaSectionInput.setSelection(truncated.length()); // Position cursor at end
                    areaSectionInput.addTextChangedListener(this);
                    Toast.makeText(ParkingInputDialogActivity.this, "구역은 30자까지 입력 가능합니다.", Toast.LENGTH_SHORT).show();
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
        
        // Make current location text clickable to enter edit mode
        currentLocationText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Only allow editing if there's saved data
                if (currentSavedLocation != null && !currentSavedLocation.trim().isEmpty()) {
                    editCurrentLocation();
                }
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
                currentSavedLocation = cursor.getString(0);
                Log.d(TAG, "Loaded location: " + currentSavedLocation);
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
                Log.d(TAG, "Loaded timestamp: " + savedTimestamp);
            }
            timestampCursor.close();
            
            db.close();
            
        } catch (Exception e) {
            Log.e(TAG, "Error reading from AsyncStorage: " + e.getMessage());
            currentSavedLocation = null;
            savedTimestamp = 0;
        }
    }
    
    private void updateUI() {
        // Display "없음" only in UI if no data exists
        currentLocationText.setText(currentSavedLocation != null ? currentSavedLocation : "아래 입력창에 위치를 입력후 저장해주세요.");
        
        // Show/hide buttons based on whether there's saved data
        boolean hasSavedData = currentSavedLocation != null && !currentSavedLocation.trim().isEmpty();
        editButton.setVisibility(hasSavedData ? View.VISIBLE : View.GONE);
        deleteButton.setVisibility(hasSavedData ? View.VISIBLE : View.GONE);
        
        // Update background based on whether location text is clickable
        if (hasSavedData) {
            currentLocationText.setBackgroundResource(R.drawable.location_highlight_clickable);
        } else {
            currentLocationText.setBackgroundResource(R.drawable.location_highlight);
        }
        
        // Update timestamp display
        updateTimestampDisplay();
        
        // Ensure we're not in editing mode initially
        setEditingMode(false);
    }
    
    private void editCurrentLocation() {
        if (currentSavedLocation != null && !currentSavedLocation.trim().isEmpty()) {
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
            // Add appropriate highlight back to saved location based on whether there's saved data
            boolean hasSavedData = currentSavedLocation != null && !currentSavedLocation.trim().isEmpty();
            if (hasSavedData) {
                currentLocationText.setBackgroundResource(R.drawable.location_highlight_clickable);
            } else {
                currentLocationText.setBackgroundResource(R.drawable.location_highlight);
            }
            
            // Remove highlight from input fields
            floorNumberInput.setBackgroundResource(R.drawable.input_underline);
            areaSectionInput.setBackgroundResource(R.drawable.input_underline);
        }
    }
    
    private void deleteCurrentLocation() {
        new AlertDialog.Builder(this)
            .setTitle("주차 메모 삭제")
            .setMessage("저장된 주차 메모를 삭제하시겠습니까?")
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
            db.delete("catalystLocalStorage", "key = ?", new String[]{"parkingLocationTimestamp"});
            db.close();
            
            // Update widgets
            ParkingWidgetMediumProvider.updateAllWidgets(this);
            ParkingWidgetSquareProvider.updateAllWidgets(this);
            ParkingWidgetWideProvider.updateAllWidgets(this);
            
            Toast.makeText(this, "저장된 주차 메모가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "층수를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate floor number range (0-99)
        try {
            int floorNum = Integer.parseInt(floorNumber);
            if (floorNum < 0 || floorNum > 99) {
                Toast.makeText(this, "층수는 0부터 99까지 입력 가능합니다.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "올바른 층수를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate area section length (max 30 characters)
        if (areaSection.length() > 30) {
            Toast.makeText(this, "구역은 30자까지 입력 가능합니다.", Toast.LENGTH_SHORT).show();
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
            
            ContentValues locationValues = new ContentValues();
            locationValues.put("key", "parkingLocation");
            locationValues.put("value", combinedLocation);
            
            // Save timestamp
            ContentValues timestampValues = new ContentValues();
            timestampValues.put("key", "parkingLocationTimestamp");
            timestampValues.put("value", String.valueOf(System.currentTimeMillis()));
            
            // Insert or replace both location and timestamp
            db.insertWithOnConflict("catalystLocalStorage", null, locationValues, SQLiteDatabase.CONFLICT_REPLACE);
            db.insertWithOnConflict("catalystLocalStorage", null, timestampValues, SQLiteDatabase.CONFLICT_REPLACE);
            db.close();
            
            // Update widgets
            ParkingWidgetMediumProvider.updateAllWidgets(this);
            ParkingWidgetSquareProvider.updateAllWidgets(this);
            ParkingWidgetWideProvider.updateAllWidgets(this);
            
            Toast.makeText(this, combinedLocation + "으로 저장되었습니다.", Toast.LENGTH_SHORT).show();
            setEditingMode(false);
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving location: " + e.getMessage());
            Toast.makeText(this, "저장 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }
} 