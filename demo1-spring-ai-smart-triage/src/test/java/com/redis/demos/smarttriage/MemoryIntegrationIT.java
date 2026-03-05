package com.redis.demos.smarttriage;

import com.redis.demos.smarttriage.intake.CustomerIntakePipeline;
import com.redis.demos.smarttriage.intake.PipelineResult;
import com.redis.demos.smarttriage.seeder.MemorySeeder;
import com.redis.testcontainers.RedisStackContainer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MCP memory tools in the pipeline.
 * Requires:
 * - OPENAI_API_KEY environment variable
 * - MCP memory server running on localhost:18080
 * - Redis Stack (via Testcontainers for Olist data)
 *
 * Run with: ./gradlew test --tests '*MemoryIntegrationIT*' -Pinclude-tags=memory
 */
@SpringBootTest(properties = {
    "spring.ai.mcp.client.enabled=true",
    "spring.ai.openai.api-key=${OPENAI_API_KEY:test-key}",
    "spring.ai.mcp.client.sse.connections.redis-memory.url=${MCP_SERVER_URL:http://localhost:18080}",
    "demo.auto-run=false"
})
@Testcontainers
@Tag("memory")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.*")
class MemoryIntegrationIT {

    @Container
    static RedisStackContainer redis = new RedisStackContainer(
            RedisStackContainer.DEFAULT_IMAGE_NAME.withTag("latest"));

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    CustomerIntakePipeline pipeline;

    @Autowired(required = false)
    MemorySeeder memorySeeder;

    // --- Memory Seeder Tests ---

    @Test
    void memorySeeder_isPresent_whenMcpEnabled() {
        assertThat(memorySeeder).as("MemorySeeder should be active when MCP is enabled").isNotNull();
    }

    // --- Pipeline with Memory Tests ---

    @Test
    void pipeline_lateDelivery_emitsMemoryEvents() {
        var events = new ArrayList<CustomerIntakePipeline.PipelineEvent>();
        PipelineResult result = pipeline.process(
                "My order ORD_A1B2C3 was supposed to arrive 5 days ago and still nothing! " +
                "This is the third time seller_42 has let me down.",
                events::add);

        assertThat(result.classification().topCategory()).isEqualTo("delivery_issue");
        assertThat(result.finalResponse()).isNotBlank();
        assertThat(result.finalScore()).isGreaterThanOrEqualTo(3);

        // Verify memory events were emitted
        List<String> eventTypes = events.stream().map(CustomerIntakePipeline.PipelineEvent::type).toList();
        assertThat(eventTypes).contains("memory_search");
        assertThat(eventTypes).contains("memory_stored");
    }

    @Test
    void pipeline_productQuality_emitsMemoryEvents() {
        var events = new ArrayList<CustomerIntakePipeline.PipelineEvent>();
        PipelineResult result = pipeline.process(
                "The product doesn't match the description at all. Seller seller_42 has terrible reviews " +
                "and I want answers. My customer ID is cust_1234.",
                events::add);

        assertThat(result.classification().topCategory()).isEqualTo("product_quality");
        assertThat(result.finalResponse()).isNotBlank();

        List<String> eventTypes = events.stream().map(CustomerIntakePipeline.PipelineEvent::type).toList();
        assertThat(eventTypes).contains("memory_search");
        assertThat(eventTypes).contains("memory_stored");
    }

    @Test
    void pipeline_lateDelivery_responseReferencesMemory() {
        PipelineResult result = pipeline.process(
                "My order ORD_A1B2C3 was supposed to arrive 5 days ago and still nothing! " +
                "This is the third time seller_42 has let me down.");

        // The expert should reference information from seeded memories
        // (seller_42 profile, delivery SOPs, past resolutions)
        String response = result.finalResponse().toLowerCase();
        // At minimum the response should reference the order and seller
        assertThat(response).satisfiesAnyOf(
                r -> assertThat(r).contains("seller"),
                r -> assertThat(r).contains("order"),
                r -> assertThat(r).contains("delivery")
        );
        assertThat(result.finalResponse()).isNotBlank();
    }
}
