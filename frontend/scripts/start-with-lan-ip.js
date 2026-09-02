#!/usr/bin/env node
// 실기기(Expo Go)에서 백엔드(로컬 8080)에 접근하려면 "localhost"가 아니라 이 컴퓨터의 LAN IP가
// 필요하다 (client.ts의 EXPO_PUBLIC_API_BASE_URL). IP가 바뀔 때마다 매번 수동으로 넣어주는 걸
// 깜빡하기 쉬워서, 이 스크립트가 현재 LAN IP를 자동으로 찾아 넣어주고 그 뒤에 expo를 그대로 실행한다.
//
// 이미 EXPO_PUBLIC_API_BASE_URL이 설정돼 있으면(예: CI, 원격 백엔드) 그 값을 그대로 존중하고 덮어쓰지 않는다.
const { networkInterfaces } = require('os');
const { spawn } = require('child_process');

const BACKEND_PORT = process.env.BACKEND_PORT || '8080';

function findLanIp() {
  const nets = networkInterfaces();
  const candidates = [];
  for (const [name, addrs] of Object.entries(nets)) {
    for (const addr of addrs || []) {
      if (addr.family === 'IPv4' && !addr.internal) {
        candidates.push({ name, address: addr.address });
      }
    }
  }
  if (candidates.length === 0) return null;
  // en0(Mac Wi-Fi)처럼 흔한 유선/무선 인터페이스 이름을 우선한다.
  const preferred = candidates.find((c) => /^(en0|en1|wlan0|eth0)$/.test(c.name));
  return (preferred || candidates[0]).address;
}

let apiBaseUrl = process.env.EXPO_PUBLIC_API_BASE_URL;
if (!apiBaseUrl) {
  const ip = findLanIp();
  if (ip) {
    apiBaseUrl = `http://${ip}:${BACKEND_PORT}`;
    console.log(`[start-with-lan-ip] EXPO_PUBLIC_API_BASE_URL=${apiBaseUrl} (자동 감지)`);
  } else {
    console.warn(
      '[start-with-lan-ip] LAN IP를 찾지 못했습니다 - localhost로 기본 동작합니다 (실기기에서는 접속 실패할 수 있음).',
    );
  }
}

const args = process.argv.slice(2);
const child = spawn('npx', ['expo', 'start', ...args], {
  stdio: 'inherit',
  env: { ...process.env, ...(apiBaseUrl ? { EXPO_PUBLIC_API_BASE_URL: apiBaseUrl } : {}) },
  shell: process.platform === 'win32',
});

child.on('exit', (code) => process.exit(code ?? 0));
