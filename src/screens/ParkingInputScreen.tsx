import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TextInput,
  Button,
  StyleSheet,
  Alert,
  NativeModules,
  BackHandler,
  TouchableOpacity,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

export default function ParkingInputScreen() {
  const [currentSavedLocation, setCurrentSavedLocation] =
    useState<string>('없음');
  const [floorType, setFloorType] = useState<'지하' | '지상'>('지하');
  const [floorNumber, setFloorNumber] = useState<string>('');
  const [areaSection, setAreaSection] = useState<string>('');

  useEffect(() => {
    // Load saved location on component mount
    loadSavedLocation();
  }, []);

  const loadSavedLocation = async () => {
    try {
      const savedData = await AsyncStorage.getItem('parkingLocation');
      if (savedData) {
        setCurrentSavedLocation(savedData);
      }
    } catch (err) {
      console.error('저장된 위치 로드 오류:', err);
    }
  };

  const editCurrentLocation = () => {
    if (currentSavedLocation && currentSavedLocation !== '없음') {
      // Parse the saved location back into components
      const parts = currentSavedLocation.split(' ');
      if (parts.length >= 2) {
        // Extract floor type (지하/지상)
        const floorTypeFromSaved = parts[0] as '지하' | '지상';
        setFloorType(floorTypeFromSaved);

        // Extract floor number (remove '층' if present)
        const floorNumberFromSaved = parts[1].replace('층', '');
        setFloorNumber(floorNumberFromSaved);

        // Extract area (everything after floor number)
        const areaFromSaved = parts.slice(2).join(' ');
        setAreaSection(areaFromSaved);
      }
    }
  };

  const removeCurrentLocation = () => {
    Alert.alert('위치 삭제', '저장된 주차 위치를 삭제하시겠습니까?', [
      {
        text: '취소',
        style: 'cancel',
      },
      {
        text: '삭제',
        style: 'destructive',
        onPress: async () => {
          try {
            await AsyncStorage.removeItem('parkingLocation');
            setCurrentSavedLocation('없음');

            // Update widget
            if (NativeModules.WidgetUpdateModule) {
              NativeModules.WidgetUpdateModule.updateWidget();
            }

            Alert.alert('삭제 완료', '저장된 주차 위치가 삭제되었습니다.');
          } catch (err) {
            console.error('삭제 오류:', err);
            Alert.alert('오류', '삭제 중 오류가 발생했습니다.');
          }
        },
      },
    ]);
  };

  const saveLocation = async () => {
    if (!floorNumber.trim()) {
      Alert.alert('입력 오류', '층수는 필수입니다.');
      return;
    }

    // Combine the three inputs
    const combinedLocation = `${floorType} ${floorNumber}층 ${areaSection}`;

    try {
      await AsyncStorage.setItem('parkingLocation', combinedLocation);
      setCurrentSavedLocation(combinedLocation);

      if (NativeModules.WidgetUpdateModule) {
        NativeModules.WidgetUpdateModule.updateWidget();
      }

      Alert.alert(
        '저장 완료',
        `${combinedLocation}로 저장되었습니다.\n위젯이 업데이트되었습니다.`,
        [
          {
            text: '취소',
            style: 'cancel',
          },
          {
            text: '확인',
            onPress: () => {
              BackHandler.exitApp();
            },
          },
        ],
      );
    } catch (err) {
      console.error('저장 오류:', err);
      Alert.alert(
        '오류',
        err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다.',
      );
    }
  };

  return (
    <View style={styles.container}>
      {/* Current saved location display */}
      <View style={styles.currentLocationContainer}>
        <Text style={styles.currentLocationLabel}>현재 저장된 위치:</Text>
        <Text style={styles.currentLocationText}>{currentSavedLocation}</Text>

        {/* Edit and Remove buttons - only show if there's saved data */}
        {currentSavedLocation && currentSavedLocation !== '없음' && (
          <View style={styles.actionButtonsContainer}>
            <TouchableOpacity
              style={styles.actionButton}
              onPress={editCurrentLocation}
            >
              <Text style={styles.actionButtonText}>✏️ 편집</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.actionButton, styles.removeButton]}
              onPress={removeCurrentLocation}
            >
              <Text style={[styles.actionButtonText, styles.removeButtonText]}>
                🗑️ 삭제
              </Text>
            </TouchableOpacity>
          </View>
        )}
      </View>

      <Text style={styles.label}>주차 위치 입력</Text>

      {/* Floor type selector */}
      <View style={styles.floorTypeContainer}>
        <TouchableOpacity
          style={[
            styles.floorTypeButton,
            floorType === '지하' && styles.floorTypeButtonActive,
          ]}
          onPress={() => setFloorType('지하')}
        >
          <Text
            style={[
              styles.floorTypeButtonText,
              floorType === '지하' && styles.floorTypeButtonTextActive,
            ]}
          >
            지하
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[
            styles.floorTypeButton,
            floorType === '지상' && styles.floorTypeButtonActive,
          ]}
          onPress={() => setFloorType('지상')}
        >
          <Text
            style={[
              styles.floorTypeButtonText,
              floorType === '지상' && styles.floorTypeButtonTextActive,
            ]}
          >
            지상
          </Text>
        </TouchableOpacity>
      </View>

      {/* Floor number input */}
      <TextInput
        style={styles.input}
        placeholder="층수 (예: 2)"
        value={floorNumber}
        onChangeText={setFloorNumber}
        keyboardType="numeric"
      />

      {/* Area section input */}
      <TextInput
        style={styles.input}
        placeholder="구역 (예: A구역, 2D)"
        value={areaSection}
        onChangeText={setAreaSection}
      />

      {/* Stylish save button */}
      <TouchableOpacity style={styles.saveButton} onPress={saveLocation}>
        <Text style={styles.saveButtonText}>💾 저장</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 24,
    justifyContent: 'center',
  },
  // Current location display
  currentLocationContainer: {
    backgroundColor: '#f0f0f0',
    padding: 16,
    borderRadius: 8,
    marginBottom: 24,
    alignItems: 'center',
  },
  currentLocationLabel: {
    fontSize: 14,
    color: '#666',
    marginBottom: 4,
  },
  currentLocationText: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333',
  },
  actionButtonsContainer: {
    flexDirection: 'row',
    marginTop: 12,
    gap: 8,
  },
  actionButton: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 6,
    backgroundColor: '#007AFF',
    flex: 1,
    alignItems: 'center',
  },
  removeButton: {
    backgroundColor: '#FF3B30',
  },
  actionButtonText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: '600',
  },
  removeButtonText: {
    color: '#fff',
  },
  label: {
    fontSize: 20,
    marginBottom: 12,
    fontWeight: 'bold',
  },
  // Floor type selector
  floorTypeContainer: {
    flexDirection: 'row',
    marginBottom: 16,
    gap: 8,
  },
  floorTypeButton: {
    flex: 1,
    padding: 12,
    borderRadius: 8,
    borderWidth: 2,
    borderColor: '#ddd',
    backgroundColor: '#fff',
    alignItems: 'center',
  },
  floorTypeButtonActive: {
    borderColor: '#007AFF',
    backgroundColor: '#007AFF',
  },
  floorTypeButtonText: {
    fontSize: 16,
    color: '#666',
    fontWeight: '600',
  },
  floorTypeButtonTextActive: {
    color: '#fff',
  },
  input: {
    borderWidth: 1,
    borderColor: '#aaa',
    padding: 12,
    borderRadius: 8,
    marginBottom: 16,
    fontSize: 16,
  },
  // Stylish save button
  saveButton: {
    backgroundColor: '#007AFF',
    padding: 16,
    borderRadius: 12,
    alignItems: 'center',
    marginTop: 8,
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
    elevation: 5,
  },
  saveButtonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: 'bold',
  },
});
