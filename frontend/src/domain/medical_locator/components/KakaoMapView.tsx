import { useEffect, useMemo, useRef } from 'react';
import { StyleSheet } from 'react-native';
import { WebView } from 'react-native-webview';

const KAKAO_JS_KEY = 'd2ca7fd693afc98a1e534caf7e697b48';

export interface MapMarkerData {
  id: string;
  name: string;
  latitude: number;
  longitude: number;
  category: string; // '병원' | '약국' | '응급실' (핀 아이콘 종류)
  color: string;
}

export interface MapFitPoint {
  latitude: number;
  longitude: number;
}

interface WebViewHandle {
  injectJavaScript: (script: string) => void;
}

interface KakaoMapViewProps {
  centerLatitude: number;
  centerLongitude: number;
  markers: MapMarkerData[];
  selectedId?: string | null;
  // 지도가 한 화면에 담아야 할 지점들(가까운 곳 + 내 위치)
  fitPoints: MapFitPoint[];
  // 필터/검색이 바뀌면 값이 바뀌고, 그때 사용자가 움직인 기록을 지우고 다시 맞춘다
  fitKey: string;
  onMarkerPress?: (id: string) => void;
}

// 지도는 WebView 안에서 돌아간다. RN -> WebView 로는 window.setMapState(상태 JSON) 하나만 호출하고,
// WebView 안에서 이전 상태와 비교해 바뀐 마커만 추가/삭제한다(전부 다시 그리지 않음).
const HTML_TEMPLATE = `
<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <style>
    html, body, #map { width: 100%; height: 100%; margin: 0; padding: 0; }
    .label {
      color: #fff; padding: 6px 12px; border-radius: 16px;
      font-size: 13px; font-weight: 700; white-space: nowrap;
      box-shadow: 0 1px 4px rgba(0,0,0,0.3); pointer-events: none;
    }
    .cluster {
      display: flex; align-items: center; justify-content: center;
      border-radius: 50%; background: rgba(187,210,240,0.92);
      color: #1a3d73; font-weight: 700; font-size: 15px;
    }
  </style>
</head>
<body>
  <div id="map"></div>
  <script src="https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&autoload=false&libraries=clusterer"></script>
  <script>
    var map = null;
    var clusterer = null;
    var ready = false;
    var pending = null;          // 지도 준비 전에 도착한 가장 최근 상태
    var entries = {};            // id -> { marker, data }
    var labelOverlay = null;
    var selectedId = null;
    var userMoved = false;       // 사용자가 직접 움직이면 자동 맞춤을 멈춘다
    var programmatic = false;    // 코드가 지도를 움직이는 중에는 사용자 조작으로 보지 않는다
    var lastFitKey = null;
    var lastFitSig = null;
    var CENTER = null;

    function svgIcon(category, color, size) {
      var inner;
      if (category === '병원') {
        inner = '<rect x="17" y="9" width="6" height="22" rx="1.5" fill="#fff"/>' +
                '<rect x="9" y="17" width="22" height="6" rx="1.5" fill="#fff"/>';
      } else if (category === '약국') {
        inner = '<g transform="rotate(-45 20 20)">' +
                '<rect x="9" y="14" width="22" height="12" rx="6" fill="#fff"/>' +
                '<line x1="20" y1="14" x2="20" y2="26" stroke="' + color + '" stroke-width="2"/></g>';
      } else {
        inner = '<rect x="8" y="14" width="16" height="12" rx="2" fill="#fff"/>' +
                '<path d="M24 17h5l3 4v5h-8z" fill="#fff"/>' +
                '<rect x="14" y="17" width="4" height="6" rx="1" fill="' + color + '"/>' +
                '<rect x="13" y="18.5" width="6" height="3" rx="1" fill="' + color + '"/>' +
                '<circle cx="14" cy="27" r="2.6" fill="' + color + '" stroke="#fff" stroke-width="1.5"/>' +
                '<circle cx="28" cy="27" r="2.6" fill="' + color + '" stroke="#fff" stroke-width="1.5"/>';
      }
      var svg = '<svg xmlns="http://www.w3.org/2000/svg" width="' + size + '" height="' + size +
                '" viewBox="0 0 40 40"><circle cx="20" cy="20" r="19" fill="' + color +
                '" stroke="#fff" stroke-width="2"/>' + inner + '</svg>';
      return new kakao.maps.MarkerImage(
        'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg),
        new kakao.maps.Size(size, size),
        { offset: new kakao.maps.Point(size / 2, size / 2) }
      );
    }

    function post(payload) {
      window.ReactNativeWebView.postMessage(JSON.stringify(payload));
    }

    function fixMapSize() {
      if (!map) return;
      var center = map.getCenter();
      map.relayout();
      map.setCenter(center);
    }
    window.fixMapSize = fixMapSize;
    window.addEventListener('resize', fixMapSize);
    if (window.ResizeObserver) {
      new ResizeObserver(fixMapSize).observe(document.getElementById('map'));
    }

    kakao.maps.load(function () {
      CENTER = new kakao.maps.LatLng(__CENTER_LAT__, __CENTER_LNG__);
      map = new kakao.maps.Map(document.getElementById('map'), { center: CENTER, level: 5 });

      // 내 위치 (파란 점)
      new kakao.maps.Circle({
        center: CENTER, radius: 30, strokeWeight: 2, strokeColor: '#1E88E5',
        strokeOpacity: 1, fillColor: '#1E88E5', fillOpacity: 0.6
      }).setMap(map);

      clusterer = new kakao.maps.MarkerClusterer({
        map: map,
        averageCenter: true,
        minLevel: 4,
        calculator: [5, 10],
        styles: [
          { width: '44px', height: '44px', className: 'cluster' },
          { width: '54px', height: '54px', className: 'cluster' },
          { width: '64px', height: '64px', className: 'cluster' }
        ]
      });

      // 사용자가 직접 움직이거나 확대/축소하면 그 뒤로는 자동 맞춤을 하지 않는다
      kakao.maps.event.addListener(map, 'dragstart', function () { userMoved = true; });
      kakao.maps.event.addListener(map, 'zoom_start', function () {
        if (!programmatic) userMoved = true;
      });

      ready = true;
      if (pending) { apply(pending); }
    });

    function setSelected(id) {
      // 이전 선택 해제: 일반 크기로 되돌리고 클러스터에 다시 넣는다
      if (selectedId && entries[selectedId]) {
        var prev = entries[selectedId];
        prev.marker.setImage(svgIcon(prev.data.category, prev.data.color, 36));
        prev.marker.setZIndex(1);
        clusterer.addMarker(prev.marker);
      }
      if (labelOverlay) { labelOverlay.setMap(null); labelOverlay = null; }
      selectedId = id;

      if (id && entries[id]) {
        var cur = entries[id];
        // 선택된 핀은 묶이지 않고 크게 보이며 이름표가 붙는다
        clusterer.removeMarker(cur.marker);
        cur.marker.setImage(svgIcon(cur.data.category, cur.data.color, 46));
        cur.marker.setZIndex(10);
        cur.marker.setMap(map);

        var label = document.createElement('div');
        label.className = 'label';
        label.style.background = cur.data.color;
        label.innerText = cur.data.name;
        labelOverlay = new kakao.maps.CustomOverlay({
          position: cur.marker.getPosition(), content: label, yAnchor: 1.7, zIndex: 11
        });
        labelOverlay.setMap(map);
      }
    }

    function syncMarkers(list) {
      var next = {};
      list.forEach(function (d) { next[d.id] = d; });

      // 사라진 마커만 제거
      Object.keys(entries).forEach(function (id) {
        if (!next[id]) {
          clusterer.removeMarker(entries[id].marker);
          entries[id].marker.setMap(null);
          delete entries[id];
          if (selectedId === id) { selectedId = null; if (labelOverlay) { labelOverlay.setMap(null); labelOverlay = null; } }
        }
      });

      // 새로 생긴 마커만 추가
      var added = [];
      list.forEach(function (d) {
        if (entries[d.id]) return;
        var marker = new kakao.maps.Marker({
          position: new kakao.maps.LatLng(d.latitude, d.longitude),
          image: svgIcon(d.category, d.color, 36),
          zIndex: 1
        });
        kakao.maps.event.addListener(marker, 'click', function () { post({ type: 'MARKER_PRESS', id: d.id }); });
        entries[d.id] = { marker: marker, data: d };
        added.push(marker);
      });
      if (added.length) clusterer.addMarkers(added);
    }

    function fit(points, key) {
      if (key !== lastFitKey) { lastFitKey = key; userMoved = false; lastFitSig = null; }
      if (userMoved) return;
      var sig = JSON.stringify(points);
      if (sig === lastFitSig) return;
      lastFitSig = sig;

      programmatic = true;
      if (!points.length) {
        map.setCenter(CENTER);
      } else {
        var bounds = new kakao.maps.LatLngBounds();
        bounds.extend(CENTER);
        points.forEach(function (p) { bounds.extend(new kakao.maps.LatLng(p.latitude, p.longitude)); });
        map.setBounds(bounds, 60, 40, 60, 40);
      }
      setTimeout(function () { programmatic = false; }, 700);
    }

    function apply(state) {
      syncMarkers(state.markers);
      setSelected(state.selectedId);
      fit(state.fitPoints, state.fitKey);
    }

    window.setMapState = function (state) {
      if (!ready) { pending = state; return; }
      apply(state);
    };
  </script>
</body>
</html>
`;

export default function KakaoMapView({
  centerLatitude,
  centerLongitude,
  markers,
  selectedId,
  fitPoints,
  fitKey,
  onMarkerPress,
}: KakaoMapViewProps) {
  // react-native-webview 14 의 타입 정의가 React 19.3 타입과 맞지 않아, 쓰는 메서드만 직접 정의한다
  const webviewRef = useRef<WebViewHandle>(null);

  // 위치가 같으면 같은 HTML 이어야 WebView 가 다시 로드되지 않는다
  const html = useMemo(
    () =>
      HTML_TEMPLATE.replace(/__CENTER_LAT__/g, String(centerLatitude)).replace(
        /__CENTER_LNG__/g,
        String(centerLongitude),
      ),
    [centerLatitude, centerLongitude],
  );

  const sendState = () => {
    const state = JSON.stringify({
      markers,
      selectedId: selectedId ?? null,
      fitPoints,
      fitKey,
    });
    webviewRef.current?.injectJavaScript(
      `window.setMapState && window.setMapState(${state}); true;`,
    );
  };

  // markers / selectedId / fitPoints / fitKey 가 실제로 바뀔 때만 전달한다
  useEffect(() => {
    sendState();
  }, [markers, selectedId, fitPoints, fitKey]);

  return (
    <WebView
      // @ts-expect-error react-native-webview 14 의 ref 타입이 React 19.3 타입과 맞지 않음 (런타임에는 정상)
      ref={webviewRef}
      style={styles.webview}
      originWhitelist={['*']}
      source={{ html }}
      onError={(e) => console.warn('WebView 에러:', e.nativeEvent)}
      onHttpError={(e) => console.warn('WebView HTTP 에러:', e.nativeEvent)}
      onLoadEnd={sendState}
      onLayout={() =>
        webviewRef.current?.injectJavaScript('if (window.fixMapSize) { fixMapSize(); } true;')
      }
      onMessage={(e) => {
        try {
          const payload = JSON.parse(e.nativeEvent.data);
          if (payload.type === 'MARKER_PRESS') {
            onMarkerPress?.(payload.id);
          }
        } catch (err) {
          console.warn('WebView 메시지 파싱 실패:', err);
        }
      }}
    />
  );
}

const styles = StyleSheet.create({
  webview: {
    flex: 1,
  },
});
