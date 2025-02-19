package com.teguh.book.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthenticationRequest {
    @NotEmpty()
    @NotBlank()
    @Email()
    private String email;

    @NotEmpty()
    @NotBlank()
    @Size(min = 8)
    private String password;
}
