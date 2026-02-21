package com.college.icrs.repository;

import com.college.icrs.model.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    List<StatusHistory> findByGrievanceIdOrderByChangedAtDesc(Long grievanceId);
}
