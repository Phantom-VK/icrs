package com.college.icrs.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CommentResponseDTO {
    private Long id;
    private String body;
    private String authorName;
    private String authorEmail;
    private LocalDateTime createdAt;
}
