package com.college.icrs.ai.service;

import com.college.icrs.ai.agent.GrievanceWorkflowGraph;
import com.college.icrs.config.IcrsProperties;
import com.college.icrs.logging.IcrsLog;
import com.college.icrs.model.Grievance;
import com.college.icrs.service.GrievanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@lombok.RequiredArgsConstructor
public class AgenticAiService {

    private final IcrsProperties icrsProperties;
    private final GrievanceWorkflowGraph grievanceWorkflowGraph;

    @Async("aiTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processNewGrievanceAsync(Long grievanceId) {
        log.info(IcrsLog.event("ai.workflow.async-dispatched", "grievanceId", grievanceId));
        processNewGrievance(grievanceId);
    }

    public void processNewGrievance(Long grievanceId) {
        log.info(IcrsLog.event("ai.workflow.start", "grievanceId", grievanceId));
        if (!icrsProperties.getAi().isEnabled()) {
            log.info(IcrsLog.event("ai.workflow.skipped", "grievanceId", grievanceId, "reason", "ai-disabled"));
            return;
        }

        try {
            grievanceWorkflowGraph.process(grievanceId);
        } catch (Exception e) {
            log.error(IcrsLog.event("ai.workflow.failed", "grievanceId", grievanceId, "reason", e.getClass().getSimpleName()), e);
        }
    }
}
