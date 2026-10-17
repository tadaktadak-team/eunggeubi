import type { ButtonVariant } from '../components/Button';

// Alert.alert을 대체한다. react-native-web의 Alert는 빈 함수여서 웹에서 아무것도
// 뜨지 않고 버튼 콜백도 실행되지 않는다. 직접 만든 Modal이라 세 플랫폼에서 같이 돈다.
//
// 화면마다 visible 상태를 들지 않아도 되도록, 루트에 하나만 띄워두고 모듈 함수로 연다.
// Alert.alert과 호출 모양을 맞춰서 교체 비용을 낮췄다.

export type DialogAction = {
  text: string;
  onPress?: () => void;
  /** 되돌릴 수 없는 동작이면 'destructive', 물러나는 쪽이면 'cancel' */
  style?: 'default' | 'cancel' | 'destructive';
};

export type DialogRequest = {
  title: string;
  message?: string;
  actions: DialogAction[];
  /** 아이콘 이름(Ionicons). 생략하면 성격에 맞춰 고른다 */
  icon?: string;
};

type Listener = (req: DialogRequest | null) => void;

let listener: Listener | null = null;
// Provider가 붙기 전에 호출되면 잃어버리므로 한 건만 들고 있다가 붙을 때 넘긴다.
let pending: DialogRequest | null = null;

export function subscribe(fn: Listener | null) {
  listener = fn;
  if (fn && pending) {
    fn(pending);
    pending = null;
  }
}

/** 확인 버튼 하나짜리 알림 */
export function showAlert(title: string, message?: string, actions?: DialogAction[]) {
  const req: DialogRequest = { title, message, actions: actions?.length ? actions : [{ text: '확인' }] };
  if (listener) listener(req);
  else pending = req;
}

export function closeDialog() {
  listener?.(null);
}

/** action.style을 Button의 variant로 옮긴다 */
export function variantFor(a: DialogAction, index: number, total: number): ButtonVariant {
  if (a.style === 'destructive') return 'destructive';
  if (a.style === 'cancel') return total > 2 ? 'text' : 'secondary';
  // 선택지가 여럿일 때 두 번째부터는 보조로 내린다
  return index === 0 ? 'brand' : 'secondary';
}
