package com.college.icrs.utils;

import com.college.icrs.dto.GrievanceRequestDTO;
import com.college.icrs.dto.GrievanceResponseDTO;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.StatusHistory;
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
        grievance.setRegistrationNumber(dto.getRegistrationNumber());
        return grievance;
    }

    public GrievanceResponseDTO toDTO(Grievance grievance) {
        return toDTO(grievance, false);
    }

    public GrievanceResponseDTO toDTO(Grievance grievance, boolean maskIdentity) {
        if (grievance == null) {
            return null;
        }

        GrievanceResponseDTO dto = new GrievanceResponseDTO();
        dto.setId(grievance.getId());
        dto.setTitle(grievance.getTitle());
        dto.setDescription(grievance.getDescription());
        if (grievance.getCategory() != null) {
            dto.setCategoryId(grievance.getCategory().getId());
            dto.setCategoryName(grievance.getCategory().getName());
            dto.setCategory(grievance.getCategory().getName());
            dto.setSensitiveCategory(Boolean.TRUE.equals(grievance.getCategory().getSensitive()));
        }
        if (grievance.getSubcategory() != null) {
            dto.setSubcategoryId(grievance.getSubcategory().getId());
            dto.setSubcategoryName(grievance.getSubcategory().getName());
            dto.setSubcategory(grievance.getSubcategory().getName());
        }
        dto.setStatus(grievance.getStatus());
        boolean hideIdentity = maskIdentity && grievance.getCategory() != null && Boolean.TRUE.equals(grievance.getCategory().getHideIdentity());
        dto.setIdentityHidden(hideIdentity);

        if (hideIdentity) {
            dto.setRegistrationNumber(null);
            dto.setMaskedRegistrationNumber("Hidden for compliance");
        } else {
            dto.setRegistrationNumber(grievance.getRegistrationNumber());
            dto.setMaskedRegistrationNumber(grievance.getRegistrationNumber());
        }
        // Map user names
        if (grievance.getStudent() != null && !hideIdentity) {
            dto.setStudentName(grievance.getStudent().getUsername());
        }
        if (grievance.getAssignedTo() != null) {
            dto.setAssignedToName(grievance.getAssignedTo().getUsername());
        }

        dto.setCreatedAt(grievance.getCreatedAt());
        dto.setUpdatedAt(grievance.getUpdatedAt());
        if (grievance.getStatusHistory() != null) {
            List<GrievanceResponseDTO.StatusHistoryItem> history = grievance.getStatusHistory().stream()
                    .map(this::mapStatusHistory)
                    .toList();
            dto.setStatusHistory(history);
        }

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
    }

    private GrievanceResponseDTO.StatusHistoryItem mapStatusHistory(StatusHistory history) {
        GrievanceResponseDTO.StatusHistoryItem item = new GrievanceResponseDTO.StatusHistoryItem();
        item.setFromStatus(history.getFromStatus() != null ? history.getFromStatus().name() : null);
        item.setToStatus(history.getToStatus() != null ? history.getToStatus().name() : null);
        item.setChangedAt(history.getChangedAt());
        if (history.getActor() != null) {
            item.setActorName(history.getActor().getUsername());
        }
        item.setReason(history.getReason());
        return item;
    }
}
