package com.tadaktadak.eunggeubi.domain.health.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class MedicationItemTest {

    @Test
    void 이름_목록에_null이나_빈_값이_섞여도_서버_오류_없이_걸러낸다() {
        List<MedicationItem> items = MedicationItem.distinct(
                MedicationItem.fromNames(Arrays.asList("타이레놀", null, "  ", "", "리피토")));

        assertThat(items).extracting(MedicationItem::name).containsExactly("타이레놀", "리피토");
    }

    @Test
    void 이름이_null인_항목이_직접_들어와도_건너뛴다() {
        List<MedicationItem> items = MedicationItem.distinct(
                Arrays.asList(new MedicationItem(null, "200410085"), null, new MedicationItem("타이레놀", null)));

        assertThat(items).extracting(MedicationItem::name).containsExactly("타이레놀");
    }

    @Test
    void 같은_품목번호는_이름이_달라도_한_번만_남긴다() {
        List<MedicationItem> items = MedicationItem.distinct(List.of(
                new MedicationItem("리피토정", "200410085"),
                new MedicationItem("리피토정 10mg", "200410085"),
                new MedicationItem("직접입력", null),
                new MedicationItem("직접입력", null)));

        assertThat(items).hasSize(2);
        assertThat(items.get(0).name()).isEqualTo("리피토정");
    }

    @Test
    void 이름만_이은_문자열에서는_이름_안의_콤마를_공백으로_바꾼다() {
        String csv = MedicationItem.toCsv(List.of(new MedicationItem("A,B", null), new MedicationItem("C", null)));

        assertThat(csv).isEqualTo("A B,C");
        assertThat(MedicationItem.toCsv(List.of())).isNull();
    }
}
