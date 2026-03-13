package com.college.icrs.repository;

import com.college.icrs.model.Grievance;
import com.college.icrs.model.Priority;
import com.college.icrs.model.Sentiment;
import com.college.icrs.model.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrievanceRepository extends JpaRepository<Grievance, Long> {

    List<Grievance> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    Page<Grievance> findByStatus(Status status, Pageable pageable);

    List<Grievance> findByCategoryIdAndStatus(Long categoryId, Status status);

    List<Grievance> findByAssignedToId(Long facultyId);

    List<Grievance> findByTitleContainingIgnoreCase(String title);

    List<Grievance> findByTitleContainingIgnoreCaseOrAiTitleContainingIgnoreCase(String title, String aiTitle);

    Page<Grievance> findByAiResolved(boolean aiResolved, Pageable pageable);

    Page<Grievance> findByPriority(Priority priority, Pageable pageable);

    Page<Grievance> findBySentiment(Sentiment sentiment, Pageable pageable);

    long countByAiResolvedTrue();

    long countByAiResolvedFalse();
}
