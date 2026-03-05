package com.redis.demos.smarttriage.intake;

import java.util.List;

/**
 * Full result from the customer intake pipeline.
 */
public record PipelineResult(
    IssueClassification classification,
    String selectedExpert,
    List<String> toolCallsSummary,
    String finalResponse,
    int criticIterations,
    int finalScore
) {}
