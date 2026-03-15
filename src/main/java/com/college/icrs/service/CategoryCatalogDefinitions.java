package com.college.icrs.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CategoryCatalogDefinitions {

    private static final List<CatalogCategoryDefinition> CATALOG = List.of(
            new CatalogCategoryDefinition(1L, "Academic", "Coursework, grades, attendance", false, false, "faculty@college.edu", List.of(
                    new CatalogSubcategoryDefinition(101L, "Exams", "Exam timetable, hall tickets", "examcell@college.edu"),
                    new CatalogSubcategoryDefinition(102L, "Grades", "Grade corrections and disputes", "faculty@college.edu"),
                    new CatalogSubcategoryDefinition(103L, "Attendance", "Attendance shortages / regularization", "faculty@college.edu")
            )),
            new CatalogCategoryDefinition(2L, "Administrative", "Certificates, IDs, admin processes", false, false, "faculty2@college.edu", List.of(
                    new CatalogSubcategoryDefinition(201L, "Certificates", "Bonafide, transcripts, ID cards", "faculty2@college.edu"),
                    new CatalogSubcategoryDefinition(202L, "Transfers", "Section change, branch transfer requests", "faculty2@college.edu")
            )),
            new CatalogCategoryDefinition(3L, "IT Support", "LMS, email, Wi-Fi, lab access", false, false, "it.support@college.edu", List.of(
                    new CatalogSubcategoryDefinition(301L, "WiFi / Network", "Campus Wi-Fi, VPN, network access", "it.support@college.edu"),
                    new CatalogSubcategoryDefinition(302L, "LMS / Email", "LMS access, college email issues", "it.support@college.edu"),
                    new CatalogSubcategoryDefinition(303L, "Lab Machines", "Lab desktop / software access", "it.support@college.edu")
            )),
            new CatalogCategoryDefinition(4L, "Hostel & Accommodation", "Room allocation, facilities, maintenance", false, false, "hostel@college.edu", List.of(
                    new CatalogSubcategoryDefinition(401L, "Room Allocation", "Allotment, change requests", "hostel@college.edu"),
                    new CatalogSubcategoryDefinition(402L, "Maintenance", "Repairs, cleanliness, electricity", "hostel@college.edu"),
                    new CatalogSubcategoryDefinition(403L, "Mess", "Food quality, billing issues", "hostel@college.edu")
            )),
            new CatalogCategoryDefinition(5L, "Finance & Scholarships", "Fees, refunds, stipends, scholarships", false, false, "finance@college.edu", List.of(
                    new CatalogSubcategoryDefinition(501L, "Fee Payment", "Payment failures, late fees", "finance@college.edu"),
                    new CatalogSubcategoryDefinition(502L, "Scholarship", "Disbursement delays, eligibility", "finance@college.edu"),
                    new CatalogSubcategoryDefinition(503L, "Refunds", "Refund status and timelines", "finance@college.edu")
            )),
            new CatalogCategoryDefinition(6L, "Discipline & Safety", "Code of conduct, harassment, security", false, false, "discipline@college.edu", List.of(
                    new CatalogSubcategoryDefinition(601L, "Code of Conduct", "Ragging, harassment, misconduct", "discipline@college.edu"),
                    new CatalogSubcategoryDefinition(602L, "Security", "Campus security or safety concerns", "discipline@college.edu")
            )),
            new CatalogCategoryDefinition(7L, "Examinations", "Timetable, hall tickets, revaluation", false, false, "examcell@college.edu", List.of(
                    new CatalogSubcategoryDefinition(701L, "Revaluation", "Revaluation / recounting requests", "examcell@college.edu"),
                    new CatalogSubcategoryDefinition(702L, "Exam Schedule", "Timetable clashes or errors", "examcell@college.edu"),
                    new CatalogSubcategoryDefinition(703L, "Hall Ticket", "Download / correction issues", "examcell@college.edu")
            )),
            new CatalogCategoryDefinition(8L, "Harassment / PoSH", "PoSH complaints routed to ICC", true, true, "icc@college.edu", List.of(
                    new CatalogSubcategoryDefinition(801L, "PoSH Complaint", "Sexual harassment / PoSH case", "icc@college.edu"),
                    new CatalogSubcategoryDefinition(802L, "Ragging", "Ragging incidents", "icc@college.edu")
            ))
    );

    public List<CatalogCategoryDefinition> all() {
        return CATALOG;
    }

    public Optional<CatalogCategoryDefinition> findCategory(Long categoryId, String categoryName) {
        return CATALOG.stream()
                .filter(category -> matches(category.id(), categoryId) || matches(category.name(), categoryName))
                .findFirst();
    }

    public Optional<CatalogSubcategoryDefinition> findSubcategory(
            CatalogCategoryDefinition category,
            Long subcategoryId,
            String subcategoryName
    ) {
        return category.subcategories().stream()
                .filter(subcategory -> matches(subcategory.id(), subcategoryId) || matches(subcategory.name(), subcategoryName))
                .findFirst();
    }

    private boolean matches(Long left, Long right) {
        return left != null && left.equals(right);
    }

    private boolean matches(String left, String right) {
        return left != null && left.equalsIgnoreCase(right);
    }
}
