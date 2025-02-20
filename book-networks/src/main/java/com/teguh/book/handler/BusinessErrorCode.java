package com.teguh.book.handler;

import org.springframework.http.HttpStatus;

import lombok.Getter;

public enum BusinessErrorCode {
    NO_CODE(0, HttpStatus.NOT_IMPLEMENTED, "Nod code"),
    INCORRECT_CURRENT_PASSWORD(300, HttpStatus.BAD_REQUEST, "Incorrect current password"),
    NEW_PASSWORD_DOES_NOT_MATCH(301, HttpStatus.BAD_REQUEST, "New password does not match"),
    ACCOUNT_LOCKED(302, HttpStatus.FORBIDDEN, "User account is locked"),
    ACCOUNT_DISABLED(303, HttpStatus.FORBIDDEN, "User account is disabled"),
    BAD_CREDENTIALS(304, HttpStatus.FORBIDDEN, "Email and / or password is incorrect");

    @Getter
    private final int code;
    @Getter
    private final String description;
    @Getter
    private final HttpStatus httpStatus;

    BusinessErrorCode(int code, HttpStatus httpStatus, String description) {
        this.code = code;
        this.description = description;
        this.httpStatus = httpStatus;
    }
}
