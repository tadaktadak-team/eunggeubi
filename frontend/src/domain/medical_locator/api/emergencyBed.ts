import { EmergencyBed } from "../types/emergencyBed";

const API_BASE_URL = "http://192.168.0.12:8080";

export async function getEmergencyBeds(
  latitude: number,
  longitude: number,
  stage1: string,
  stage2: string
): Promise<EmergencyBed[]> {
  const params = new URLSearchParams({
    stage1,
    stage2,
    latitude: String(latitude),
    longitude: String(longitude),
  });

  const url = `${API_BASE_URL}/api/emergency-beds?${params.toString()}`;

  console.log("응급실 API 요청:", url);

  const response = await fetch(url);

  console.log("응급실 API 응답:", response.status);

  if (!response.ok) {
    throw new Error(`응급실 API 요청 실패: ${response.status}`);
  }

  return response.json();
}