import { Ionicons } from '@expo/vector-icons';
import { useEffect, useState } from 'react';
import { Modal, Pressable, StyleSheet, Text, View } from 'react-native';

import Button from '../components/Button';
import { colors, font, spacing } from '../theme/theme';
import { DialogRequest, subscribe, variantFor } from './store';

// 앱 루트에 한 번만 올린다. showAlert()가 부르면 여기서 뜬다.
//
// 화면 가운데에 뜨는 형태다. 아래에서 올라오는 바텀시트(LoginRequiredSheet 등)는
// 기능 안내용이라 성격이 다르므로 그대로 둔다.

// 제목만 보고 성격을 짐작해 아이콘을 고른다. 호출부에서 일일이 정하지 않아도 되도록.
//
// 테두리 없는 글리프만 쓴다. alert-circle 같은 것은 아이콘 자체가 원이라
// 아래 iconCircle 안에 들어가면 원이 두 겹으로 보인다.
function pickIcon(req: DialogRequest): keyof typeof Ionicons.glyphMap {
  if (req.icon) return req.icon as keyof typeof Ionicons.glyphMap;
  if (req.actions.some((a) => a.style === 'destructive')) return 'warning';
  if (/실패|오류|없음/.test(req.title)) return 'close';
  if (/완료|됨|성공/.test(req.title)) return 'checkmark';
  return 'information';
}

export default function DialogHost() {
  const [req, setReq] = useState<DialogRequest | null>(null);

  useEffect(() => {
    subscribe(setReq);
    return () => subscribe(null);
  }, []);

  if (!req) return null;

  const run = (onPress?: () => void) => {
    // 닫고 나서 실행한다. 화면 이동이 섞이면 모달이 남는 경우가 있다.
    setReq(null);
    onPress?.();
  };

  // 버튼이 둘이면 가로로, 그 이상이면 세로로 쌓는다.
  const side = req.actions.length === 2;

  return (
    <Modal visible transparent animationType="fade" onRequestClose={() => run()}>
      <View style={styles.backdrop}>
        <Pressable style={styles.card} onPress={() => {}}>
          <View style={styles.iconCircle}>
            <Ionicons name={pickIcon(req)} size={24} color={colors.primary} />
          </View>

          <Text style={styles.title}>{req.title}</Text>
          {!!req.message && <Text style={styles.message}>{req.message}</Text>}

          <View style={[styles.actions, side && styles.actionsRow]}>
            {req.actions.map((a, i) => (
              <Button
                key={a.text}
                title={a.text}
                variant={variantFor(a, i, req.actions.length)}
                onPress={() => run(a.onPress)}
                style={side ? styles.flexBtn : undefined}
              />
            ))}
          </View>
        </Pressable>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  // 배경을 눌러서는 닫지 않는다. 묻고 있는 중이라 실수로 넘어가면 안 된다.
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: spacing.xl,
  },
  card: {
    width: '100%',
    maxWidth: 400,
    backgroundColor: colors.white,
    borderRadius: 20,
    paddingHorizontal: spacing.xl,
    paddingTop: spacing.xl,
    paddingBottom: spacing.lg,
    alignItems: 'center',
  },
  iconCircle: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: spacing.md,
  },
  title: { fontSize: font.h3, fontWeight: '800', color: colors.text, textAlign: 'center' },
  message: {
    fontSize: font.sub,
    color: colors.textSub,
    textAlign: 'center',
    lineHeight: 20,
    marginTop: spacing.xs,
  },
  actions: { alignSelf: 'stretch', gap: spacing.sm, marginTop: spacing.lg },
  actionsRow: { flexDirection: 'row' },
  flexBtn: { flex: 1 },
});
