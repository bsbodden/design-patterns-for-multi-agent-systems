package com.redis.demos.researchplanner.domain;

import com.embabel.agent.domain.library.HasContent;
import com.fasterxml.jackson.annotation.JsonClassDescription;

@JsonClassDescription("Business impact assessment of an operational issue including revenue impact and affected scope")
public record ImpactAssessment(
        String issueType,
        String severity,
        String revenueImpact,
        String customerSatisfactionImpact,
        String geographicScope,
        String sellerMetrics,
        String affectedOrdersSummary,
        String content
) implements HasContent {
    @Override
    public String getContent() {
        return content;
    }
}
