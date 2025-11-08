package com.college.icrs.service;


import com.college.icrs.model.Grievance;
import com.college.icrs.model.Priority;
import com.college.icrs.model.Status;
import com.college.icrs.model.User;
import com.college.icrs.repository.GrievanceRepository;
import com.college.icrs.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class GrievanceService {

    private final GrievanceRepository grievanceRepository;
    private final UserRepository userRepository;

    public GrievanceService(GrievanceRepository grievanceRepository, UserRepository userRepository) {
        this.grievanceRepository = grievanceRepository;
        this.userRepository = userRepository;
    }

    // Create new grievance
    public Grievance createGrievance(Grievance grievance, Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));
        grievance.setStudent(student);
        grievance.setStatus(Status.SUBMITTED);
        return grievanceRepository.save(grievance);
    }

    // Get grievance by ID
    public Grievance getGrievanceById(Long id) {
        return grievanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Grievance not found with id: " + id));
    }

    // Update grievance
    public Grievance updateGrievance(Long id, Grievance grievanceDetails) {
        Grievance grievance = getGrievanceById(id);

        grievance.setTitle(grievanceDetails.getTitle());
        grievance.setDescription(grievanceDetails.getDescription());
        grievance.setCategory(grievanceDetails.getCategory());
        grievance.setSubcategory(grievanceDetails.getSubcategory());
        grievance.setPriority(grievanceDetails.getPriority());
        grievance.setStatus(grievanceDetails.getStatus());

        return grievanceRepository.save(grievance);
    }

    // Delete grievance
    public void deleteGrievance(Long id) {
        Grievance grievance = getGrievanceById(id);
        grievanceRepository.delete(grievance);
    }

    // Get all grievances with pagination
    public Page<Grievance> getAllGrievances(Pageable pageable) {
        return grievanceRepository.findAll(pageable);
    }

    // Get grievances by student ID
    public List<Grievance> getGrievancesByStudent(Long studentId) {
        return grievanceRepository.findByStudentId(studentId);
    }

    // Get grievances by status with pagination
    public Page<Grievance> getGrievancesByStatus(Status status, Pageable pageable) {
        return grievanceRepository.findByStatus(status, pageable);
    }

    // Get grievances by priority and category
    public List<Grievance> getGrievancesByPriorityAndCategory(Priority priority, String category) {
        return grievanceRepository.findByPriorityAndCategory(priority, category);
    }

    // Get grievances by category and status
    public List<Grievance> getGrievancesByCategoryAndStatus(String category, Status status) {
        return grievanceRepository.findByCategoryAndStatus(category, status);
    }

    // Get grievances assigned to faculty
    public List<Grievance> getGrievancesByFaculty(Long facultyId) {
        return grievanceRepository.findByAssignedToId(facultyId);
    }

    // Assign grievance to faculty
    public Grievance assignGrievanceToFaculty(Long grievanceId, Long facultyId) {
        Grievance grievance = getGrievanceById(grievanceId);
        User faculty = userRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found with id: " + facultyId));

        grievance.setAssignedTo(faculty);
        grievance.setStatus(Status.INPROGRESS);

        return grievanceRepository.save(grievance);
    }

    // Update grievance status
    public Grievance updateGrievanceStatus(Long grievanceId, Status status) {
        Grievance grievance = getGrievanceById(grievanceId);
        grievance.setStatus(status);
        return grievanceRepository.save(grievance);
    }

    // Resolve grievance
    public Grievance resolveGrievance(Long grievanceId) {
        Grievance grievance = getGrievanceById(grievanceId);
        grievance.setStatus(Status.RESOLVED);
        return grievanceRepository.save(grievance);
    }


    // Search grievances by title (custom implementation)
    public List<Grievance> searchGrievancesByTitle(String title) {
        return grievanceRepository.findAll().stream()
                .filter(g -> g.getTitle().toLowerCase().contains(title.toLowerCase()))
                .collect(Collectors.toList());
    }

    // Get grievance statistics
    public Map<String, Long> getGrievanceStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", grievanceRepository.count());
        stats.put("submitted", grievanceRepository.findByStatus(Status.SUBMITTED, Pageable.unpaged()).getTotalElements());
        stats.put("inProgress", grievanceRepository.findByStatus(Status.INPROGRESS, Pageable.unpaged()).getTotalElements());
        stats.put("resolved", grievanceRepository.findByStatus(Status.RESOLVED, Pageable.unpaged()).getTotalElements());
        return stats;
    }
}
