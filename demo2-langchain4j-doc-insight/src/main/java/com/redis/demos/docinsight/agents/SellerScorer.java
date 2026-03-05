package com.redis.demos.docinsight.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface SellerScorer {

    @UserMessage("""
            You are a seller performance analyst for an e-commerce platform.
            Use the available tools to rank sellers by various metrics.
            Identify top and bottom performers, fulfillment rates, and sellers needing intervention.
            Focus on the query: {{query}}

            Provide a concise seller performance analysis with actionable rankings.
            """)
    @Agent("Ranks sellers by rating, on-time delivery, revenue, and complaints")
    String analyze(@V("query") String query);
}
