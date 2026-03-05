package com.redis.demos.smarttriage.intake;

import com.redis.demos.smarttriage.tools.OlistTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Orchestrates the full customer intake pipeline:
 * 1. Route (classify issue)
 * 2. Expert handles with tools
 * 3. Critic evaluates and refines
 */
@Component
public class CustomerIntakePipeline {

    private static final Logger log = LoggerFactory.getLogger(CustomerIntakePipeline.class);
    private static final int MAX_CRITIC_ITERATIONS = 3;

    private final IssueRouter router;
    private final ResponseEvaluator evaluator;
    private final ChatClient.Builder chatClientBuilder;
    private final OlistTools olistTools;
    private final ToolCallbackProvider mcpToolProvider; // null when MCP is disabled

    public CustomerIntakePipeline(IssueRouter router, ResponseEvaluator evaluator,
                                   ChatClient.Builder chatClientBuilder, OlistTools olistTools,
                                   ObjectProvider<ToolCallbackProvider> mcpToolProvider) {
        this.router = router;
        this.evaluator = evaluator;
        this.chatClientBuilder = chatClientBuilder;
        this.olistTools = olistTools;
        this.mcpToolProvider = mcpToolProvider.getIfAvailable();
    }

    public PipelineResult process(String customerMessage) {
        return process(customerMessage, event -> {});
    }

    public PipelineResult process(String customerMessage, Consumer<PipelineEvent> eventSink) {
        // Step 1: Route
        eventSink.accept(new PipelineEvent("routing", "Classifying issue..."));
        var classification = router.classify(customerMessage);
        String expert = classification.isMultiIssue() ? "delivery_issue" : classification.topCategory();
        eventSink.accept(new PipelineEvent("routed", "Classified as %s (%.0f%%)".formatted(
                expert, getTopScore(classification) * 100)));

        // Step 2: Handoff to expert with full context
        eventSink.accept(new PipelineEvent("handoff", "Router → %s expert | Customer message + classification forwarded".formatted(expert)));
        eventSink.accept(new PipelineEvent("expert_start", "Expert: " + expert));
        if (mcpToolProvider != null) {
            eventSink.accept(new PipelineEvent("memory_search", "Searching agent memory for SOPs and past resolutions"));
        }
        var toolCalls = new ArrayList<String>();
        String draftResponse = callExpert(expert, customerMessage, toolCalls);
        for (String tc : toolCalls) {
            eventSink.accept(new PipelineEvent("tool_call", tc));
        }
        eventSink.accept(new PipelineEvent("draft_response", "Draft generated"));

        // Step 3: Handoff to critic loop
        eventSink.accept(new PipelineEvent("handoff", "%s expert → Critic | Draft response + original message forwarded".formatted(expert)));
        int iterations = 0;
        int finalScore = 0;
        String currentResponse = draftResponse;

        for (int i = 0; i < MAX_CRITIC_ITERATIONS; i++) {
            iterations++;
            eventSink.accept(new PipelineEvent("evaluation", "Evaluating (iteration %d)...".formatted(iterations)));

            var evaluation = evaluator.evaluate(customerMessage, currentResponse);
            finalScore = evaluation.score();
            eventSink.accept(new PipelineEvent("evaluation_result",
                    "Score: %d/4 — %s".formatted(evaluation.score(), evaluation.feedback())));

            if (evaluation.passes()) {
                break;
            }

            // Handoff back to expert with critic feedback
            eventSink.accept(new PipelineEvent("handoff", "Critic → %s expert | Feedback + draft returned for refinement".formatted(expert)));
            currentResponse = refine(expert, customerMessage, currentResponse, evaluation.feedback());
        }

        // Store successful resolution as episodic memory for future reference
        if (mcpToolProvider != null) {
            storeResolution(expert, customerMessage, currentResponse);
            eventSink.accept(new PipelineEvent("memory_stored", "Resolution stored in agent memory"));
        }

        eventSink.accept(new PipelineEvent("complete", "Pipeline complete"));

        return new PipelineResult(
                classification, expert, toolCalls, currentResponse, iterations, finalScore);
    }

    private void storeResolution(String expertType, String customerMessage, String finalResponse) {
        try {
            chatClientBuilder.build()
                    .prompt()
                    .system("""
                        You are a memory management agent. Store the following customer interaction
                        as an episodic memory for future reference. Use the memory_store tool with:
                        - memoryType: "episodic"
                        - userId: "system"
                        - A concise text summarizing the issue, resolution, and outcome
                        - topics: include the issue category
                        - entities: include any seller IDs, customer IDs, or order IDs mentioned
                        """)
                    .user("""
                        Issue category: %s
                        Customer message: %s
                        Resolution provided: %s
                        """.formatted(expertType, customerMessage, finalResponse))
                    .toolCallbacks(mcpToolProvider)
                    .call()
                    .content();
            log.info("Stored resolution as episodic memory for {} issue", expertType);
        } catch (Exception e) {
            log.warn("Failed to store resolution memory: {}", e.getMessage());
        }
    }

    private static final String MEMORY_INSTRUCTIONS = """

        You also have access to memory tools for searching past resolutions and organizational knowledge.
        Before drafting your response:
        1. Use memory_search to find relevant procedural SOPs for this issue type (query for the issue category, userId "system")
        2. Use memory_search to find past interactions with any sellers or customers mentioned (query for entity names, userId "system")
        3. Use memory_search to find entity profiles for sellers, customers, or regions mentioned (query for entity names, userId "system")

        Reference what you find from memory in your response where relevant — cite past resolutions,
        apply SOPs, and note any entity profiles (e.g. seller track record).
        """;

    private String callExpert(String expertType, String customerMessage, List<String> toolCalls) {
        String memoryAddendum = mcpToolProvider != null ? MEMORY_INSTRUCTIONS : "";
        String systemPrompt = switch (expertType) {
            case "delivery_issue" -> """
                You are a delivery issue specialist for an e-commerce platform.
                Use the available tools to look up order details, check delivery status,
                get seller ratings, and find the customer's history.
                Based on the data, draft a helpful customer response that:
                - Acknowledges their frustration
                - References specific order details
                - Offers concrete resolution per company policy
                - Provides clear next steps
                """ + memoryAddendum;
            case "product_quality" -> """
                You are a product quality specialist for an e-commerce platform.
                Use the available tools to look up seller ratings, customer history,
                and product information.
                Draft a response addressing product quality concerns with:
                - Empathy for their disappointment
                - Specific seller/product data
                - Return/refund options
                - Quality improvement commitments
                """ + memoryAddendum;
            case "payment_problem" -> """
                You are a payment and refund specialist for an e-commerce platform.
                Use the available tools to look up order and payment details.
                Draft a response addressing payment concerns with:
                - Clear explanation of payment status
                - Refund timeline if applicable
                - Step-by-step resolution process
                """ + memoryAddendum;
            default -> """
                You are a general customer support agent for an e-commerce platform.
                Use available tools to find relevant information.
                Provide a helpful, friendly response.
                """ + memoryAddendum;
        };

        var request = chatClientBuilder.build()
                .prompt()
                .system(systemPrompt)
                .user(customerMessage)
                .tools(olistTools);
        if (mcpToolProvider != null) {
            request = request.toolCallbacks(mcpToolProvider);
        }
        return request.call().content();
    }

    private String refine(String expertType, String customerMessage,
                          String currentResponse, String feedback) {
        return chatClientBuilder.build()
                .prompt()
                .system("""
                    You are a customer service expert. Improve the draft response based on the critic's feedback.
                    Maintain the same factual content but address the specific issues raised.
                    Keep the response professional and empathetic.
                    """)
                .user("""
                    Original customer message: %s

                    Current draft response: %s

                    Critic feedback: %s

                    Please produce an improved version of the response.
                    """.formatted(customerMessage, currentResponse, feedback))
                .call()
                .content();
    }

    private double getTopScore(IssueClassification c) {
        return Math.max(Math.max(c.deliveryIssue(), c.productQuality()),
                       Math.max(c.paymentProblem(), c.generalQuestion()));
    }

    public record PipelineEvent(String type, String detail) {}
}
