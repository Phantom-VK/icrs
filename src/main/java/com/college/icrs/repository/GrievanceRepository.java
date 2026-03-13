package com.college.icrs.repository;

import com.college.icrs.model.Grievance;
import com.college.icrs.model.Status;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrievanceRepository extends JpaRepository<Grievance, Long> {

    @EntityGraph(attributePaths = {"category", "subcategory", "student", "assignedTo"})
    List<Grievance> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    @EntityGraph(attributePaths = {"category", "subcategory", "student", "assignedTo"})
    Page<Grievance> findByStatus(Status status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"category", "subcategory", "student", "assignedTo"})
    Page<Grievance> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"category", "subcategory", "student", "assignedTo", "statusHistory", "statusHistory.actor"})
    java.util.Optional<Grievance> findById(Long id);

    long countByStatus(Status status);

    long countByAiResolvedTrue();
}
