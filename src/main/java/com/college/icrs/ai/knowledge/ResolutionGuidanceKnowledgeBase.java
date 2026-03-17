package com.college.icrs.ai.knowledge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResolutionGuidanceKnowledgeBase {

    private List<ResolutionGuidanceCategory> categories = new ArrayList<>();
}
