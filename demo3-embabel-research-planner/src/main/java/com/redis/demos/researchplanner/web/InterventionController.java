package com.redis.demos.researchplanner.web;

import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.domain.io.UserInput;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST controller that wraps the Embabel AgentPlatform to invoke the
 * InterventionPlanner agent from the web UI. The SSE streaming of events
 * is handled by Embabel's built-in SseController at /events/process/{id}.
 */
@RestController
@RequestMapping("/api")
public class InterventionController {

    private static final Logger log = LoggerFactory.getLogger(InterventionController.class);

    private final AgentPlatform agentPlatform;
    private final ObjectMapper objectMapper;

    public InterventionController(AgentPlatform agentPlatform, ObjectMapper objectMapper) {
        this.agentPlatform = agentPlatform;
        this.objectMapper = objectMapper;
    }

    /**
     * Start an intervention planning process. Returns the process ID and SSE URL
     * for the client to connect to for real-time event streaming.
     */
    @PostMapping(value = "/intervention/run", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> runIntervention(@RequestBody Map<String, String> request) {
        String input = request.getOrDefault("input", "");
        if (input.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Input is required");
        }

        log.info("Starting intervention planning for: {}", input.substring(0, Math.min(80, input.length())));

        // Find the InterventionPlanner agent
        var agent = agentPlatform.agents().stream()
                .filter(a -> a.getName().toLowerCase().contains("intervention"))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "InterventionPlanner agent not found"));

        // Create process with the user input bound
        AgentProcess process = agentPlatform.createAgentProcessFrom(
                agent,
                new ProcessOptions(),
                new UserInput(input)
        );

        // Start async execution
        agentPlatform.start(process);

        // Return process info for client to connect SSE
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("processId", process.getId());
        response.put("status", process.getStatus().name());
        response.put("sseUrl", "/events/process/" + process.getId());
        response.put("statusUrl", "/api/v1/process/" + process.getId());

        return response;
    }

    /**
     * List available agents and their goals (useful for the UI to show the action graph).
     */
    @GetMapping(value = "/agents", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> listAgents() {
        var agents = agentPlatform.agents();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agentCount", agents.size());
        result.put("agents", agents.stream().map(a -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", a.getName());
            info.put("description", a.getDescription());
            return info;
        }).toList());
        return result;
    }
}
