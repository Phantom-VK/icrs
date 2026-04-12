package com.college.icrs.service;

import com.college.icrs.dto.CategoryResponseDTO;
import com.college.icrs.exception.InvalidRequestException;
import com.college.icrs.exception.ResourceNotFoundException;
import com.college.icrs.model.Category;
import com.college.icrs.model.Role;
import com.college.icrs.model.Subcategory;
import com.college.icrs.model.User;
import com.college.icrs.repository.CategoryRepository;
import com.college.icrs.repository.SubcategoryRepository;
import com.college.icrs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryCatalogServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SubcategoryRepository subcategoryRepository;

    @Mock
    private UserRepository userRepository;

    private CategoryCatalogService categoryCatalogService;

    @BeforeEach
    void setUp() {
        categoryCatalogService = new CategoryCatalogService(
                categoryRepository,
                subcategoryRepository,
                userRepository,
                new CategoryCatalogDefinitions(),
                new CategoryCatalogDtoMapper()
        );

        lenient().when(userRepository.findByEmail(anyString()))
                .thenAnswer(invocation -> Optional.of(assignee(invocation.getArgument(0))));
        lenient().when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(subcategoryRepository.save(any(Subcategory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldExposeCategoriesUsedByProjectTestPlan() {
        List<CategoryResponseDTO> categories = categoryCatalogService.listCatalog();

        assertThat(categories)
                .extracting(CategoryResponseDTO::getName)
                .containsExactly(
                        "Academic",
                        "Administrative",
                        "IT Support",
                        "Hostel & Accommodation",
                        "Finance & Scholarships",
                        "Discipline & Safety",
                        "Examinations",
                        "Harassment / PoSH"
                );

        assertThat(categories.get(7).isSensitive()).isTrue();
        assertThat(categories.get(7).isHideIdentity()).isTrue();
        assertThat(categories.get(2).getSubcategories())
                .extracting(CategoryResponseDTO.SubcategoryResponseDTO::getName)
                .containsExactly("WiFi / Network", "LMS / Email", "Lab Machines");
    }

    @Test
    void shouldResolveKnownCategoryAndSubcategoryNames() {
        when(categoryRepository.findByNameIgnoreCase("IT Support")).thenReturn(Optional.empty());
        when(subcategoryRepository.findByNameIgnoreCaseAndCategoryId("WiFi / Network", 3L)).thenReturn(Optional.empty());

        Category category = categoryCatalogService.resolveCategory(3L, null);
        category.setId(3L);

        Subcategory subcategory = categoryCatalogService.resolveSubcategory(category, null, "WiFi / Network");

        assertThat(category.getName()).isEqualTo("IT Support");
        assertThat(category.getDefaultAssignee().getEmail()).isEqualTo("it.support@college.edu");
        assertThat(subcategory.getName()).isEqualTo("WiFi / Network");
        assertThat(subcategory.getDefaultAssignee().getEmail()).isEqualTo("it.support@college.edu");
    }

    @Test
    void shouldRejectUnknownOrIncompleteCatalogSelections() {
        Category category = new Category();
        category.setId(3L);
        category.setName("IT Support");

        assertThatThrownBy(() -> categoryCatalogService.resolveCategory(null, "Unknown Category"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found.");

        assertThatThrownBy(() -> categoryCatalogService.resolveSubcategory(null, 301L, null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Category is required to resolve subcategory.");

        assertThatThrownBy(() -> categoryCatalogService.resolveSubcategory(category, null, "Unknown Subcategory"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Subcategory not found.");
    }

    private User assignee(String email) {
        return User.builder()
                .id(1L)
                .username(email.substring(0, email.indexOf('@')).replace('.', ' '))
                .email(email)
                .password("secret")
                .role(Role.FACULTY)
                .enabled(true)
                .build();
    }
}
