package com.redis.demos.researchplanner.domain;

import com.embabel.agent.domain.library.HasContent;
import com.fasterxml.jackson.annotation.JsonClassDescription;

@JsonClassDescription("Human-approved intervention plan cleared for execution")
public record ApprovedPlan(
        String planSummary,
        String risks,
        String metricsToTrack,
        String content
) implements HasContent {
    @Override
    public String getContent() {
        return content;
    }

    public static ApprovedPlan from(ValidatedPlan plan) {
        return new ApprovedPlan(plan.planSummary(), plan.risks(), plan.metricsToTrack(), plan.content());
    }
}
