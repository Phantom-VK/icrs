package com.college.icrs.controller;

import com.college.icrs.dto.LoginUserDto;
import com.college.icrs.dto.RegisterUserDto;
import com.college.icrs.dto.VerifyUserDto;
import com.college.icrs.model.User;
import com.college.icrs.responses.LoginResponse;
import com.college.icrs.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import java.util.HashMap;
import java.util.Map;

//import java.util.HashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth") // ✅ Plain route — no "/api"
@CrossOrigin(origins = "http://localhost:3000") // ✅ Allow frontend during dev
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    // ✅ Signup
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody RegisterUserDto registerUserDto) {
        try {
            User user = authenticationService.signup(registerUserDto);

            // 🔥 Create safe response (shows verification code ONLY during signup)
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("email", user.getEmail());
            response.put("username", user.getUsername()); // Full name
            response.put("department", user.getDepartment());
            response.put("studentId", user.getStudentId());
            response.put("enabled", user.isEnabled());

            // 👇 The important part
            response.put("verificationCode", user.getVerificationCode());
            response.put("verificationCodeExpiresAt", user.getVerificationCodeExpiresAt());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("❌ Signup failed: " + e.getMessage());
        }
    }

    // ✅ Login
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginUserDto loginUserDto) {
        try {
            // AuthenticationService now returns the UPDATED LoginResponse
            LoginResponse response = authenticationService.login(loginUserDto);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // ❌ Error response must ALSO satisfy the 5-argument LoginResponse constructor
            LoginResponse errorResponse = new LoginResponse(
                    null, // token
                    0, // expiresIn
                    null, // role
                    null, // username
                    "❌ " + e.getMessage() // email field repurposed as message
            );

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    // ✅ Verify account
    @PostMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestBody VerifyUserDto verifyUserDto) {
        try {
            authenticationService.verifyUser(verifyUserDto);
            return ResponseEntity.ok("✅ Account verified successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("❌ Verification failed: " + e.getMessage());
        }
    }

    // ✅ Resend verification code
    @PostMapping("/resend")
    public ResponseEntity<String> resendVerificationCode(@RequestParam String email) {
        try {
            authenticationService.resendVerificationCode(email);
            return ResponseEntity.ok("✅ Verification code sent successfully!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("❌ Could not resend: " + e.getMessage());
        }
    }
}
