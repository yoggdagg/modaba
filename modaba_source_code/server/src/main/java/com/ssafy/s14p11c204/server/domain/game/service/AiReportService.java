package com.ssafy.s14p11c204.server.domain.game.service;

import com.ssafy.s14p11c204.server.domain.game.dao.ReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReportService {

    private final ReportMapper reportMapper;

    @Transactional
    public Long createReport(Long sessionId, Long userId) {
        // 1. 집중도 데이터 조회
        String focusData = reportMapper.findFocusScores(sessionId, userId);
        
        if (focusData == null) {
            log.warn("No focus data found for session {} user {}", sessionId, userId);
            return null;
        }

        // 2. AI 분석 요청 (TODO: 실제 Gemini/GPT API 연동)
        // 여기서는 간단한 분석 로직으로 대체
        String analysisResult = mockAiAnalysis(focusData);
        String summary = "오늘의 집중도는 훌륭했습니다!";

        // 3. 리포트 저장
        reportMapper.insertReport(sessionId, userId, analysisResult, summary);
        
        log.info("Report created for user {} in session {}", userId, sessionId);
        
        // ID를 반환하려면 Mapper에서 keyProperty를 받아와야 함 (일단 1L 리턴)
        return 1L; 
    }

    private String mockAiAnalysis(String jsonData) {
        // 데이터 길이에 따라 다른 멘트 생성
        int dataLength = jsonData.length();
        if (dataLength > 100) {
            return "전반적으로 매우 높은 집중력을 유지했습니다. 특히 중반부의 몰입도가 인상적입니다. 다음번엔 휴식 시간을 조금 더 규칙적으로 가져보면 좋겠네요.";
        } else {
            return "공부 시간이 조금 짧았지만, 짧은 시간 동안 집중하려 노력한 흔적이 보입니다. 다음엔 목표 시간을 조금 더 늘려볼까요?";
        }
    }
}
