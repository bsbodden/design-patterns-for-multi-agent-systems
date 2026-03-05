package com.redis.demos.researchplanner.domain;

import com.fasterxml.jackson.annotation.JsonClassDescription;

@JsonClassDescription("Classification of an operational issue with type, severity, and extracted entities")
public record IssueClassification(
        String type,
        String severity,
        String sellerId,
        String state,
        String timeRange
) {}
