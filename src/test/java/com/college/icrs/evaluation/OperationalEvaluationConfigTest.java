package com.college.icrs.evaluation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OperationalEvaluationConfigTest {

    @AfterEach
    void clearProperties() {
        System.clearProperty("operationalEvaluationVariant");
        System.clearProperty("icrs.ai.rag.enabled");
    }

    @Test
    void shouldDefaultVariantToRagEnabledWhenNoOverrideIsPresent() {
        OperationalEvaluationConfig config = OperationalEvaluationConfig.fromSystemProperties();

        assertThat(config.experimentVariant()).isEqualTo("rag_enabled");
    }

    @Test
    void shouldDeriveVariantFromDisabledRagFlag() {
        System.setProperty("icrs.ai.rag.enabled", "false");

        OperationalEvaluationConfig config = OperationalEvaluationConfig.fromSystemProperties();

        assertThat(config.experimentVariant()).isEqualTo("rag_disabled");
    }

    @Test
    void shouldPreferExplicitVariantOverDerivedRagState() {
        System.setProperty("icrs.ai.rag.enabled", "false");
        System.setProperty("operationalEvaluationVariant", "custom_comparison_leg");

        OperationalEvaluationConfig config = OperationalEvaluationConfig.fromSystemProperties();

        assertThat(config.experimentVariant()).isEqualTo("custom_comparison_leg");
    }
}
