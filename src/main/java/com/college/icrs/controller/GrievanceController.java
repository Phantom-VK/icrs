package com.college.icrs.controller;

import com.college.icrs.dto.GrievanceRequestDTO;
import com.college.icrs.dto.GrievanceResponseDTO;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Status;
import com.college.icrs.model.User;
import com.college.icrs.repository.UserRepository;
import com.college.icrs.service.GrievanceService;
import com.college.icrs.utils.GrievanceMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/grievances")
@Validated
public class GrievanceController {

    private final GrievanceService grievanceService;
    private final GrievanceMapper grievanceMapper;
    private final UserRepository userRepository;

    public GrievanceController(GrievanceService grievanceService,
                               GrievanceMapper grievanceMapper,
                               UserRepository userRepository) {
        this.grievanceService = grievanceService;
        this.grievanceMapper = grievanceMapper;
        this.userRepository = userRepository;
    }

    /**
     * ✅ Create a new grievance (Student submission)
     */
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GrievanceResponseDTO> createGrievance(
            @Valid @RequestBody GrievanceRequestDTO grievanceDTO,
            Authentication authentication) {

        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }

        String email = authentication.getName();
        System.out.println("📩 Grievance submitted by: " + email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found for email: " + email));

        Grievance grievance = grievanceMapper.toEntity(grievanceDTO);
        Grievance createdGrievance = grievanceService.createGrievance(grievance, user.getId());

        System.out.println("✅ Grievance created with ID: " + createdGrievance.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(grievanceMapper.toDTO(createdGrievance));
    }

    /**
     * ✅ Get all grievances (Faculty/Admin)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    public ResponseEntity<Page<GrievanceResponseDTO>> getAllGrievances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<Grievance> grievances = grievanceService.getAllGrievances(pageable);
        return ResponseEntity.ok(grievances.map(grievanceMapper::toDTO));
    }

    /**
     * ✅ Get specific grievance by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    public ResponseEntity<GrievanceResponseDTO> getGrievanceById(@PathVariable Long id) {
        Grievance grievance = grievanceService.getGrievanceById(id);
        return ResponseEntity.ok(grievanceMapper.toDTO(grievance));
    }

    /**
     * ✅ Update grievance (Faculty/Admin)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    public ResponseEntity<GrievanceResponseDTO> updateGrievance(
            @PathVariable Long id,
            @Valid @RequestBody GrievanceRequestDTO grievanceDTO) {

        Grievance grievance = grievanceMapper.toEntity(grievanceDTO);
        Grievance updated = grievanceService.updateGrievance(id, grievance);
        return ResponseEntity.ok(grievanceMapper.toDTO(updated));
    }

    /**
     * ✅ Delete grievance (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteGrievance(@PathVariable Long id) {
        grievanceService.deleteGrievance(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * ✅ Get grievances for a specific student (legacy)
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    public ResponseEntity<List<GrievanceResponseDTO>> getGrievancesByStudent(@PathVariable Long studentId) {
        List<Grievance> grievances = grievanceService.getGrievancesByStudent(studentId);
        List<GrievanceResponseDTO> dtoList = grievances.stream()
                .map(grievanceMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    /**
     * ✅ Get grievances for the logged-in student (via JWT)
     */
    @GetMapping("/student/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<GrievanceResponseDTO>> getMyGrievances(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        System.out.println("🎓 Fetching grievances for student: " + email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found for email: " + email));

        List<Grievance> grievances = grievanceService.getGrievancesByStudent(user.getId());
        List<GrievanceResponseDTO> dtoList = grievances.stream()
                .map(grievanceMapper::toDTO)
                .collect(Collectors.toList());

        System.out.println("📄 Found " + grievances.size() + " grievances for " + email);
        return ResponseEntity.ok(dtoList);
    }

    /**
     * ✅ Get grievances filtered by status (Faculty/Admin)
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    public ResponseEntity<Page<GrievanceResponseDTO>> getGrievancesByStatus(
            @PathVariable Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Grievance> grievances = grievanceService.getGrievancesByStatus(status, pageable);
        return ResponseEntity.ok(grievances.map(grievanceMapper::toDTO));
    }

    /**
     * ✅ Assign grievance to a faculty member
     */
    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    public ResponseEntity<GrievanceResponseDTO> assignGrievance(
            @PathVariable Long id,
            @RequestParam Long facultyId) {

        Grievance updated = grievanceService.assignGrievanceToFaculty(id, facultyId);
        return ResponseEntity.ok(grievanceMapper.toDTO(updated));
    }

    /**
     * ✅ Update grievance status
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    public ResponseEntity<GrievanceResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam Status status) {

        Grievance updated = grievanceService.updateGrievanceStatus(id, status);
        return ResponseEntity.ok(grievanceMapper.toDTO(updated));
    }

    /**
     * ✅ Get grievance statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    public ResponseEntity<Map<String, Long>> getStatistics() {
        return ResponseEntity.ok(grievanceService.getGrievanceStatistics());
    }
}
