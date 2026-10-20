// 디자인 토큰 (색/간격/폰트).
//
// 빨강 계열은 앱 아이콘에서 뽑았다. 로고 하트·사이렌·맥박선이 모두 H359 S78% L61%
// (#E94E51)이라, 색조와 채도를 그대로 두고 명도만 바꿔 한 가족으로 만들었다.
// 로고 원색은 흰 글자가 3.70:1밖에 안 나와서 버튼에 쓰지 못한다. 스플래시처럼
// 글자가 올라가지 않는 큰 도형에만 쓸 것.
//
// 색은 모두 WCAG 2.1 AA(본문 4.5:1)를 기준으로 맞췄다. 괄호 안이 대비비.
// 토큰을 바꿀 때는 "그 위에 무엇이 올라가는지"를 같이 봐야 한다.
export const colors = {
  primary: '#DE1B1F',      // 로고색을 명도만 낮춘 것. 버튼 배경 (흰 글자 4.90:1)
  // primaryLight 카드 위의 제목·아이콘. 작은 글자라 primary보다 한 단계 더 어둡다
  // (primaryLight 위 6.52:1). 눌림 상태용이 아니다.
  primaryDark: '#A81517',
  primaryLight: '#FBDFDF', // 빨강 계열 카드 배경. 흰 배경과 1.26:1로 경계가 보여야 한다
  black: '#111111',        // 중립 액션 버튼 배경 (로그인·검색·인증요청)
  white: '#FFFFFF',
  bg: '#FFFFFF',
  inputBg: '#F4F4F6',      // 입력창 배경 (회색)
  text: '#1A1A1A',         // 본문 텍스트. 버튼 배경으로 쓰지 말 것 (black을 쓴다)
  textSub: '#6B7280',      // 보조 텍스트 (흰 배경 4.83:1)
  placeholder: '#656C7A',  // 입력창의 유일한 라벨이므로 본문 수준 대비가 필요 (inputBg 위 4.80:1)
  border: '#E5E7EB',
  kakao: '#FEE500',
  kakaoText: '#3C1E1E',
  // 에러 문구, 탈퇴 버튼 테두리, 119 버튼 배경 (흰 배경 4.90:1).
  // primary와 같은 값이다 - 빨강을 하나로 쓰기로 한 결정에 따른 것이지, 우연이 아니다.
  // 따라서 색만으로는 119와 저장 버튼이 구분되지 않는다. 구분이 필요해지면
  // 이 토큰만 바꾸면 Button의 emergency·destructive가 같이 따라온다.
  danger: '#DE1B1F',
  naver: '#03C75A',
  // 성공·인증완료 (흰 글자 8.35:1). primary와 명도를 1.70:1 벌려두었다 -
  // 빨강과 초록이 같은 밝기면 적록색약에게 같은 회색으로 보이기 때문이다.
  success: '#0E5A2B',
  disabled: '#E5E7EB',     // 비활성 배경. 위에 흰 글자를 올리면 안 읽힌다
  disabledText: '#8A919E', // 비활성 글자 (disabled 위 2.56:1)
};

export const spacing = { xs: 4, sm: 8, md: 12, lg: 16, xl: 24, xxl: 32 };
export const radius = { sm: 8, md: 12, lg: 16, pill: 999 };
export const font = { h1: 28, h2: 22, h3: 18, body: 15, sub: 13, caption: 12 };
