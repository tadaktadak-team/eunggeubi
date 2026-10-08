import { useEffect, useRef, useState } from 'react';
import { Linking, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import BottomSheet, { BottomSheetScrollView } from '@gorhom/bottom-sheet';
import { Ionicons } from '@expo/vector-icons';
import Animated, { interpolate, useAnimatedStyle, useSharedValue } from 'react-native-reanimated';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { getHospitalDetail } from '../api/hospitalDetail';
import { HospitalDetail } from '../types/hospitalDetail';
import {
  DAY_LABELS,
  NO_INFO_MESSAGE,
  formatTime,
  getTodayHoursText,
  getWeekdayHoursSummary,
} from '../utils/hospitalHours';
import { CHECKLIST_ITEMS } from '../utils/visitChecklist';
import { openDirections } from '../utils/openDirections';

export interface SheetItem {
  id: string;
  name: string;
  address: string;
  phone: string;
  distance: number | null;
  category: string;
  ykiho: string | null;
  latitude: number | null;
  longitude: number | null;
  availableBeds: number | null;
  congestion: number | null;
  hasEmergency: boolean;
}

interface FacilitySheetProps {
  item: SheetItem;
  accentColor: string;
  onClose: () => void;
}

type TabType = 'info' | 'checklist';

// 0: 미리보기(접힌 상태), 1: 화면 가득(펼친 상태)
// 미리보기 높이는 (이름+버튼+요약) 실제 높이에 맞춰 계산한다. 측정 전 임시값만 비율로 둔다.
const HANDLE_HEIGHT = 28;

export default function FacilitySheet({ item, accentColor, onClose }: FacilitySheetProps) {
  const insets = useSafeAreaInsets();
  const sheetRef = useRef<BottomSheet>(null);
  const [previewHeight, setPreviewHeight] = useState<number | null>(null);
  const previewSnap = previewHeight != null ? previewHeight + HANDLE_HEIGHT : '30%';
  // 시트가 올라온 정도(0=미리보기, 1=가득). 맨 위 바를 서서히 보이게 하는 데 쓴다
  const animatedIndex = useSharedValue(0);
  const topBarStyle = useAnimatedStyle(() => ({
    height: interpolate(animatedIndex.value, [0, 1], [0, 48], 'clamp'),
    opacity: interpolate(animatedIndex.value, [0.5, 1], [0, 1], 'clamp'),
  }));
  const [activeTab, setActiveTab] = useState<TabType>('info');
  const [detail, setDetail] = useState<HospitalDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState(false);
  const [checkedItems, setCheckedItems] = useState<Record<string, boolean>>({});

  const isHospital = item.category !== '약국' && item.ykiho != null;
  // 약국은 보여줄 상세 정보가 없어서 미리보기 높이로만 열린다 (위로 펼쳐지지 않음)
  const snapPoints = item.category === '약국' ? [previewSnap] : [previewSnap, '100%'];

  // 병원일 때만 진료과목/진료시간/체크리스트를 불러온다
  useEffect(() => {
    if (!isHospital || !item.ykiho) return;

    let cancelled = false;
    setDetailLoading(true);
    getHospitalDetail(item.ykiho)
      .then((data) => {
        if (!cancelled) setDetail(data);
      })
      .catch((e) => {
        console.error('병원 상세정보 조회 실패:', e);
        if (!cancelled) setDetailError(true);
      })
      .finally(() => {
        if (!cancelled) setDetailLoading(false);
      });

    AsyncStorage.getItem(`checklist_${item.ykiho}`)
      .then((saved) => {
        if (!cancelled && saved) setCheckedItems(JSON.parse(saved));
      })
      .catch((e) => console.error('체크리스트 불러오기 실패:', e));

    return () => {
      cancelled = true;
    };
  }, [item.id]);

  function toggleChecklistItem(id: string) {
    setCheckedItems((prev) => {
      const updated = { ...prev, [id]: !prev[id] };
      AsyncStorage.setItem(`checklist_${item.ykiho}`, JSON.stringify(updated)).catch((e) =>
        console.error('체크리스트 저장 실패:', e),
      );
      return updated;
    });
  }

  // 요약 한 줄: 평일 시간이 같으면 평일 요약, 아니면 오늘 정보
  const hoursText = [
    getWeekdayHoursSummary(detail) ??
      getTodayHoursText(detail) ??
      (detail ? NO_INFO_MESSAGE : null),
    item.hasEmergency ? '응급실 24시' : null,
  ]
    .filter(Boolean)
    .join(' / ');

  const hasHours =
    detail != null &&
    DAY_LABELS.some(
      ({ key, endKey }) =>
        formatTime(detail[key] as string | null) && formatTime(detail[endKey] as string | null),
    );

  const erTag =
    item.hasEmergency && item.category === '병원' ? (
      <View style={styles.erTag}>
        <View style={styles.erDot} />
        <Text style={styles.erTagText}>응급실 운영</Text>
      </View>
    ) : null;

  const actionButtons = (
    <View style={styles.actionRow}>
      {item.latitude != null && item.longitude != null && (
        <TouchableOpacity
          style={[styles.actionFilled, { backgroundColor: accentColor }]}
          onPress={() => openDirections(item.name, item.latitude, item.longitude)}
        >
          <Ionicons name="navigate-outline" size={16} color="#fff" />
          <Text style={styles.actionFilledText}>길찾기</Text>
        </TouchableOpacity>
      )}
      {!!item.phone && (
        <TouchableOpacity
          style={styles.actionOutline}
          onPress={() => Linking.openURL(`tel:${item.phone}`)}
        >
          <Ionicons name="call-outline" size={16} color="#333" />
          <Text style={styles.actionOutlineText}>전화</Text>
        </TouchableOpacity>
      )}
    </View>
  );

  // 주소 / 진료시간 요약 / 거리 (미리보기와 펼친 화면 공통)
  const summary = (
    <View style={styles.summary}>
      <View style={styles.summaryRow}>
        <Ionicons name="location-outline" size={18} color="#999" />
        <Text style={styles.summaryText}>{item.address}</Text>
      </View>
      {!!hoursText && (
        <View style={styles.summaryRow}>
          <Ionicons name="time-outline" size={18} color="#999" />
          <Text style={styles.summaryText}>{hoursText}</Text>
        </View>
      )}
      {item.distance != null && (
        <View style={styles.summaryRow}>
          <Ionicons name="walk-outline" size={18} color="#999" />
          <Text style={styles.summaryText}>{item.distance}km</Text>
        </View>
      )}
    </View>
  );

  return (
    <BottomSheet
      ref={sheetRef}
      snapPoints={snapPoints}
      enableDynamicSizing={false}
      enableOverDrag={item.category !== '약국'}
      index={0}
      animatedIndex={animatedIndex}
      topInset={insets.top}
      enablePanDownToClose
      onClose={onClose}
      handleIndicatorStyle={styles.handle}
    >
      {/* 맨 위 바: 끌어올릴수록 서서히 나타남 (뒤로가기 + 가운데 병원 이름) */}
      <Animated.View style={[styles.topBar, topBarStyle]}>
        <TouchableOpacity style={styles.backButton} onPress={() => sheetRef.current?.close()}>
          <Ionicons name="chevron-back" size={26} color="#222" />
        </TouchableOpacity>
      </Animated.View>

      {/* 미리보기와 상세가 하나로 이어진 내용: 시트가 올라온 만큼 아래가 드러남 */}
      <BottomSheetScrollView
        scrollEnabled={item.category !== '약국'}
        contentContainerStyle={styles.content}
      >
        <View onLayout={(e) => setPreviewHeight(e.nativeEvent.layout.height)}>
          <View style={styles.previewHeader}>
            <View style={styles.previewTitleWrap}>
              <Text style={styles.previewTitle} numberOfLines={2}>
                {item.name}
              </Text>
              {erTag}
            </View>
            {actionButtons}
          </View>

          {summary}
        </View>

        {isHospital && (
          <View style={styles.tabRow}>
            {(
              [
                { key: 'info', label: '기본정보' },
                { key: 'checklist', label: '방문준비 체크리스트' },
              ] as { key: TabType; label: string }[]
            ).map((tab) => (
              <TouchableOpacity
                key={tab.key}
                style={styles.tabButton}
                onPress={() => setActiveTab(tab.key)}
              >
                <Text style={[styles.tabLabel, activeTab === tab.key && { color: accentColor }]}>
                  {tab.label}
                </Text>
                {activeTab === tab.key && (
                  <View style={[styles.tabIndicator, { backgroundColor: accentColor }]} />
                )}
              </TouchableOpacity>
            ))}
          </View>
        )}

        {activeTab === 'info' || !isHospital ? (
          <>
            {item.availableBeds != null && (
              <View style={styles.bedSection}>
                <View style={styles.bedLeft}>
                  <Ionicons name="pulse-outline" size={20} color="#2E7D32" />
                  <View>
                    <Text style={styles.bedTitle}>응급실 잔여 병상</Text>
                    {item.congestion != null && (
                      <Text style={styles.congestion}>혼잡도 {item.congestion}%</Text>
                    )}
                  </View>
                </View>
                <Text style={styles.bedCount}>
                  {item.availableBeds}
                  <Text style={styles.bedUnit}>병상</Text>
                </Text>
              </View>
            )}

            {isHospital && (
              <>
                {detailLoading && <Text style={styles.message}>상세정보를 불러오고 있어요...</Text>}
                {detailError && (
                  <Text style={styles.error}>
                    상세정보를 불러오지 못했어요. 잠시 후 다시 시도해주세요.
                  </Text>
                )}
                {detail && (
                  <>
                    <View style={styles.section}>
                      <Text style={styles.sectionTitle}>진료과목</Text>
                      {detail.departments.length === 0 ? (
                        <Text style={styles.messageInline}>{NO_INFO_MESSAGE}</Text>
                      ) : (
                        <View style={styles.chipWrap}>
                          {detail.departments.map((dept) => (
                            <View key={dept.name} style={styles.chip}>
                              <Text style={styles.chipText}>{dept.name}</Text>
                            </View>
                          ))}
                        </View>
                      )}
                    </View>

                    <View style={styles.section}>
                      <Text style={styles.sectionTitle}>진료시간</Text>
                      {!hasHours ? (
                        <Text style={styles.messageInline}>{NO_INFO_MESSAGE}</Text>
                      ) : (
                        DAY_LABELS.map(({ key, endKey, label }) => {
                          const start = formatTime(detail[key] as string | null);
                          const end = formatTime(detail[endKey] as string | null);
                          return (
                            <View key={label} style={styles.timeRow}>
                              <Text style={styles.dayLabel}>{label}</Text>
                              <Text style={styles.timeValue}>
                                {start && end ? `${start} - ${end}` : '휴진'}
                              </Text>
                            </View>
                          );
                        })
                      )}
                      {detail.lunchTime && (
                        <Text style={styles.note}>점심시간 {detail.lunchTime}</Text>
                      )}
                      {detail.closedOnSunday && (
                        <Text style={styles.note}>일요일: {detail.closedOnSunday}</Text>
                      )}
                      {detail.closedOnHoliday && (
                        <Text style={styles.note}>공휴일: {detail.closedOnHoliday}</Text>
                      )}
                    </View>
                  </>
                )}
              </>
            )}
          </>
        ) : (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>준비물 체크리스트</Text>
            {CHECKLIST_ITEMS.map((check) => {
              const checked = !!checkedItems[check.id];
              return (
                <TouchableOpacity
                  key={check.id}
                  style={styles.checklistRow}
                  onPress={() => toggleChecklistItem(check.id)}
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
                  <Text style={styles.checklistLabel}>{check.label}</Text>
                </TouchableOpacity>
              );
            })}
          </View>
        )}
      </BottomSheetScrollView>
    </BottomSheet>
  );
}

const styles = StyleSheet.create({
  handle: {
    backgroundColor: '#ddd',
    width: 40,
  },

  // ---- 맨 위 바 (펼칠수록 나타남) ----
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    overflow: 'hidden',
  },

  backButton: {
    width: 48,
    alignItems: 'center',
    justifyContent: 'center',
  },

  // ---- 이름 + 버튼 ----
  previewHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingTop: 4,
    paddingBottom: 12,
  },

  previewTitleWrap: {
    flex: 1,
    marginRight: 8,
  },

  previewTitle: {
    fontSize: 22,
    fontWeight: '700',
  },

  erTag: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 4,
  },

  erDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#2E7D32',
    marginRight: 4,
  },

  erTagText: {
    fontSize: 13,
    fontWeight: '700',
    color: '#2E7D32',
  },

  summary: {
    paddingHorizontal: 20,
    paddingTop: 4,
  },

  summaryRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 10,
  },

  summaryText: {
    flex: 1,
    marginLeft: 10,
    fontSize: 15,
    color: '#555',
  },

  actionRow: {
    flexDirection: 'row',
    gap: 8,
  },

  actionFilled: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: 8,
  },

  actionFilledText: {
    marginLeft: 4,
    color: '#fff',
    fontWeight: '700',
    fontSize: 14,
  },

  actionOutline: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#ddd',
  },

  actionOutlineText: {
    marginLeft: 4,
    color: '#333',
    fontWeight: '600',
    fontSize: 14,
  },

  // ---- 탭 ----
  tabRow: {
    flexDirection: 'row',
    marginTop: 8,
  },

  tabButton: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 12,
  },

  tabLabel: {
    fontSize: 15,
    color: '#999',
    fontWeight: '600',
  },

  tabIndicator: {
    marginTop: 8,
    height: 2,
    width: '100%',
  },

  content: {
    paddingBottom: 40,
  },

  // ---- 상세 내용 ----
  bedSection: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginHorizontal: 20,
    marginVertical: 12,
    padding: 16,
    borderRadius: 12,
    backgroundColor: '#E8F5E9',
  },

  bedLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },

  bedTitle: {
    fontSize: 15,
    color: '#2E7D32',
    fontWeight: '700',
  },

  congestion: {
    fontSize: 12,
    color: '#2E7D32',
    marginTop: 2,
  },

  bedCount: {
    fontSize: 28,
    fontWeight: '700',
    color: '#2E7D32',
  },

  bedUnit: {
    fontSize: 14,
    fontWeight: '600',
  },

  section: {
    paddingHorizontal: 20,
    paddingVertical: 16,
  },

  sectionTitle: {
    fontSize: 17,
    fontWeight: '700',
    marginBottom: 12,
  },

  message: {
    color: '#666',
    paddingHorizontal: 20,
  },

  messageInline: {
    color: '#666',
  },

  error: {
    color: '#d00',
    paddingHorizontal: 20,
  },

  chipWrap: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },

  chip: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
    backgroundColor: '#F1F1F1',
    borderWidth: 1,
    borderColor: '#E3E3E3',
  },

  chipText: {
    fontSize: 13,
    color: '#333',
    fontWeight: '600',
  },

  timeRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 4,
  },

  dayLabel: {
    fontSize: 14,
    color: '#666',
  },

  timeValue: {
    fontSize: 14,
    fontWeight: '600',
  },

  note: {
    marginTop: 8,
    fontSize: 13,
    color: '#666',
  },

  checklistRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
  },

  checkbox: {
    width: 24,
    height: 24,
    borderRadius: 6,
    borderWidth: 2,
    borderColor: '#ccc',
    marginRight: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },

  checkboxMark: {
    color: '#fff',
    fontWeight: '700',
    fontSize: 14,
  },

  checklistLabel: {
    fontSize: 15,
  },
});
