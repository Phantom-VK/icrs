package com.college.icrs.ai.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResolutionDecision implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Boolean autoResolve;
    private String resolutionText;
    private String internalComment;
    private Double confidence;
}
