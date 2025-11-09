package com.college.icrs.repository;

import com.college.icrs.model.Grievance;
import com.college.icrs.model.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrievanceRepository extends JpaRepository<Grievance, Long> {

    // ✅ Fetch grievances submitted by a specific student
    List<Grievance> findByStudentId(Long studentId);

    // ✅ Fetch grievances filtered by current status (with pagination)
    Page<Grievance> findByStatus(Status status, Pageable pageable);

    // ✅ Fetch grievances by category and status (e.g., for departmental filters)
    List<Grievance> findByCategoryAndStatus(String category, Status status);

    // ✅ Fetch grievances assigned to a specific faculty member
    List<Grievance> findByAssignedToId(Long facultyId);

    // ✅ Optional: Case-insensitive title search (useful for “search by keyword”)
    List<Grievance> findByTitleContainingIgnoreCase(String title);
}
