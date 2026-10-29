// 같은 질병에서 나온 출처는 한 줄로 묶는다 - [두통, 연관 증상], [두통, 자가 관리] → '두통 - 연관 증상, 자가 관리'.
// section이 없는 출처는 묶지 않고 그대로 한 줄로 둔다.
export function groupSourceLines(sources: { disease: string; section?: string }[]): string[] {
  const groups = new Map<string, string[]>();
  for (const { disease, section } of sources) {
    const sections = groups.get(disease) ?? [];
    if (section && !sections.includes(section)) sections.push(section);
    groups.set(disease, sections);
  }
  return [...groups].map(([disease, sections]) =>
    sections.length ? `${disease} - ${sections.join(', ')}` : disease,
  );
}
