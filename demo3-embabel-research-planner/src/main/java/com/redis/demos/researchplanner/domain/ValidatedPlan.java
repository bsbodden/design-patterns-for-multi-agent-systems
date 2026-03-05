package com.redis.demos.researchplanner.domain;

import com.embabel.agent.domain.library.HasContent;
import com.fasterxml.jackson.annotation.JsonClassDescription;

@JsonClassDescription("Validated intervention plan reviewed against business constraints and original issue")
public record ValidatedPlan(
        boolean approved,
        String planSummary,
        String risks,
        String metricsToTrack,
        String content
) implements HasContent {
    @Override
    public String getContent() {
        return content;
    }
}
