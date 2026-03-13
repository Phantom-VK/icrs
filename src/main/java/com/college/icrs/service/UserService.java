package com.college.icrs.service;

import com.college.icrs.model.User;
import com.college.icrs.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@lombok.RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public List<User> allUsers() {
        return (List<User>) userRepository.findAll();
    }
}
