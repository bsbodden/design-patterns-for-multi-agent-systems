package com.redis.demos.docinsight.agents;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface DeliveryAnalyzer {

    @UserMessage("""
            You are a delivery performance analyst for an e-commerce platform.
            Use the available tools to query delivery metrics.
            Analyze on-time delivery rates, average delays, and identify worst-performing sellers.
            Focus on the query: {{query}}

            Provide a concise analysis highlighting delivery issues and problem sellers.
            """)
    @Agent("Analyzes on-time delivery rates, delay patterns by region and seller")
    String analyze(@V("query") String query);
}
