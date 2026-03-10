package com.college.icrs.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "grievances")
@Getter
@Setter
@NoArgsConstructor
public class Grievance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    @NonNull
    private String title;

    @NonNull
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private Subcategory subcategory;

    @Column(name = "registration_number")
    private String registrationNumber;

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private Sentiment sentiment;

    @Column(name = "ai_resolved", nullable = false)
    private boolean aiResolved = false;

    @Column(name = "ai_confidence")
    private Double aiConfidence;

    @Column(name = "ai_title")
    private String aiTitle;

    @Column(name = "ai_resolution_text", columnDefinition = "TEXT")
    private String aiResolutionText;

    @Column(name = "ai_resolution_comment", columnDefinition = "TEXT")
    private String aiResolutionComment;

    @Column(name = "ai_model_name")
    private String aiModelName;

    @Column(name = "ai_decision_at")
    private LocalDateTime aiDecisionAt;

    @Column(name = "ai_decision_source")
    private String aiDecisionSource;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "grievance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "grievance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "grievance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StatusHistory> statusHistory = new ArrayList<>();

}
