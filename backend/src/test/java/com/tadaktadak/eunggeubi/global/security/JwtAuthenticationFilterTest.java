package com.tadaktadak.eunggeubi.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tadaktadak.eunggeubi.domain.user.entity.UserStatus;
import com.tadaktadak.eunggeubi.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

// 탈퇴한 회원의 access 토큰은 만료 전이어도 인증하지 않는지 확인한다.
class JwtAuthenticationFilterTest {

    private final JwtProvider jwtProvider = mock(JwtProvider.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtProvider, userRepository);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void requestWithToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        when(jwtProvider.isAccessToken("access-token")).thenReturn(true);
        when(jwtProvider.getUserId("access-token")).thenReturn(1L);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    @Test
    void 정상_회원의_토큰은_인증한다() throws Exception {
        when(userRepository.existsByIdAndStatus(1L, UserStatus.ACTIVE)).thenReturn(true);

        requestWithToken();

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(1L);
    }

    @Test
    void 탈퇴한_회원의_토큰은_인증하지_않는다() throws Exception {
        when(userRepository.existsByIdAndStatus(1L, UserStatus.ACTIVE)).thenReturn(false);

        requestWithToken();

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
