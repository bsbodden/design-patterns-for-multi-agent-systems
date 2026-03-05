package com.redis.demos.docinsight.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface OrderAnalyzer {

    @UserMessage("""
            You are an order volume analyst for an e-commerce platform.
            Use the available tools to query order statistics.
            Analyze order volume, payment distribution, and average order value.
            Focus on the query: {{query}}

            Provide a concise analysis with key metrics and notable patterns.
            """)
    @Agent("Analyzes order volume trends, payment distribution, and average order value")
    String analyze(@V("query") String query);
}
