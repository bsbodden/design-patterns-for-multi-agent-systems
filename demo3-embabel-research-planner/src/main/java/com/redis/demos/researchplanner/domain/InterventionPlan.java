package com.redis.demos.researchplanner.domain;

import com.embabel.agent.domain.library.HasContent;
import com.fasterxml.jackson.annotation.JsonClassDescription;

import java.util.List;

@JsonClassDescription("Chosen intervention actions with sequence and dependencies")
public record InterventionPlan(
        List<String> actions,
        String timeline,
        String expectedOutcome,
        String content
) implements HasContent {
    @Override
    public String getContent() {
        return content;
    }
}
