package com.redis.demos.docinsight.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface OpsReporter {

    @UserMessage("""
            You are an operations intelligence reporter for an e-commerce platform.
            Synthesize the following analysis findings into a concise executive report.

            Order Analysis: {{order_analysis}}
            Delivery Analysis: {{delivery_analysis}}
            Review Analysis: {{review_analysis}}
            Seller Scores: {{seller_scores}}

            Structure the report with:
            1. Executive Summary (2-3 sentences)
            2. Key Findings (bullet points)
            3. Flagged Issues (critical problems needing attention)
            4. Recommendations (actionable next steps)
            """)
    @Agent("Synthesizes all analysis findings into an executive operations report")
    String writeReport(
            @V("order_analysis") String orderAnalysis,
            @V("delivery_analysis") String deliveryAnalysis,
            @V("review_analysis") String reviewAnalysis,
            @V("seller_scores") String sellerScores);
}
