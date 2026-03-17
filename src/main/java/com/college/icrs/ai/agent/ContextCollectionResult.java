package com.college.icrs.ai.agent;

public record ContextCollectionResult(
        String policyContext,
        boolean policyFetched,
        String commentContext,
        boolean commentFetched,
        String statusHistoryContext,
        boolean statusHistoryFetched,
        String resolutionGuidanceContext,
        boolean resolutionGuidanceFetched,
        String plannerTrace,
        String routeTrace
) {

    public static ContextCollectionResult empty() {
        return new ContextCollectionResult("", false, "", false, "", false, "", false, "", "");
    }
}
