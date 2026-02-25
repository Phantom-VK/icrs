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
        // Faculty pool
        User faculty1 = ensureFaculty("faculty@college.edu", "Academic Desk");
        User faculty2 = ensureFaculty("faculty2@college.edu", "Admin Desk");
        User itFaculty = ensureFaculty("it.support@college.edu", "IT Support");
        User hostelFaculty = ensureFaculty("hostel@college.edu", "Hostel Office");
        User financeFaculty = ensureFaculty("finance@college.edu", "Finance Office");
        User disciplineFaculty = ensureFaculty("discipline@college.edu", "Discipline Cell");
        User examFaculty = ensureFaculty("examcell@college.edu", "Exam Cell");

        // Categories with default assignees
        Category academic = ensureCategory("Academic", "Coursework, grades, attendance", faculty1);
        Category administrative = ensureCategory("Administrative", "Certificates, IDs, admin processes", faculty2);
        Category itSupport = ensureCategory("IT Support", "LMS, email, Wi‑Fi, lab access", itFaculty);
        Category hostel = ensureCategory("Hostel & Accommodation", "Room allocation, facilities, maintenance", hostelFaculty);
        Category finance = ensureCategory("Finance & Scholarships", "Fees, refunds, stipends, scholarships", financeFaculty);
        Category discipline = ensureCategory("Discipline & Safety", "Code of conduct, harassment, security", disciplineFaculty);
        Category examinations = ensureCategory("Examinations", "Timetable, hall tickets, revaluation", examFaculty);

        // Subcategories pointing to defaults
        ensureSubcategory("Exams", "Exam timetable, hall tickets", academic, examFaculty);
        ensureSubcategory("Grades", "Grade corrections and disputes", academic, faculty1);
        ensureSubcategory("Attendance", "Attendance shortages / regularization", academic, faculty1);

        ensureSubcategory("Certificates", "Bonafide, transcripts, ID cards", administrative, faculty2);
        ensureSubcategory("Transfers", "Section change, branch transfer requests", administrative, faculty2);

        ensureSubcategory("WiFi / Network", "Campus Wi‑Fi, VPN, network access", itSupport, itFaculty);
        ensureSubcategory("LMS / Email", "LMS access, college email issues", itSupport, itFaculty);
        ensureSubcategory("Lab Machines", "Lab desktop / software access", itSupport, itFaculty);

        ensureSubcategory("Room Allocation", "Allotment, change requests", hostel, hostelFaculty);
        ensureSubcategory("Maintenance", "Repairs, cleanliness, electricity", hostel, hostelFaculty);
        ensureSubcategory("Mess", "Food quality, billing issues", hostel, hostelFaculty);

        ensureSubcategory("Fee Payment", "Payment failures, late fees", finance, financeFaculty);
        ensureSubcategory("Scholarship", "Disbursement delays, eligibility", finance, financeFaculty);
        ensureSubcategory("Refunds", "Refund status and timelines", finance, financeFaculty);

        ensureSubcategory("Code of Conduct", "Ragging, harassment, misconduct", discipline, disciplineFaculty);
        ensureSubcategory("Security", "Campus security or safety concerns", discipline, disciplineFaculty);

        ensureSubcategory("Revaluation", "Revaluation / recounting requests", examinations, examFaculty);
        ensureSubcategory("Exam Schedule", "Timetable clashes or errors", examinations, examFaculty);
        ensureSubcategory("Hall Ticket", "Download / correction issues", examinations, examFaculty);
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
