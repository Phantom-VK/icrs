package com.college.icrs.controller;

import com.college.icrs.dto.LoginUserDto;
import com.college.icrs.dto.RegisterUserDto;
import com.college.icrs.dto.VerifyUserDto;
import com.college.icrs.logging.IcrsLog;
import com.college.icrs.model.User;
import com.college.icrs.responses.LoginResponse;
import com.college.icrs.service.AuthenticationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody RegisterUserDto registerUserDto) {
        log.info(IcrsLog.event("auth.signup.request", "email", registerUserDto.getEmail()));
        User user = authenticationService.signup(registerUserDto);

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("username", user.getUsername());
        response.put("department", user.getDepartment());
        response.put("studentId", user.getStudentId());
        response.put("enabled", user.isEnabled());
        response.put("verificationCode", user.getVerificationCode());
        response.put("verificationCodeExpiresAt", user.getVerificationCodeExpiresAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginUserDto loginUserDto) {
        log.info(IcrsLog.event("auth.login.request", "email", loginUserDto.getEmail()));
        LoginResponse response = authenticationService.login(loginUserDto);
        log.info(IcrsLog.event("auth.login.success", "email", loginUserDto.getEmail(), "role", response.getRole()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyUser(@Valid @RequestBody VerifyUserDto verifyUserDto) {
        log.info(IcrsLog.event("auth.verify.request", "email", verifyUserDto.getEmail()));
        authenticationService.verifyUser(verifyUserDto);
        return ResponseEntity.ok("Account verified successfully.");
    }

    @PostMapping("/resend")
    public ResponseEntity<String> resendVerificationCode(@RequestParam @Email String email) {
        log.info(IcrsLog.event("auth.resend.request", "email", email));
        authenticationService.resendVerificationCode(email);
        return ResponseEntity.ok("Verification code sent successfully!");
    }
}
