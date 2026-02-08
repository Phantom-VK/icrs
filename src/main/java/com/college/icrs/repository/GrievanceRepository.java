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

    List<Grievance> findByStudentId(Long studentId);

    Page<Grievance> findByStatus(Status status, Pageable pageable);

    List<Grievance> findByCategoryAndStatus(String category, Status status);

    List<Grievance> findByAssignedToId(Long facultyId);

    List<Grievance> findByTitleContainingIgnoreCase(String title);
}
