package com.college.icrs.controller;

import com.college.icrs.exception.ResourceNotFoundException;
import com.college.icrs.model.User;
import com.college.icrs.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;

@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final UserService userService;

    public User requireAuthenticatedUser(Authentication authentication) {
        String email = requireAuthenticatedEmail(authentication);
        User user = userService.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found for email: " + email);
        }
        return user;
    }

    public String requireAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return authentication.getName();
    }
}
