package com.redis.demos.researchplanner.domain;

import com.embabel.agent.domain.library.HasContent;
import com.fasterxml.jackson.annotation.JsonClassDescription;

import java.util.List;

@JsonClassDescription("Possible intervention actions ranked by cost and effectiveness")
public record InterventionOptions(
        List<Option> options,
        String content
) implements HasContent {

    public record Option(String action, String costLevel, String effectiveness, String timeline) {}

    @Override
    public String getContent() {
        return content;
    }
}
