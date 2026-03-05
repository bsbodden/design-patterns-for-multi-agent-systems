package com.redis.demos.researchplanner;

import com.redis.demos.researchplanner.repository.OrderRepository;
import com.redis.demos.researchplanner.repository.SellerRepository;
import com.redis.demos.researchplanner.service.OlistDataService;
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
 * Integration tests for the intervention planning pipeline.
 * Tests data seeding, OlistDataService, and the InterventionPlanner agent.
 */
@SpringBootTest
@Testcontainers
class InterventionPlannerIT {

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
    OlistDataService dataService;

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
    void seeder_seller42Exists() {
        var seller = sellerRepo.findById("seller_42");
        assertThat(seller).isPresent();
        assertThat(seller.get().getAvgRating()).isEqualTo(2.3);
        assertThat(seller.get().getOnTimePct()).isEqualTo(60.0);
    }

    // --- OlistDataService Tests ---

    @Test
    void dataService_getSellerMetrics_returnsKnownSeller() {
        var seller = dataService.getSellerMetrics("seller_42");
        assertThat(seller.getSellerId()).isEqualTo("seller_42");
        assertThat(seller.getState()).isEqualTo("SP");
    }

    @Test
    void dataService_getAffectedOrders_findsOrdersForSeller() {
        var orders = dataService.getAffectedOrders("seller_42");
        assertThat(orders).isNotEmpty();
    }

    @Test
    void dataService_getSellersInRegion_findsSPSellers() {
        var sellers = dataService.getSellersInRegion("SP");
        assertThat(sellers).isNotEmpty();
        assertThat(sellers).anyMatch(s -> "seller_42".equals(s.getSellerId()));
    }

    @Test
    void dataService_formatSellerSummary_containsMetrics() {
        var seller = dataService.getSellerMetrics("seller_42");
        var summary = dataService.formatSellerSummary(seller);
        assertThat(summary).contains("seller_42");
        assertThat(summary).contains("2.3");
        assertThat(summary).contains("60");
    }

    @Test
    void dataService_formatAffectedOrdersSummary_containsCounts() {
        var orders = dataService.getAffectedOrders("seller_42");
        var summary = dataService.formatAffectedOrdersSummary(orders);
        assertThat(summary).contains("Affected orders:");
        assertThat(summary).contains("Total value:");
    }
}
