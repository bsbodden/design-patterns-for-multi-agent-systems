package com.redis.demos.smarttriage.seeder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Seeds agent memories via MCP memory server at application startup.
 * Loads procedural SOPs, episodic interactions, entity profiles, and semantic knowledge
 * from JSON files and stores them using the memory_store MCP tool.
 *
 * Only active when MCP client is enabled (spring.ai.mcp.client.enabled=true).
 */
@Component
@Order(2) // Run after OlistDataSeeder
@ConditionalOnProperty(name = "spring.ai.mcp.client.enabled", havingValue = "true")
public class MemorySeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MemorySeeder.class);

    private static final String[] MEMORY_FILES = {
            "olist/memories/procedural-sops.json",
            "olist/memories/episodic-interactions.json",
            "olist/memories/entity-profiles.json",
            "olist/memories/semantic-knowledge.json"
    };

    private final ChatClient.Builder chatClientBuilder;
    private final ToolCallbackProvider mcpToolProvider;
    private final ObjectMapper mapper;

    public MemorySeeder(ChatClient.Builder chatClientBuilder,
                        ToolCallbackProvider mcpToolProvider,
                        ObjectMapper mapper) {
        this.chatClientBuilder = chatClientBuilder;
        this.mcpToolProvider = mcpToolProvider;
        this.mapper = mapper;
    }

    @Override
    public void run(String... args) throws Exception {
        // Check if memories are already seeded by searching for a known marker
        try {
            String searchResult = chatClientBuilder.build()
                    .prompt()
                    .system("You are a memory management agent. Search for memories and return ONLY the raw result, no commentary.")
                    .user("Use memory_search with query 'DELIVERY DELAY SOP' and userId 'system' to check if procedural memories exist. " +
                          "If memories are found, respond with exactly 'ALREADY_SEEDED'. If no memories are found, respond with exactly 'NEEDS_SEEDING'.")
                    .toolCallbacks(mcpToolProvider)
                    .call()
                    .content();

            if (searchResult != null && searchResult.contains("ALREADY_SEEDED")) {
                log.info("Agent memories already seeded — skipping");
                return;
            }
        } catch (Exception e) {
            log.warn("Could not check for existing memories (MCP server may be unavailable): {}", e.getMessage());
            log.info("Proceeding with memory seeding attempt...");
        }

        log.info("Seeding agent memories via MCP memory server...");
        int totalSeeded = 0;

        for (String filePath : MEMORY_FILES) {
            try {
                int count = seedFromFile(filePath);
                totalSeeded += count;
            } catch (Exception e) {
                log.warn("Failed to seed memories from {}: {}", filePath, e.getMessage());
            }
        }

        log.info("Seeded {} agent memories across {} files", totalSeeded, MEMORY_FILES.length);
    }

    @SuppressWarnings("unchecked")
    private int seedFromFile(String filePath) throws Exception {
        try (InputStream is = new ClassPathResource(filePath).getInputStream()) {
            List<Map<String, Object>> memories = mapper.readValue(is,
                    mapper.getTypeFactory().constructCollectionType(List.class, Map.class));

            int count = 0;
            for (Map<String, Object> memory : memories) {
                try {
                    storeMemory(memory);
                    count++;
                } catch (Exception e) {
                    log.warn("Failed to store memory: {}", e.getMessage());
                }
            }

            log.info("  Loaded {} memories from {}", count, filePath);
            return count;
        }
    }

    private void storeMemory(Map<String, Object> memory) {
        String text = (String) memory.get("text");
        String memoryType = (String) memory.get("memoryType");
        String userId = (String) memory.get("userId");
        @SuppressWarnings("unchecked")
        List<String> topics = (List<String>) memory.get("topics");
        @SuppressWarnings("unchecked")
        List<String> entities = (List<String>) memory.get("entities");

        String topicsStr = topics != null && !topics.isEmpty() ? String.join(", ", topics) : "none";
        String entitiesStr = entities != null && !entities.isEmpty() ? String.join(", ", entities) : "none";

        chatClientBuilder.build()
                .prompt()
                .system("You are a memory management agent. Store the provided memory using the memory_store tool. " +
                        "Do not add commentary — just call the tool and confirm storage.")
                .user("""
                    Store the following memory using memory_store:
                    - text: %s
                    - memoryType: %s
                    - userId: %s
                    - topics: [%s]
                    - entities: [%s]
                    """.formatted(text, memoryType, userId, topicsStr, entitiesStr))
                .toolCallbacks(mcpToolProvider)
                .call()
                .content();
    }
}
