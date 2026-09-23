import { GuardianResult } from '../domain/emergency/types';

export type RootStackParamList = {
  Splash: undefined;
  Login: undefined;
  Signup: undefined;
  SignupComplete: { email: string };
  FindAccount: { tab?: 'FIND_ID' | 'FIND_PW' } | undefined;
  Legal: { tab?: 'terms' | 'privacy'; section?: string } | undefined;
  Tabs: undefined;
  EditProfile: undefined;
  GuardianConsent: { userId: number; email: string };
  GuardianWaiting: { userId: number; email: string; maskedPhone: string };
  SocialConsent: { ticket: string };
  SocialExtraInfo: { ticket: string };

  EmergencyResult: {
    address: string;
    latitude: number;
    longitude: number;
    sentAt: string;
    message: string;
    guardians: GuardianResult[];
  };

   HospitalDetail: {
    ykiho: string;
    name: string;
    address: string;
    phone: string;
    distance: number | null;
    latitude: number | null;
    longitude: number | null;
    availableBeds?: number | null;
    congestion?: number | null;
  };
};