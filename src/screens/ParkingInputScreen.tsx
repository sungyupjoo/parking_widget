import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  Alert,
  NativeModules,
  BackHandler,
  TouchableOpacity,
  KeyboardAvoidingView,
  ScrollView,
  Platform,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Colors } from '../constants/colors';

export default function ParkingInputScreen() {
  const [currentSavedLocation, setCurrentSavedLocation] =
    useState<string>('없음');
  const [floorType, setFloorType] = useState<'지하' | '지상'>('지하');
  const [floorNumber, setFloorNumber] = useState<string>('');
  const [areaSection, setAreaSection] = useState<string>('');
  const [isEditingMode, setIsEditingMode] = useState<boolean>(false);

  const scrollViewRef = useRef<ScrollView>(null);

  const isSaveEnabled = floorType && floorNumber.trim();

  const scrollToSaveButton = () => {
    if (scrollViewRef.current) {
      scrollViewRef.current.scrollToEnd({ animated: true });
    }
  };

  useEffect(() => {
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
      const parts = currentSavedLocation.split(' ');
      if (parts.length >= 2) {
        const floorTypeFromSaved = parts[0] as '지하' | '지상';
        setFloorType(floorTypeFromSaved);

        const floorNumberFromSaved = parts[1].replace('층', '');
        setFloorNumber(floorNumberFromSaved);

        const areaFromSaved = parts.slice(2).join(' ');
        setAreaSection(areaFromSaved);
      }
      setIsEditingMode(true);
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
    if (!isSaveEnabled) {
      Alert.alert('알림', '지하/지상과 층수를 모두 선택해주세요.');
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

      // Reset editing mode after successful save
      setIsEditingMode(false);

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
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      keyboardVerticalOffset={Platform.OS === 'ios' ? 0 : 20}
    >
      <ScrollView
        ref={scrollViewRef}
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContentContainer}
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps="handled"
      >
        {/* Current saved location display - Enhanced */}
        <View style={styles.currentLocationContainer}>
          <View style={styles.locationHeaderContainer}>
            <Text style={styles.locationIcon}>🚗</Text>
            <Text style={styles.currentLocationLabel}>현재 저장된 위치</Text>
          </View>
          <View style={styles.locationValueContainer}>
            <Text style={styles.currentLocationText}>
              {currentSavedLocation}
            </Text>
          </View>

          {/* Edit and Remove buttons - only show if there's saved data */}
          {currentSavedLocation && currentSavedLocation !== '없음' ? (
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
                <Text
                  style={[styles.actionButtonText, styles.removeButtonText]}
                >
                  🗑️ 삭제
                </Text>
              </TouchableOpacity>
            </View>
          ) : (
            <View style={styles.actionButtonsContainer}>
              <Text style={styles.notificationText}>
                아래에 주차 위치를 입력하고 저장하세요.
              </Text>
            </View>
          )}
        </View>

        {/* Input section with editing mode emphasis */}
        <View
          style={[
            styles.inputSection,
            isEditingMode && styles.inputSectionEditing,
          ]}
        >
          <Text style={styles.label}>주차 위치 입력</Text>

          <View style={styles.floorTypeContainer}>
            <TouchableOpacity
              style={[
                styles.floorTypeButton,
                floorType === '지하' && styles.floorTypeButtonActive,
              ]}
              onPress={() => setFloorType('지하')}
            >
              <Text style={styles.floorTypeEmoji}>🅿️</Text>
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
                floorType === '지상' && styles.floorTypeButtonActiveGreen,
              ]}
              onPress={() => setFloorType('지상')}
            >
              <Text style={styles.floorTypeEmoji}>🌤️</Text>
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

          <View style={styles.inputContainer}>
            <Text style={styles.inputLabel}>
              층수 <Text style={styles.requiredMark}>필수</Text>
            </Text>
            <TextInput
              style={styles.input}
              placeholder="예: 2"
              value={floorNumber}
              onChangeText={setFloorNumber}
              keyboardType="numeric"
              returnKeyType="next"
              onFocus={scrollToSaveButton}
            />
          </View>

          <View style={styles.inputContainer}>
            <Text style={styles.inputLabel}>
              구역 <Text style={styles.optionalMark}>선택사항</Text>
            </Text>
            <TextInput
              style={styles.input}
              placeholder="예: A구역, 2D"
              value={areaSection}
              onChangeText={setAreaSection}
              returnKeyType="done"
              onFocus={scrollToSaveButton}
            />
          </View>

          <TouchableOpacity
            style={[
              styles.saveButton,
              !isSaveEnabled && styles.saveButtonDisabled,
            ]}
            onPress={saveLocation}
            disabled={!isSaveEnabled}
          >
            <Text
              style={[
                styles.saveButtonText,
                !isSaveEnabled && styles.saveButtonTextDisabled,
              ]}
            >
              ✓ 저장
            </Text>
          </TouchableOpacity>
          <Text style={styles.cautionSaveText}>
            {isSaveEnabled ? '' : '층수를 입력해주세요.'}
          </Text>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  scrollView: {
    flex: 1,
  },
  scrollContentContainer: {
    flexGrow: 1,
    padding: 24,
    paddingTop: 40,
    paddingBottom: 40,
  },

  currentLocationContainer: {
    backgroundColor: Colors.lightGray,
    padding: 20,
    borderRadius: 12,
    marginBottom: 32,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#e9ecef',
    shadowColor: Colors.shadow,
    shadowOffset: {
      width: 0,
      height: 1,
    },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  locationHeaderContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  locationIcon: {
    fontSize: 20,
    marginRight: 8,
  },
  currentLocationLabel: {
    fontSize: 16,
    color: Colors.gray,
    fontWeight: '600',
  },
  locationValueContainer: {
    backgroundColor: Colors.white,
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderRadius: 8,
    minWidth: '80%',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#dee2e6',
  },
  currentLocationText: {
    fontSize: 18,
    fontWeight: 'bold',
    color: Colors.darkGray,
  },
  actionButtonsContainer: {
    flexDirection: 'row',
    marginTop: 16,
    gap: 8,
  },
  actionButton: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 8,
    backgroundColor: Colors.green,
    flex: 1,
    alignItems: 'center',
  },
  notificationText: {
    fontSize: 14,
    color: Colors.red,
    fontWeight: '500',
  },
  removeButton: {
    backgroundColor: Colors.red,
  },
  actionButtonText: {
    color: Colors.white,
    fontSize: 13,
    fontWeight: '600',
  },
  removeButtonText: {
    color: Colors.white,
  },
  label: {
    fontSize: 20,
    marginBottom: 16,
    fontWeight: 'bold',
    color: Colors.darkGray,
  },
  floorTypeContainer: {
    flexDirection: 'row',
    marginBottom: 20,
    gap: 12,
  },
  floorTypeButton: {
    flex: 1,
    padding: 16,
    borderRadius: 12,
    borderWidth: 2,
    borderColor: '#e9ecef',
    backgroundColor: Colors.white,
    alignItems: 'center',
    shadowColor: Colors.shadow,
    shadowOffset: {
      width: 0,
      height: 1,
    },
    shadowOpacity: 0.05,
    shadowRadius: 2,
    elevation: 1,
  },
  floorTypeButtonActive: {
    borderColor: Colors.gray,
    backgroundColor: Colors.gray,
  },
  floorTypeButtonActiveGreen: {
    borderColor: Colors.blue,
    backgroundColor: Colors.blue,
  },
  floorTypeEmoji: {
    fontSize: 20,
    marginBottom: 4,
  },
  floorTypeButtonText: {
    fontSize: 16,
    color: Colors.gray,
    fontWeight: '600',
  },
  floorTypeButtonTextActive: {
    color: Colors.white,
  },
  // Input styling
  inputContainer: {
    marginBottom: 20,
  },
  inputLabel: {
    fontSize: 16,
    color: Colors.gray,
    fontWeight: '600',
    marginBottom: 8,
  },
  requiredMark: {
    color: Colors.red,
    fontSize: 12,
    fontWeight: '700',
    backgroundColor: Colors.redBackground,
    paddingVertical: 1,
    borderRadius: 4,
  },
  optionalMark: {
    color: Colors.disabled,
    fontSize: 12,
    fontWeight: '500',
    backgroundColor: Colors.lightGray,
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ced4da',
    padding: 14,
    borderRadius: 8,
    fontSize: 16,
    backgroundColor: Colors.white,
  },
  // Subtle save button
  // Input section styling
  inputSection: {
    borderRadius: 12,
    padding: 2,
    marginBottom: 8,
  },
  inputSectionEditing: {
    borderWidth: 2,
    borderColor: Colors.green,
    backgroundColor: Colors.greenBackground,
    padding: 16,
    shadowColor: Colors.green,
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 2,
  },
  saveButton: {
    backgroundColor: Colors.blue,
    padding: 18,
    borderRadius: 12,
    alignItems: 'center',
    marginTop: 16,
    shadowColor: Colors.shadow,
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.15,
    shadowRadius: 3.84,
    elevation: 3,
  },
  saveButtonDisabled: {
    backgroundColor: Colors.disabled,
    shadowOpacity: 0.05,
    elevation: 1,
  },
  saveButtonText: {
    color: Colors.white,
    fontSize: 18,
    fontWeight: 'bold',
  },
  saveButtonTextDisabled: {
    color: Colors.lightGray,
    fontWeight: '500',
  },
  cautionSaveText: {
    color: Colors.red,
    fontSize: 14,
    fontWeight: '500',
    marginTop: 8,
    textAlign: 'center',
  },
});
