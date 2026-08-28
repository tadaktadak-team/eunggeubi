import { useEffect, useState } from "react";
import {
  ActivityIndicator,
  FlatList,
  StyleSheet,
  Text,
  View,
} from "react-native";
import * as Location from "expo-location";

import { getEmergencyBeds } from "../api/emergencyBed";
import { EmergencyBed } from "../types/emergencyBed";

export default function MedicalLocatorScreen() {
  const [beds, setBeds] = useState<EmergencyBed[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadEmergencyBeds();
  }, []);

  async function loadEmergencyBeds() {
    try {
      setLoading(true);
      setError(null);

      const { status } =
        await Location.requestForegroundPermissionsAsync();

      if (status !== "granted") {
        throw new Error("위치 권한이 필요합니다.");
      }

      const location = await Location.getCurrentPositionAsync({
        accuracy: Location.Accuracy.Balanced,
      });

      const latitude = location.coords.latitude;
      const longitude = location.coords.longitude;

      console.log("현재 위치:", latitude, longitude);

      const addresses = await Location.reverseGeocodeAsync({
        latitude,
        longitude,
      });

      if (addresses.length === 0) {
        throw new Error("현재 위치의 주소를 확인할 수 없습니다.");
      }

      const address = addresses[0];

      console.log("현재 주소:", address);

      const stage1 = address.region;
      const stage2 = "도봉구";

      if (!stage1 || !stage2) {
        throw new Error("현재 위치의 지역 정보를 확인할 수 없습니다.");
      }

      console.log("조회 지역:", stage1, stage2);

      const data = await getEmergencyBeds(
        latitude,
        longitude,
        stage1,
        stage2
      );

      setBeds(data);
    } catch (e) {
      console.error("응급실 조회 실패:", e);

      if (e instanceof Error) {
        setError(e.message);
      } else {
        setError("응급실 정보를 불러오지 못했습니다.");
      }
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" />
        <Text style={styles.message}>
          주변 응급실을 찾고 있어요...
        </Text>
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.center}>
        <Text style={styles.error}>{error}</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>주변 응급실</Text>

      <FlatList
        data={beds}
        keyExtractor={(item) => item.hpid}
        renderItem={({ item }) => (
          <View style={styles.card}>
            <View style={styles.row}>
              <Text style={styles.name}>{item.name}</Text>

              <Text style={styles.beds}>
                {item.availableBeds ?? "-"}병상
              </Text>
            </View>

            <Text style={styles.distance}>
              {item.distance != null
                ? `${item.distance}km`
                : "거리 정보 없음"}
            </Text>

            <Text style={styles.address}>{item.address}</Text>

            <Text style={styles.congestion}>
              혼잡도{" "}
              {item.congestion != null
                ? `${item.congestion}%`
                : "-"}
            </Text>
          </View>
        )}
        ListEmptyComponent={
          <Text style={styles.message}>
            주변 응급실 정보가 없습니다.
          </Text>
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    backgroundColor: "#fff",
  },

  center: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    padding: 20,
  },

  title: {
    fontSize: 24,
    fontWeight: "700",
    marginBottom: 20,
  },

  card: {
    padding: 16,
    marginBottom: 12,
    borderRadius: 12,
    backgroundColor: "#f5f5f5",
  },

  row: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },

  name: {
    flex: 1,
    fontSize: 17,
    fontWeight: "600",
  },

  beds: {
    fontSize: 16,
    fontWeight: "700",
  },

  distance: {
    marginTop: 8,
    fontSize: 14,
  },

  address: {
    marginTop: 4,
    color: "#666",
  },

  congestion: {
    marginTop: 8,
    fontWeight: "600",
  },

  message: {
    marginTop: 12,
    color: "#666",
  },

  error: {
    color: "#d00",
    textAlign: "center",
  },
});