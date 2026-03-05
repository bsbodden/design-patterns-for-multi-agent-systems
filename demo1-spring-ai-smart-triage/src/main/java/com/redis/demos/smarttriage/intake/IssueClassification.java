package com.redis.demos.smarttriage.intake;

/**
 * Structured output from the LLM issue router.
 * Confidence scores must sum to ~1.0.
 */
public record IssueClassification(
    double deliveryIssue,
    double productQuality,
    double paymentProblem,
    double generalQuestion
) {
    public String topCategory() {
        double max = Math.max(Math.max(deliveryIssue, productQuality),
                              Math.max(paymentProblem, generalQuestion));
        if (max == deliveryIssue) return "delivery_issue";
        if (max == productQuality) return "product_quality";
        if (max == paymentProblem) return "payment_problem";
        return "general_question";
    }

    public boolean isMultiIssue() {
        // No single category dominates — multiple concerns
        double max = Math.max(Math.max(deliveryIssue, productQuality),
                              Math.max(paymentProblem, generalQuestion));
        return max < 0.70;
    }
}
