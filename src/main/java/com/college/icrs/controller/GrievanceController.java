package com.college.icrs.controller;

import com.college.icrs.ai.service.AgenticAiService;
import com.college.icrs.dto.GrievanceRequestDTO;
import com.college.icrs.dto.GrievanceResponseDTO;
import com.college.icrs.dto.CommentRequestDTO;
import com.college.icrs.dto.CommentResponseDTO;
import com.college.icrs.logging.IcrsLog;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Status;
import com.college.icrs.model.User;
import com.college.icrs.service.GrievanceService;
import com.college.icrs.utils.GrievanceMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@RestController
@RequestMapping("/grievances")
@Validated
@lombok.RequiredArgsConstructor
@Slf4j
public class GrievanceController {

    private final GrievanceService grievanceService;
    private final GrievanceMapper grievanceMapper;
    private final CurrentUserResolver currentUserResolver;
    private final GrievanceRequestAssembler grievanceRequestAssembler;
    private final AgenticAiService agenticAiService;

    /** Create a new grievance (Student submission) */
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<GrievanceResponseDTO> createGrievance(
            @Valid @RequestBody GrievanceRequestDTO grievanceDTO,
            Authentication authentication) {

        User user = currentUserResolver.requireAuthenticatedUser(authentication);
        String email = user.getEmail();
        log.info(IcrsLog.event("grievance.submit.request",
                "studentEmail", email,
                "categoryId", grievanceDTO.getCategoryId(),
                "subcategoryId", grievanceDTO.getSubcategoryId()));
        Grievance grievance = grievanceRequestAssembler.toEntity(grievanceDTO);
        Grievance createdGrievance = grievanceService.createGrievance(grievance, user.getId());
        agenticAiService.processNewGrievanceAsync(createdGrievance.getId());
        log.info(IcrsLog.event("grievance.submit.persisted",
                "grievanceId", createdGrievance.getId(),
                "status", createdGrievance.getStatus(),
                "assignedTo", createdGrievance.getAssignedTo() != null ? createdGrievance.getAssignedTo().getEmail() : null));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(grievanceMapper.toDTO(createdGrievance));
    }

    /** Get all grievances (Faculty/Admin) */
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
        return ResponseEntity.ok(grievances.map(g -> grievanceMapper.toDTO(g, true)));
    }

    /** Get specific grievance by ID */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    public ResponseEntity<GrievanceResponseDTO> getGrievanceById(@PathVariable Long id) {
        Grievance grievance = grievanceService.getGrievanceById(id);
        return ResponseEntity.ok(grievanceMapper.toDTO(grievance, true));
    }

    /** Update grievance (Faculty/Admin) */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    public ResponseEntity<GrievanceResponseDTO> updateGrievance(
            @PathVariable Long id,
            @Valid @RequestBody GrievanceRequestDTO grievanceDTO) {

        Grievance grievance = grievanceRequestAssembler.toEntity(grievanceDTO);
        Grievance updated = grievanceService.updateGrievance(id, grievance);
        return ResponseEntity.ok(grievanceMapper.toDTO(updated));
    }

    /** Delete grievance (Admin only) */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteGrievance(@PathVariable Long id) {
        grievanceService.deleteGrievance(id);
        return ResponseEntity.noContent().build();
    }

    /** Get grievances for a specific student (legacy) */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    public ResponseEntity<List<GrievanceResponseDTO>> getGrievancesByStudent(@PathVariable Long studentId) {
        List<Grievance> grievances = grievanceService.getGrievancesByStudent(studentId);
        return ResponseEntity.ok(toMaskedDtoList(grievances));
    }

    /** Get grievances for the logged-in student (via JWT) */
    @GetMapping("/student/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<GrievanceResponseDTO>> getMyGrievances(Authentication authentication) {
        User user = currentUserResolver.requireAuthenticatedUser(authentication);
        List<Grievance> grievances = grievanceService.getGrievancesByStudent(user.getId());
        return ResponseEntity.ok(grievanceMapper.toDTOList(grievances));
    }

    /** Get grievances filtered by status (Faculty/Admin) */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    public ResponseEntity<Page<GrievanceResponseDTO>> getGrievancesByStatus(
            @PathVariable Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Grievance> grievances = grievanceService.getGrievancesByStatus(status, pageable);
        return ResponseEntity.ok(grievances.map(g -> grievanceMapper.toDTO(g, true)));
    }

    /** Assign grievance to a faculty member */
    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    public ResponseEntity<GrievanceResponseDTO> assignGrievance(
            @PathVariable Long id,
            @RequestParam Long facultyId) {

        Grievance updated = grievanceService.assignGrievanceToFaculty(id, facultyId);
        return ResponseEntity.ok(grievanceMapper.toDTO(updated));
    }

    /** Update grievance status */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    public ResponseEntity<GrievanceResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam Status status) {
        log.info(IcrsLog.event("grievance.status.update.request", "grievanceId", id, "status", status));

        Grievance updated = grievanceService.updateGrievanceStatus(id, status);
        return ResponseEntity.ok(grievanceMapper.toDTO(updated));
    }

    /** Get grievance statistics */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('FACULTY','ADMIN')")
    public ResponseEntity<Map<String, Long>> getStatistics() {
        return ResponseEntity.ok(grievanceService.getGrievanceStatistics());
    }

    /** Add a comment to a grievance */
    @PostMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponseDTO> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequestDTO requestDTO,
            Authentication authentication) {

        String email = currentUserResolver.requireAuthenticatedEmail(authentication);
        log.info(IcrsLog.event("grievance.comment.create.request", "grievanceId", id, "authorEmail", email));
        CommentResponseDTO dto = grievanceService.addComment(id, email, requestDTO.getBody());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /** Get comments for a grievance */
    @GetMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CommentResponseDTO>> getComments(
            @PathVariable Long id,
            Authentication authentication) {

        String email = currentUserResolver.requireAuthenticatedEmail(authentication);
        List<CommentResponseDTO> comments = grievanceService.getComments(id, email);
        return ResponseEntity.ok(comments);
    }

    private List<GrievanceResponseDTO> toMaskedDtoList(List<Grievance> grievances) {
        return grievances.stream()
                .map(grievance -> grievanceMapper.toDTO(grievance, true))
                .toList();
    }
}
