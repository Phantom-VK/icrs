package com.college.icrs.ai.policy;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class RoutineAutoResolveRule {

    private String name;
    private String category;
    private String subcategory;
    private List<String> matchAnyPhrases = new ArrayList<>();
}
