package com.college.icrs.responses;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginResponse {

    private String token;
    private long expiresIn;

    public LoginResponse(String token, long expiresIn) {
        this.token = token;
        this.expiresIn = expiresIn;
    }
}
