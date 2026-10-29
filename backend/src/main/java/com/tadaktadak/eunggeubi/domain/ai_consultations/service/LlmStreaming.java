package com.tadaktadak.eunggeubi.domain.ai_consultations.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;

// 구조화 출력(JSON)을 스트리밍으로 받으면서, 아직 다 안 온 JSON에서 화면에 먼저 보여줄 글을 뽑는다.
// AiConsultationService(1차 답변)와 ConsultationRegenerationService(재생성), FirstAidGuideService(응급처치)가 같이 쓴다.
public final class LlmStreaming {

    private static final ObjectMapper JSON = new ObjectMapper();
    // 문자열 값 안에서 다음 문장이 시작된 지점(문장부호 + 공백/줄바꿈). 공백까지 와야 끊어서 "38.5도"의
    // "38."에서 잘리지 않는다.
    private static final Pattern SENTENCE_END = Pattern.compile("[.!?。](?=\\s)|\\n");
    // 끝이 덜 온 이스케이프(백슬래시 하나, 또는 유니코드 이스케이프의 앞부분) - 디코딩 전에 떼어낸다.
    private static final Pattern TRAILING_PARTIAL_ESCAPE = Pattern.compile("\\\\(u[0-9a-fA-F]{0,3})?$");
    // 배열 안의 닫힌 문자열 원소 하나(앞 쉼표·공백 포함).
    private static final Pattern ARRAY_ITEM = Pattern.compile("\\s*,?\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private LlmStreaming() {
    }

    // entity()와 같은 형식 지시(BeanOutputConverter)를 붙여 스트리밍으로 받고, 토큰이 올 때마다 지금까지
    // 받은 원문 전체를 onBuffer로 넘긴다. 다 받으면 같은 변환기로 type을 만든다.
    public static <T> T stream(ChatClient chatClient, String system, String user, Class<T> type,
                        Consumer<CharSequence> onBuffer) {
        BeanOutputConverter<T> converter = new BeanOutputConverter<>(type);
        StringBuilder buffer = new StringBuilder();
        chatClient.prompt()
                .system(system)
                .user(user + System.lineSeparator() + converter.getFormat())
                .stream()
                .content()
                .doOnNext(token -> {
                    buffer.append(token);
                    onBuffer.accept(buffer);
                })
                .blockLast();
        return converter.convert(buffer.toString());
    }

    // 닫힌 "field": "..." 값들(=완성된 값). ponytail: 토큰마다 버퍼 전체를 다시 훑는다(O(n²)) - 답변이 몇
    // KB라 무시할 수준이고, 훨씬 길어지면 마지막 위치부터 이어서 찾게 바꿀 것.
    static List<String> closedStrings(CharSequence partialJson, String field) {
        List<String> values = new ArrayList<>();
        Matcher m = closedPattern(field).matcher(partialJson);
        while (m.find()) {
            String value = decode(m.group(1));
            if (value == null) {
                break; // 이스케이프가 깨진 값 - 뒤는 최종 응답에 맡긴다
            }
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    // "field": ["...", "...", "... 처럼 문자열 배열에서 닫힌 원소들(=완성된 원소). 배열이 아직 안 열렸으면 빈 목록.
    public static List<String> closedArrayItems(CharSequence partialJson, String field) {
        Matcher open = Pattern.compile("\"" + field + "\"\\s*:\\s*\\[").matcher(partialJson);
        if (!open.find()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        // lookingAt으로 바로 앞 원소에 이어지는 것만 잡는다 - "]"를 만나면 거기서 멈춘다.
        Matcher item = ARRAY_ITEM.matcher(partialJson);
        for (int pos = open.end(); item.region(pos, partialJson.length()).lookingAt(); pos = item.end()) {
            String value = decode(item.group(1));
            if (value == null) {
                break;
            }
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    // 아직 안 닫힌 "field": "... 값에서 끝난 문장까지만. 쓰고 있는 문장은 다 끝나야 보여준다.
    static String completedSentences(CharSequence partialJson, String field) {
        Matcher m = openPattern(field).matcher(partialJson);
        if (!m.find()) {
            return "";
        }
        String value = decode(TRAILING_PARTIAL_ESCAPE.matcher(m.group(1)).replaceFirst(""));
        if (value == null) {
            return "";
        }
        if (m.group(2) != null) {
            return value.strip(); // 값이 닫혔다 = 마지막 문장도 끝났다
        }
        int end = -1;
        Matcher sentence = SENTENCE_END.matcher(value);
        while (sentence.find()) {
            end = sentence.end();
        }
        return end < 0 ? "" : value.substring(0, end).strip();
    }

    private static Pattern closedPattern(String field) {
        return Pattern.compile("\"" + field + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    }

    private static Pattern openPattern(String field) {
        return Pattern.compile("\"" + field + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)(\")?");
    }

    private static String decode(String jsonStringBody) {
        try {
            return JSON.readValue("\"" + jsonStringBody + "\"", String.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
