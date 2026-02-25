package com.college.icrs.controller;

import com.college.icrs.model.User;
import com.college.icrs.service.UserService;
import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "http://localhost:3000")
@lombok.RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostConstruct
    public void printRoutes() {
        System.out.println("UserController active: /users/me, /users/test, /users");
    }

    /** Returns details of the authenticated user (derived from JWT email) */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> getAuthenticatedUser(Authentication authentication) {
        System.out.println("/users/me endpoint hit");

        if (authentication == null || authentication.getName() == null) {
            System.out.println("No authentication context found — unauthorized request");
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        System.out.println("Extracted email from JWT: " + email);

        User currentUser = userService.findByEmail(email);
        if (currentUser == null) {
            System.out.println("No user found for email: " + email);
            return ResponseEntity.notFound().build();
        }

        currentUser.setPassword(null);
        currentUser.setVerificationCode(null);
        currentUser.setVerificationCodeExpiresAt(null);

        System.out.println("Returning user: " + currentUser.getEmail());
        return ResponseEntity.ok(currentUser);
    }

    /** Returns all users (intended for admin use only) */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','FACULTY')")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.allUsers();
        users.forEach(u -> {
            u.setPassword(null);
            u.setVerificationCode(null);
            u.setVerificationCodeExpiresAt(null);
        });
        return ResponseEntity.ok(users);
    }

    /** Test route for connectivity verification */
    @GetMapping("/test")
    public String testRoute() {
        System.out.println("/users/test route hit successfully");
        return "/users/test route works!";
    }
}
