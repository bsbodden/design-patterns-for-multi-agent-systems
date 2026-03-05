package com.redis.demos.researchplanner.domain;

import com.embabel.agent.domain.library.HasContent;
import com.fasterxml.jackson.annotation.JsonClassDescription;

@JsonClassDescription("Executive escalation report for management review, written in Portuguese")
public record EscalationReport(
        String executiveSummary,
        String recommendedActions,
        String timeline,
        String riskAssessment,
        String content
) implements HasContent {
    @Override
    public String getContent() {
        return content;
    }
}
