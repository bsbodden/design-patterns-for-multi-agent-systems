package com.redis.demos.docinsight;

import com.redis.demos.docinsight.agents.OpsAnalysisPipeline;
import com.redis.demos.docinsight.repository.OrderRepository;
import com.redis.demos.docinsight.repository.SellerRepository;
import com.redis.demos.docinsight.tools.OlistDataTools;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the operations analysis pipeline components.
 * Tests data seeding, @Tool methods, and agent orchestration.
 */
@SpringBootTest(properties = {
    "demo.auto-run=false"
})
@Testcontainers
class OpsAnalysisIT {

    @Container
    static RedisStackContainer redis = new RedisStackContainer(
            RedisStackContainer.DEFAULT_IMAGE_NAME.withTag("latest"));

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    OrderRepository orderRepo;

    @Autowired
    SellerRepository sellerRepo;

    @Autowired
    OlistDataTools olistDataTools;

    @Autowired
    OpsAnalysisPipeline pipeline;

    // --- Data Seeding Tests ---

    @Test
    void seeder_populatesOrders() {
        assertThat(orderRepo.count()).isGreaterThanOrEqualTo(40);
    }

    @Test
    void seeder_populatesSellers() {
        assertThat(sellerRepo.count()).isGreaterThanOrEqualTo(12);
    }

    @Test
    void seeder_knownSeller42Exists() {
        var seller = sellerRepo.findById("seller_42");
        assertThat(seller).isPresent();
        assertThat(seller.get().getAvgRating()).isEqualTo(2.3);
        assertThat(seller.get().getOnTimePct()).isEqualTo(60.0);
        assertThat(seller.get().getState()).isEqualTo("SP");
    }

    @Test
    void seeder_knownSeller7Exists() {
        var seller = sellerRepo.findById("seller_7");
        assertThat(seller).isPresent();
        assertThat(seller.get().getAvgRating()).isEqualTo(4.8);
        assertThat(seller.get().getOnTimePct()).isEqualTo(98.0);
    }

    // --- OlistDataTools Tests (no LLM needed) ---

    @Test
    void getOrderStats_allOrders_returnsStats() {
        String result = olistDataTools.getOrderStats("ALL");
        assertThat(result).contains("Total orders:");
        assertThat(result).contains("Average order value:");
        assertThat(result).contains("Payment breakdown:");
    }

    @Test
    void getOrderStats_spState_filtersCorrectly() {
        String result = olistDataTools.getOrderStats("SP");
        assertThat(result).contains("Order Statistics (state: SP)");
    }

    @Test
    void getDeliveryMetrics_all_returnsMetrics() {
        String result = olistDataTools.getDeliveryMetrics("ALL");
        assertThat(result).contains("Average on-time:");
        assertThat(result).contains("Worst performing sellers:");
        assertThat(result).contains("seller_42");
    }

    @Test
    void getDeliveryMetrics_spState_includesSeller42() {
        String result = olistDataTools.getDeliveryMetrics("SP");
        assertThat(result).contains("seller_42");
    }

    @Test
    void getReviewBreakdown_all_returnsDistribution() {
        String result = olistDataTools.getReviewBreakdown("ALL");
        assertThat(result).contains("Total reviews:");
        assertThat(result).contains("Average score:");
        assertThat(result).contains("Score distribution:");
    }

    @Test
    void getSellerRankings_topByRating_returnsHighRated() {
        String result = olistDataTools.getSellerRankings("rating", "top", 3);
        assertThat(result).contains("Seller Rankings");
        assertThat(result).contains("seller_7"); // 4.8 rating — should be top
    }

    @Test
    void getSellerRankings_bottomByRating_returnsLowRated() {
        String result = olistDataTools.getSellerRankings("rating", "bottom", 3);
        assertThat(result).contains("seller_42"); // 2.3 rating — should be bottom
    }

    @Test
    void getSellerRankings_topByRevenue_returnsResults() {
        String result = olistDataTools.getSellerRankings("revenue", "top", 5);
        assertThat(result).contains("Seller Rankings");
        assertThat(result).contains("revenue");
    }

    // --- LLM-Dependent Agent Pipeline Tests ---

    @Test
    @Tag("llm")
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.*")
    void sequential_fullAnalysis_producesReport() {
        var result = pipeline.runSequential("Analyze operations and flag issues");
        assertThat(result.report()).isNotBlank();
        assertThat(result.agentResults()).containsKeys("order_analysis", "delivery_analysis",
                "review_analysis", "seller_scores");
        assertThat(result.mode()).isEqualTo("sequential");
        assertThat(result.agentsRun()).isEqualTo(5);
    }

    @Test
    @Tag("llm")
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.*")
    void parallel_fullAnalysis_producesReport() {
        var result = pipeline.runParallel("Analyze operations and flag issues");
        assertThat(result.report()).isNotBlank();
        assertThat(result.agentResults()).containsKeys("order_analysis", "delivery_analysis",
                "review_analysis", "seller_scores");
        assertThat(result.mode()).isEqualTo("parallel");
        assertThat(result.agentsRun()).isEqualTo(5);
    }

    @Test
    @Tag("llm")
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.*")
    void supervisor_focusedQuery_producesReport() {
        var result = pipeline.runSupervisor("What's causing delivery delays in São Paulo?");
        assertThat(result.report()).isNotBlank();
        assertThat(result.mode()).isEqualTo("supervisor");
    }
}
