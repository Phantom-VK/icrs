package com.college.icrs.service;

import com.college.icrs.model.Grievance;
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

@Service
@Transactional
public class GrievanceService {

    private final GrievanceRepository grievanceRepository;
    private final UserRepository userRepository;

    public GrievanceService(GrievanceRepository grievanceRepository, UserRepository userRepository) {
        this.grievanceRepository = grievanceRepository;
        this.userRepository = userRepository;
    }

    // ✅ Create and link grievance directly to student
    public Grievance createGrievance(Grievance grievance, Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + studentId));

        grievance.setStudent(student);
        grievance.setStatus(Status.SUBMITTED);
       

        Grievance saved = grievanceRepository.save(grievance);
        System.out.println("✅ Grievance created for student ID " + studentId + " | Grievance ID: " + saved.getId());
        return saved;
    }

    // ✅ Simple save (used internally for direct entity persistence)
    public Grievance saveGrievance(Grievance grievance) {
        Grievance saved = grievanceRepository.save(grievance);
        System.out.println("📝 Grievance saved: ID=" + saved.getId());
        return saved;
    }

    // ✅ Retrieve single grievance
    public Grievance getGrievanceById(Long id) {
        return grievanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Grievance not found with id: " + id));
    }

    // ✅ Update grievance details
    public Grievance updateGrievance(Long id, Grievance grievanceDetails) {
        Grievance grievance = getGrievanceById(id);

        grievance.setTitle(grievanceDetails.getTitle());
        grievance.setDescription(grievanceDetails.getDescription());
        grievance.setCategory(grievanceDetails.getCategory());
        grievance.setSubcategory(grievanceDetails.getSubcategory());
        grievance.setStatus(grievanceDetails.getStatus());

        return grievanceRepository.save(grievance);
    }

    // ✅ Delete grievance
    public void deleteGrievance(Long id) {
        Grievance grievance = getGrievanceById(id);
        grievanceRepository.delete(grievance);
        System.out.println("🗑️ Grievance deleted: ID=" + id);
    }

    // ✅ Retrieve all grievances (for faculty/admin)
    public Page<Grievance> getAllGrievances(Pageable pageable) {
        return grievanceRepository.findAll(pageable);
    }

    // ✅ Retrieve grievances by student
    public List<Grievance> getGrievancesByStudent(Long studentId) {
        return grievanceRepository.findByStudentId(studentId);
    }

    // ✅ Retrieve grievances filtered by status
    public Page<Grievance> getGrievancesByStatus(Status status, Pageable pageable) {
        return grievanceRepository.findByStatus(status, pageable);
    }

    // ✅ Retrieve grievances filtered by category + status
    public List<Grievance> getGrievancesByCategoryAndStatus(String category, Status status) {
        return grievanceRepository.findByCategoryAndStatus(category, status);
    }

    // ✅ Retrieve grievances assigned to a faculty member
    public List<Grievance> getGrievancesByFaculty(Long facultyId) {
        return grievanceRepository.findByAssignedToId(facultyId);
    }

    // ✅ Assign grievance to faculty (moves to IN_PROGRESS)
    public Grievance assignGrievanceToFaculty(Long grievanceId, Long facultyId) {
        Grievance grievance = getGrievanceById(grievanceId);
        User faculty = userRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found with id: " + facultyId));

        grievance.setAssignedTo(faculty);
        grievance.setStatus(Status.IN_PROGRESS);
        return grievanceRepository.save(grievance);
    }

    // ✅ Update grievance status (Faculty/Admin use)
    public Grievance updateGrievanceStatus(Long grievanceId, Status status) {
        Grievance grievance = getGrievanceById(grievanceId);
        grievance.setStatus(status);
        return grievanceRepository.save(grievance);
    }

    // ✅ Resolve grievance directly
    public Grievance resolveGrievance(Long grievanceId) {
        Grievance grievance = getGrievanceById(grievanceId);
        grievance.setStatus(Status.RESOLVED);
        return grievanceRepository.save(grievance);
    }

    // ✅ Search grievances by title (basic, case-insensitive)
    public List<Grievance> searchGrievancesByTitle(String title) {
        return grievanceRepository.findAll().stream()
                .filter(g -> g.getTitle() != null &&
                        g.getTitle().toLowerCase().contains(title.toLowerCase()))
                .toList();
    }

    // ✅ Generate grievance statistics summary
    public Map<String, Long> getGrievanceStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", grievanceRepository.count());
        stats.put("submitted", grievanceRepository.findByStatus(Status.SUBMITTED, Pageable.unpaged()).getTotalElements());
        stats.put("inProgress", grievanceRepository.findByStatus(Status.IN_PROGRESS, Pageable.unpaged()).getTotalElements());
        stats.put("resolved", grievanceRepository.findByStatus(Status.RESOLVED, Pageable.unpaged()).getTotalElements());
        return stats;
    }
}
