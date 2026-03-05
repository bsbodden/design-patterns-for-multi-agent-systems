package com.redis.demos.smarttriage.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.demos.smarttriage.intake.CustomerIntakePipeline;
import com.redis.demos.smarttriage.intake.IssueClassification;
import com.redis.demos.smarttriage.intake.PipelineResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class DemoOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(DemoOrchestrator.class);

    private final CustomerIntakePipeline pipeline;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private static final String[] DEMO_QUERIES = {
            "My order ORD_A1B2C3 was supposed to arrive 5 days ago and still nothing! " +
                    "This is the third time seller_42 has let me down.",
            "The product doesn't match the description at all. Seller seller_42 has terrible reviews " +
                    "and I want answers. My customer ID is cust_1234.",
            "Order ORD_X7Y8Z9 is late, the product I already received from the same seller was broken, " +
                    "AND I want a refund for both!"
    };

    public DemoOrchestrator(CustomerIntakePipeline pipeline, ObjectMapper objectMapper) {
        this.pipeline = pipeline;
        this.objectMapper = objectMapper;
    }

    public SseEmitter processQuery(String query) {
        SseEmitter emitter = new SseEmitter(300_000L);
        executor.execute(() -> {
            try {
                runPipeline(query, emitter);
                emitter.complete();
            } catch (Exception e) {
                log.error("Pipeline error", e);
                emitEvent(emitter, "error", new PipelineEvent.ErrorEvent(e.getMessage()));
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    public SseEmitter runFullDemo() {
        SseEmitter emitter = new SseEmitter(600_000L);
        executor.execute(() -> {
            try {
                for (int i = 0; i < DEMO_QUERIES.length; i++) {
                    String query = DEMO_QUERIES[i];
                    emitEvent(emitter, "query", new PipelineEvent.QueryEvent(query, i));

                    if (i > 0) {
                        Thread.sleep(2000);
                    }

                    runPipeline(query, emitter);
                }
                emitter.complete();
            } catch (Exception e) {
                log.error("Full demo error", e);
                emitEvent(emitter, "error", new PipelineEvent.ErrorEvent(e.getMessage()));
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    private void runPipeline(String query, SseEmitter emitter) {
        int[] evalIteration = {0};
        PipelineResult result = pipeline.process(query, event -> {
            switch (event.type()) {
                case "routed" -> {
                    // Parse classification from the pipeline to build routing scores
                    // The detail contains the summary; we emit classification scores
                }
                case "routing" -> emitEvent(emitter, "status",
                        new PipelineEvent.StatusEvent("routing", event.detail()));
                case "handoff" -> {
                    var parts = event.detail().split(" \\| ", 2);
                    var agents = parts[0].split(" → ", 2);
                    emitEvent(emitter, "handoff", new PipelineEvent.HandoffEvent(
                            agents[0].trim(), agents.length > 1 ? agents[1].trim() : "",
                            parts.length > 1 ? parts[1].trim() : ""));
                }
                case "expert_start" -> emitEvent(emitter, "handling",
                        new PipelineEvent.HandlingEvent(event.detail(), List.of()));
                case "tool_call" -> emitEvent(emitter, "tool_call",
                        new PipelineEvent.ToolCallEvent(event.detail()));
                case "memory_search" -> emitEvent(emitter, "memory",
                        new PipelineEvent.MemoryEvent("search", event.detail()));
                case "memory_stored" -> emitEvent(emitter, "memory",
                        new PipelineEvent.MemoryEvent("store", event.detail()));
                case "draft_response" -> emitEvent(emitter, "draft",
                        new PipelineEvent.DraftEvent(event.detail()));
                case "evaluation_result" -> {
                    evalIteration[0]++;
                    var detail = event.detail();
                    int score = 0;
                    String feedback = detail;
                    // Parse "Score: N/4 — feedback text"
                    if (detail.startsWith("Score: ")) {
                        int slashIdx = detail.indexOf('/');
                        if (slashIdx > 7) {
                            try { score = Integer.parseInt(detail.substring(7, slashIdx)); }
                            catch (NumberFormatException ignored) {}
                        }
                        int dashIdx = detail.indexOf(" — ");
                        if (dashIdx >= 0) feedback = detail.substring(dashIdx + 3);
                    }
                    emitEvent(emitter, "evaluation",
                            new PipelineEvent.EvaluationEvent(score, feedback, evalIteration[0]));
                }
                case "refining" -> emitEvent(emitter, "refinement",
                        new PipelineEvent.StatusEvent("refinement", event.detail()));
                default -> emitEvent(emitter, "status",
                        new PipelineEvent.StatusEvent(event.type(), event.detail()));
            }
        });

        // Emit structured routing event with scores
        var c = result.classification();
        List<PipelineEvent.ScoreEntry> scores = List.of(
                new PipelineEvent.ScoreEntry("delivery_issue", c.deliveryIssue()),
                new PipelineEvent.ScoreEntry("product_quality", c.productQuality()),
                new PipelineEvent.ScoreEntry("payment_problem", c.paymentProblem()),
                new PipelineEvent.ScoreEntry("general_question", c.generalQuestion())
        );
        emitEvent(emitter, "routing",
                new PipelineEvent.RoutingEvent(result.selectedExpert(), "", scores));

        // Emit final evaluation summary
        emitEvent(emitter, "evaluation",
                new PipelineEvent.EvaluationEvent(result.finalScore(), "", result.criticIterations()));

        // Emit complete
        emitEvent(emitter, "complete", new PipelineEvent.CompleteEvent(result.finalResponse()));
    }

    private void emitEvent(SseEmitter emitter, String eventName, PipelineEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            emitter.send(SseEmitter.event().name(eventName).data(json));
        } catch (Exception e) {
            log.warn("Failed to emit SSE event: {}", eventName, e);
        }
    }
}
