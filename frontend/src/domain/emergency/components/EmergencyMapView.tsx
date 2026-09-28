import { StyleSheet } from 'react-native';
import { WebView } from 'react-native-webview';

import { KAKAO_JS_KEY } from '../../../shared/constants/kakao';

interface EmergencyMapViewProps {
  latitude: number;
  longitude: number;
}

// 보호자에게 전송된 위치 한 곳만 보여주는 지도. 결과 화면이 세로 스크롤이라 이동·확대는 막았다.
const HTML_TEMPLATE = `
<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <style>
    html, body, #map { width: 100%; height: 100%; margin: 0; padding: 0; }
    /* 카카오맵 기본 핀과 같은 물방울 모양. 회전 때문에 끝점이 래퍼 바닥 가운데에 오도록 맞춘다. */
    .pin-wrap {
      position: relative;
      width: 30px;
      height: 40px;
      filter: drop-shadow(0 2px 3px rgba(0, 0, 0, 0.35));
    }
    .pin {
      position: absolute;
      top: 6px;
      left: 1px;
      width: 28px;
      height: 28px;
      background: #E53935;
      border-radius: 50% 50% 50% 0;
      transform: rotate(-45deg);
    }
    .pin::after {
      content: '';
      position: absolute;
      top: 8px;
      left: 8px;
      width: 12px;
      height: 12px;
      border-radius: 50%;
      background: #fff;
    }
  </style>
</head>
<body>
  <div id="map"></div>
  <script src="https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&autoload=false"></script>
  <script>
    kakao.maps.load(function () {
      var position = new kakao.maps.LatLng(__LAT__, __LNG__);
      var map = new kakao.maps.Map(document.getElementById('map'), {
        center: position,
        level: 5,
        draggable: false
      });
      map.setZoomable(false);

      var wrap = document.createElement('div');
      wrap.className = 'pin-wrap';
      wrap.appendChild(document.createElement('div')).className = 'pin';

      new kakao.maps.CustomOverlay({
        position: position,
        content: wrap,
        xAnchor: 0.5,
        yAnchor: 1
      }).setMap(map);
    });
  </script>
</body>
</html>
`;

export default function EmergencyMapView({ latitude, longitude }: EmergencyMapViewProps) {
  const html = HTML_TEMPLATE.replace(/__LAT__/g, String(latitude)).replace(
    /__LNG__/g,
    String(longitude),
  );

  return (
    <WebView
      style={styles.webview}
      originWhitelist={['*']}
      source={{ html }}
      scrollEnabled={false}
      onError={(e) => console.log('지도 로딩 오류:', e.nativeEvent)}
    />
  );
}

const styles = StyleSheet.create({
  webview: { flex: 1 },
});
