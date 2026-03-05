package com.redis.demos.smarttriage.intake;

/**
 * Structured output from the response evaluator (critic).
 * Score 1-4 based on rubric: empathy, specificity, resolution, completeness.
 */
public record ResponseEvaluation(
    int score,
    String feedback
) {
    public boolean passes() {
        return score >= 3;
    }
}
