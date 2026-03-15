package com.college.icrs.service;

import com.college.icrs.dto.CategoryResponseDTO;
import com.college.icrs.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class CategoryCatalogDtoMapper {

    public CategoryResponseDTO toDto(CatalogCategoryDefinition category, Function<String, User> assigneeResolver) {
        CategoryResponseDTO dto = new CategoryResponseDTO();
        dto.setId(category.id());
        dto.setName(category.name());
        dto.setDescription(category.description());
        dto.setSensitive(category.sensitive());
        dto.setHideIdentity(category.hideIdentity());
        dto.setDefaultAssigneeName(assigneeResolver.apply(category.defaultAssigneeEmail()).getDisplayName());
        dto.setSubcategories(category.subcategories().stream()
                .sorted(Comparator.comparing(CatalogSubcategoryDefinition::id))
                .map(subcategory -> toDto(subcategory, assigneeResolver))
                .toList());
        return dto;
    }

    private CategoryResponseDTO.SubcategoryResponseDTO toDto(
            CatalogSubcategoryDefinition subcategory,
            Function<String, User> assigneeResolver
    ) {
        CategoryResponseDTO.SubcategoryResponseDTO dto = new CategoryResponseDTO.SubcategoryResponseDTO();
        dto.setId(subcategory.id());
        dto.setName(subcategory.name());
        dto.setDescription(subcategory.description());
        dto.setDefaultAssigneeName(assigneeResolver.apply(subcategory.defaultAssigneeEmail()).getDisplayName());
        return dto;
    }
}
