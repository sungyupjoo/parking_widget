import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  Button,
  StyleSheet,
  Alert,
  NativeModules,
  BackHandler,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

export default function ParkingInputScreen() {
  const [parkingLocation, setParkingLocation] = useState<string>('');

  const saveLocation = async () => {
    if (!parkingLocation) {
      Alert.alert('입력 오류', '주차 위치를 입력해주세요.');
      return;
    }
    try {
      await AsyncStorage.setItem('parkingLocation', parkingLocation);

      const savedData = await AsyncStorage.getItem('parkingLocation');

      if (NativeModules.WidgetUpdateModule) {
        NativeModules.WidgetUpdateModule.updateWidget();
      }

      Alert.alert(
        '저장 완료',
        `${parkingLocation}로 저장되었습니다.\n위젯이 업데이트되었습니다.`,
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

  const testAsyncStorage = async () => {
    try {
      const data = await AsyncStorage.getItem('parkingLocation');
      Alert.alert('AsyncStorage 테스트', `저장된 데이터: ${data || '없음'}`);
    } catch (err) {
      console.error('AsyncStorage 읽기 오류:', err);
      Alert.alert('오류', '데이터 읽기 실패');
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.label}>주차 위치 입력</Text>
      <TextInput
        style={styles.input}
        placeholder="예: B2 2D"
        value={parkingLocation}
        onChangeText={setParkingLocation}
      />
      <Button title="저장" onPress={saveLocation} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 24,
    justifyContent: 'center',
  },
  label: {
    fontSize: 20,
    marginBottom: 12,
  },
  input: {
    borderWidth: 1,
    borderColor: '#aaa',
    padding: 12,
    borderRadius: 8,
    marginBottom: 16,
  },
});
