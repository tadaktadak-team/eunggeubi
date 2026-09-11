import { api } from "../../../shared/api/client";
import { MedicalFacility } from "../types/medicalFacility";

export async function getNearbyHospitals(
  latitude: number,
  longitude: number
): Promise<MedicalFacility[]> {
  const params = new URLSearchParams({
    lat: String(latitude),
    lng: String(longitude),
  });

  return api.get<MedicalFacility[]>(`/api/hospitals?${params.toString()}`);
}

export async function getNearbyPharmacies(
  latitude: number,
  longitude: number
): Promise<MedicalFacility[]> {
  const params = new URLSearchParams({
    lat: String(latitude),
    lng: String(longitude),
  });

  return api.get<MedicalFacility[]>(`/api/pharmacies?${params.toString()}`);
}