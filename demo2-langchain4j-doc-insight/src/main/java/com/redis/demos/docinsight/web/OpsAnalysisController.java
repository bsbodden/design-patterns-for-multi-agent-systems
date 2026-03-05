package com.redis.demos.docinsight.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.demos.docinsight.agents.OpsAnalysisPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class OpsAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(OpsAnalysisController.class);

    private final OpsAnalysisPipeline pipeline;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor();

    private static final String DEFAULT_QUERY = "Analyze Q4 2017 São Paulo operations and flag issues";

    public OpsAnalysisController(OpsAnalysisPipeline pipeline, ObjectMapper objectMapper) {
        this.pipeline = pipeline;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/analyze", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyze(
            @RequestParam(defaultValue = "sequential") String mode,
            @RequestParam(defaultValue = DEFAULT_QUERY) String query) {

        SseEmitter emitter = new SseEmitter(300_000L);
        executor.execute(() -> {
            ScheduledFuture<?> hb = startHeartbeat(emitter);
            try {
                emitEvent(emitter, "start", Map.of("mode", mode, "query", query));

                OpsAnalysisPipeline.AnalysisResult result = switch (mode) {
                    case "parallel" -> pipeline.runParallel(query);
                    case "supervisor" -> pipeline.runSupervisor(query);
                    default -> pipeline.runSequential(query);
                };

                emitEvent(emitter, "complete", Map.of(
                        "mode", result.mode(),
                        "report", result.report() != null ? result.report() : "",
                        "agentResults", result.agentResults(),
                        "agentsRun", result.agentsRun(),
                        "durationMs", result.durationMs()
                ));
                emitter.complete();
            } catch (Exception e) {
                log.error("Analysis error ({})", mode, e);
                emitEvent(emitter, "error", Map.of("message", e.getMessage()));
                emitter.completeWithError(e);
            } finally {
                hb.cancel(false);
            }
        });
        return emitter;
    }

    @GetMapping(value = "/demo/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter runFullDemo(@RequestParam(defaultValue = DEFAULT_QUERY) String query) {
        SseEmitter emitter = new SseEmitter(600_000L);
        executor.execute(() -> {
            ScheduledFuture<?> hb = startHeartbeat(emitter);
            try {
                String[] modes = {"sequential", "parallel", "supervisor"};
                String[] queries = {
                        "Analyze Q4 2017 São Paulo operations and flag issues",
                        "Analyze Q4 2017 São Paulo operations and flag issues",
                        "What's causing delivery delays in São Paulo?"
                };

                for (int i = 0; i < modes.length; i++) {
                    String mode = modes[i];
                    String q = queries[i];

                    emitEvent(emitter, "start", Map.of("mode", mode, "query", q, "index", i));

                    OpsAnalysisPipeline.AnalysisResult result = switch (mode) {
                        case "parallel" -> pipeline.runParallel(q);
                        case "supervisor" -> pipeline.runSupervisor(q);
                        default -> pipeline.runSequential(q);
                    };

                    emitEvent(emitter, "complete", Map.of(
                            "mode", result.mode(),
                            "report", result.report() != null ? result.report() : "",
                            "agentsRun", result.agentsRun(),
                            "durationMs", result.durationMs()
                    ));

                    if (i < modes.length - 1) {
                        Thread.sleep(1000);
                    }
                }
                emitter.complete();
            } catch (Exception e) {
                log.error("Full demo error", e);
                emitEvent(emitter, "error", Map.of("message", e.getMessage()));
                emitter.completeWithError(e);
            } finally {
                hb.cancel(false);
            }
        });
        return emitter;
    }

    private ScheduledFuture<?> startHeartbeat(SseEmitter emitter) {
        return heartbeat.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().comment("keepalive"));
            } catch (Exception ignored) {}
        }, 5, 5, TimeUnit.SECONDS);
    }

    private void emitEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event().name(eventName).data(json));
        } catch (Exception e) {
            log.warn("Failed to emit SSE event: {}", eventName, e);
        }
    }
}
