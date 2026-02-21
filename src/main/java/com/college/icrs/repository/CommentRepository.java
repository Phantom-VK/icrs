package com.college.icrs.repository;

import com.college.icrs.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByGrievanceIdOrderByCreatedAtAsc(Long grievanceId);
}
