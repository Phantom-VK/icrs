package com.college.icrs.ai.policy;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AutoResolutionPolicyKnowledgeBase {

    private List<RoutineAutoResolveRule> routineRules = new ArrayList<>();
}
