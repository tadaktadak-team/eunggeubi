import { useFocusEffect } from '@react-navigation/native';
import * as Location from 'expo-location';
import { useCallback, useRef, useState } from 'react';

import { isInKorea } from '../../../shared/utils/geo';
import { getEmergencyBeds } from '../api/emergencyBed';
import { EmergencyBed } from '../types/emergencyBed';

export type NearestBedState =
  | { status: 'loading' }
  | { status: 'ready'; beds: EmergencyBed[] }
  | { status: 'empty' | 'denied' | 'outside' | 'error' };

const REFRESH_MS = 60_000;
const MAX_BEDS = 3;

// 현재 위치에서 가까운 응급실 최대 3곳. 화면에 다시 들어올 때 1분이 지났으면 새로 조회한다
export function useNearestEmergencyBed() {
  const [state, setState] = useState<NearestBedState>({ status: 'loading' });
  const loadedAtRef = useRef(0);
  const requestIdRef = useRef(0);

  const load = useCallback(async () => {
    const requestId = ++requestIdRef.current;
    const done = (next: NearestBedState) => {
      if (requestId === requestIdRef.current) setState(next);
    };
    // 이미 보여주던 응급실이 있으면 새로 조회하는 동안에도 그대로 둔다
    setState((prev) => (prev.status === 'ready' ? prev : { status: 'loading' }));

    try {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') return done({ status: 'denied' });

      const pos =
        (await Location.getLastKnownPositionAsync()) ??
        (await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced }));
      const { latitude, longitude } = pos.coords;
      if (!isInKorea(latitude, longitude)) return done({ status: 'outside' });

      const geo = await Location.reverseGeocodeAsync({ latitude, longitude });
      const stage1 = geo[0]?.region;
      if (!stage1) return done({ status: 'error' });

      const beds = await getEmergencyBeds(latitude, longitude, stage1);
      const nearest = beds
        .filter((bed) => bed.distance != null)
        .sort((a, b) => (a.distance as number) - (b.distance as number))
        .slice(0, MAX_BEDS);

      loadedAtRef.current = Date.now();
      done(nearest.length > 0 ? { status: 'ready', beds: nearest } : { status: 'empty' });
    } catch (e) {
      console.error('가까운 응급실 조회 실패:', e);
      done({ status: 'error' });
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      if (Date.now() - loadedAtRef.current > REFRESH_MS) load();
    }, [load]),
  );

  return { state, retry: load };
}
