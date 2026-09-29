// e약은요 Open API가 효능/용법/주의사항 필드에 HTML 태그(<br/> 등)를 섞어서 내려줘서
// 화면에 그대로 렌더링하면 태그가 텍스트로 노출된다. 태그만 제거하고 텍스트만 보여준다.
export function stripHtmlTags(value: string): string {
  return value.replace(/<[^>]*>?/g, '');
}
