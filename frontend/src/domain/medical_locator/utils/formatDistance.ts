// 서버가 주는 km 값을 카드/시트에 보여줄 문구로 바꾼다 (1km 미만은 m 단위)
export function formatDistance(km: number | null | undefined) {
  if (km == null) return null;
  if (km < 1) return `${Math.round(km * 1000)}m`;
  return `${km}km`;
}
