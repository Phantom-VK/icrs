package com.college.icrs.service;

import com.college.icrs.dto.CategoryResponseDTO;
import com.college.icrs.model.Category;
import com.college.icrs.model.Subcategory;
import com.college.icrs.model.User;
import com.college.icrs.repository.CategoryRepository;
import com.college.icrs.repository.SubcategoryRepository;
import com.college.icrs.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryCatalogService {

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final UserRepository userRepository;

    private static final List<CatalogCategory> CATALOG = List.of(
            new CatalogCategory(1L, "Academic", "Coursework, grades, attendance", false, false, "faculty@college.edu", List.of(
                    new CatalogSubcategory(101L, "Exams", "Exam timetable, hall tickets", "examcell@college.edu"),
                    new CatalogSubcategory(102L, "Grades", "Grade corrections and disputes", "faculty@college.edu"),
                    new CatalogSubcategory(103L, "Attendance", "Attendance shortages / regularization", "faculty@college.edu")
            )),
            new CatalogCategory(2L, "Administrative", "Certificates, IDs, admin processes", false, false, "faculty2@college.edu", List.of(
                    new CatalogSubcategory(201L, "Certificates", "Bonafide, transcripts, ID cards", "faculty2@college.edu"),
                    new CatalogSubcategory(202L, "Transfers", "Section change, branch transfer requests", "faculty2@college.edu")
            )),
            new CatalogCategory(3L, "IT Support", "LMS, email, Wi-Fi, lab access", false, false, "it.support@college.edu", List.of(
                    new CatalogSubcategory(301L, "WiFi / Network", "Campus Wi-Fi, VPN, network access", "it.support@college.edu"),
                    new CatalogSubcategory(302L, "LMS / Email", "LMS access, college email issues", "it.support@college.edu"),
                    new CatalogSubcategory(303L, "Lab Machines", "Lab desktop / software access", "it.support@college.edu")
            )),
            new CatalogCategory(4L, "Hostel & Accommodation", "Room allocation, facilities, maintenance", false, false, "hostel@college.edu", List.of(
                    new CatalogSubcategory(401L, "Room Allocation", "Allotment, change requests", "hostel@college.edu"),
                    new CatalogSubcategory(402L, "Maintenance", "Repairs, cleanliness, electricity", "hostel@college.edu"),
                    new CatalogSubcategory(403L, "Mess", "Food quality, billing issues", "hostel@college.edu")
            )),
            new CatalogCategory(5L, "Finance & Scholarships", "Fees, refunds, stipends, scholarships", false, false, "finance@college.edu", List.of(
                    new CatalogSubcategory(501L, "Fee Payment", "Payment failures, late fees", "finance@college.edu"),
                    new CatalogSubcategory(502L, "Scholarship", "Disbursement delays, eligibility", "finance@college.edu"),
                    new CatalogSubcategory(503L, "Refunds", "Refund status and timelines", "finance@college.edu")
            )),
            new CatalogCategory(6L, "Discipline & Safety", "Code of conduct, harassment, security", false, false, "discipline@college.edu", List.of(
                    new CatalogSubcategory(601L, "Code of Conduct", "Ragging, harassment, misconduct", "discipline@college.edu"),
                    new CatalogSubcategory(602L, "Security", "Campus security or safety concerns", "discipline@college.edu")
            )),
            new CatalogCategory(7L, "Examinations", "Timetable, hall tickets, revaluation", false, false, "examcell@college.edu", List.of(
                    new CatalogSubcategory(701L, "Revaluation", "Revaluation / recounting requests", "examcell@college.edu"),
                    new CatalogSubcategory(702L, "Exam Schedule", "Timetable clashes or errors", "examcell@college.edu"),
                    new CatalogSubcategory(703L, "Hall Ticket", "Download / correction issues", "examcell@college.edu")
            )),
            new CatalogCategory(8L, "Harassment / PoSH", "PoSH complaints routed to ICC", true, true, "icc@college.edu", List.of(
                    new CatalogSubcategory(801L, "PoSH Complaint", "Sexual harassment / PoSH case", "icc@college.edu"),
                    new CatalogSubcategory(802L, "Ragging", "Ragging incidents", "icc@college.edu")
            ))
    );

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<CategoryResponseDTO> listCatalog() {
        return CATALOG.stream()
                .sorted(Comparator.comparing(CatalogCategory::id))
                .map(this::toDto)
                .toList();
    }

    public Category resolveCategory(Long categoryId, String categoryName) {
        CatalogCategory catalogCategory = findCategory(categoryId, categoryName)
                .orElseThrow(() -> new java.util.NoSuchElementException("Category not found"));

        Category category = categoryRepository.findByNameIgnoreCase(catalogCategory.name()).orElseGet(Category::new);
        category.setName(catalogCategory.name());
        category.setDescription(catalogCategory.description());
        category.setSensitive(catalogCategory.sensitive());
        category.setHideIdentity(catalogCategory.hideIdentity());
        category.setDefaultAssignee(findUserByEmail(catalogCategory.defaultAssigneeEmail()));
        return categoryRepository.save(category);
    }

    public Subcategory resolveSubcategory(Category category, Long subcategoryId, String subcategoryName) {
        if (category == null) {
            throw new IllegalArgumentException("Category is required to resolve subcategory");
        }

        CatalogCategory catalogCategory = findCategory(null, category.getName())
                .orElseThrow(() -> new java.util.NoSuchElementException("Category not found"));

        CatalogSubcategory catalogSubcategory = findSubcategory(catalogCategory, subcategoryId, subcategoryName)
                .orElseThrow(() -> new java.util.NoSuchElementException("Subcategory not found"));

        Subcategory subcategory = subcategoryRepository
                .findByNameIgnoreCaseAndCategoryId(catalogSubcategory.name(), category.getId())
                .orElseGet(Subcategory::new);
        subcategory.setName(catalogSubcategory.name());
        subcategory.setDescription(catalogSubcategory.description());
        subcategory.setCategory(category);
        subcategory.setDefaultAssignee(findUserByEmail(catalogSubcategory.defaultAssigneeEmail()));
        return subcategoryRepository.save(subcategory);
    }

    private Optional<CatalogCategory> findCategory(Long categoryId, String categoryName) {
        return CATALOG.stream()
                .filter(category -> matches(category.id(), categoryId) || matches(category.name(), categoryName))
                .findFirst();
    }

    private Optional<CatalogSubcategory> findSubcategory(CatalogCategory category, Long subcategoryId, String subcategoryName) {
        return category.subcategories().stream()
                .filter(subcategory -> matches(subcategory.id(), subcategoryId) || matches(subcategory.name(), subcategoryName))
                .findFirst();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new java.util.NoSuchElementException("Default assignee not found for email: " + email));
    }

    private CategoryResponseDTO toDto(CatalogCategory category) {
        CategoryResponseDTO dto = new CategoryResponseDTO();
        dto.setId(category.id());
        dto.setName(category.name());
        dto.setDescription(category.description());
        dto.setSensitive(category.sensitive());
        dto.setHideIdentity(category.hideIdentity());
        dto.setDefaultAssigneeName(findUserByEmail(category.defaultAssigneeEmail()).getDisplayName());
        dto.setSubcategories(category.subcategories().stream()
                .sorted(Comparator.comparing(CatalogSubcategory::id))
                .map(this::toDto)
                .toList());
        return dto;
    }

    private CategoryResponseDTO.SubcategoryResponseDTO toDto(CatalogSubcategory subcategory) {
        CategoryResponseDTO.SubcategoryResponseDTO dto = new CategoryResponseDTO.SubcategoryResponseDTO();
        dto.setId(subcategory.id());
        dto.setName(subcategory.name());
        dto.setDescription(subcategory.description());
        dto.setDefaultAssigneeName(findUserByEmail(subcategory.defaultAssigneeEmail()).getDisplayName());
        return dto;
    }

    private boolean matches(Long left, Long right) {
        return left != null && right != null && left.equals(right);
    }

    private boolean matches(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private record CatalogCategory(
            Long id,
            String name,
            String description,
            boolean sensitive,
            boolean hideIdentity,
            String defaultAssigneeEmail,
            List<CatalogSubcategory> subcategories
    ) { }

    private record CatalogSubcategory(
            Long id,
            String name,
            String description,
            String defaultAssigneeEmail
    ) { }
}
