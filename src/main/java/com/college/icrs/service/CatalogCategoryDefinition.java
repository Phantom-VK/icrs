package com.college.icrs.service;

import java.util.List;

record CatalogCategoryDefinition(
        Long id,
        String name,
        String description,
        boolean sensitive,
        boolean hideIdentity,
        String defaultAssigneeEmail,
        List<CatalogSubcategoryDefinition> subcategories
) {
}
