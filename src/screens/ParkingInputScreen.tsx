import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  Alert,
  BackHandler,
  TouchableOpacity,
  KeyboardAvoidingView,
  ScrollView,
  Platform,
  Modal,
  Dimensions,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Colors } from '../constants/colors';
import ParkingIcon from '../components/ParkingIcon';

export default function ParkingInputScreen() {
  const [currentSavedLocation, setCurrentSavedLocation] = useState<
    string | undefined
  >(undefined);
  const [floorType, setFloorType] = useState<'지하' | '지상'>('지하');
  const [floorNumber, setFloorNumber] = useState<string>('');
  const [areaSection, setAreaSection] = useState<string>('');
  const [isEditingMode, setIsEditingMode] = useState<boolean>(false);
  const [showWidgetGuide, setShowWidgetGuide] = useState<boolean>(false);

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
      setCurrentSavedLocation(savedData || undefined);
    } catch (err) {
      console.error('저장된 위치 로드 오류:', err);
      setCurrentSavedLocation(undefined);
    }
  };

  const editCurrentLocation = () => {
    if (currentSavedLocation) {
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
            await AsyncStorage.removeItem('parkingLocationTimestamp');
            setCurrentSavedLocation(undefined);

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
      Alert.alert('알림', '층수를 입력해주세요.');
      return;
    }

    // Combine the three inputs
    const combinedLocation = `${floorType} ${floorNumber}층 ${areaSection}`;

    try {
      await AsyncStorage.setItem('parkingLocation', combinedLocation);
      // Save timestamp for consistency with Android
      await AsyncStorage.setItem(
        'parkingLocationTimestamp',
        String(Date.now()),
      );
      setCurrentSavedLocation(combinedLocation);

      // Reset editing mode after successful save
      setIsEditingMode(false);

      Alert.alert(
        '저장 완료',
        `${combinedLocation}으로 저장되었습니다.\n위젯이 업데이트되었습니다.`,
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

  const WidgetGuideModal = () => (
    <Modal
      animationType="slide"
      transparent={true}
      visible={showWidgetGuide}
      onRequestClose={() => setShowWidgetGuide(false)}
    >
      <View style={styles.modalOverlay}>
        <View style={styles.modalContainer}>
          <ScrollView style={styles.modalScrollView}>
            <Text style={styles.modalTitle}>위젯 사용법</Text>

            <View style={styles.guideSection}>
              <Text style={styles.guideSectionTitle}>🎯 위젯이란?</Text>
              <Text style={styles.guideText}>
                홈화면에서 <Text style={styles.boldText}>앱을 열지 않고도</Text>{' '}
                주차 위치를 <Text style={styles.boldText}>바로 확인</Text>할 수
                있는 편리한 기능입니다.
              </Text>
            </View>

            <View style={styles.guideSection}>
              <Text style={styles.guideSectionTitle}>📋 Android 위젯 설치</Text>
              <Text style={styles.guideStep}>
                1. 홈화면 빈 공간을 길게 누르세요
              </Text>
              <Text style={styles.guideStep}>
                2. 화면 아래에서 "위젯" 또는 "Widget" 메뉴를 선택하세요
              </Text>
              <Text style={styles.guideStep}>
                3. 화면 상단의 검색창에 "주차 메모" 앱을 검색하여 선택하세요.
              </Text>
              <Text style={styles.guideStep}>
                (화면 아래의 목록을 내려 "주차 메모" 앱을 선택하셔도 됩니다.)
              </Text>
              <Text style={styles.guideStep}>
                4. 원하시는 크기와 디자인의 위젯을 선택하세요. 언제든지 바꾸실
                수 있습니다!
              </Text>
              <Text style={styles.guideStep}>
                5. 위젯의 위치를 옮기고 싶다면, 위젯을 길게 누르시고 원하시는
                위치로 끌어주세요
              </Text>
            </View>

            <View style={styles.guideSection}>
              <Text style={styles.guideSectionTitle}>💡 사용 팁</Text>
              <Text style={styles.guideText}>
                • 위젯을 탭하면 앱으로 들어오시지 않고도 위치를 수정할 수
                있습니다
              </Text>
              <Text style={styles.guideText}>
                • 주차 위치를 저장하면 위젯이 자동으로 업데이트됩니다.
              </Text>
            </View>
          </ScrollView>

          <View style={styles.modalButtonContainer}>
            <TouchableOpacity
              style={styles.modalCloseButton}
              onPress={() => setShowWidgetGuide(false)}
            >
              <Text style={styles.modalCloseButtonText}>닫기</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      keyboardVerticalOffset={Platform.OS === 'ios' ? 0 : 20}
    >
      {/* Header Menu */}
      <View style={styles.headerContainer}>
        <View style={styles.headerTitleContainer}>
          <ParkingIcon width={30} height={30} color={Colors.blue} />
          <Text style={styles.headerTitle}>주차 메모</Text>
        </View>

        <TouchableOpacity
          style={styles.headerDescriptionContainer}
          onPress={() => setShowWidgetGuide(true)}
          activeOpacity={0.7}
        >
          <Text style={styles.descriptionText}>
            주차 메모는 <Text style={styles.descriptionTextBold}>위젯</Text>으로
            이용하시면 더 편리합니다!
          </Text>
          <Text style={styles.guideIcon}>위젯 사용법</Text>
        </TouchableOpacity>
      </View>

      <ScrollView
        ref={scrollViewRef}
        style={styles.scrollView}
        contentContainerStyle={styles.scrollContentContainer}
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.currentLocationContainer}>
          <View style={styles.locationHeaderContainer}>
            <Text style={styles.currentLocationLabel}>
              마지막으로 저장한 위치
            </Text>
          </View>
          <View style={styles.locationValueContainer}>
            <Text style={styles.currentLocationText}>
              {currentSavedLocation || '없음'}
            </Text>
          </View>

          {/* Edit and Remove buttons - only show if there's saved data */}
          {currentSavedLocation ? (
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

      <WidgetGuideModal />
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
    padding: 20,
    paddingTop: 16,
    paddingBottom: 32,
  },
  descriptionText: {
    fontSize: 13,
    color: Colors.gray,
    fontWeight: '500',
    textAlign: 'center',
  },
  descriptionTextBold: {
    fontWeight: 'bold',
    color: Colors.white,
    backgroundColor: Colors.blue,
  },
  currentLocationContainer: {
    backgroundColor: Colors.lightGray,
    padding: 16,
    borderRadius: 12,
    marginBottom: 24,
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
    gap: 2,
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
  boldText: {
    fontWeight: '800',
  },
  label: {
    fontSize: 18,
    marginBottom: 12,
    fontWeight: 'bold',
    color: Colors.darkGray,
  },
  floorTypeContainer: {
    flexDirection: 'row',
    marginBottom: 16,
    gap: 10,
  },
  floorTypeButton: {
    flex: 1,
    padding: 12,
    borderRadius: 10,
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
    marginBottom: 16,
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
    padding: 16,
    borderRadius: 10,
    alignItems: 'center',
    marginTop: 12,
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
  headerContainer: {
    flexDirection: 'column',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingTop: 32,
    paddingBottom: 8,
    backgroundColor: Colors.white,
    borderBottomWidth: 1,
    borderBottomColor: '#e9ecef',
    shadowColor: Colors.shadow,
    shadowOffset: {
      width: 0,
      height: 1,
    },
    shadowOpacity: 0.1,
    shadowRadius: 2,
    elevation: 2,
  },
  headerTitleContainer: {
    width: '100%',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
  },
  headerTitle: {
    fontSize: 22,
    fontWeight: 'bold',
    color: Colors.darkGray,
  },
  headerDescriptionContainer: {
    width: '100%',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 6,
    backgroundColor: Colors.lightGray,
    marginTop: 6,
    gap: 6,
  },
  guideIcon: {
    fontSize: 13,
    color: Colors.blue,
    textDecorationLine: 'underline',
  },
  modalOverlay: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0,0,0,0.5)',
  },
  modalContainer: {
    backgroundColor: Colors.white,
    borderRadius: 16,
    width: Dimensions.get('window').width * 0.9,
    maxHeight: Dimensions.get('window').height * 0.8,
    overflow: 'hidden',
    shadowColor: Colors.shadow,
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowOpacity: 0.2,
    shadowRadius: 8,
    elevation: 5,
  },
  modalScrollView: {
    padding: 24,
  },
  modalButtonContainer: {
    padding: 16,
    paddingTop: 8,
    borderTopWidth: 1,
    borderTopColor: '#e9ecef',
  },
  modalTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: Colors.darkGray,
    textAlign: 'center',
    marginBottom: 20,
  },
  guideSection: {
    marginBottom: 20,
  },
  guideSectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: Colors.darkGray,
    marginBottom: 10,
  },
  guideText: {
    fontSize: 16,
    color: Colors.gray,
    lineHeight: 24,
    marginBottom: 10,
  },
  guideStep: {
    fontSize: 16,
    color: Colors.darkGray,
    fontWeight: '500',
    marginBottom: 5,
  },
  modalCloseButton: {
    backgroundColor: Colors.blue,
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 12,
    alignItems: 'center',
  },
  modalCloseButtonText: {
    color: Colors.white,
    fontSize: 16,
    fontWeight: 'bold',
  },
});
