package com.tadaktadak.eunggeubi.domain.health.dto;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

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

    // ---- 저장된 값 복원(fromStored) ----

    @Test
    void 저장된_JSON이_정상이면_품목번호까지_복원한다() {
        List<MedicationItem> items = MedicationItem.fromStored(
                "[{\"name\":\"리피토정\",\"itemSeq\":\"200410085\"},{\"name\":\"직접입력\",\"itemSeq\":null}]", "무시됨");

        assertThat(items).containsExactly(new MedicationItem("리피토정", "200410085"), new MedicationItem("직접입력", null));
    }

    @Test
    void JSON을_읽을_수_없으면_빈_목록이_아니라_이름_목록으로_복원한다() {
        List<MedicationItem> items = MedicationItem.fromStored("[{깨진 JSON", "리피토정,타이레놀");

        assertThat(items).extracting(MedicationItem::name).containsExactly("리피토정", "타이레놀");
    }

    @Test
    void JSON이_없으면_이름_목록으로_복원한다() {
        assertThat(MedicationItem.fromStored(null, "타이레놀")).extracting(MedicationItem::name).containsExactly("타이레놀");
        assertThat(MedicationItem.fromStored(" ", null)).isEmpty();
    }

    @Test
    void 나중에_늘어난_항목이_있어도_읽을_수_있다() {
        List<MedicationItem> items = MedicationItem.fromStored(
                "[{\"name\":\"리피토정\",\"itemSeq\":\"200410085\",\"dose\":\"10mg\"}]", null);

        assertThat(items).containsExactly(new MedicationItem("리피토정", "200410085"));
    }

    @Test
    void 읽기에_실패해도_로그에는_저장된_내용을_남기지_않는다() {
        Logger logger = (Logger) LoggerFactory.getLogger(MedicationItem.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            MedicationItem.fromStored("[{\"name\":\"고혈압약-비밀\", 깨짐", "리피토정");

            assertThat(appender.list).isNotEmpty();
            assertThat(appender.list).allSatisfy(event -> {
                assertThat(event.getFormattedMessage()).doesNotContain("고혈압약-비밀");
                assertThat(event.getThrowableProxy()).isNull();
            });
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void 이름만_이은_문자열에서는_이름_안의_콤마를_공백으로_바꾼다() {
        String csv = MedicationItem.toCsv(List.of(new MedicationItem("A,B", null), new MedicationItem("C", null)));

        assertThat(csv).isEqualTo("A B,C");
        assertThat(MedicationItem.toCsv(List.of())).isNull();
    }
}
