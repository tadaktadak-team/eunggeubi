package com.tadaktadak.eunggeubi.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tadaktadak.eunggeubi.domain.auth.service.GuardianConsentService;
import com.tadaktadak.eunggeubi.domain.user.entity.Guardian;
import com.tadaktadak.eunggeubi.domain.user.repository.GuardianRepository;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import com.tadaktadak.eunggeubi.global.exception.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GuardianServiceTest {

    private final GuardianRepository guardianRepository = mock(GuardianRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final GuardianConsentService consentService = mock(GuardianConsentService.class);
    private GuardianService service;

    @BeforeEach
    void setUp() {
        service = new GuardianService(guardianRepository, userRepository, consentService);
    }

    @Test
    void 없는_보호자와_남의_보호자는_똑같은_응답이다() {
        Guardian others = mock(Guardian.class);
        when(others.getUserId()).thenReturn(2L);
        when(guardianRepository.findById(10L)).thenReturn(Optional.of(others));
        when(guardianRepository.findById(99L)).thenReturn(Optional.empty());

        Throwable missing = org.assertj.core.api.Assertions.catchThrowable(() -> service.deleteGuardian(1L, 99L));
        Throwable notMine = org.assertj.core.api.Assertions.catchThrowable(() -> service.deleteGuardian(1L, 10L));

        assertThat(missing).isInstanceOf(ResourceNotFoundException.class);
        assertThat(notMine).isInstanceOf(ResourceNotFoundException.class);
        // 메시지가 다르면 번호를 바꿔 가며 어떤 보호자가 존재하는지 알아낼 수 있다
        assertThat(notMine.getMessage()).isEqualTo(missing.getMessage());
        verify(guardianRepository, never()).delete(others);
        verify(consentService, never()).expirePending(10L);
    }

    @Test
    void 내_보호자는_삭제되고_보낸_동의_링크는_무효가_된다() {
        Guardian mine = mock(Guardian.class);
        when(mine.getUserId()).thenReturn(1L);
        when(guardianRepository.findById(10L)).thenReturn(Optional.of(mine));

        service.deleteGuardian(1L, 10L);

        verify(guardianRepository).delete(mine);
        verify(consentService).expirePending(10L);
    }

    @Test
    void 남의_보호자에_동의_문자를_다시_보낼_수_없다() {
        Guardian others = mock(Guardian.class);
        when(others.getUserId()).thenReturn(2L);
        when(guardianRepository.findById(10L)).thenReturn(Optional.of(others));

        assertThatThrownBy(() -> service.resendVerification(1L, 10L)).isInstanceOf(ResourceNotFoundException.class);
        verify(consentService, never()).requestVerification(others);
    }
}
