package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;

// KDCA 건강정보 API 응답 루트 <XML>. 실제 토큰으로 호출해 검증 완료
// (cntntsSn=5684 기준 루트 태그가 <response>가 아니라 <XML>인 것 확인).
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "XML")
public class KdcaHealthInfoResponse {

    @JacksonXmlProperty(localName = "HEAD")
    private Head head;

    @JacksonXmlProperty(localName = "svc")
    private Svc svc;

    public boolean isSuccess() {
        return head != null && "S001".equals(head.getCode());
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Head {
        @JacksonXmlProperty(localName = "CODE")
        private String code;

        @JacksonXmlProperty(localName = "MESSAGE")
        private String message;
    }
}
