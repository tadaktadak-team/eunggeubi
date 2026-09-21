import { api } from "../../../shared/api/client";
import { HospitalDetail } from "../types/hospitalDetail";

export async function getHospitalDetail(
  ykiho: string
): Promise<HospitalDetail> {
  const params = new URLSearchParams({ ykiho });
  return api.get<HospitalDetail>(`/api/hospitals/detail?${params.toString()}`);
}