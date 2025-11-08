package com.college.icrs.controller;


import com.college.icrs.dto.GrievanceRequestDTO;
import com.college.icrs.dto.GrievanceResponseDTO;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Status;
import com.college.icrs.model.User;
import com.college.icrs.repository.UserRepository;
import com.college.icrs.service.GrievanceService;
import com.college.icrs.utils.GrievanceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/grievances")
@Validated
public class GrievanceController {

    private final GrievanceService grievanceService;
    private final GrievanceMapper grievanceMapper;
    private final UserRepository userRepository;

    public GrievanceController(GrievanceService grievanceService, GrievanceMapper grievanceMapper, UserRepository userRepository) {
        this.grievanceService = grievanceService;
        this.grievanceMapper = grievanceMapper;
        this.userRepository= userRepository;
    }

    // CREATE - POST
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<GrievanceResponseDTO> createGrievance(
            @Valid @RequestBody GrievanceRequestDTO grievanceDTO,
            Authentication authentication) {

        // Get user from JWT token
//        String username = authentication.getName();

        String username = "Vikramaditya Ganesh Khupse";
        Grievance grievance = grievanceMapper.toEntity(grievanceDTO);


        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: "+ username));

        Grievance createdGrievance = grievanceService.createGrievance(grievance, user.getId());

        GrievanceResponseDTO responseDTO = grievanceMapper.toDTO(createdGrievance);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    // READ ALL - GET (with pagination)
    @GetMapping
    public ResponseEntity<Page<GrievanceResponseDTO>> getAllGrievances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<Grievance> grievances = grievanceService.getAllGrievances(pageable);
        Page<GrievanceResponseDTO> responseDTOs = grievances.map(grievanceMapper::toDTO);

        return ResponseEntity.ok(responseDTOs);
    }

    // READ ONE - GET
    @GetMapping("/{id}")
    public ResponseEntity<GrievanceResponseDTO> getGrievanceById(@PathVariable Long id) {
        Grievance grievance = grievanceService.getGrievanceById(id);
        GrievanceResponseDTO responseDTO = grievanceMapper.toDTO(grievance);
        return ResponseEntity.ok(responseDTO);
    }

    // UPDATE - PUT
    @PutMapping("/{id}")
    public ResponseEntity<GrievanceResponseDTO> updateGrievance(
            @PathVariable Long id,
            @Valid @RequestBody GrievanceRequestDTO grievanceDTO) {

        Grievance grievanceDetails = grievanceMapper.toEntity(grievanceDTO);
        Grievance updatedGrievance = grievanceService.updateGrievance(id, grievanceDetails);
        GrievanceResponseDTO responseDTO = grievanceMapper.toDTO(updatedGrievance);

        return ResponseEntity.ok(responseDTO);
    }

    // DELETE - DELETE
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteGrievance(@PathVariable Long id) {
        grievanceService.deleteGrievance(id);
        return ResponseEntity.noContent().build();
    }

    // CUSTOM ENDPOINTS

    // Get grievances by student
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<GrievanceResponseDTO>> getGrievancesByStudent(
            @PathVariable Long studentId) {
        List<Grievance> grievances = grievanceService.getGrievancesByStudent(studentId);
        List<GrievanceResponseDTO> responseDTOs = grievances.stream()
                .map(grievanceMapper::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    // Get grievances by status
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<GrievanceResponseDTO>> getGrievancesByStatus(
            @PathVariable Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Grievance> grievances = grievanceService.getGrievancesByStatus(status, pageable);
        Page<GrievanceResponseDTO> responseDTOs = grievances.map(grievanceMapper::toDTO);

        return ResponseEntity.ok(responseDTOs);
    }

    // Assign grievance to faculty
    @PatchMapping("/{id}/assign")
    public ResponseEntity<GrievanceResponseDTO> assignGrievance(
            @PathVariable Long id,
            @RequestParam Long facultyId) {

        Grievance assignedGrievance = grievanceService.assignGrievanceToFaculty(id, facultyId);
        GrievanceResponseDTO responseDTO = grievanceMapper.toDTO(assignedGrievance);

        return ResponseEntity.ok(responseDTO);
    }

    // Update status
    @PatchMapping("/{id}/status")
    public ResponseEntity<GrievanceResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestParam Status status) {

        Grievance updatedGrievance = grievanceService.updateGrievanceStatus(id, status);
        GrievanceResponseDTO responseDTO = grievanceMapper.toDTO(updatedGrievance);

        return ResponseEntity.ok(responseDTO);
    }

    // Get statistics
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getStatistics() {
        Map<String, Long> stats = grievanceService.getGrievanceStatistics();
        return ResponseEntity.ok(stats);
    }
}
