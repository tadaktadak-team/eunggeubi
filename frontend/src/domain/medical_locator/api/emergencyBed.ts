import { api } from "../../../shared/api/client";
import { EmergencyBed } from "../types/emergencyBed";

export async function getEmergencyBeds(
  latitude: number,
  longitude: number,
  stage1: string
): Promise<EmergencyBed[]> {
  const params = new URLSearchParams({
    stage1,
    latitude: String(latitude),
    longitude: String(longitude),
  });

  console.log(
    "응급실 API 요청:",
    `/api/emergency-beds?${params.toString()}`
  );

  return api.get<EmergencyBed[]>(`/api/emergency-beds?${params.toString()}`);
}