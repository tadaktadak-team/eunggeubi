package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tadaktadak.eunggeubi.domain.ai_consultations.entity.AiConsultation;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.AiConsultationRepository;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.ChecklistRepository;
import com.tadaktadak.eunggeubi.domain.ai_consultations.repository.ChecklistResponseRepository;
import com.tadaktadak.eunggeubi.global.exception.ResourceNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsultationHistoryServiceTest {

    private final AiConsultationRepository repository = mock(AiConsultationRepository.class);
    private final ConsultationHistoryService service = new ConsultationHistoryService(
            repository, mock(ChecklistRepository.class), mock(ChecklistResponseRepository.class));

    @Test
    void 없는_상담과_남의_상담은_똑같은_404_응답이다() {
        AiConsultation others = mock(AiConsultation.class);
        when(others.isOwnedBy(1L, null)).thenReturn(false);
        when(repository.findBySessionIdOrderByCreatedAtAsc("others")).thenReturn(List.of(others));
        when(repository.findBySessionIdOrderByCreatedAtAsc("missing")).thenReturn(List.of());

        Throwable notMine = catchThrowable(() -> service.getMyConsultationDetail(1L, "others"));
        Throwable missing = catchThrowable(() -> service.getMyConsultationDetail(1L, "missing"));

        assertThat(notMine).isInstanceOf(ResourceNotFoundException.class);
        assertThat(missing).isInstanceOf(ResourceNotFoundException.class);
        // 메시지가 다르면 남의 상담이 존재한다는 사실을 알 수 있다
        assertThat(notMine.getMessage()).isEqualTo(missing.getMessage());
    }
}
