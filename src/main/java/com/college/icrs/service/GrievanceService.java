package com.college.icrs.service;

import com.college.icrs.model.Grievance;
import com.college.icrs.model.Status;
import com.college.icrs.model.StatusHistory;
import com.college.icrs.model.User;
import com.college.icrs.repository.GrievanceRepository;
import com.college.icrs.repository.StatusHistoryRepository;
import com.college.icrs.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class GrievanceService {

    private final GrievanceRepository grievanceRepository;
    private final UserRepository userRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public GrievanceService(GrievanceRepository grievanceRepository,
                            UserRepository userRepository,
                            StatusHistoryRepository statusHistoryRepository) {
        this.grievanceRepository = grievanceRepository;
        this.userRepository = userRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    public Grievance createGrievance(Grievance grievance, Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        grievance.setStudent(student);
        grievance.setStatus(Status.SUBMITTED);

        // auto-assign to default assignee if present on subcategory or category
        if (grievance.getAssignedTo() == null && grievance.getSubcategory() != null && grievance.getSubcategory().getDefaultAssignee() != null) {
            grievance.setAssignedTo(grievance.getSubcategory().getDefaultAssignee());
            grievance.setStatus(Status.IN_PROGRESS);
        } else if (grievance.getAssignedTo() == null && grievance.getCategory() != null && grievance.getCategory().getDefaultAssignee() != null) {
            grievance.setAssignedTo(grievance.getCategory().getDefaultAssignee());
            grievance.setStatus(Status.IN_PROGRESS);
        }

        Grievance saved = grievanceRepository.save(grievance);
        System.out.println("Grievance created for student ID " + studentId + " | Grievance ID: " + saved.getId());
        return saved;
    }

    public Grievance saveGrievance(Grievance grievance) {
        Grievance saved = grievanceRepository.save(grievance);
        System.out.println("Grievance saved: ID=" + saved.getId());
        return saved;
    }

    public Grievance getGrievanceById(Long id) {
        return grievanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Grievance not found with id: " + id));
    }

    public Grievance updateGrievance(Long id, Grievance grievanceDetails) {
        Grievance grievance = getGrievanceById(id);

        grievance.setTitle(grievanceDetails.getTitle());
        grievance.setDescription(grievanceDetails.getDescription());
        grievance.setCategory(grievanceDetails.getCategory());
        grievance.setSubcategory(grievanceDetails.getSubcategory());
        if (grievanceDetails.getStatus() != null) {
            grievance.setStatus(grievanceDetails.getStatus());
        }

        return grievanceRepository.save(grievance);
    }

    public void deleteGrievance(Long id) {
        Grievance grievance = getGrievanceById(id);
        grievanceRepository.delete(grievance);
        System.out.println("Grievance deleted: ID=" + id);
    }

    public Page<Grievance> getAllGrievances(Pageable pageable) {
        return grievanceRepository.findAll(pageable);
    }

    public List<Grievance> getGrievancesByStudent(Long studentId) {
        return grievanceRepository.findByStudentId(studentId);
    }

    public Page<Grievance> getGrievancesByStatus(Status status, Pageable pageable) {
        return grievanceRepository.findByStatus(status, pageable);
    }

    public List<Grievance> getGrievancesByCategoryAndStatus(Long categoryId, Status status) {
        return grievanceRepository.findByCategoryIdAndStatus(categoryId, status);
    }

    public List<Grievance> getGrievancesByFaculty(Long facultyId) {
        return grievanceRepository.findByAssignedToId(facultyId);
    }

    public Grievance assignGrievanceToFaculty(Long grievanceId, Long facultyId) {
        Grievance grievance = getGrievanceById(grievanceId);
        User faculty = userRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found with id: " + facultyId));

        grievance.setAssignedTo(faculty);
        grievance.setStatus(Status.IN_PROGRESS);
        return grievanceRepository.save(grievance);
    }

    public Grievance updateGrievanceStatus(Long grievanceId, Status status) {
        Grievance grievance = getGrievanceById(grievanceId);
        Status fromStatus = grievance.getStatus();
        grievance.setStatus(status);
        Grievance saved = grievanceRepository.save(grievance);

        StatusHistory history = new StatusHistory();
        history.setGrievance(saved);
        history.setFromStatus(fromStatus);
        history.setToStatus(status);
        statusHistoryRepository.save(history);

        return saved;
    }

    public Grievance resolveGrievance(Long grievanceId) {
        Grievance grievance = getGrievanceById(grievanceId);
        grievance.setStatus(Status.RESOLVED);
        return grievanceRepository.save(grievance);
    }

    public List<Grievance> searchGrievancesByTitle(String title) {
        return grievanceRepository.findAll().stream()
                .filter(g -> g.getTitle() != null &&
                        g.getTitle().toLowerCase().contains(title.toLowerCase()))
                .toList();
    }

    public Map<String, Long> getGrievanceStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", grievanceRepository.count());
        stats.put("submitted", grievanceRepository.findByStatus(Status.SUBMITTED, Pageable.unpaged()).getTotalElements());
        stats.put("inProgress", grievanceRepository.findByStatus(Status.IN_PROGRESS, Pageable.unpaged()).getTotalElements());
        stats.put("resolved", grievanceRepository.findByStatus(Status.RESOLVED, Pageable.unpaged()).getTotalElements());
        return stats;
    }
}
