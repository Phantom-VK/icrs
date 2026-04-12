package com.college.icrs.evaluation;

import com.college.icrs.dto.CategoryResponseDTO;
import com.college.icrs.service.CategoryCatalogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationalEvaluationDatasetValidatorTest {

    @Mock
    private CategoryCatalogService categoryCatalogService;

    @TempDir
    Path tempDir;

    private OperationalEvaluationDatasetValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new OperationalEvaluationDatasetValidator(objectMapper, categoryCatalogService);
        when(categoryCatalogService.listCatalog()).thenReturn(catalog());
    }

    @Test
    void shouldValidateAlignedOperationalDatasets() throws Exception {
        Path historicalFile = tempDir.resolve("historical.json");
        Path liveFile = tempDir.resolve("live.json");

        objectMapper.writeValue(historicalFile.toFile(), List.of(
                new HistoricalInput("hist-1", "Resolved WiFi issue", "Hostel WiFi was restored after router reset.", "IT Support", "WiFi / Network", "2022BIT001"),
                new HistoricalInput("hist-2", "Fee receipt delay", "Receipt was generated after finance desk reconciliation.", "Finance & Scholarships", "Fee Payment", "2022BIT002")
        ));
        objectMapper.writeValue(liveFile.toFile(), List.of(
                new LiveInput("live-1", "WiFi disconnects in hostel", "Connection drops every 15 minutes.", "IT Support", "WiFi / Network", "2022BIT003"),
                new LiveInput("live-2", "Exam schedule clash", "Two exams overlap in the timetable.", "Examinations", "Exam Schedule", "2022BIT004")
        ));

        OperationalEvaluationDatasets datasets = validator.validate(historicalFile, liveFile);

        assertThat(datasets.historicalCases()).hasSize(2);
        assertThat(datasets.liveCases()).hasSize(2);
        assertThat(datasets.liveCases().getFirst().caseId()).isEqualTo("live-1");
        assertThat(datasets.liveCases().getFirst().sensitiveCategory()).isFalse();
    }

    @Test
    void shouldRejectUnknownCategoryAndSubcategorySelections() throws Exception {
        Path historicalFile = tempDir.resolve("historical.json");
        Path liveFile = tempDir.resolve("live.json");

        objectMapper.writeValue(historicalFile.toFile(), List.of(
                new HistoricalInput("hist-1", "Old case", "Description", "Unknown Category", "WiFi / Network", "2022BIT001")
        ));
        objectMapper.writeValue(liveFile.toFile(), List.of(
                new LiveInput("live-1", "New case", "Description", "IT Support", "Unknown Subcategory", "2022BIT002")
        ));

        assertThatThrownBy(() -> validator.validate(historicalFile, liveFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown category: Unknown Category")
                .hasMessageContaining("unknown subcategory 'Unknown Subcategory' for category 'IT Support'");
    }

    @Test
    void shouldRejectDuplicateStableCaseIds() throws Exception {
        Path historicalFile = tempDir.resolve("historical.json");
        Path liveFile = tempDir.resolve("live.json");

        objectMapper.writeValue(historicalFile.toFile(), List.of(
                new HistoricalInput("hist-1", "Old case", "Description", "IT Support", "WiFi / Network", "2022BIT001")
        ));
        objectMapper.writeValue(liveFile.toFile(), List.of(
                new LiveInput("dup-1", "Case one", "Description", "IT Support", "WiFi / Network", "2022BIT002"),
                new LiveInput("dup-1", "Case two", "Description", "Examinations", "Exam Schedule", "2022BIT003")
        ));

        assertThatThrownBy(() -> validator.validate(historicalFile, liveFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate case id: dup-1");
    }

    @Test
    void shouldRejectBlankTitleDescriptionAndRegistrationNumber() throws Exception {
        Path historicalFile = tempDir.resolve("historical.json");
        Path liveFile = tempDir.resolve("live.json");

        objectMapper.writeValue(historicalFile.toFile(), List.of(
                new HistoricalInput("hist-1", " ", "\t", "IT Support", "WiFi / Network", "")
        ));
        objectMapper.writeValue(liveFile.toFile(), List.of(
                new LiveInput("live-1", "Valid title", "Valid description", "IT Support", "WiFi / Network", "2022BIT009")
        ));

        assertThatThrownBy(() -> validator.validate(historicalFile, liveFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title must not be blank")
                .hasMessageContaining("description must not be blank")
                .hasMessageContaining("registrationNumber must not be blank");
    }

    private List<CategoryResponseDTO> catalog() {
        return List.of(
                category("IT Support", false, subcategory("WiFi / Network"), subcategory("LMS / Email")),
                category("Finance & Scholarships", false, subcategory("Fee Payment"), subcategory("Scholarship")),
                category("Examinations", false, subcategory("Exam Schedule"), subcategory("Hall Ticket")),
                category("Harassment / PoSH", true, subcategory("PoSH Complaint"), subcategory("Ragging"))
        );
    }

    private CategoryResponseDTO category(String name, boolean sensitive, CategoryResponseDTO.SubcategoryResponseDTO... subcategories) {
        CategoryResponseDTO category = new CategoryResponseDTO();
        category.setName(name);
        category.setSensitive(sensitive);
        category.setSubcategories(List.of(subcategories));
        return category;
    }

    private CategoryResponseDTO.SubcategoryResponseDTO subcategory(String name) {
        CategoryResponseDTO.SubcategoryResponseDTO dto = new CategoryResponseDTO.SubcategoryResponseDTO();
        dto.setName(name);
        return dto;
    }

    private record HistoricalInput(
            String documentId,
            String title,
            String description,
            String category,
            String subcategory,
            String registrationNumber
    ) {
    }

    private record LiveInput(
            String caseId,
            String title,
            String description,
            String category,
            String subcategory,
            String registrationNumber
    ) {
    }
}
