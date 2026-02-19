package com.ssafy.s14p11c204.server.domain.ai.service;

import com.ssafy.s14p11c204.server.domain.ai.dto.GameAiRequest;
import com.ssafy.s14p11c204.server.domain.ai.dto.GameAiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AiTestService {

    private final ChatClient chatClient;

    public AiTestService(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    public String testAiConnection() {
        log.info("AI 연결 테스트 시작: '안녕, 너는 누구니?'라고 물어봅니다.");
        
        try {
            String response = chatClient.prompt()
                    .user("안녕, 너는 누구니? 짧게 대답해줘.")
                    .call()
                    .content();
            log.info("AI 응답 수신 성공: {}", response);
            return response;
        } catch (Exception e) {
            log.error("AI 연결 실패: {}", e.getMessage(), e);
            return "AI 연결 실패: " + e.getMessage();
        }
    }

    public GameAiResponse generateGameReport(GameAiRequest request) {
        log.info("AI 게임 리포트 생성 시작: role={}, result={}", request.role(), request.result());

        // 1. 고정 키워드 리스트 정의 (나중에 DB나 설정파일로 분리 가능)
        List<String> policeTitles = List.of("신속한 추격자", "끈질긴 사냥개", "냉철한 분석가", "골목의 수호자", "기동타격대", "베테랑 형사");
        List<String> thiefTitles = List.of("조용한 도둑", "번개 같은 도주자", "수완동의 유령", "담대한 전략가", "그림자 보행자", "골목길 마스터");

        List<String> availableTitles = request.role().equals("POLICE") ? policeTitles : thiefTitles;
        var converter = new BeanOutputConverter<>(GameAiResponse.class);

        // 2. 시스템 프롬프트 설정 (페르소나 및 키워드 선택 강제)
        String systemPrompt = request.role().equals("POLICE") ?
                """
                당신은 베테랑 수사반장이다. 방금 작전을 마친 경찰 플레이어의 활동 데이터를 보고 냉철하면서도 실력을 인정해주는 수사 리포트를 작성하라.
                말투는 '~했군', '~다'로 끝나는 딱딱하고 전문적인 문체를 사용하라.
                """ :
                """
                당신은 전설적인 대도이자 플레이어의 범죄 조력자다. 경찰의 포위망을 따돌린 플레이어의 활동 데이터를 보고, 그들의 대담함과 영리함을 칭송하는 지하 세계의 기록을 작성하라.
                말투는 거칠고 냉소적이지만 플레이어의 무용담을 흥미진진하게 묘사하라.
                """;

        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(u -> u.text("""
                            아래 게임 데이터를 분석해서 리포트를 작성해줘.
                            
                            [데이터]
                            {data}
                            
                            [필수 규칙]
                            1. 제목(summary_title)은 반드시 아래 제공된 [선택 가능한 제목 목록] 중 데이터와 가장 잘 어울리는 '하나'를 선택하여 그대로 사용할 것. 절대로 목록에 없는 제목을 지어내지 마라.
                            2. 본문(commentary)은 선택한 제목의 느낌에 맞춰서, 왜 플레이어가 그 타이틀을 얻었는지 데이터(거리, 속도, 지명)를 근거로 들어 설명할 것.
                            3. 반드시 언급된 지명({locations})을 포함할 것.
                            
                            [선택 가능한 제목 목록]
                            {titleList}
                            
                            출력 형식: {format}
                            """)
                            .param("data", request.toString())
                            .param("locations", String.join(", ", request.locations()))
                            .param("titleList", String.join(", ", availableTitles))
                            .param("format", converter.getFormat()))
                    .call()
                    .entity(converter);
        } catch (Exception e) {
            log.error("AI 리포트 생성 실패: {}", e.getMessage(), e);
            return new GameAiResponse("기록 누락", "통신 장애로 인해 현장 기록이 소실되었습니다.", List.of("데이터 없음"), "0kcal");
        }
    }
}
