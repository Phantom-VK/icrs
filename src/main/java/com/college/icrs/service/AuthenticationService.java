package com.college.icrs.service;

import com.college.icrs.dto.LoginUserDto;
import com.college.icrs.dto.RegisterUserDto;
import com.college.icrs.dto.VerifyUserDto;
import com.college.icrs.model.Role;
import com.college.icrs.model.User;
import com.college.icrs.repository.UserRepository;
import com.college.icrs.responses.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final JwtService jwtService;

    /**
     * ✅ Register a new user and send verification email
     */
    public User signup(RegisterUserDto input) {
        if (userRepository.findByEmail(input.getEmail()).isPresent()) {
            throw new RuntimeException("User with email " + input.getEmail() + " already exists.");
        }

        User user = new User();

        // ✅ Correct mapping
        user.setUsername(input.getUsername()); // full name (display)
        user.setEmail(input.getEmail()); // login identity
        user.setPassword(passwordEncoder.encode(input.getPassword()));
        user.setDepartment(input.getDepartment());
        user.setStudentId(input.getStudentId()); // ✅ previously missing
        user.setRole(Role.STUDENT);
        user.setEnabled(false); // must verify before login
        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        System.out.println("🧾 Signup DTO received: " + input);

        userRepository.save(user);
        sendVerificationEmail(user);

        return user;
    }

    /**
     * ✅ Login with email and password, returns JWT token + expiry
     */
    public LoginResponse login(LoginUserDto input) {
        // Verify user exists
        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + input.getEmail()));

        if (!user.isEnabled()) {
            throw new RuntimeException("Account not verified. Please verify your email.");
        }

        // Spring Security authentication
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(input.getEmail(), input.getPassword()));

        // ✅ Generate JWT token with email as subject
        String jwtToken = jwtService.generateToken(user);

        return new LoginResponse(
                jwtToken,
                jwtService.getExpirationTime(),
                user.getRole().name(), // 👈 send FACULTY or STUDENT
                user.getUsername(),
                user.getEmail());

    }

    /**
     * ✅ Verify user using email + verification code
     */
    public void verifyUser(VerifyUserDto input) {
        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getVerificationCodeExpiresAt() == null ||
                user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Verification code expired. Please request a new one.");
        }

        if (!user.getVerificationCode().equals(input.getVerificationCode())) {
            throw new RuntimeException("Invalid verification code.");
        }

        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);
    }

    /**
     * ✅ Resend verification code for unverified accounts
     */
    public void resendVerificationCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEnabled()) {
            throw new RuntimeException("Account already verified.");
        }

        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        sendVerificationEmail(user);
    }

    /**
     * ✅ Send verification email
     */
    private void sendVerificationEmail(User user) {
        String subject = "ICRS Account Verification Code";
        String html = "<h2>Welcome to ICRS</h2>"
                + "<p>Your verification code is: <b>" + user.getVerificationCode() + "</b></p>"
                + "<p>This code will expire in 15 minutes.</p>";

        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, html);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email: " + e.getMessage());
        }
    }

    /**
     * ✅ Generate a secure random 6-digit code
     */
    private String generateVerificationCode() {
        int code = new Random().nextInt(900000) + 100000; // 100000–999999
        return String.valueOf(code);
    }

    /**
     * ✅ Retrieve the authenticated user from the SecurityContext
     */
    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            Optional<User> user = userRepository.findByEmail(userDetails.getUsername());
            return user.orElse(null);
        }
        return null;
    }
}
