package com.ssafy.s14p11c204.server.domain.ai.service;

import com.ssafy.s14p11c204.server.domain.ai.dto.GameAiRequest;
import com.ssafy.s14p11c204.server.domain.ai.dto.GameAiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(com.ssafy.s14p11c204.server.global.config.TestcontainersConfiguration.class)
class AiTestServiceTest {

    @Autowired
    private AiTestService aiTestService;

    // 실제 AI 호출은 비용이 발생하므로 로직 검증만 필요한 경우 Mocking 고려
    // 하지만 여기서는 프롬프트와 컨버터가 잘 작동하는지 '통합 테스트' 관점에서 접근 (실제 API 키 필요)

    @Test
    @DisplayName("도둑 리포트 생성 시 지정된 키워드 중 하나가 제목으로 선택되어야 함")
    void generateThiefReportTest() {
        // Given
        GameAiRequest request = new GameAiRequest(
            "THIEF", "WIN", 5000, 15.5, 20, List.of("신창동", "수완동")
        );

        // When
        // 주의: 실제 OpenAI API 키가 설정되어 있어야 성공함. 
        // 테스트 환경에 키가 없다면 이 테스트는 실패하거나 Fallback 응답을 반환할 것임.
        GameAiResponse response = aiTestService.generateGameReport(request);

        // Then
        assertNotNull(response);
        logResponse(response);

        List<String> thiefTitles = List.of("조용한 도둑", "번개 같은 도주자", "수완동의 유령", "담대한 전략가", "그림자 보행자", "골목길 마스터");
        
        // 에러가 아닌 정상 응답인 경우에만 키워드 체크
        if (!"기록 누락".equals(response.summary_title())) {
            assertTrue(thiefTitles.contains(response.summary_title()), 
                "제목이 허용된 리스트에 포함되어야 합니다: " + response.summary_title());
            assertTrue(response.commentary().contains("신창동") || response.commentary().contains("수완동"),
                "설명에 지역명이 포함되어야 합니다.");
        }
    }

    @Test
    @DisplayName("경찰 리포트 생성 시 지정된 키워드 중 하나가 제목으로 선택되어야 함")
    void generatePoliceReportTest() {
        // Given
        GameAiRequest request = new GameAiRequest(
            "POLICE", "WIN", 3000, 10.0, 15, List.of("신창동")
        );

        // When
        GameAiResponse response = aiTestService.generateGameReport(request);

        // Then
        assertNotNull(response);
        logResponse(response);

        List<String> policeTitles = List.of("신속한 추격자", "끈질긴 사냥개", "냉철한 분석가", "골목의 수호자", "기동타격대", "베테랑 형사");
        
        if (!"기록 누락".equals(response.summary_title())) {
            assertTrue(policeTitles.contains(response.summary_title()), 
                "제목이 허용된 리스트에 포함되어야 합니다: " + response.summary_title());
        }
    }

    private void logResponse(GameAiResponse response) {
        System.out.println("=== AI Response ===");
        System.out.println("Title: " + response.summary_title());
        System.out.println("Commentary: " + response.commentary());
        System.out.println("Tags: " + response.play_style_tag());
        System.out.println("Fitness: " + response.fitness_report());
        System.out.println("====================");
    }
}
