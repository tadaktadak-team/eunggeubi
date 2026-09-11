export interface EmergencyBed {
  hpid: string;
  name: string;
  address: string;
  phone: string;
  latitude: number | null;
  longitude: number | null;
  distance: number | null;
  availableBeds: number | null;
  standardBeds: number | null;
  congestion: number | null;
  updatedAt: string;
}