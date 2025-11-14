package com.college.icrs.utils;

import com.college.icrs.dto.GrievanceRequestDTO;
import com.college.icrs.dto.GrievanceResponseDTO;
import com.college.icrs.model.Grievance;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GrievanceMapper {

    public Grievance toEntity(GrievanceRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        Grievance grievance = new Grievance();
        grievance.setTitle(dto.getTitle());
        grievance.setDescription(dto.getDescription());
        grievance.setCategory(dto.getCategory());
        grievance.setSubcategory(dto.getSubcategory());
        grievance.setRegistrationNumber(dto.getRegistrationNumber());
        return grievance;
    }

    public GrievanceResponseDTO toDTO(Grievance grievance) {
        if (grievance == null) {
            return null;
        }

        GrievanceResponseDTO dto = new GrievanceResponseDTO();
        dto.setId(grievance.getId());
        dto.setTitle(grievance.getTitle());
        dto.setDescription(grievance.getDescription());
        dto.setCategory(grievance.getCategory());
        dto.setSubcategory(grievance.getSubcategory());
        dto.setStatus(grievance.getStatus());
        dto.setRegistrationNumber(grievance.getRegistrationNumber());
        // Map user names
        if (grievance.getStudent() != null) {
            dto.setStudentName(grievance.getStudent().getUsername());
        }
        if (grievance.getAssignedTo() != null) {
            dto.setAssignedToName(grievance.getAssignedTo().getUsername());
        }

        dto.setCreatedAt(grievance.getCreatedAt());
        dto.setUpdatedAt(grievance.getUpdatedAt());

        return dto;
    }

    public List<GrievanceResponseDTO> toDTOList(List<Grievance> grievances) {
        return grievances.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Page<GrievanceResponseDTO> toDTOPage(Page<Grievance> grievances) {
        return grievances.map(this::toDTO);
    }

    public void updateEntityFromDTO(GrievanceRequestDTO dto, Grievance grievance) {
        if (dto == null || grievance == null) {
            return;
        }

        grievance.setTitle(dto.getTitle());
        grievance.setDescription(dto.getDescription());
        grievance.setCategory(dto.getCategory());
        grievance.setSubcategory(dto.getSubcategory());
    }
}