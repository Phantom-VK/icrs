package com.college.icrs.service;

import com.college.icrs.dto.CategoryResponseDTO;
import com.college.icrs.exception.InvalidRequestException;
import com.college.icrs.exception.ResourceNotFoundException;
import com.college.icrs.logging.IcrsLog;
import com.college.icrs.model.Category;
import com.college.icrs.model.Subcategory;
import com.college.icrs.model.User;
import com.college.icrs.repository.CategoryRepository;
import com.college.icrs.repository.SubcategoryRepository;
import com.college.icrs.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CategoryCatalogService {

    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final UserRepository userRepository;
    private final CategoryCatalogDefinitions catalogDefinitions;
    private final CategoryCatalogDtoMapper categoryCatalogDtoMapper;

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<CategoryResponseDTO> listCatalog() {
        return catalogDefinitions.all().stream()
                .sorted(Comparator.comparing(CatalogCategoryDefinition::id))
                .map(category -> categoryCatalogDtoMapper.toDto(category, this::findUserByEmail))
                .toList();
    }

    public Category resolveCategory(Long categoryId, String categoryName) {
        log.debug(IcrsLog.event("catalog.category.resolve.start", "categoryId", categoryId, "categoryName", categoryName));
        CatalogCategoryDefinition catalogCategory = catalogDefinitions.findCategory(categoryId, categoryName)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));

        Category category = categoryRepository.findByNameIgnoreCase(catalogCategory.name()).orElseGet(Category::new);
        category.setName(catalogCategory.name());
        category.setDescription(catalogCategory.description());
        category.setSensitive(catalogCategory.sensitive());
        category.setHideIdentity(catalogCategory.hideIdentity());
        category.setDefaultAssignee(findUserByEmail(catalogCategory.defaultAssigneeEmail()));
        Category saved = categoryRepository.save(category);
        log.debug(IcrsLog.event("catalog.category.resolve.completed", "categoryId", saved.getId(), "categoryName", saved.getName()));
        return saved;
    }

    public Subcategory resolveSubcategory(Category category, Long subcategoryId, String subcategoryName) {
        log.debug(IcrsLog.event("catalog.subcategory.resolve.start",
                "categoryId", category != null ? category.getId() : null,
                "subcategoryId", subcategoryId,
                "subcategoryName", subcategoryName));
        if (category == null) {
            throw new InvalidRequestException("Category is required to resolve subcategory.");
        }

        CatalogCategoryDefinition catalogCategory = catalogDefinitions.findCategory(null, category.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found."));

        CatalogSubcategoryDefinition catalogSubcategory = catalogDefinitions.findSubcategory(catalogCategory, subcategoryId, subcategoryName)
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found."));

        Subcategory subcategory = subcategoryRepository
                .findByNameIgnoreCaseAndCategoryId(catalogSubcategory.name(), category.getId())
                .orElseGet(Subcategory::new);
        subcategory.setName(catalogSubcategory.name());
        subcategory.setDescription(catalogSubcategory.description());
        subcategory.setCategory(category);
        subcategory.setDefaultAssignee(findUserByEmail(catalogSubcategory.defaultAssigneeEmail()));
        Subcategory saved = subcategoryRepository.save(subcategory);
        log.debug(IcrsLog.event("catalog.subcategory.resolve.completed",
                "subcategoryId", saved.getId(),
                "subcategoryName", saved.getName(),
                "categoryId", category.getId()));
        return saved;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Default assignee not found for email: " + email));
    }
}
