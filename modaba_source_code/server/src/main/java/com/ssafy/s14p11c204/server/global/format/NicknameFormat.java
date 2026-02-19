package com.ssafy.s14p11c204.server.global.format;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Size(min = 2, max = 10, message = "닉네임은 2~10자 사이여야 합니다.")
@Pattern(
        regexp = "^[가-힣a-zA-Z0-9]++$",
        message = "닉네임은 한글, 영문, 숫자만 사용할 수 있습니다."
)
@Documented
public @interface NicknameFormat {
    String message() default "닉네임 형식이 올바르지 않습니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}