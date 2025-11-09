package com.college.icrs.controller;

import com.college.icrs.model.User;
import com.college.icrs.service.UserService;
import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users") // ✅ Unified API prefix
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostConstruct
    public void printRoutes() {
        System.out.println("🗺️ UserController active: /api/users/me, /api/users/test");
    }

    /**
     * ✅ Returns authenticated user details (email derived from JWT)
     */
    @GetMapping("/me")
    public ResponseEntity<User> getAuthenticatedUser(Authentication authentication) {
        System.out.println("🟢 /api/users/me endpoint hit");

        if (authentication == null || authentication.getName() == null) {
            System.out.println("⚠️ No authentication context found");
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        System.out.println("🔍 Extracted email from JWT: " + email);

        User currentUser = userService.findByEmail(email);
        if (currentUser == null) {
            System.out.println("⚠️ No user found for email: " + email);
            return ResponseEntity.notFound().build();
        }

        // ✅ Sanitize sensitive fields
        currentUser.setPassword(null);
        currentUser.setVerificationCode(null);
        currentUser.setVerificationCodeExpiresAt(null);

        System.out.println("✅ Returning user: " + currentUser.getEmail());
        return ResponseEntity.ok(currentUser);
    }

    /**
     * ✅ Returns all users (admin use only)
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.allUsers();
        users.forEach(u -> u.setPassword(null));
        return ResponseEntity.ok(users);
    }

    /**
     * ✅ Test route for connectivity
     */
    @GetMapping("/test")
    public String testRoute() {
        return "✅ /api/users/test route works!";
    }
}
