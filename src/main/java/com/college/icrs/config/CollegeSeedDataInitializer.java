package com.college.icrs.config;

import com.college.icrs.model.*;
import com.college.icrs.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@lombok.RequiredArgsConstructor
public class CollegeSeedDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        ensureFaculty("faculty@college.edu", "Academic Desk");
        ensureFaculty("faculty2@college.edu", "Admin Desk");
        ensureFaculty("it.support@college.edu", "IT Support");
        ensureFaculty("hostel@college.edu", "Hostel Office");
        ensureFaculty("finance@college.edu", "Finance Office");
        ensureFaculty("discipline@college.edu", "Discipline Cell");
        ensureFaculty("examcell@college.edu", "Exam Cell");
        ensureFaculty("icc@college.edu", "ICC Committee");
    }

    private User ensureFaculty(String email, String name) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User faculty = new User();
            faculty.setUsername(name);
            faculty.setEmail(email);
            faculty.setPassword(passwordEncoder.encode("faculty123"));
            faculty.setRole(Role.FACULTY);
            faculty.setEnabled(true);
            return userRepository.save(faculty);
        });
    }

}
