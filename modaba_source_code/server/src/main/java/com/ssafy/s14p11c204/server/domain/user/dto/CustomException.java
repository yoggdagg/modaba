package com.ssafy.s14p11c204.server.domain.user.dto;

public class CustomException extends RuntimeException {
    private final String errorCode;
    private final String errorMessage;

    public CustomException(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public class NoAccountException extends CustomException {
        public NoAccountException(String errorCode, String errorMessage) {
            super(errorCode, errorMessage);
        }
    }

    public class NoUserException extends CustomException {
        public NoUserException(String errorCode, String errorMessage) {
            super(errorCode, errorMessage);
        }
    }

    public class NoNumberException extends CustomException {
        public NoNumberException(String errorCode, String errorMessage) {
            super(errorCode, errorMessage);
        }
    }
}
