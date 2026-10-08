// https://docs.expo.dev/guides/using-eslint/
const { defineConfig } = require('eslint/config');
const expoConfig = require("eslint-config-expo/flat");
const eslintConfigPrettier = require("eslint-config-prettier");

module.exports = defineConfig([
  expoConfig,
  eslintConfigPrettier,
  {
    ignores: ["dist/*"],
  },
  {
    // 실패·예외는 console.error, 예상된 문제는 console.warn으로 남긴다.
    // console.log 는 개발 중 확인용이라 커밋하지 않는다(위치·주소 같은 개인 정보가 로그에 남을 수 있다).
    // 앱 코드에만 적용한다. scripts/ 는 터미널에 출력하는 도구라 console.log 가 정상이다.
    files: ["src/**/*.{js,jsx,ts,tsx}"],
    rules: {
      "no-console": ["warn", { allow: ["warn", "error"] }],
    },
  }
]);
