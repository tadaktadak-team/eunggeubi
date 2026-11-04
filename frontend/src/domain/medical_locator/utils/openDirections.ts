import { Linking } from "react-native";

export async function openDirections(
  name: string,
  latitude: number | null,
  longitude: number | null
) {
  if (latitude == null || longitude == null) return;

  const kakaoMapUrl = `kakaomap://look?p=${latitude},${longitude}`;
  const kakaoWebUrl = `https://map.kakao.com/link/to/${encodeURIComponent(
    name
  )},${latitude},${longitude}`;

  try {
    const canOpen = await Linking.canOpenURL(kakaoMapUrl);
    await Linking.openURL(canOpen ? kakaoMapUrl : kakaoWebUrl);
  } catch (e) {
    console.error("길찾기 열기 실패:", e);
  }
}
