package com.college.icrs.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;      // JWT token
    private long expiresIn;// Token expiration time (ms)
    private String role;
    private String username;  // optional but useful
    private String email;
}
