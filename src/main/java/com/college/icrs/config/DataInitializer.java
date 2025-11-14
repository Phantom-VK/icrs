package com.college.icrs.config;

import com.college.icrs.model.Role;
import com.college.icrs.model.User;
import com.college.icrs.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // faculty account
        String facultyEmail = "faculty@college.edu";
        if (userRepository.findByEmail(facultyEmail).isEmpty()) {
            User faculty = new User();
            faculty.setUsername("Faculty Member");
            faculty.setEmail(facultyEmail);
            faculty.setPassword(passwordEncoder.encode("faculty123")); // hardcoded password
            faculty.setRole(Role.FACULTY);
            faculty.setEnabled(true);
            userRepository.save(faculty);
            System.out.println("Seeded faculty user: " + facultyEmail + " / faculty123");
        }
    }
}
