package com.college.icrs.dto;

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
    private Long categoryId;
    private String categoryName;
    private String category;
    private Long subcategoryId;
    private String subcategoryName;
    private String subcategory;
    private String registrationNumber;
    private Status status;
    private String studentName;
    private String assignedToName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
