import { useEffect, useState } from "react";
import {
  ActivityIndicator,
  Linking,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { RouteProp, useNavigation, useRoute } from "@react-navigation/native";
import { NativeStackNavigationProp } from "@react-navigation/native-stack";
import { RootStackParamList } from "../../../navigation/types";
import { getHospitalDetail } from "../api/hospitalDetail";
import { HospitalDetail } from "../types/hospitalDetail";

type HospitalDetailRouteProp = RouteProp<RootStackParamList, "HospitalDetail">;
type NavigationProp = NativeStackNavigationProp<RootStackParamList>;

type TabType = "info" | "checklist";

const DAY_LABELS: { key: keyof HospitalDetail; label: string; endKey: keyof HospitalDetail }[] = [
  { key: "mondayStart", endKey: "mondayEnd", label: "월요일" },
  { key: "tuesdayStart", endKey: "tuesdayEnd", label: "화요일" },
  { key: "wednesdayStart", endKey: "wednesdayEnd", label: "수요일" },
  { key: "thursdayStart", endKey: "thursdayEnd", label: "목요일" },
  { key: "fridayStart", endKey: "fridayEnd", label: "금요일" },
  { key: "saturdayStart", endKey: "saturdayEnd", label: "토요일" },
];

const CHECKLIST_ITEMS = [
  { id: "id_card", label: "신분증" },
  { id: "insurance", label: "건강보험증 / 진료의뢰서" },
  { id: "medication", label: "복용 중인 약 목록" },
  { id: "symptom_memo", label: "증상 메모 또는 사진" },
];

function formatTime(value: string | null | undefined) {
  if (!value || value.length !== 4) return null;
  return `${value.slice(0, 2)}:${value.slice(2, 4)}`;
}

export default function HospitalDetailScreen() {
  const navigation = useNavigation<NavigationProp>();
  const route = useRoute<HospitalDetailRouteProp>();
  const {
    ykiho,
    name,
    address,
    phone,
    distance,
    latitude,
    longitude,
    availableBeds,
    congestion,
  } = route.params;

  const [activeTab, setActiveTab] = useState<TabType>("info");
  const [detail, setDetail] = useState<HospitalDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [checkedItems, setCheckedItems] = useState<Record<string, boolean>>(
    {}
  );

  useEffect(() => {
    loadDetail();
  }, [ykiho]);

  async function openDirections() {
    if (latitude == null || longitude == null) {
      return;
    }

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

  async function loadDetail() {
    try {
      setLoading(true);
      setError(null);
      const data = await getHospitalDetail(ykiho);
      setDetail(data);
    } catch (e) {
      console.error("병원 상세정보 조회 실패:", e);
      setError(
        e instanceof Error ? e.message : "병원 상세정보를 불러오지 못했습니다."
      );
    } finally {
      setLoading(false);
    }
  }

  function toggleChecklistItem(id: string) {
    setCheckedItems((prev) => ({ ...prev, [id]: !prev[id] }));
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.backButton} onPress={() => navigation.goBack()}>
          {"< 뒤로"}
        </Text>
        <Text style={styles.title}>{name}</Text>
      </View>

      {/* 탭 */}
      <View style={styles.tabRow}>
        <TouchableOpacity
          style={styles.tabButton}
          onPress={() => setActiveTab("info")}
        >
          <Text
            style={[
              styles.tabLabel,
              activeTab === "info" && styles.tabLabelActive,
            ]}
          >
            기본정보
          </Text>
          {activeTab === "info" && <View style={styles.tabIndicator} />}
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.tabButton}
          onPress={() => setActiveTab("checklist")}
        >
          <Text
            style={[
              styles.tabLabel,
              activeTab === "checklist" && styles.tabLabelActive,
            ]}
          >
            방문준비
          </Text>
          {activeTab === "checklist" && <View style={styles.tabIndicator} />}
        </TouchableOpacity>
      </View>

      {activeTab === "info" ? (
        <ScrollView contentContainerStyle={styles.content}>
          {/* 기본 정보 (목록에서 넘겨받은 값, 항상 표시) */}
          <View style={styles.section}>
            <Text style={styles.address}>{address}</Text>
            <Text style={styles.phone}>{phone}</Text>
            {distance != null && (
              <Text style={styles.distance}>{distance}km</Text>
            )}

            {latitude != null && longitude != null && (
              <TouchableOpacity
                style={styles.directionsButton}
                onPress={openDirections}
              >
                <Text style={styles.directionsButtonText}>길찾기</Text>
              </TouchableOpacity>
            )}
          </View>

          {/* 응급실 잔여 병상 (A안: 응급실 카테고리에서 들어온 경우에만 존재) */}
          {availableBeds != null && (
            <View style={styles.bedSection}>
              <Text style={styles.bedTitle}>응급실 잔여 병상</Text>
              <Text style={styles.bedCount}>{availableBeds}병상</Text>
              {congestion != null && (
                <Text style={styles.congestion}>혼잡도 {congestion}%</Text>
              )}
            </View>
          )}

          {/* 진료시간/진료과목 (상세 API) */}
          {loading && (
            <View style={styles.center}>
              <ActivityIndicator size="large" />
              <Text style={styles.message}>상세정보를 불러오고 있어요...</Text>
            </View>
          )}

          {error && (
            <View style={styles.center}>
              <Text style={styles.error}>{error}</Text>
            </View>
          )}

          {detail && (
            <>
              <View style={styles.section}>
                <Text style={styles.sectionTitle}>진료시간</Text>
                {DAY_LABELS.map(({ key, endKey, label }) => {
                  const start = formatTime(detail[key] as string | null);
                  const end = formatTime(detail[endKey] as string | null);
                  return (
                    <View key={label} style={styles.timeRow}>
                      <Text style={styles.dayLabel}>{label}</Text>
                      <Text style={styles.timeValue}>
                        {start && end ? `${start} - ${end}` : "정보 없음"}
                      </Text>
                    </View>
                  );
                })}
                {detail.lunchTime && (
                  <Text style={styles.lunchTime}>
                    점심시간 {detail.lunchTime}
                  </Text>
                )}
                {detail.closedOnSunday && (
                  <Text style={styles.closedInfo}>
                    일요일: {detail.closedOnSunday}
                  </Text>
                )}
                {detail.closedOnHoliday && (
                  <Text style={styles.closedInfo}>
                    공휴일: {detail.closedOnHoliday}
                  </Text>
                )}
              </View>

              <View style={styles.section}>
                <Text style={styles.sectionTitle}>진료과목</Text>
                {detail.departments.length === 0 ? (
                  <Text style={styles.message}>
                    등록된 진료과목이 없습니다.
                  </Text>
                ) : (
                  <View style={styles.departmentWrap}>
                    {detail.departments.map((dept) => (
                      <View key={dept.name} style={styles.departmentChip}>
                        <Text style={styles.departmentText}>
                          {dept.name}
                        </Text>
                      </View>
                    ))}
                  </View>
                )}
              </View>
            </>
          )}
        </ScrollView>
      ) : (
        <ScrollView contentContainerStyle={styles.content}>
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>준비물 체크리스트</Text>

            {CHECKLIST_ITEMS.map((item) => {
              const checked = !!checkedItems[item.id];
              return (
                <TouchableOpacity
                  key={item.id}
                  style={styles.checklistRow}
                  onPress={() => toggleChecklistItem(item.id)}
                >
                  <View
                    style={[
                      styles.checkbox,
                      checked && styles.checkboxChecked,
                    ]}
                  >
                    {checked && <Text style={styles.checkboxMark}>✓</Text>}
                  </View>
                  <Text style={styles.checklistLabel}>{item.label}</Text>
                </TouchableOpacity>
              );
            })}
          </View>
        </ScrollView>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#fff",
  },

  header: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 20,
    paddingVertical: 12,
  },

  backButton: {
    fontSize: 15,
    color: "#1E88E5",
    marginRight: 12,
  },

  title: {
    fontSize: 20,
    fontWeight: "700",
    flex: 1,
  },

  tabRow: {
    flexDirection: "row",
    borderBottomWidth: 1,
    borderBottomColor: "#f0f0f0",
  },

  tabButton: {
    flex: 1,
    alignItems: "center",
    paddingVertical: 12,
  },

  tabLabel: {
    fontSize: 15,
    color: "#999",
    fontWeight: "600",
  },

  tabLabelActive: {
    color: "#1E88E5",
  },

  tabIndicator: {
    marginTop: 8,
    height: 2,
    width: "60%",
    backgroundColor: "#1E88E5",
  },

  content: {
    paddingBottom: 40,
  },

  section: {
    paddingHorizontal: 20,
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: "#f0f0f0",
  },

  sectionTitle: {
    fontSize: 17,
    fontWeight: "700",
    marginBottom: 12,
  },

  address: {
    fontSize: 15,
    marginBottom: 4,
  },

  phone: {
    fontSize: 15,
    color: "#1E88E5",
    marginBottom: 4,
  },

  distance: {
    fontSize: 14,
    color: "#666",
  },

  directionsButton: {
    marginTop: 12,
    paddingVertical: 10,
    borderRadius: 8,
    backgroundColor: "#1E88E5",
    alignItems: "center",
  },

  directionsButtonText: {
    color: "#fff",
    fontWeight: "700",
    fontSize: 14,
  },

  bedSection: {
    marginHorizontal: 20,
    marginVertical: 12,
    padding: 16,
    borderRadius: 12,
    backgroundColor: "#FFEBEE",
  },

  bedTitle: {
    fontSize: 14,
    color: "#E53935",
    fontWeight: "600",
  },

  bedCount: {
    fontSize: 24,
    fontWeight: "700",
    color: "#E53935",
    marginTop: 4,
  },

  congestion: {
    fontSize: 13,
    color: "#E53935",
    marginTop: 4,
  },

  center: {
    padding: 20,
    alignItems: "center",
  },

  message: {
    color: "#666",
  },

  error: {
    color: "#d00",
  },

  timeRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    paddingVertical: 4,
  },

  dayLabel: {
    fontSize: 14,
    color: "#666",
  },

  timeValue: {
    fontSize: 14,
    fontWeight: "600",
  },

  lunchTime: {
    marginTop: 8,
    fontSize: 13,
    color: "#666",
  },

  closedInfo: {
    fontSize: 13,
    color: "#666",
  },

  departmentWrap: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
  },

  departmentChip: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
    backgroundColor: "#E3F2FD",
  },

  departmentText: {
    fontSize: 13,
    color: "#1E88E5",
    fontWeight: "600",
  },

  checklistRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 12,
  },

  checkbox: {
    width: 24,
    height: 24,
    borderRadius: 6,
    borderWidth: 2,
    borderColor: "#ccc",
    marginRight: 12,
    alignItems: "center",
    justifyContent: "center",
  },

  checkboxChecked: {
    backgroundColor: "#1E88E5",
    borderColor: "#1E88E5",
  },

  checkboxMark: {
    color: "#fff",
    fontWeight: "700",
    fontSize: 14,
  },

  checklistLabel: {
    fontSize: 15,
  },
});