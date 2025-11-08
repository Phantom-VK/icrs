package com.college.icrs.dto;

import com.college.icrs.model.Priority;
import com.college.icrs.model.Status;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class GrievanceResponseDTO {

    private long id;
    private String title;
    private  String description;
    private  String category;
    private  String subcategory;
    private Priority priority;
    private Status status;
    private String studentName;
    private String assignedToName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
