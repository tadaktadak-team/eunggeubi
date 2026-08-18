package com.tadaktadak.eunggeubi.domain.ai_consultations.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

// <svc> 안에 질병명/ID와 섹션 목록(cntntsClList)이 들어있다.
// cntntsCl이 1개일 땐 XML상 객체 하나로만 오고, 여러 개일 땐 반복 태그로 온다.
// Jackson XML은 필드 타입을 List로 선언해두면 1개/여러 개 둘 다 알아서 리스트로 묶어준다.
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Svc {

    @JacksonXmlProperty(localName = "CNTNTSSJ")
    private String diseaseName;

    @JacksonXmlProperty(localName = "CNTNTS_SN")
    private String cntntsSn;

    @JacksonXmlElementWrapper(localName = "cntntsClList")
    @JacksonXmlProperty(localName = "cntntsCl")
    private List<Section> sections;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Section {
        @JacksonXmlProperty(localName = "CNTNTS_CL_NM")
        private String name;

        @JacksonXmlProperty(localName = "CNTNTS_CL_CN")
        private String content;
    }
}
