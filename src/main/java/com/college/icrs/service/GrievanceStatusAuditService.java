package com.college.icrs.service;

import com.college.icrs.model.Grievance;
import com.college.icrs.model.Status;
import com.college.icrs.model.StatusHistory;
import com.college.icrs.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class GrievanceStatusAuditService {

    private final StatusHistoryRepository statusHistoryRepository;

    public void appendStatusHistory(Grievance grievance, Status fromStatus, Status toStatus, String reason) {
        if (fromStatus == toStatus && !StringUtils.hasText(reason)) {
            return;
        }
        StatusHistory history = new StatusHistory();
        history.setGrievance(grievance);
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setReason(reason);
        statusHistoryRepository.save(history);
    }
}
