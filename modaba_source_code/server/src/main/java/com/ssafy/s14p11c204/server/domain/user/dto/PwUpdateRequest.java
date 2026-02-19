package com.ssafy.s14p11c204.server.domain.user.dto;

import com.ssafy.s14p11c204.server.global.format.PasswordFormat;

public record PwUpdateRequest(
        @PasswordFormat String oldPass,
        @PasswordFormat String newPass
) {
}
