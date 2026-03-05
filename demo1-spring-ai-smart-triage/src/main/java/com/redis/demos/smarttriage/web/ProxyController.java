package com.redis.demos.smarttriage.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Proxies API calls from the unified UI to Demo 2 (LangChain4J) and Demo 3 (Embabel) backends.
 * Handles both regular REST responses and SSE streams.
 */
@RestController
public class ProxyController {

    private static final Logger log = LoggerFactory.getLogger(ProxyController.class);

    private final HttpClient httpClient;
    private final String demo2Url;
    private final String demo3Url;

    public ProxyController(
            @Value("${demo.backend.demo2-url:http://localhost:8882}") String demo2Url,
            @Value("${demo.backend.demo3-url:http://localhost:8883}") String demo3Url) {
        this.demo2Url = demo2Url.replaceAll("/$", "");
        this.demo3Url = demo3Url.replaceAll("/$", "");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        log.info("Proxy configured: demo2={}, demo3={}", this.demo2Url, this.demo3Url);
    }

    // ── Demo 2: LangChain4J Analysis ──────────────────────────────────

    @GetMapping(value = "/demo2/api/analyze", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> proxyAnalyze(
            @RequestParam(defaultValue = "sequential") String mode,
            @RequestParam(defaultValue = "") String query) {
        String url = demo2Url + "/api/analyze?mode=" + enc(mode) + "&query=" + enc(query);
        return streamProxy(url);
    }

    @GetMapping(value = "/demo2/api/demo/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> proxyAnalysisDemo(
            @RequestParam(defaultValue = "") String query) {
        String url = demo2Url + "/api/demo/stream?query=" + enc(query);
        return streamProxy(url);
    }

    // ── Demo 3: Embabel Intervention ──────────────────────────────────

    @PostMapping(value = "/demo3/api/intervention/run", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> proxyInterventionRun(@RequestBody byte[] body) {
        return postProxy(demo3Url + "/api/intervention/run", body, "application/json");
    }

    @GetMapping(value = "/demo3/events/process/{processId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> proxyEmbabelEvents(@PathVariable String processId) {
        return streamProxy(demo3Url + "/events/process/" + enc(processId));
    }

    @GetMapping(value = "/demo3/api/v1/process/{processId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> proxyProcessStatus(@PathVariable String processId) {
        return getProxy(demo3Url + "/api/v1/process/" + enc(processId));
    }

    @GetMapping(value = "/demo3/api/agents", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> proxyAgents() {
        return getProxy(demo3Url + "/api/agents");
    }

    // ── Proxy internals ───────────────────────────────────────────────

    private ResponseEntity<StreamingResponseBody> streamProxy(String url) {
        log.debug("SSE proxy → {}", url);
        StreamingResponseBody responseBody = outputStream -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofMinutes(5))
                        .build();
                HttpResponse<InputStream> resp = httpClient.send(req,
                        HttpResponse.BodyHandlers.ofInputStream());
                byte[] buf = new byte[1024];
                int n;
                try (InputStream is = resp.body()) {
                    while ((n = is.read(buf)) != -1) {
                        outputStream.write(buf, 0, n);
                        outputStream.flush();
                    }
                }
            } catch (Exception e) {
                log.debug("SSE proxy stream closed: {}", e.getMessage());
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(responseBody);
    }

    private ResponseEntity<byte[]> getProxy(String url) {
        log.debug("GET proxy → {}", url);
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMinutes(2))
                    .build();
            HttpResponse<byte[]> resp = httpClient.send(req,
                    HttpResponse.BodyHandlers.ofByteArray());
            String ct = resp.headers().firstValue("Content-Type").orElse("application/json");
            return ResponseEntity.status(resp.statusCode())
                    .header("Content-Type", ct)
                    .body(resp.body());
        } catch (Exception e) {
            log.error("GET proxy error: {}", e.getMessage());
            return ResponseEntity.status(502)
                    .body(("{\"error\":\"" + e.getMessage() + "\"}").getBytes());
        }
    }

    private ResponseEntity<byte[]> postProxy(String url, byte[] body, String contentType) {
        log.debug("POST proxy → {}", url);
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMinutes(2))
                    .header("Content-Type", contentType)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<byte[]> resp = httpClient.send(req,
                    HttpResponse.BodyHandlers.ofByteArray());
            String ct = resp.headers().firstValue("Content-Type").orElse("application/json");
            return ResponseEntity.status(resp.statusCode())
                    .header("Content-Type", ct)
                    .body(resp.body());
        } catch (Exception e) {
            log.error("POST proxy error: {}", e.getMessage());
            return ResponseEntity.status(502)
                    .body(("{\"error\":\"" + e.getMessage() + "\"}").getBytes());
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
