package com.tadaktadak.eunggeubi.domain.drug.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tadaktadak.eunggeubi.domain.drug.service.DrugService;
import com.tadaktadak.eunggeubi.domain.drug.service.DurService;
import com.tadaktadak.eunggeubi.domain.drug.service.PillService;
import com.tadaktadak.eunggeubi.global.exception.GlobalExceptionHandler;
import com.tadaktadak.eunggeubi.global.exception.ResourceNotFoundException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

// 약물 API의 입력 상한과 오류 응답(400/404) 형식을 서버 없이 확인한다.
class DrugInputValidationTest {

    private final PillService pillService = mock(PillService.class);
    private final DurService durService = mock(DurService.class);
    private final DrugService drugService = mock(DrugService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders
                .standaloneSetup(new PillController(pillService), new InteractionController(durService),
                        new DrugController(drugService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                // 클래스패스에 XML 변환기도 있어 지정하지 않으면 응답이 XML로 나올 수 있다(실서버는 JSON으로 응답한다)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    // ---- 낱알 검색 ----

    @Test
    void 낱알검색_정상_요청은_통과한다() throws Exception {
        mvc.perform(get("/api/drugs/pills/identification")
                        .param("drugShape", "원형").param("colorClass", "하양").param("imprint", "G50")
                        .param("pageNo", "2").param("numOfRows", "20"))
                .andExpect(status().isOk());
        verify(pillService).searchPills(any());
    }

    @Test
    void 낱알검색_쿼리를_안_줘도_기본값으로_통과한다() throws Exception {
        mvc.perform(get("/api/drugs/pills/identification")).andExpect(status().isOk());
    }

    @Test
    void 낱알검색_pageNo가_숫자가_아니면_변환_오류_문장_대신_짧은_안내를_준다() throws Exception {
        mvc.perform(get("/api/drugs/pills/identification").param("pageNo", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("pageNo 값의 형식이 올바르지 않습니다."))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("abc"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("java.lang"))));
        verifyNoInteractions(pillService);
    }

    @Test
    void 낱알검색_식별문자가_50자를_넘으면_거절한다() throws Exception {
        mvc.perform(get("/api/drugs/pills/identification").param("imprint", "A".repeat(51)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("식별문자는 50자 이내로 입력해주세요."));
        verifyNoInteractions(pillService);
    }

    @Test
    void 낱알검색_식별문자가_정확히_50자면_통과한다() throws Exception {
        mvc.perform(get("/api/drugs/pills/identification").param("imprint", "A".repeat(50)))
                .andExpect(status().isOk());
    }

    @Test
    void 낱알검색_조회_건수와_페이지_범위를_벗어나면_거절한다() throws Exception {
        mvc.perform(get("/api/drugs/pills/identification").param("numOfRows", "101"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/drugs/pills/identification").param("numOfRows", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/drugs/pills/identification").param("pageNo", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/drugs/pills/identification").param("pageNo", "100001"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(pillService);
    }

    // ---- 상호작용 체크 ----

    @Test
    void 상호작용_약이_20개까지는_통과한다() throws Exception {
        mvc.perform(post("/api/drugs/interactions/check").contentType(MediaType.APPLICATION_JSON)
                        .content(body(20)))
                .andExpect(status().isOk());
        verify(durService).checkInteractions(any());
    }

    @Test
    void 상호작용_약이_21개면_거절한다() throws Exception {
        mvc.perform(post("/api/drugs/interactions/check").contentType(MediaType.APPLICATION_JSON)
                        .content(body(21)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("한 번에 최대 20개까지 확인할 수 있습니다."));
        verifyNoInteractions(durService);
    }

    @Test
    void 상호작용_품목번호가_숫자가_아니거나_너무_길면_거절한다() throws Exception {
        mvc.perform(post("/api/drugs/interactions/check").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemSeqs\":[\"200410085\",\"abc\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("약품 번호가 올바르지 않습니다."));
        mvc.perform(post("/api/drugs/interactions/check").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemSeqs\":[\"" + "1".repeat(21) + "\"]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/drugs/interactions/check").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemSeqs\":[\"\"]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(durService);
    }

    @Test
    void 상호작용_목록이_없으면_거절한다() throws Exception {
        mvc.perform(post("/api/drugs/interactions/check").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("약 목록이 필요합니다."));
        verifyNoInteractions(durService);
    }

    // ---- 약 검색/상세 ----

    @Test
    void 약검색_pageNo가_숫자가_아니면_짧은_안내를_준다() throws Exception {
        mvc.perform(get("/api/drugs/search").param("keyword", "타이레놀").param("pageNo", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("pageNo 값의 형식이 올바르지 않습니다."))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("abc"))));
    }

    @Test
    void 없는_약을_조회하면_404이고_입력값은_응답에_넣지_않는다() throws Exception {
        when(drugService.getDrugDetail("999999999"))
                .thenThrow(new ResourceNotFoundException("해당 약물 정보를 찾을 수 없습니다."));
        mvc.perform(get("/api/drugs/999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("해당 약물 정보를 찾을 수 없습니다."))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("999999999"))));
    }

    @Test
    void 약품_번호_형식이_틀리면_404가_아니라_400이다() throws Exception {
        mvc.perform(get("/api/drugs/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("약품 번호가 올바르지 않습니다."));
        verifyNoInteractions(drugService);
    }

    private static String body(int count) {
        String items = IntStream.rangeClosed(1, count)
                .mapToObj(i -> "\"" + (200000000 + i) + "\"")
                .collect(Collectors.joining(","));
        return "{\"itemSeqs\":[" + items + "]}";
    }
}
