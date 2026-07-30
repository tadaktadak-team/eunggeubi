import { GuardianResult } from '../domain/emergency/types';

export type RootStackParamList = {
  Splash: undefined;
  Login: undefined;
  Signup: undefined;
  SignupComplete: { email: string };
  FindAccount: { tab?: 'FIND_ID' | 'FIND_PW' } | undefined;
  Tabs: undefined;
  EditProfile: undefined;
  GuardianConsent: { userId: number; email: string };
  GuardianWaiting: { userId: number; email: string; maskedPhone: string };

  EmergencyResult: {
    address: string;
    sentAt: string;
    guardians: GuardianResult[];
  };
};