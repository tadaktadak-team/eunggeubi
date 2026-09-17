export interface HospitalDepartment {
  name: string;
  doctorCount: number | null;
}

export interface HospitalDetail {
  ykiho: string;
  mondayStart: string | null;
  mondayEnd: string | null;
  tuesdayStart: string | null;
  tuesdayEnd: string | null;
  wednesdayStart: string | null;
  wednesdayEnd: string | null;
  thursdayStart: string | null;
  thursdayEnd: string | null;
  fridayStart: string | null;
  fridayEnd: string | null;
  saturdayStart: string | null;
  saturdayEnd: string | null;
  lunchTime: string | null;
  closedOnSunday: string | null;
  closedOnHoliday: string | null;
  parkingCapacity: number | null;
  parkingFee: string | null;
  parkingNote: string | null;
  nearestSubwayStation: string | null;
  subwayExit: string | null;
  subwayDistance: string | null;
  departments: HospitalDepartment[];
}