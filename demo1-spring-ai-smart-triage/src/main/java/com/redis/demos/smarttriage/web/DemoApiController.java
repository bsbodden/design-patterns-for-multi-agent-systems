package com.redis.demos.smarttriage.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class DemoApiController {

    private final DemoOrchestrator orchestrator;

    public DemoApiController(DemoOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestParam String query) {
        return orchestrator.processQuery(query);
    }

    @GetMapping(value = "/demo/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter runFullDemo() {
        return orchestrator.runFullDemo();
    }
}
