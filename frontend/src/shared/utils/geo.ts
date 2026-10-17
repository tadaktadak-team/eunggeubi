// 서비스 지원 범위(대한민국 본토·제주·울릉·독도)를 감싸는 좌표 범위. 백엔드 검증과 같은 값이다.
export const isInKorea = (latitude: number, longitude: number) =>
  latitude >= 33 && latitude <= 39 && longitude >= 124 && longitude <= 132;
