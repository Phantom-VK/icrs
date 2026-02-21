package com.college.icrs.config;

import com.college.icrs.model.*;
import com.college.icrs.repository.CategoryRepository;
import com.college.icrs.repository.SubcategoryRepository;
import com.college.icrs.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;

    public TestDataInitializer(UserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               CategoryRepository categoryRepository,
                               SubcategoryRepository subcategoryRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        User faculty1 = ensureFaculty("faculty@college.edu", "Faculty Member");
        User faculty2 = ensureFaculty("faculty2@college.edu", "Faculty Reviewer");

        Category academic = ensureCategory("Academic", "Academic related issues", faculty1);
        Category administrative = ensureCategory("Administrative", "Administrative issues", faculty2);

        ensureSubcategory("Exams", "Exam related queries", academic, faculty1);
        ensureSubcategory("Grades", "Grade corrections", academic, faculty1);
        ensureSubcategory("Fees", "Fee payment issues", administrative, faculty2);
        ensureSubcategory("Hostel", "Hostel administration", administrative, faculty2);
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

    private Category ensureCategory(String name, String description, User defaultAssignee) {
        return categoryRepository.findByNameIgnoreCase(name).orElseGet(() -> {
            Category category = new Category();
            category.setName(name);
            category.setDescription(description);
            category.setDefaultAssignee(defaultAssignee);
            return categoryRepository.save(category);
        });
    }

    private Subcategory ensureSubcategory(String name, String description, Category category, User defaultAssignee) {
        return subcategoryRepository.findByNameIgnoreCaseAndCategoryId(name, category.getId())
                .orElseGet(() -> {
                    Subcategory subcategory = new Subcategory();
                    subcategory.setName(name);
                    subcategory.setDescription(description);
                    subcategory.setCategory(category);
                    subcategory.setDefaultAssignee(defaultAssignee);
                    return subcategoryRepository.save(subcategory);
                });
    }
}
