import { useEffect, useState } from "react";
import {
  ActivityIndicator,
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
import AsyncStorage from "@react-native-async-storage/async-storage";
import { getHospitalDetail } from "../api/hospitalDetail";
import { HospitalDetail } from "../types/hospitalDetail";
import { Ionicons } from "@expo/vector-icons";
import { colors } from "../../../shared/theme/theme";
import {
  DAY_LABELS,
  NO_INFO_MESSAGE,
  formatTime,
  getWeekdayHoursSummary,
} from "../utils/hospitalHours";
import { openDirections as openMapDirections } from "../utils/openDirections";

type HospitalDetailRouteProp = RouteProp<RootStackParamList, "HospitalDetail">;
type NavigationProp = NativeStackNavigationProp<RootStackParamList>;

type TabType = "info" | "checklist";

const CHECKLIST_ITEMS = [
  { id: "id_card", label: "신분증" },
  { id: "insurance", label: "건강보험증 / 진료의뢰서" },
  { id: "medication", label: "복용 중인 약 목록" },
  { id: "symptom_memo", label: "증상 메모 또는 사진" },
];

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
    hasEmergency,
    category,
  } = route.params;
  // 병원은 파랑, 응급실 탭에서 들어오면 빨강 (목록/시트와 동일한 색 구분)
  const accentColor = category === "응급실" ? "#E53935" : "#1E88E5";
  const isEmergencyHospital = hasEmergency ?? availableBeds != null;

  const [activeTab, setActiveTab] = useState<TabType>("info");
  const [detail, setDetail] = useState<HospitalDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [checkedItems, setCheckedItems] = useState<Record<string, boolean>>(
    {}
  );

  useEffect(() => {
    loadDetail();
    loadChecklist();
  }, [ykiho]);

  async function loadChecklist() {
    try {
      const saved = await AsyncStorage.getItem(`checklist_${ykiho}`);
      if (saved) {
        setCheckedItems(JSON.parse(saved));
      } else {
        setCheckedItems({});
      }
    } catch (e) {
      console.error("체크리스트 불러오기 실패:", e);
    }
  }

  function openDirections() {
    return openMapDirections(name, latitude, longitude);
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
    setCheckedItems((prev) => {
      const updated = { ...prev, [id]: !prev[id] };
      AsyncStorage.setItem(`checklist_${ykiho}`, JSON.stringify(updated)).catch(
        (e) => console.error("체크리스트 저장 실패:", e)
      );
      return updated;
    });
  }

  const hoursSummary = getWeekdayHoursSummary(detail);

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity
          style={styles.backButton}
          onPress={() => navigation.goBack()}
        >
          <Ionicons name="chevron-back" size={26} color="#222" />
        </TouchableOpacity>

        <View style={styles.titleWrap}>
          <Text style={styles.title} numberOfLines={1}>
            {name}
          </Text>
          {isEmergencyHospital && (
            <View style={styles.erTag}>
              <View style={styles.erDot} />
              <Text style={styles.erTagText}>응급실 운영</Text>
            </View>
          )}
        </View>

        {latitude != null && longitude != null && (
          <TouchableOpacity
            style={[styles.directionsButton, { backgroundColor: accentColor }]}
            onPress={openDirections}
          >
            <Ionicons name="navigate-outline" size={16} color="#fff" />
            <Text style={styles.directionsButtonText}>길찾기</Text>
          </TouchableOpacity>
        )}
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
              activeTab === "info" && { color: accentColor },
            ]}
          >
            기본정보
          </Text>
          {activeTab === "info" && (
            <View style={[styles.tabIndicator, { backgroundColor: accentColor }]} />
          )}
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.tabButton}
          onPress={() => setActiveTab("checklist")}
        >
          <Text
            style={[
              styles.tabLabel,
              activeTab === "checklist" && { color: accentColor },
            ]}
          >
            방문준비
          </Text>
          {activeTab === "checklist" && (
            <View style={[styles.tabIndicator, { backgroundColor: accentColor }]} />
          )}
        </TouchableOpacity>
      </View>

      {activeTab === "info" ? (
        <ScrollView contentContainerStyle={styles.content}>
          {/* 기본 정보 (목록에서 넘겨받은 값, 항상 표시) */}
          <View style={styles.infoSection}>
            <View style={styles.infoRow}>
              <Ionicons name="location-outline" size={18} color="#999" />
              <Text style={styles.infoText}>{address}</Text>
            </View>
            <View style={styles.infoRow}>
              <Ionicons name="call-outline" size={18} color="#999" />
              <Text style={styles.infoText}>{phone}</Text>
            </View>
            {(hoursSummary || isEmergencyHospital) && (
              <View style={styles.infoRow}>
                <Ionicons name="time-outline" size={18} color="#999" />
                <Text style={styles.infoText}>
                  {[hoursSummary, isEmergencyHospital ? "응급실 24시" : null]
                    .filter(Boolean)
                    .join(" / ")}
                </Text>
              </View>
            )}
            {distance != null && (
              <View style={styles.infoRow}>
                <Ionicons name="walk-outline" size={18} color="#999" />
                <Text style={styles.infoText}>{distance}km</Text>
              </View>
            )}
          </View>

          {/* 응급실 잔여 병상 (응급실이 있는 병원이고 병상 정보가 있을 때) */}
          {availableBeds != null && (
            <View style={styles.bedSection}>
              <View style={styles.bedLeft}>
                <Ionicons name="pulse-outline" size={20} color="#2E7D32" />
                <View>
                  <Text style={styles.bedTitle}>응급실 잔여 병상</Text>
                  {congestion != null && (
                    <Text style={styles.congestion}>혼잡도 {congestion}%</Text>
                  )}
                </View>
              </View>
              <Text style={styles.bedCount}>
                {availableBeds}
                <Text style={styles.bedUnit}>병상</Text>
              </Text>
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
                <Text style={styles.sectionTitle}>진료과목</Text>
                {detail.departments.length === 0 ? (
                  <Text style={styles.message}>{NO_INFO_MESSAGE}</Text>
                ) : (
                  <View style={styles.departmentWrap}>
                    {detail.departments.map((dept) => (
                      <View key={dept.name} style={styles.departmentChip}>
                        <Text style={styles.departmentText}>{dept.name}</Text>
                      </View>
                    ))}
                  </View>
                )}
              </View>

              <View style={styles.section}>
                <Text style={styles.sectionTitle}>진료시간</Text>
                {DAY_LABELS.every(({ key, endKey }) => {
                  return (
                    !formatTime(detail[key] as string | null) ||
                    !formatTime(detail[endKey] as string | null)
                  );
                }) ? (
                  <Text style={styles.message}>{NO_INFO_MESSAGE}</Text>
                ) : (
                  DAY_LABELS.map(({ key, endKey, label }) => {
                    const start = formatTime(detail[key] as string | null);
                    const end = formatTime(detail[endKey] as string | null);
                    return (
                      <View key={label} style={styles.timeRow}>
                        <Text style={styles.dayLabel}>{label}</Text>
                        <Text style={styles.timeValue}>
                          {start && end ? `${start} - ${end}` : "휴진"}
                        </Text>
                      </View>
                    );
                  })
                )}
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
                      checked && {
                      backgroundColor: accentColor,
                      borderColor: accentColor,
                    },
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
    paddingRight: 16,
    paddingVertical: 12,
  },

  backButton: {
    paddingHorizontal: 12,
    paddingVertical: 8,
  },

  titleWrap: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
  },

  title: {
    flexShrink: 1,
    fontSize: 20,
    fontWeight: "700",
  },

  erTag: {
    flexDirection: "row",
    alignItems: "center",
    marginLeft: 8,
  },

  erDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: "#2E7D32",
    marginRight: 4,
  },

  erTagText: {
    fontSize: 13,
    fontWeight: "700",
    color: "#2E7D32",
  },

  tabRow: {
    flexDirection: "row",
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
    color: colors.primary,
  },

  tabIndicator: {
    marginTop: 8,
    height: 2,
    width: "100%",
    backgroundColor: colors.primary,
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

  infoSection: {
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 8,
  },

  infoRow: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 12,
  },

  infoText: {
    flex: 1,
    marginLeft: 10,
    fontSize: 15,
    color: "#555",
  },

  directionsButton: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: 8,
    backgroundColor: colors.primary,
  },

  directionsButtonText: {
    marginLeft: 4,
    color: "#fff",
    fontWeight: "700",
    fontSize: 14,
  },

  bedSection: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginHorizontal: 20,
    marginVertical: 12,
    padding: 16,
    borderRadius: 12,
    backgroundColor: "#E8F5E9",
  },

  bedLeft: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },

  bedTitle: {
    fontSize: 15,
    color: "#2E7D32",
    fontWeight: "700",
  },

  congestion: {
    fontSize: 12,
    color: "#2E7D32",
    marginTop: 2,
  },

  bedCount: {
    fontSize: 28,
    fontWeight: "700",
    color: "#2E7D32",
  },

  bedUnit: {
    fontSize: 14,
    fontWeight: "600",
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
    backgroundColor: "#F1F1F1",
    borderWidth: 1,
    borderColor: "#E3E3E3",
  },

  departmentText: {
    fontSize: 13,
    color: "#333",
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
    backgroundColor: colors.primary,
    borderColor: colors.primary,
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