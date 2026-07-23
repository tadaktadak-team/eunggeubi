import { GuardianResult } from '../domain/emergency/types';

export type RootStackParamList = {
  Tabs: undefined;
  EmergencyResult: {
    address: string;
    sentAt: string;
    guardians: GuardianResult[];
  };
};