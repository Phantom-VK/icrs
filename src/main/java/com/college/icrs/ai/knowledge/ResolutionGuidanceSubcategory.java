package com.college.icrs.ai.knowledge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResolutionGuidanceSubcategory {

    private String subcategory;
    private ResolutionGuidanceEntry guidance;
}
