package com.redis.demos.researchplanner.domain;

import com.embabel.agent.domain.library.HasContent;
import com.fasterxml.jackson.annotation.JsonClassDescription;

@JsonClassDescription("An operational issue reported in the e-commerce platform")
public record OperationalIssue(String description, String sellerId, String state) implements HasContent {
    @Override
    public String getContent() {
        return description;
    }
}
