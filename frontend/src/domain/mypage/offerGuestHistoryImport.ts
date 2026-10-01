import { Alert } from 'react-native';

import {
  clearGuestCode,
  getDeclinedGuestCode,
  getGuestCode,
  saveDeclinedGuestCode,
} from '../../shared/storage/guestCodeStorage';
import { claimGuestConsultations } from './api/consultation';

// 로그인 직후(AuthProvider) 호출. 이 기기에 비회원 AI 상담 기록(guestCode)이 있으면 내 계정으로
// 가져올지 묻는다. 가족이 같이 쓰는 기기면 남의 건강 상담 기록일 수 있어서 자동으로 옮기지 않는다.
// 거절하면 코드는 기기에 그대로 두고(원래 주인이 나중에 가져갈 수 있게) 같은 코드로는 다시 묻지 않는다.
export async function offerGuestHistoryImport() {
  const code = await getGuestCode();
  if (!code || code === (await getDeclinedGuestCode())) return;

  Alert.alert(
    '비회원 상담 기록',
    '이 기기에서 비회원으로 한 AI 상담 기록이 있어요.\n내 계정으로 가져올까요?\n\n기기를 다른 사람과 같이 쓴다면 본인 기록인지 확인해주세요.',
    [
      { text: '가져오지 않기', style: 'cancel', onPress: () => saveDeclinedGuestCode(code).catch(console.error) },
      {
        text: '가져오기',
        onPress: async () => {
          try {
            const { claimed } = await claimGuestConsultations(code);
            // 0건이어도 코드는 비운다 - 이미 다른 계정으로 옮겨졌거나 기록이 없는 코드라 다시 물을 이유가 없다.
            await clearGuestCode();
            if (claimed > 0) {
              Alert.alert('가져오기 완료', '상담 기록을 가져왔어요. 마이페이지 상담 내역에서 볼 수 있어요.');
            } else {
              Alert.alert('가져올 기록 없음', '가져올 상담 기록이 없어요. 이미 다른 계정으로 옮겨졌을 수 있어요.');
            }
          } catch (e) {
            Alert.alert('가져오기 실패', e instanceof Error ? e.message : '다시 로그인하면 다시 시도할 수 있어요.');
          }
        },
      },
    ],
  );
}
