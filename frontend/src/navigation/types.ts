import { GuardianResult } from '../domain/emergency/types';

export type RootStackParamList = {
  Splash: undefined;
  Login: undefined;
  Signup: undefined;
  SignupComplete: { email: string };
  FindAccount: { tab?: 'FIND_ID' | 'FIND_PW' } | undefined;
  Tabs: undefined;
  EmergencyResult: {
    address: string;
    sentAt: string;
    message: string;
    guardians: GuardianResult[];
  };
};