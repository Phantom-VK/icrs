package com.college.icrs.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CategoryResponseDTO {
    private Long id;
    private String name;
    private String description;
    private String defaultAssigneeName;
    private boolean sensitive;
    private boolean hideIdentity;
    private List<SubcategoryResponseDTO> subcategories;

    @Getter
    @Setter
    public static class SubcategoryResponseDTO {
        private Long id;
        private String name;
        private String description;
        private String defaultAssigneeName;
    }
}
