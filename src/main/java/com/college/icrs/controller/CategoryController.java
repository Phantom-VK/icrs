package com.college.icrs.controller;

import com.college.icrs.dto.CategoryResponseDTO;
import com.college.icrs.model.Category;
import com.college.icrs.model.Subcategory;
import com.college.icrs.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<CategoryResponseDTO>> listCategories() {
        List<CategoryResponseDTO> categories = categoryRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(categories);
    }

    private CategoryResponseDTO toDto(Category category) {
        CategoryResponseDTO dto = new CategoryResponseDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        if (category.getDefaultAssignee() != null) {
            dto.setDefaultAssigneeName(category.getDefaultAssignee().getUsername());
        }
        if (category.getSubcategories() != null) {
            dto.setSubcategories(category.getSubcategories().stream().map(this::toDto).toList());
        }
        return dto;
    }

    private CategoryResponseDTO.SubcategoryResponseDTO toDto(Subcategory subcategory) {
        CategoryResponseDTO.SubcategoryResponseDTO dto = new CategoryResponseDTO.SubcategoryResponseDTO();
        dto.setId(subcategory.getId());
        dto.setName(subcategory.getName());
        dto.setDescription(subcategory.getDescription());
        if (subcategory.getDefaultAssignee() != null) {
            dto.setDefaultAssigneeName(subcategory.getDefaultAssignee().getUsername());
        }
        return dto;
    }
}
