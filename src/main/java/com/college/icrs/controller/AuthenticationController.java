package com.college.icrs.controller;

import com.college.icrs.dto.LoginUserDto;
import com.college.icrs.dto.RegisterUserDto;
import com.college.icrs.dto.VerifyUserDto;
import com.college.icrs.model.User;
import com.college.icrs.responses.LoginResponse;
import com.college.icrs.service.AuthenticationService;
import com.college.icrs.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth") // ✅ Unified API prefix
public class AuthenticationController {

    private final JwtService jwtService;
    private final AuthenticationService authenticationService;

    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    /**
     * ✅ Register a new user and send verification email
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody RegisterUserDto registerUserDto) {
        try {
            User registeredUser = authenticationService.signup(registerUserDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(registeredUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("❌ Signup failed: " + e.getMessage());
        }
    }

    /**
     * ✅ Login user and return JWT token + expiration time
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginUserDto loginUserDto) {
        try {
            String jwtToken = authenticationService.login(loginUserDto); // returns token string
            LoginResponse response = new LoginResponse(jwtToken, jwtService.getExpirationTime());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse("❌ " + e.getMessage(), 0));
        }
    }

    /**
     * ✅ Verify user account using email + code
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyUserDto verifyUserDto) {
        try {
            authenticationService.verifyUser(verifyUserDto);
            return ResponseEntity.ok("✅ Account verified successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("❌ Verification failed: " + e.getMessage());
        }
    }

    /**
     * ✅ Resend verification code
     */
    @PostMapping("/resend")
    public ResponseEntity<?> resendVerificationCode(@RequestParam String email) {
        try {
            authenticationService.resendVerificationCode(email);
            return ResponseEntity.ok("✅ Verification code sent successfully!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("❌ Could not resend: " + e.getMessage());
        }
    }
}
