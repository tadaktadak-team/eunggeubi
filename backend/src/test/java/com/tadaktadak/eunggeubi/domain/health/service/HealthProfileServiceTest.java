package com.tadaktadak.eunggeubi.domain.health.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tadaktadak.eunggeubi.domain.health.dto.HealthProfileRequest;
import com.tadaktadak.eunggeubi.domain.health.dto.HealthProfileResponse;
import com.tadaktadak.eunggeubi.domain.health.entity.HealthProfile;
import com.tadaktadak.eunggeubi.domain.health.repository.HealthProfileRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// 병명·알레르기는 콤마로 이어 저장한다. 빈 값(null)이 섞여도, 항목 안에 콤마가 있어도 저장한 그대로 다시 읽히는지 확인한다.
class HealthProfileServiceTest {

    private final HealthProfileRepository repository = mock(HealthProfileRepository.class);
    private final HealthProfileService service = new HealthProfileService(repository);

    private HealthProfileResponse save(List<String> diseases, List<String> allergies) {
        when(repository.findByUserId(1L)).thenReturn(Optional.empty());
        when(repository.save(any(HealthProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        return service.saveProfile(1L, new HealthProfileRequest("A", diseases, null, null, allergies));
    }

    @Test
    void 목록에_null이_섞여도_서버_오류_없이_걸러낸다() {
        HealthProfileResponse res = save(Arrays.asList("고혈압", null), Arrays.asList(null, "땅콩"));

        assertThat(res.diseases()).containsExactly("고혈압");
        assertThat(res.allergies()).containsExactly("땅콩");
    }

    @Test
    void 콤마가_든_항목은_나눠_저장해_다시_읽어도_같다() {
        HealthProfileResponse res = save(List.of("고혈압, 당뇨"), List.of("땅콩,호두", "땅콩"));

        assertThat(res.diseases()).containsExactly("고혈압", "당뇨");
        assertThat(res.allergies()).containsExactly("땅콩", "호두");
    }

    @Test
    void 빈_항목만_있으면_비어_있는_목록이다() {
        HealthProfileResponse res = save(List.of(" ", ","), List.of());

        assertThat(res.diseases()).isEmpty();
        assertThat(res.allergies()).isEmpty();
    }
}
