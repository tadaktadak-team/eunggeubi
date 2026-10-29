import { EmergencyBed } from "../types/emergencyBed";
import { MedicalFacility } from "../types/medicalFacility";

// 응급실 API(hpid)와 병원 API(ykiho)는 ID 체계가 달라서 이름/좌표로 같은 병원인지 판단한다.
const NEARBY_KM = 0.15;

function normalizeName(name: string) {
  return name
    .replace(/\(.*?\)/g, "")
    .replace(/의료법인|학교법인|재단법인|사회복지법인|사단법인|특수법인|국립대학법인/g, "")
    .replace(/\s+/g, "")
    .toLowerCase();
}

function distanceKm(
  lat1: number,
  lng1: number,
  lat2: number,
  lng2: number
) {
  const toRad = (d: number) => (d * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
  return 6371 * 2 * Math.asin(Math.sqrt(a));
}

function isSameHospital(bed: EmergencyBed, hospital: MedicalFacility) {
  const a = normalizeName(bed.name);
  const b = normalizeName(hospital.name);
  if (!a || !b) return false;
  if (a === b) return true;

  // 이름이 서로 포함 관계이면서 위치가 거의 같을 때만 같은 병원으로 본다.
  if (
    (a.includes(b) || b.includes(a)) &&
    bed.latitude != null &&
    bed.longitude != null &&
    hospital.latitude != null &&
    hospital.longitude != null
  ) {
    return (
      distanceKm(
        bed.latitude,
        bed.longitude,
        hospital.latitude,
        hospital.longitude
      ) <= NEARBY_KM
    );
  }
  return false;
}

// 응급실(hpid) -> 같은 병원(MedicalFacility) 매핑. 병원이 하나도 안 맞으면 키가 없다.
export function matchBedsToHospitals(
  beds: EmergencyBed[],
  hospitals: MedicalFacility[]
): Map<string, MedicalFacility> {
  const result = new Map<string, MedicalFacility>();
  const used = new Set<string>();

  beds.forEach((bed) => {
    const found = hospitals.find(
      (h) => !used.has(h.ykiho) && isSameHospital(bed, h)
    );
    if (found) {
      used.add(found.ykiho);
      result.set(bed.hpid, found);
    }
  });

  return result;
}
