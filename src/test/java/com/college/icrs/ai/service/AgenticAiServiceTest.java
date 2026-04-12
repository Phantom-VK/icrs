package com.college.icrs.ai.service;

import com.college.icrs.ai.agent.GrievanceWorkflowGraph;
import com.college.icrs.config.IcrsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AgenticAiServiceTest {

    @Mock
    private GrievanceWorkflowGraph grievanceWorkflowGraph;

    private IcrsProperties icrsProperties;
    private AgenticAiService agenticAiService;

    @BeforeEach
    void setUp() {
        icrsProperties = new IcrsProperties();
        agenticAiService = new AgenticAiService(icrsProperties, grievanceWorkflowGraph);
    }

    @Test
    void shouldSkipWorkflowWhenAiIsDisabled() {
        icrsProperties.getAi().setEnabled(false);

        agenticAiService.processNewGrievance(42L);

        verifyNoInteractions(grievanceWorkflowGraph);
    }

    @Test
    void shouldProcessWorkflowWhenAiIsEnabled() {
        icrsProperties.getAi().setEnabled(true);

        agenticAiService.processNewGrievance(42L);

        verify(grievanceWorkflowGraph).process(42L);
    }

    @Test
    void shouldSwallowWorkflowExceptions() {
        icrsProperties.getAi().setEnabled(true);
        doThrow(new IllegalStateException("workflow failed"))
                .when(grievanceWorkflowGraph).process(42L);

        assertThatNoException().isThrownBy(() -> agenticAiService.processNewGrievance(42L));
    }
}
