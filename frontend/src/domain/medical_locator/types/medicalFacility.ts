export interface MedicalFacility {
  ykiho: string;
  name: string;
  address: string;
  phone: string;
  latitude: number | null;
  longitude: number | null;
  type: string;
  distance: number | null;
}