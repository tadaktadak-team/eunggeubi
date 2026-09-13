import { forwardRef, useEffect, useImperativeHandle, useRef } from "react";
import { StyleSheet } from "react-native";
import { WebView } from "react-native-webview";

const KAKAO_JS_KEY = "d2ca7fd693afc98a1e534caf7e697b48";

export interface MapMarkerData {
  id: string;
  name: string;
  latitude: number;
  longitude: number;
  color: string;
}

interface KakaoMapViewProps {
  centerLatitude: number;
  centerLongitude: number;
  markers: MapMarkerData[];
}

export interface KakaoMapViewHandle {
  updateMarkers: (markers: MapMarkerData[]) => void;
}

const HTML_TEMPLATE = `
<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <style>
    html, body, #map { width: 100%; height: 100%; margin: 0; padding: 0; }
  </style>
</head>
<body>
  <div id="map"></div>
  <script src="https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&autoload=false"></script>
  <script>
    var map;
    var markers = [];

    kakao.maps.load(function () {
      var container = document.getElementById('map');
      var options = {
        center: new kakao.maps.LatLng(__CENTER_LAT__, __CENTER_LNG__),
        level: 5
      };
      map = new kakao.maps.Map(container, options);

      // 현재 위치 마커 (파란 점)
      var myLocMarker = new kakao.maps.Circle({
        center: new kakao.maps.LatLng(__CENTER_LAT__, __CENTER_LNG__),
        radius: 30,
        strokeWeight: 2,
        strokeColor: '#1E88E5',
        strokeOpacity: 1,
        fillColor: '#1E88E5',
        fillOpacity: 0.6
      });
      myLocMarker.setMap(map);
    });

    function clearMarkers() {
      markers.forEach(function (m) { m.setMap(null); });
      markers = [];
    }

    function addMarkers(data) {
      clearMarkers();
      data.forEach(function (item) {
        var position = new kakao.maps.LatLng(item.latitude, item.longitude);

        var content = document.createElement('div');
        content.innerText = item.name;
        content.style.background = item.color;
        content.style.color = '#fff';
        content.style.padding = '4px 8px';
        content.style.borderRadius = '6px';
        content.style.fontSize = '12px';
        content.style.fontWeight = '700';
        content.style.whiteSpace = 'nowrap';

        var overlay = new kakao.maps.CustomOverlay({
          position: position,
          content: content,
          yAnchor: 1
        });
        overlay.setMap(map);
        markers.push(overlay);
      });
    }

    document.addEventListener('message', function (event) {
      var payload = JSON.parse(event.data);
      if (payload.type === 'UPDATE_MARKERS') {
        addMarkers(payload.markers);
      }
    });
    window.addEventListener('message', function (event) {
      var payload = JSON.parse(event.data);
      if (payload.type === 'UPDATE_MARKERS') {
        addMarkers(payload.markers);
      }
    });
  </script>
</body>
</html>
`;

const KakaoMapView = forwardRef<KakaoMapViewHandle, KakaoMapViewProps>(
  ({ centerLatitude, centerLongitude, markers }, ref) => {
    const webviewRef = useRef<WebView>(null);

    const sendMarkersToWebView = (data: MapMarkerData[]) => {
      const message = JSON.stringify({
        type: "UPDATE_MARKERS",
        markers: data,
      });
      webviewRef.current?.injectJavaScript(
        `window.dispatchEvent(new MessageEvent('message', { data: ${JSON.stringify(
          message
        )} })); true;`
      );
    };

    useImperativeHandle(ref, () => ({
      updateMarkers: sendMarkersToWebView,
    }));

    useEffect(() => {
      sendMarkersToWebView(markers);
    }, [markers]);

    const html = HTML_TEMPLATE.replace(
      /__CENTER_LAT__/g,
      String(centerLatitude)
    ).replace(/__CENTER_LNG__/g, String(centerLongitude));

    return (
      <WebView
        ref={webviewRef}
        style={styles.webview}
        originWhitelist={["*"]}
        source={{ html }}
        onError={(e) => console.log("WebView 에러:", e.nativeEvent)}
        onHttpError={(e) => console.log("WebView HTTP 에러:", e.nativeEvent)}
        onLoadEnd={() => sendMarkersToWebView(markers)}
      />
    );
  }
);

export default KakaoMapView;

const styles = StyleSheet.create({
  webview: {
    flex: 1,
  },
});