package com.redis.demos.docinsight.agents;

import com.redis.demos.docinsight.tools.OlistDataTools;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.agentic.supervisor.SupervisorContextStrategy;
import dev.langchain4j.agentic.supervisor.SupervisorResponseStrategy;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

@Component
public class OpsAnalysisPipeline {

    private static final Logger log = LoggerFactory.getLogger(OpsAnalysisPipeline.class);

    private final OlistDataTools olistDataTools;
    private final ChatModel chatModel;

    public OpsAnalysisPipeline(OlistDataTools olistDataTools,
                                @Value("${OPENAI_API_KEY:#{null}}") String apiKey) {
        this.olistDataTools = olistDataTools;
        this.chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey != null ? apiKey : System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o")
                .build();
    }

    public AnalysisResult runSequential(String query) {
        log.info("Running SEQUENTIAL analysis: {}", query);
        long start = System.currentTimeMillis();

        var agents = buildAgents();

        UntypedAgent sequential = AgenticServices.sequenceBuilder()
                .subAgents(agents.orderAnalyzer, agents.deliveryAnalyzer,
                        agents.reviewAnalyzer, agents.sellerScorer, agents.opsReporter)
                .outputKey("ops_report")
                .build();

        String report = (String) sequential.invoke(Map.of("query", query));
        long durationMs = System.currentTimeMillis() - start;

        // Extract intermediate results from scope (sequential runs in shared scope)
        var agentResults = new HashMap<String, String>();
        agentResults.put("order_analysis", "[completed]");
        agentResults.put("delivery_analysis", "[completed]");
        agentResults.put("review_analysis", "[completed]");
        agentResults.put("seller_scores", "[completed]");

        return new AnalysisResult("sequential", report, agentResults, 5, durationMs);
    }

    public AnalysisResult runParallel(String query) {
        log.info("Running PARALLEL analysis: {}", query);
        long start = System.currentTimeMillis();

        var agents = buildAgents();
        var executor = Executors.newFixedThreadPool(4);

        try {
            UntypedAgent parallelAnalysis = AgenticServices.parallelBuilder()
                    .subAgents(agents.orderAnalyzer, agents.deliveryAnalyzer,
                            agents.reviewAnalyzer, agents.sellerScorer)
                    .executor(executor)
                    .outputKey("parallel_done")
                    .build();

            UntypedAgent pipeline = AgenticServices.sequenceBuilder()
                    .subAgents(parallelAnalysis, agents.opsReporter)
                    .outputKey("ops_report")
                    .build();

            String report = (String) pipeline.invoke(Map.of("query", query));
            long durationMs = System.currentTimeMillis() - start;

            var agentResults = new HashMap<String, String>();
            agentResults.put("order_analysis", "[completed]");
            agentResults.put("delivery_analysis", "[completed]");
            agentResults.put("review_analysis", "[completed]");
            agentResults.put("seller_scores", "[completed]");

            return new AnalysisResult("parallel", report, agentResults, 5, durationMs);
        } finally {
            executor.shutdown();
        }
    }

    public AnalysisResult runSupervisor(String query) {
        log.info("Running SUPERVISOR analysis: {}", query);
        long start = System.currentTimeMillis();

        var agents = buildAgents();

        SupervisorAgent supervisor = AgenticServices.supervisorBuilder()
                .chatModel(chatModel)
                .subAgents(agents.orderAnalyzer, agents.deliveryAnalyzer,
                        agents.reviewAnalyzer, agents.sellerScorer, agents.opsReporter)
                .contextGenerationStrategy(SupervisorContextStrategy.CHAT_MEMORY_AND_SUMMARIZATION)
                .responseStrategy(SupervisorResponseStrategy.SUMMARY)
                .supervisorContext("""
                        You are an operations analysis supervisor for an e-commerce platform.
                        Analyze the query and decide which specialist agents to invoke.
                        You don't need to use all agents — only invoke those relevant to the query.
                        For delivery-focused queries, prioritize DeliveryAnalyzer and SellerScorer.
                        For review-focused queries, prioritize ReviewAnalyzer.
                        Always finish with OpsReporter to produce the final report.
                        When invoking agents, use pure JSON (no backticks).
                        """)
                .build();

        String report = (String) supervisor.invoke(
                "Analyze this e-commerce operations query and produce a report:\n\n" + query);
        long durationMs = System.currentTimeMillis() - start;

        return new AnalysisResult("supervisor", report, Map.of(), -1, durationMs);
    }

    private AgentSet buildAgents() {
        OrderAnalyzer orderAnalyzer = AgenticServices
                .agentBuilder(OrderAnalyzer.class)
                .chatModel(chatModel)
                .tools(olistDataTools)
                .outputKey("order_analysis")
                .build();

        DeliveryAnalyzer deliveryAnalyzer = AgenticServices
                .agentBuilder(DeliveryAnalyzer.class)
                .chatModel(chatModel)
                .tools(olistDataTools)
                .outputKey("delivery_analysis")
                .build();

        ReviewAnalyzer reviewAnalyzer = AgenticServices
                .agentBuilder(ReviewAnalyzer.class)
                .chatModel(chatModel)
                .tools(olistDataTools)
                .outputKey("review_analysis")
                .build();

        SellerScorer sellerScorer = AgenticServices
                .agentBuilder(SellerScorer.class)
                .chatModel(chatModel)
                .tools(olistDataTools)
                .outputKey("seller_scores")
                .build();

        OpsReporter opsReporter = AgenticServices
                .agentBuilder(OpsReporter.class)
                .chatModel(chatModel)
                .outputKey("ops_report")
                .build();

        return new AgentSet(orderAnalyzer, deliveryAnalyzer, reviewAnalyzer, sellerScorer, opsReporter);
    }

    private record AgentSet(
            OrderAnalyzer orderAnalyzer,
            DeliveryAnalyzer deliveryAnalyzer,
            ReviewAnalyzer reviewAnalyzer,
            SellerScorer sellerScorer,
            OpsReporter opsReporter) {}

    public record AnalysisResult(
            String mode,
            String report,
            Map<String, String> agentResults,
            int agentsRun,
            long durationMs) {}
}
