package com.redis.demos.docinsight.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ReviewAnalyzer {

    @UserMessage("""
            You are a customer review analyst for an e-commerce platform.
            Use the available tools to query review breakdowns.
            Analyze review score distribution, complaint themes, and identify categories with issues.
            Focus on the query: {{query}}

            Provide a concise analysis of customer sentiment and key complaint patterns.
            """)
    @Agent("Analyzes sentiment distribution, complaint themes, and review score trends")
    String analyze(@V("query") String query);
}
