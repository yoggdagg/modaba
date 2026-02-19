package com.ssafy.s14p11c204.server.domain.user.service;

import com.ssafy.s14p11c204.server.domain.user.User;
import com.ssafy.s14p11c204.server.domain.user.dto.MmrHistoryDto;
import com.ssafy.s14p11c204.server.domain.user.dto.ProfileDetailResponse;
import com.ssafy.s14p11c204.server.domain.user.dto.ProfileUpdateRequest;
import com.ssafy.s14p11c204.server.domain.user.dto.PwUpdateRequest;

import java.util.List;

public interface AccountService {
    User getDetailedProfile(String email);

    void updateProfile(String email, ProfileUpdateRequest request);

    void updatePassword(String email, PwUpdateRequest request);

    void unregister(String email);
    
    // MMR 히스토리 조회 (추가됨)
    List<MmrHistoryDto> getMmrHistory(String email);
}
