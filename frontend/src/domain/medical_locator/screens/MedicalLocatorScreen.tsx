import { useEffect, useState } from "react";
import {
  ActivityIndicator,
  FlatList,
  Modal,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  Linking,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useNavigation } from "@react-navigation/native";
import { NativeStackNavigationProp } from "@react-navigation/native-stack";
import { RootStackParamList } from "../../../navigation/types";
import * as Location from "expo-location";
import KakaoMapView, { MapMarkerData } from "../components/KakaoMapView";
import { getEmergencyBeds } from "../api/emergencyBed";
import { EmergencyBed } from "../types/emergencyBed";
import { getNearbyHospitals, getNearbyPharmacies } from "../api/medicalFacility";
import { MedicalFacility } from "../types/medicalFacility";
import CharacterSlot from "../../../shared/components/CharacterSlot";
import { colors } from "../../../shared/theme/theme";
import { isInKorea } from "../../../shared/utils/geo";

type FilterType = "all" | "hospital" | "pharmacy" | "emergency";

const FILTER_COLORS: Record<FilterType, string> = {
  all: "#616161",
  hospital: "#1E88E5",
  pharmacy: "#43A047",
  emergency: "#E53935",
};

type NavigationProp = NativeStackNavigationProp<RootStackParamList>;

export default function MedicalLocatorScreen() {
  const navigation = useNavigation<NavigationProp>();
  const [beds, setBeds] = useState<EmergencyBed[]>([]);
  const [hospitals, setHospitals] = useState<MedicalFacility[]>([]);
  const [pharmacies, setPharmacies] = useState<MedicalFacility[]>([]);
  const [filter, setFilter] = useState<FilterType>("all");
  const [searchText, setSearchText] = useState("");
  const [visibleCount, setVisibleCount] = useState(5);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [outOfKorea, setOutOfKorea] = useState(false);
  const [stage1, setStage1] = useState<string | null>(null);
  const [bedsFailed, setBedsFailed] = useState(false);
  const [hospitalsFailed, setHospitalsFailed] = useState(false);
  const [pharmaciesFailed, setPharmaciesFailed] = useState(false);
  const [location, setLocation] =
    useState<Location.LocationObject | null>(null);

    const [selectedPharmacy, setSelectedPharmacy] = useState<{
    name: string;
    address: string;
    phone: string;
    distance: number | null;
    latitude: number | null;
    longitude: number | null;
  } | null>(null);

  const [selectedEmergencyBed, setSelectedEmergencyBed] = useState<{
    name: string;
    address: string;
    phone: string;
    distance: number | null;
    latitude: number | null;
    longitude: number | null;
    availableBeds: number | null;
    congestion: number | null;
  } | null>(null);

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    setVisibleCount(5);
  }, [filter, searchText]);

  // 위치 확보까지만 담당. 실패하면 화면 전체를 에러로 보여줘야 하는 전제조건이라 그대로 try/catch.
  async function loadInitialData() {
    try {
      setLoading(true);
      setError(null);
      setOutOfKorea(false);

      // 위치 권한 요청
      const { status } =
        await Location.requestForegroundPermissionsAsync();

      if (status !== "granted") {
        throw new Error("위치 권한이 필요합니다.");
      }

      // 현재 위치 가져오기
      const currentLocation =
        await Location.getCurrentPositionAsync({
          accuracy: Location.Accuracy.Balanced,
        });

      setLocation(currentLocation);

      const latitude = currentLocation.coords.latitude;
      const longitude = currentLocation.coords.longitude;

      console.log("현재 위치:", latitude, longitude);

      // 서비스 지원 지역(대한민국) 밖이면 서버에 묻지 않고 안내한다
      if (!isInKorea(latitude, longitude)) {
        setOutOfKorea(true);
        setLoading(false);
        return;
      }

      // 현재 위치의 주소 확인
      const addresses = await Location.reverseGeocodeAsync({
        latitude,
        longitude,
      });

      if (addresses.length === 0) {
        throw new Error(
          "현재 위치의 주소를 확인할 수 없습니다."
        );
      }

      const address = addresses[0];

      console.log("현재 주소:", address);

      // 시·도만 사용
      const stage1 = address.region;

      if (!stage1) {
        throw new Error(
          "현재 위치의 지역 정보를 확인할 수 없습니다."
        );
      }

      console.log("조회 지역:", stage1);
      setStage1(stage1);

      // 여기까지 성공하면 위치는 확보된 것 -> 화면을 먼저 띄운다.
      setLoading(false);

      // 응급실/병원/약국은 서로 독립적으로 실패해도 나머지는 정상 표시되게 분리
      loadBeds(latitude, longitude, stage1);
      loadHospitals(latitude, longitude);
      loadPharmacies(latitude, longitude);

    } catch (e) {
      console.error("위치 조회 실패:", e);

      if (e instanceof Error) {
        setError(e.message);
      } else {
        setError(
          "위치 정보를 불러오지 못했습니다."
        );
      }

      setLoading(false);
    }
  }

      async function openPharmacyDirections() {
    if (
      !selectedPharmacy ||
      selectedPharmacy.latitude == null ||
      selectedPharmacy.longitude == null
    ) {
      return;
    }

    const { latitude, longitude, name } = selectedPharmacy;
    const kakaoMapUrl = `kakaomap://look?p=${latitude},${longitude}`;
    const kakaoWebUrl = `https://map.kakao.com/link/to/${encodeURIComponent(
      name
    )},${latitude},${longitude}`;

    try {
      const canOpen = await Linking.canOpenURL(kakaoMapUrl);
      if (canOpen) {
        await Linking.openURL(kakaoMapUrl);
      } else {
        await Linking.openURL(kakaoWebUrl);
      }
    } catch (e) {
      console.error("길찾기 열기 실패:", e);
    }
  }

  async function openEmergencyBedDirections() {
    if (
      !selectedEmergencyBed ||
      selectedEmergencyBed.latitude == null ||
      selectedEmergencyBed.longitude == null
    ) {
      return;
    }

    const { latitude, longitude, name } = selectedEmergencyBed;
    const kakaoMapUrl = `kakaomap://look?p=${latitude},${longitude}`;
    const kakaoWebUrl = `https://map.kakao.com/link/to/${encodeURIComponent(
      name
    )},${latitude},${longitude}`;

    try {
      const canOpen = await Linking.canOpenURL(kakaoMapUrl);
      if (canOpen) {
        await Linking.openURL(kakaoMapUrl);
      } else {
        await Linking.openURL(kakaoWebUrl);
      }
    } catch (e) {
      console.error("길찾기 열기 실패:", e);
    }
  }

  async function loadBeds(
    latitude: number,
    longitude: number,
    stage1: string
  ) {
    try {
      setBedsFailed(false);
      const data = await getEmergencyBeds(latitude, longitude, stage1);
      setBeds(data);
    } catch (e) {
      console.error("응급실 조회 실패:", e);
      setBeds([]);
      setBedsFailed(true);
    }
  }

  async function loadHospitals(latitude: number, longitude: number) {
    try {
      setHospitalsFailed(false);
      const data = await getNearbyHospitals(latitude, longitude);
      setHospitals(data);
    } catch (e) {
      console.error("병원 조회 실패:", e);
      setHospitals([]);
      setHospitalsFailed(true);
    }
  }

  async function loadPharmacies(latitude: number, longitude: number) {
    try {
      setPharmaciesFailed(false);
      const data = await getNearbyPharmacies(latitude, longitude);
      setPharmacies(data);
    } catch (e) {
      console.error("약국 조회 실패:", e);
      setPharmacies([]);
      setPharmaciesFailed(true);
    }
  }

    // 필터에 따라 지도에 표시할 마커 데이터 구성
  const allMarkerItems: {
    id: string;
    name: string;
    latitude: number | null;
    longitude: number | null;
    color: string;
  }[] = [
    ...(filter === "all" || filter === "emergency"
      ? beds.map((b) => ({
          id: `bed-${b.hpid}`,
          name: b.name,
          latitude: b.latitude,
          longitude: b.longitude,
          color: FILTER_COLORS.emergency,
        }))
      : []),
    ...(filter === "all" || filter === "hospital"
      ? hospitals.map((h) => ({
          id: `hospital-${h.ykiho}`,
          name: h.name,
          latitude: h.latitude,
          longitude: h.longitude,
          color: FILTER_COLORS.hospital,
        }))
      : []),
    ...(filter === "all" || filter === "pharmacy"
      ? pharmacies.map((p) => ({
          id: `pharmacy-${p.ykiho}`,
          name: p.name,
          latitude: p.latitude,
          longitude: p.longitude,
          color: FILTER_COLORS.pharmacy,
        }))
      : []),
  ];

  // 검색어가 있으면 마커도 같이 필터링
  const markerItems = searchText.trim()
    ? allMarkerItems.filter((item) =>
        item.name.toLowerCase().includes(searchText.trim().toLowerCase())
      )
    : allMarkerItems;

  // 필터에 따라 목록에 표시할 데이터 구성 (공통 형태로 변환)
    const listItems: {
    id: string;
    name: string;
    address: string;
    phone: string;
    distance: number | null;
    badge: string;
    category: string;
    ykiho: string | null;
    latitude: number | null;
    longitude: number | null;
    availableBeds: number | null;
    congestion: number | null;
  }[] = [
    ...(filter === "all" || filter === "emergency"
      ? beds.map((b) => ({
          id: `bed-${b.hpid}`,
          name: b.name,
          address: b.address,
          phone: b.phone,
          distance: b.distance,
          badge: `${b.availableBeds ?? "-"}병상`,
          category: "응급실",
          ykiho: null,
          latitude: b.latitude,
          longitude: b.longitude,
          availableBeds: b.availableBeds,
          congestion: b.congestion,
        }))
      : []),
    ...(filter === "all" || filter === "hospital"
      ? hospitals.map((h) => ({
          id: `hospital-${h.ykiho}`,
          name: h.name,
          address: h.address,
          phone: h.phone,
          distance: h.distance,
          badge: "병원",
          category: "병원",
          ykiho: h.ykiho,
          latitude: h.latitude,
          longitude: h.longitude,
          availableBeds: null,
          congestion: null,
        }))
      : []),
    ...(filter === "all" || filter === "pharmacy"
      ? pharmacies.map((p) => ({
          id: `pharmacy-${p.ykiho}`,
          name: p.name,
          address: p.address,
          phone: p.phone,
          distance: p.distance,
          badge: "약국",
          category: "약국",
          ykiho: p.ykiho,
          latitude: p.latitude,
          longitude: p.longitude,
          availableBeds: null,
          congestion: null,
        }))
      : []),
  ].sort((a, b) => {
    const distA = a.distance ?? Infinity;
    const distB = b.distance ?? Infinity;
    return distA - distB;
  });

  // 검색어 필터링
  const filteredItems = searchText.trim()
    ? listItems.filter((item) =>
        item.name.toLowerCase().includes(searchText.trim().toLowerCase())
      )
    : listItems;

  const visibleItems = filteredItems.slice(0, visibleCount);
  const hasMore = filteredItems.length > visibleCount;

  // 로딩 화면
  if (loading) {
    return (
      <SafeAreaView style={styles.center}>
        <ActivityIndicator size="large" />

        <Text style={styles.message}>
          주변 의료기관을 찾고 있어요...
        </Text>
      </SafeAreaView>
    );
  }

  // 에러 화면
  if (error) {
    return (
      <SafeAreaView style={styles.center}>
        <Text style={styles.error}>{error}</Text>
        <TouchableOpacity style={styles.retryButton} onPress={loadInitialData}>
          <Text style={styles.retryText}>다시 시도</Text>
        </TouchableOpacity>
      </SafeAreaView>
    );
  }

  // 조회에 실패한 항목은 빈 목록(=근처에 없음)과 구분해서 보여준다
  const failedSections: { label: string; retry: () => void }[] = [];
  if (location) {
    const { latitude, longitude } = location.coords;
    if (bedsFailed && stage1) {
      failedSections.push({ label: "응급실", retry: () => loadBeds(latitude, longitude, stage1) });
    }
    if (hospitalsFailed) {
      failedSections.push({ label: "병원", retry: () => loadHospitals(latitude, longitude) });
    }
    if (pharmaciesFailed) {
      failedSections.push({ label: "약국", retry: () => loadPharmacies(latitude, longitude) });
    }
  }

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.title}>병원·약국 찾기</Text>

      {/* 필터 탭 */}
      <View style={styles.filterRow}>
        {(
          [
            { key: "all", label: "전체" },
            { key: "hospital", label: "병원" },
            { key: "pharmacy", label: "약국" },
            { key: "emergency", label: "응급실" },
          ] as { key: FilterType; label: string }[]
        ).map((item) => (
          <View key={item.key} style={styles.filterButtonWrapper}>
            <Text
              style={[
                styles.filterButton,
                filter === item.key && {
                  backgroundColor: FILTER_COLORS[item.key],
                  color: "#fff",
                },
              ]}
              onPress={() => setFilter(item.key)}
            >
              {item.label}
            </Text>
          </View>
        ))}
      </View>

      {/* 검색창 */}
      <View style={styles.searchWrapper}>
        <TextInput
          style={styles.searchInput}
          placeholder="병원, 약국 이름 검색"
          value={searchText}
          onChangeText={setSearchText}
        />
      </View>

      {/* 지도 */}
      {outOfKorea && <View style={[styles.map, styles.mapPlaceholder]} />}
      {location && !outOfKorea && (
        <View style={styles.map}>
          <KakaoMapView
            centerLatitude={location.coords.latitude}
            centerLongitude={location.coords.longitude}
            markers={markerItems
              .filter(
                (item): item is typeof item & { latitude: number; longitude: number } =>
                  item.latitude != null && item.longitude != null
              )
              .map(
                (item): MapMarkerData => ({
                  id: item.id,
                  name: item.name,
                  latitude: item.latitude,
                  longitude: item.longitude,
                  color: item.color,
                })
              )}
          />
        </View>
      )}

      {/* 목록 */}
      <Text style={styles.sectionTitle}>
        주변 의료기관
      </Text>

      {failedSections.map((section) => (
        <TouchableOpacity key={section.label} style={styles.failBanner} onPress={section.retry}>
          <Text style={styles.failText}>{section.label} 정보를 불러오지 못했어요.</Text>
          <Text style={styles.failRetry}>다시 시도</Text>
        </TouchableOpacity>
      ))}

            <FlatList
        data={visibleItems}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
                    <TouchableOpacity
            style={styles.card}
                        activeOpacity={0.7}
                                    onPress={() => {
              if (item.category === "응급실") {
                setSelectedEmergencyBed({
                  name: item.name,
                  address: item.address,
                  phone: item.phone,
                  distance: item.distance,
                  latitude: item.latitude,
                  longitude: item.longitude,
                  availableBeds: item.availableBeds,
                  congestion: item.congestion,
                });
                return;
              }

              if (!item.ykiho) return;

              if (item.category === "약국") {
                setSelectedPharmacy({
                  name: item.name,
                  address: item.address,
                  phone: item.phone,
                  distance: item.distance,
                  latitude: item.latitude,
                  longitude: item.longitude,
                });
                return;
              }

              navigation.navigate("HospitalDetail", {
                ykiho: item.ykiho,
                name: item.name,
                address: item.address,
                phone: item.phone,
                distance: item.distance,
                latitude: item.latitude,
                longitude: item.longitude,
                availableBeds: item.availableBeds,
                congestion: item.congestion,
              });
            }}
          >
            <View style={styles.row}>
              <Text style={styles.name}>
                [{item.category}] {item.name}
              </Text>

              <Text style={styles.beds}>
                {item.badge}
              </Text>
            </View>

            <Text style={styles.distance}>
              {item.distance != null
                ? `${item.distance}km`
                : "거리 정보 없음"}
            </Text>

            <Text style={styles.address}>
              {item.address}
            </Text>
          </TouchableOpacity>
        )}
        ListEmptyComponent={
          <Text style={styles.message}>
            주변 의료기관 정보가 없습니다.
          </Text>
        }
                ListFooterComponent={
          hasMore ? (
            <Text
              style={styles.moreButton}
              onPress={() => setVisibleCount((prev) => prev + 5)}
            >
              더보기 ({filteredItems.length - visibleCount}개 더 있음)
            </Text>
          ) : null
        }
      />

            {/* 약국 바텀시트 */}
      <Modal
        visible={selectedPharmacy != null}
        transparent
        animationType="slide"
        onRequestClose={() => setSelectedPharmacy(null)}
      >
        <TouchableOpacity
          style={styles.modalBackdrop}
          activeOpacity={1}
          onPress={() => setSelectedPharmacy(null)}
        >
          <TouchableOpacity
            style={styles.sheet}
            activeOpacity={1}
            onPress={() => {}}
          >
            <View style={styles.sheetHandle} />

            {selectedPharmacy && (
              <>
                <Text style={styles.sheetTitle}>{selectedPharmacy.name}</Text>
                <Text style={styles.sheetAddress}>
                  {selectedPharmacy.address}
                </Text>
                <Text style={styles.sheetPhone}>{selectedPharmacy.phone}</Text>
                {selectedPharmacy.distance != null && (
                  <Text style={styles.sheetDistance}>
                    {selectedPharmacy.distance}km
                  </Text>
                )}

                <TouchableOpacity
                  style={styles.directionsButton}
                  onPress={openPharmacyDirections}
                >
                  <Text style={styles.directionsButtonText}>길찾기</Text>
                </TouchableOpacity>
              </>
            )}
          </TouchableOpacity>
        </TouchableOpacity>
      </Modal>

      {/* 응급실 바텀시트 */}
      <Modal
        visible={selectedEmergencyBed != null}
        transparent
        animationType="slide"
        onRequestClose={() => setSelectedEmergencyBed(null)}
      >
        <TouchableOpacity
          style={styles.modalBackdrop}
          activeOpacity={1}
          onPress={() => setSelectedEmergencyBed(null)}
        >
          <TouchableOpacity
            style={styles.sheet}
            activeOpacity={1}
            onPress={() => {}}
          >
            <View style={styles.sheetHandle} />

            {selectedEmergencyBed && (
              <>
                <Text style={styles.sheetTitle}>
                  {selectedEmergencyBed.name}
                </Text>
                <Text style={styles.sheetAddress}>
                  {selectedEmergencyBed.address}
                </Text>
                <Text style={styles.sheetPhone}>
                  {selectedEmergencyBed.phone}
                </Text>
                {selectedEmergencyBed.distance != null && (
                  <Text style={styles.sheetDistance}>
                    {selectedEmergencyBed.distance}km
                  </Text>
                )}

                {selectedEmergencyBed.availableBeds != null && (
                  <View style={styles.bedInfoBox}>
                    <Text style={styles.bedInfoTitle}>응급실 잔여 병상</Text>
                    <Text style={styles.bedInfoCount}>
                      {selectedEmergencyBed.availableBeds}병상
                    </Text>
                    {selectedEmergencyBed.congestion != null && (
                      <Text style={styles.bedInfoCongestion}>
                        혼잡도 {selectedEmergencyBed.congestion}%
                      </Text>
                    )}
                  </View>
                )}

                <TouchableOpacity
                  style={[
                    styles.directionsButton,
                    { backgroundColor: FILTER_COLORS.emergency },
                  ]}
                  onPress={openEmergencyBedDirections}
                >
                  <Text style={styles.directionsButtonText}>길찾기</Text>
                </TouchableOpacity>
              </>
            )}
          </TouchableOpacity>
        </TouchableOpacity>
      </Modal>

      {/* 한국 밖이면 화면 위에 반투명 안내를 덮어 뒤 화면이 비쳐 보이게 한다 */}
      {outOfKorea && (
        <View style={styles.outOfKoreaOverlay}>
          <View style={styles.outOfKoreaCard}>
            <CharacterSlot />
            <Text style={styles.notice}>
              응급이는 대한민국 안에서만{"\n"}주변 병원·약국·응급실을 찾을 수 있어요.
            </Text>
            <TouchableOpacity style={styles.retryButton} onPress={loadInitialData}>
              <Text style={styles.retryText}>위치 다시 확인</Text>
            </TouchableOpacity>
          </View>
        </View>
      )}
    </SafeAreaView>
  );
}
const styles = StyleSheet.create({
  container: {
    flex: 1,
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
    paddingHorizontal: 20,
    paddingTop: 8,
    paddingBottom: 12,
  },

  filterRow: {
    flexDirection: "row",
    paddingHorizontal: 20,
    paddingBottom: 12,
    gap: 8,
  },

  filterButtonWrapper: {
    flex: 1,
  },

  filterButton: {
    textAlign: "center",
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: "#f0f0f0",
    color: "#666",
    fontWeight: "600",
    overflow: "hidden",
  },

  searchWrapper: {
    paddingHorizontal: 20,
    paddingBottom: 12,
  },

  searchInput: {
    backgroundColor: "#f0f0f0",
    borderRadius: 10,
    paddingHorizontal: 14,
    paddingVertical: 10,
    fontSize: 15,
  },

  map: {
    width: "100%",
    height: 300,
  },

  sectionTitle: {
    fontSize: 20,
    fontWeight: "700",
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 10,
  },

  card: {
    padding: 16,
    marginHorizontal: 20,
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
    fontSize: 14,
    fontWeight: "700",
    color: "#1E88E5",
  },

  distance: {
    marginTop: 8,
    fontSize: 14,
  },

  address: {
    marginTop: 4,
    color: "#666",
  },

  message: {
    marginTop: 12,
    paddingHorizontal: 20,
    color: "#666",
  },

  error: {
    color: "#d00",
    textAlign: "center",
  },

  outOfKoreaOverlay: {
    position: "absolute",
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "rgba(255,255,255,0.45)",
  },

  outOfKoreaCard: {
    alignItems: "center",
    marginHorizontal: 32,
    paddingVertical: 28,
    paddingHorizontal: 24,
    borderRadius: 20,
    backgroundColor: "rgba(255,255,255,0.95)",
    shadowColor: "#000",
    shadowOpacity: 0.08,
    shadowRadius: 16,
    shadowOffset: { width: 0, height: 4 },
    elevation: 4,
  },

  mapPlaceholder: {
    backgroundColor: "#ECECEE",
  },

  notice: {
    marginTop: 20,
    paddingHorizontal: 32,
    color: "#444",
    textAlign: "center",
    lineHeight: 22,
  },

  retryButton: {
    marginTop: 20,
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 12,
    backgroundColor: colors.primary,
  },

  retryText: {
    color: colors.white,
    fontWeight: "700",
  },

  failBanner: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginHorizontal: 20,
    marginBottom: 8,
    padding: 12,
    borderRadius: 10,
    backgroundColor: "#FFF4E5",
  },

  failText: {
    color: "#8A5A00",
  },

  failRetry: {
    color: "#1E88E5",
    fontWeight: "700",
  },

  markerLabel: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 6,
    maxWidth: 120,
  },

  markerText: {
    color: "#fff",
    fontWeight: "700",
    fontSize: 12,
  },

    moreButton: {
    textAlign: "center",
    paddingVertical: 14,
    marginHorizontal: 20,
    marginBottom: 20,
    borderRadius: 12,
    backgroundColor: "#f0f0f0",
    color: "#1E88E5",
    fontWeight: "700",
  },

  modalBackdrop: {
    flex: 1,
    justifyContent: "flex-end",
    backgroundColor: "rgba(0,0,0,0.4)",
  },

  sheet: {
    backgroundColor: "#fff",
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: 20,
    paddingBottom: 32,
  },

  sheetHandle: {
    alignSelf: "center",
    width: 40,
    height: 4,
    borderRadius: 2,
    backgroundColor: "#ddd",
    marginBottom: 16,
  },

  sheetTitle: {
    fontSize: 20,
    fontWeight: "700",
    marginBottom: 8,
  },

  sheetAddress: {
    fontSize: 15,
    color: "#333",
    marginBottom: 4,
  },

  sheetPhone: {
    fontSize: 15,
    color: "#43A047",
    marginBottom: 4,
  },

  sheetDistance: {
    fontSize: 14,
    color: "#666",
  },

  directionsButton: {
    marginTop: 16,
    paddingVertical: 12,
    borderRadius: 8,
    backgroundColor: "#43A047",
    alignItems: "center",
  },

    directionsButtonText: {
    color: "#fff",
    fontWeight: "700",
    fontSize: 15,
  },

  bedInfoBox: {
    marginTop: 12,
    padding: 12,
    borderRadius: 10,
    backgroundColor: "#FFEBEE",
  },

  bedInfoTitle: {
    fontSize: 13,
    color: "#E53935",
    fontWeight: "600",
  },

  bedInfoCount: {
    fontSize: 20,
    fontWeight: "700",
    color: "#E53935",
    marginTop: 4,
  },

  bedInfoCongestion: {
    fontSize: 12,
    color: "#E53935",
    marginTop: 4,
  },
});