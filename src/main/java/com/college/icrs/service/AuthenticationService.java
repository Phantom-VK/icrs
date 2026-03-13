package com.college.icrs.service;

import com.college.icrs.dto.LoginUserDto;
import com.college.icrs.dto.RegisterUserDto;
import com.college.icrs.dto.VerifyUserDto;
import com.college.icrs.exception.ConflictException;
import com.college.icrs.exception.ExternalServiceException;
import com.college.icrs.exception.InvalidRequestException;
import com.college.icrs.exception.ResourceNotFoundException;
import com.college.icrs.exception.UnauthorizedException;
import com.college.icrs.logging.IcrsLog;
import com.college.icrs.model.Role;
import com.college.icrs.model.User;
import com.college.icrs.repository.UserRepository;
import com.college.icrs.responses.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final JwtService jwtService;

    /** Register a new user and send verification email */
    public User signup(RegisterUserDto input) {
        log.info(IcrsLog.event("auth.signup.start", "email", input.getEmail(), "studentId", input.getStudentId()));
        if (userRepository.findByEmail(input.getEmail()).isPresent()) {
            log.warn(IcrsLog.event("auth.signup.conflict", "email", input.getEmail()));
            throw new ConflictException("User with email " + input.getEmail() + " already exists.");
        }

        User user = new User();

        user.setUsername(input.getUsername()); // full name (display)
        user.setEmail(input.getEmail()); // login identity
        user.setPassword(passwordEncoder.encode(input.getPassword()));
        user.setDepartment(input.getDepartment());
        user.setStudentId(input.getStudentId());
        user.setRole(Role.STUDENT);
        user.setEnabled(false); // must verify before login
        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));

        userRepository.save(user);
        sendVerificationEmail(user);
        log.info(IcrsLog.event("auth.signup.completed", "email", user.getEmail(), "enabled", user.isEnabled()));

        return user;
    }

    /** Login with email and password, returns JWT token + expiry */
    public LoginResponse login(LoginUserDto input) {
        log.info(IcrsLog.event("auth.login.start", "email", input.getEmail()));
        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!user.isEnabled()) {
            throw new UnauthorizedException("Account not verified. Please verify your email.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(input.getEmail(), input.getPassword()));

        String jwtToken = jwtService.generateToken(user);
        log.info(IcrsLog.event("auth.login.token-issued", "email", user.getEmail(), "role", user.getRole()));

        return new LoginResponse(
                jwtToken,
                jwtService.getExpirationTime(),
                user.getRole().name(),
                user.getUsername(),
                user.getEmail());

    }

    /** Verify user using email + verification code */
    public void verifyUser(VerifyUserDto input) {
        log.info(IcrsLog.event("auth.verify.start", "email", input.getEmail()));
        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (user.getVerificationCodeExpiresAt() == null ||
                user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRequestException("Verification code expired. Please request a new one.");
        }

        if (!user.getVerificationCode().equals(input.getVerificationCode())) {
            throw new InvalidRequestException("Invalid verification code.");
        }

        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);
        log.info(IcrsLog.event("auth.verify.completed", "email", user.getEmail()));
    }

    /** Resend verification code for unverified accounts */
    public void resendVerificationCode(String email) {
        log.info(IcrsLog.event("auth.resend.start", "email", email));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        if (user.isEnabled()) {
            throw new InvalidRequestException("Account already verified.");
        }

        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        sendVerificationEmail(user);
        log.info(IcrsLog.event("auth.resend.completed", "email", email));
    }

    /** Send verification email */
    private void sendVerificationEmail(User user) {
        String subject = "ICRS Account Verification Code";
        String html = "<h2>Welcome to ICRS</h2>"
                + "<p>Your verification code is: <b>" + user.getVerificationCode() + "</b></p>"
                + "<p>This code will expire in 15 minutes.</p>";

        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, html);
            log.info(IcrsLog.event("auth.verification-email.sent", "email", user.getEmail()));
        } catch (Exception e) {
            log.warn(IcrsLog.event("auth.verification-email.failed", "email", user.getEmail(), "reason", e.getClass().getSimpleName()));
            throw new ExternalServiceException("Failed to send verification email.");
        }
    }

    /** Generate a secure random 6-digit code */
    private String generateVerificationCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1_000_000);
        return String.valueOf(code);
    }
}
