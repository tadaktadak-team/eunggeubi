import { Ionicons } from '@expo/vector-icons';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, font, radius, spacing } from '../../../shared/theme/theme';
import { EmergencyBed } from '../types/emergencyBed';
import { formatBeds, formatUpdatedAt, getBedStatus } from '../utils/bedStatus';
import { callPhone, openDirections } from '../utils/contact';

type Props = {
  bed: EmergencyBed | null;
  onClose: () => void;
};

// 응급실 한 곳의 요약. 전화번호는 응급실 직통이 아니라 병원 대표번호다
export default function EmergencyBedSheet({ bed, onClose }: Props) {
  const insets = useSafeAreaInsets();
  const status = bed ? getBedStatus(bed) : null;
  const updated = bed ? formatUpdatedAt(bed.updatedAt) : null;
  const canNavigate = bed?.latitude != null && bed?.longitude != null;

  return (
    <Modal visible={bed != null} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable style={styles.backdrop} onPress={onClose}>
        <Pressable style={[styles.sheet, { paddingBottom: insets.bottom + spacing.lg }]} onPress={() => {}}>
          <View style={styles.handle} />
          {bed && (
            <>
              <View style={styles.titleRow}>
                <Text style={styles.name}>{bed.name}</Text>
                {status && (
                  <View style={[styles.chip, { borderColor: status.color }]}>
                    <Text style={[styles.chipText, { color: status.color }]}>{status.label}</Text>
                  </View>
                )}
              </View>
              <Text style={styles.address}>{bed.address}</Text>
              <Text style={styles.meta}>
                {[
                  bed.distance != null && `${bed.distance}km`,
                  formatBeds(bed.availableBeds),
                  bed.congestion != null && `혼잡도 ${bed.congestion}%`,
                ]
                  .filter(Boolean)
                  .join(' · ')}
              </Text>
              {updated && <Text style={styles.updated}>{updated} 기준 · 실제와 다를 수 있어요</Text>}

              <View style={styles.actions}>
                <Pressable
                  style={[styles.btn, !bed.phone && styles.btnDisabled]}
                  onPress={() => callPhone(bed.phone)}
                  disabled={!bed.phone}
                >
                  <Ionicons name="call-outline" size={18} color={colors.text} />
                  <Text style={styles.btnText}>병원 대표전화</Text>
                </Pressable>
                <Pressable
                  style={[styles.btn, styles.btnPrimary, !canNavigate && styles.btnDisabled]}
                  onPress={() => canNavigate && openDirections(bed.name, bed.latitude as number, bed.longitude as number)}
                  disabled={!canNavigate}
                >
                  <Ionicons name="navigate-outline" size={18} color={colors.white} />
                  <Text style={[styles.btnText, { color: colors.white }]}>길찾기</Text>
                </Pressable>
              </View>
              <Text style={styles.hint}>위급하면 먼저 119에 연락하세요.</Text>
            </>
          )}
        </Pressable>
      </Pressable>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, justifyContent: 'flex-end', backgroundColor: 'rgba(0,0,0,0.4)' },
  sheet: {
    backgroundColor: colors.white,
    borderTopLeftRadius: radius.lg,
    borderTopRightRadius: radius.lg,
    paddingHorizontal: spacing.xl,
    paddingTop: spacing.md,
    gap: spacing.sm,
  },
  handle: { alignSelf: 'center', width: 40, height: 4, borderRadius: 2, backgroundColor: colors.border, marginBottom: spacing.sm },
  titleRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.sm },
  name: { flex: 1, fontSize: font.h3, fontWeight: '800', color: colors.text },
  chip: { borderWidth: 1, borderRadius: radius.pill, paddingHorizontal: spacing.md, paddingVertical: 2 },
  chipText: { fontSize: font.caption, fontWeight: '700' },
  address: { fontSize: font.sub, color: colors.textSub },
  meta: { fontSize: font.body, fontWeight: '600', color: colors.text, marginTop: spacing.xs },
  updated: { fontSize: font.caption, color: colors.placeholder },
  actions: { flexDirection: 'row', gap: spacing.sm, marginTop: spacing.md },
  btn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.xs,
    height: 48,
    borderRadius: radius.md,
    backgroundColor: colors.inputBg,
  },
  btnPrimary: { backgroundColor: colors.primary },
  btnDisabled: { opacity: 0.4 },
  btnText: { fontSize: font.body, fontWeight: '700', color: colors.text },
  hint: { textAlign: 'center', fontSize: font.caption, color: colors.placeholder, marginTop: spacing.xs },
});
