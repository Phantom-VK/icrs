package com.college.icrs.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class GrievanceRequestDTOValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validatorFactory.close();
    }

    @Test
    void shouldRequireNonBlankTitleAndDescription() {
        GrievanceRequestDTO request = new GrievanceRequestDTO();
        request.setTitle("   ");
        request.setDescription("\t");

        Set<ConstraintViolation<GrievanceRequestDTO>> violations = validator.validate(request);

        Map<String, String> messages = violations.stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));

        assertThat(messages)
                .containsEntry("title", "Title is required")
                .containsEntry("description", "Description is required");
    }
}
