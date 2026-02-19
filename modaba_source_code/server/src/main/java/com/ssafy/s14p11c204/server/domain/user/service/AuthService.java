package com.ssafy.s14p11c204.server.domain.user.service;

import com.nimbusds.openid.connect.sdk.LogoutRequest;
import com.ssafy.s14p11c204.server.domain.user.dto.LoginRequest;
import com.ssafy.s14p11c204.server.domain.user.dto.LoginResponse;
import com.ssafy.s14p11c204.server.domain.user.dto.PwResetCodeRequest;
import com.ssafy.s14p11c204.server.domain.user.dto.SignupRequest;
import com.ssafy.s14p11c204.server.domain.user.dto.TokenReissueRequest;
import com.ssafy.s14p11c204.server.domain.user.dto.TokenReissueResponse;

public interface AuthService {
    void signup(SignupRequest request);

    LoginResponse login(LoginRequest request);

    void logout(String refreshToken);

    String reissue(String refreshToken);

    void sendCode(String email);

    void verifyCode(String email, String code);

    void resetPw(String email, String code, String newPw);
}
