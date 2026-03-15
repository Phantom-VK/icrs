package com.college.icrs.controller;

import com.college.icrs.dto.GrievanceRequestDTO;
import com.college.icrs.logging.IcrsLog;
import com.college.icrs.model.Category;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Subcategory;
import com.college.icrs.service.CategoryCatalogService;
import com.college.icrs.utils.GrievanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GrievanceRequestAssembler {

    private final GrievanceMapper grievanceMapper;
    private final CategoryCatalogService categoryCatalogService;

    public Grievance toEntity(GrievanceRequestDTO grievanceDTO) {
        Grievance grievance = grievanceMapper.toEntity(grievanceDTO);
        applyCategorySelections(grievanceDTO, grievance);
        return grievance;
    }

    private void applyCategorySelections(GrievanceRequestDTO grievanceDTO, Grievance grievance) {
        if (grievanceDTO.getCategoryId() != null || grievanceDTO.getCategory() != null) {
            Category category = categoryCatalogService.resolveCategory(
                    grievanceDTO.getCategoryId(),
                    grievanceDTO.getCategory()
            );
            grievance.setCategory(category);
            log.debug(IcrsLog.event("grievance.category.resolved",
                    "categoryId", category.getId(),
                    "categoryName", category.getName()));
        }

        if ((grievanceDTO.getSubcategoryId() != null || grievanceDTO.getSubcategory() != null) && grievance.getCategory() != null) {
            Subcategory subcategory = categoryCatalogService.resolveSubcategory(
                    grievance.getCategory(),
                    grievanceDTO.getSubcategoryId(),
                    grievanceDTO.getSubcategory()
            );
            grievance.setSubcategory(subcategory);
            log.debug(IcrsLog.event("grievance.subcategory.resolved",
                    "subcategoryId", subcategory.getId(),
                    "subcategoryName", subcategory.getName()));
        }
    }
}
