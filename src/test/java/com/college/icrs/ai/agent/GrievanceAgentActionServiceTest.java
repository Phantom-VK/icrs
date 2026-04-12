package com.college.icrs.ai.agent;

import com.college.icrs.ai.policy.AutoResolutionPolicyService;
import com.college.icrs.config.IcrsProperties;
import com.college.icrs.model.Category;
import com.college.icrs.model.Grievance;
import com.college.icrs.model.Status;
import com.college.icrs.model.Subcategory;
import com.college.icrs.service.GrievanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrievanceAgentActionServiceTest {

    @Mock
    private GrievanceService grievanceService;

    @Mock
    private AutoResolutionPolicyService autoResolutionPolicyService;

    private GrievanceAgentActionService grievanceAgentActionService;

    @BeforeEach
    void setUp() {
        IcrsProperties icrsProperties = new IcrsProperties();
        icrsProperties.getAi().setAutoResolveConfidenceThreshold(0.70d);
        grievanceAgentActionService = new GrievanceAgentActionService(
                grievanceService,
                icrsProperties,
                autoResolutionPolicyService
        );
        ReflectionTestUtils.setField(grievanceAgentActionService, "modelName", "deepseek-chat");
    }

    @Test
    void shouldAutoResolveEligibleNonSensitiveGrievance() {
        Grievance grievance = grievance(10L, false, false, "IT Support", "WiFi / Network");
        Grievance resolved = new Grievance();
        resolved.setId(10L);
        resolved.setStatus(Status.RESOLVED);

        when(grievanceService.getGrievanceById(10L)).thenReturn(grievance);
        when(grievanceService.markResolvedByAi(
                eq(10L),
                anyString(),
                anyString(),
                anyDouble(),
                anyString(),
                anyString()
        )).thenReturn(resolved);

        Grievance result = grievanceAgentActionService.finalizeDecision(
                10L,
                "mock-sentiment",
                0.94d,
                true,
                "Please reconnect to campus WiFi.",
                "Known campus WiFi issue.",
                0.90d
        );

        ArgumentCaptor<Double> confidenceCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<String> modelCaptor = ArgumentCaptor.forClass(String.class);

        assertThat(result).isSameAs(resolved);
        verify(grievanceService).markResolvedByAi(
                eq(10L),
                eq("Please reconnect to campus WiFi."),
                eq("Known campus WiFi issue."),
                confidenceCaptor.capture(),
                modelCaptor.capture(),
                eq("DEEPSEEK_AGENTIC_V1")
        );
        verify(grievanceService).addSystemComment(
                10L,
                "ai.system@icrs.local",
                "[AI Auto-Resolution]\nPlease reconnect to campus WiFi."
        );
        verify(grievanceService, never()).updateAiRecommendation(
                anyLong(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
        verifyNoInteractions(autoResolutionPolicyService);

        assertThat(confidenceCaptor.getValue()).isEqualTo(0.92d, within(0.0001d));
        assertThat(modelCaptor.getValue()).isEqualTo("deepseek-chat + sentiment:mock-sentiment");
    }

    @Test
    void shouldRouteSensitiveGrievanceToManualReviewEvenWhenAutoResolveIsRequested() {
        Grievance grievance = grievance(11L, true, true, "Harassment / PoSH", "PoSH Complaint");
        Grievance updated = new Grievance();
        updated.setId(11L);
        updated.setStatus(Status.IN_PROGRESS);

        when(grievanceService.getGrievanceById(11L)).thenReturn(grievance);
        when(grievanceService.updateAiRecommendation(
                eq(11L),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(updated);

        Grievance result = grievanceAgentActionService.finalizeDecision(
                11L,
                "mock-sentiment",
                0.97d,
                true,
                "Auto response from AI",
                "LLM suggested auto resolution",
                0.97d
        );

        ArgumentCaptor<Double> confidenceCaptor = ArgumentCaptor.forClass(Double.class);

        assertThat(result).isSameAs(updated);
        verify(grievanceService).updateAiRecommendation(
                eq(11L),
                eq("Auto response from AI"),
                eq("LLM suggested auto resolution"),
                confidenceCaptor.capture(),
                eq("deepseek-chat + sentiment:mock-sentiment"),
                eq("DEEPSEEK_AGENTIC_V1"),
                any()
        );
        verify(grievanceService, never()).markResolvedByAi(
                anyLong(),
                anyString(),
                anyString(),
                anyDouble(),
                anyString(),
                anyString()
        );
        verify(grievanceService, never()).addSystemComment(anyLong(), anyString(), anyString());
        verifyNoInteractions(autoResolutionPolicyService);

        assertThat(confidenceCaptor.getValue()).isEqualTo(0.97d, within(0.0001d));
    }

    private Grievance grievance(long id, boolean sensitive, boolean hideIdentity, String categoryName, String subcategoryName) {
        Category category = new Category();
        category.setName(categoryName);
        category.setSensitive(sensitive);
        category.setHideIdentity(hideIdentity);

        Subcategory subcategory = new Subcategory();
        subcategory.setName(subcategoryName);
        subcategory.setCategory(category);

        Grievance grievance = new Grievance();
        grievance.setId(id);
        grievance.setTitle("Network issue");
        grievance.setDescription("Campus WiFi disconnects during class.");
        grievance.setCategory(category);
        grievance.setSubcategory(subcategory);
        grievance.setStatus(Status.IN_PROGRESS);
        return grievance;
    }
}
