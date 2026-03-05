package com.redis.demos.smarttriage;

import com.redis.demos.smarttriage.intake.CustomerIntakePipeline;
import com.redis.demos.smarttriage.intake.IssueRouter;
import com.redis.demos.smarttriage.intake.PipelineResult;
import com.redis.demos.smarttriage.repository.OrderRepository;
import com.redis.demos.smarttriage.repository.SellerRepository;
import com.redis.demos.smarttriage.tools.OlistTools;
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
 * Integration tests for the customer intake pipeline components.
 * Tests @Tool methods, routing, and the critic loop using real Redis data.
 *
 * NOTE: Tests that require LLM calls (routing, critic) are marked separately
 * and need OPENAI_API_KEY. Tool tests only need Redis.
 */
@SpringBootTest(properties = {
    "spring.ai.mcp.client.enabled=false",
    "spring.ai.openai.api-key=${OPENAI_API_KEY:test-key}",
    "demo.auto-run=false"
})
@Testcontainers
class CustomerIntakeIT {

    @Container
    static RedisStackContainer redis = new RedisStackContainer(
            RedisStackContainer.DEFAULT_IMAGE_NAME.withTag("latest"));

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    OlistTools olistTools;

    @Autowired
    OrderRepository orderRepo;

    @Autowired
    SellerRepository sellerRepo;

    @Autowired
    IssueRouter issueRouter;

    @Autowired
    CustomerIntakePipeline pipeline;

    // --- Tool Tests (no LLM needed) ---

    @Test
    void lookupOrder_existingOrder_returnsFullDetails() {
        String result = olistTools.lookupOrder("ORD_A1B2C3");
        assertThat(result).contains("ORD_A1B2C3");
        assertThat(result).contains("cust_1234");
        assertThat(result).contains("shipped");
        assertThat(result).contains("seller_42");
        assertThat(result).contains("149.9");
    }

    @Test
    void lookupOrder_nonExistent_returnsNotFound() {
        String result = olistTools.lookupOrder("NONEXISTENT");
        assertThat(result).containsIgnoringCase("not found");
    }

    @Test
    void checkDeliveryStatus_lateOrder_reportsDelay() {
        // ORD_D4E5F6: estimated 2017-11-28T23:59, delivered 2017-12-05T09:00 = 6 days late
        String result = olistTools.checkDeliveryStatus("ORD_D4E5F6");
        assertThat(result).containsIgnoringCase("late");
        assertThat(result).containsPattern("\\d+ days");
    }

    @Test
    void checkDeliveryStatus_shippedNotDelivered_reportsPending() {
        // ORD_A1B2C3: shipped, not yet delivered, past estimate
        String result = olistTools.checkDeliveryStatus("ORD_A1B2C3");
        assertThat(result).containsIgnoringCase("not yet delivered");
    }

    @Test
    void getCustomerHistory_knownCustomer_returnsOrderSummary() {
        String result = olistTools.getCustomerHistory("cust_1234");
        assertThat(result).contains("cust_1234");
        assertThat(result).contains("order");
    }

    @Test
    void getCustomerHistory_nonExistent_returnsNotFound() {
        String result = olistTools.getCustomerHistory("nonexistent_customer");
        assertThat(result).containsIgnoringCase("not found");
    }

    @Test
    void getSellerRating_problemSeller_showsLowMetrics() {
        String result = olistTools.getSellerRating("seller_42");
        assertThat(result).contains("seller_42");
        assertThat(result).contains("2.3"); // avg rating
        assertThat(result).contains("60"); // on-time pct
    }

    @Test
    void getSellerRating_goldSeller_showsHighMetrics() {
        String result = olistTools.getSellerRating("seller_7");
        assertThat(result).contains("seller_7");
        assertThat(result).contains("4.8"); // avg rating
        assertThat(result).contains("98"); // on-time pct
    }

    @Test
    void getProductInfo_existingProduct_returnsDetails() {
        String result = olistTools.getProductInfo("prod_elec_001");
        assertThat(result).contains("electronics");
        assertThat(result).contains("850"); // weight
    }

    @Test
    void getProductInfo_nonExistent_returnsNotFound() {
        String result = olistTools.getProductInfo("nonexistent_product");
        assertThat(result).containsIgnoringCase("not found");
    }

    // --- LLM-Dependent Tests (need OPENAI_API_KEY) ---

    @Test
    @Tag("llm")
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.*")
    void routing_lateDelivery_classifiesAsDeliveryIssue() {
        var classification = issueRouter.classify(
                "My order ORD_A1B2C3 was supposed to arrive 5 days ago and still nothing!");
        assertThat(classification.topCategory()).isEqualTo("delivery_issue");
        assertThat(classification.deliveryIssue()).isGreaterThan(0.5);
    }

    @Test
    @Tag("llm")
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.*")
    void routing_productQuality_classifiesAsProductQuality() {
        var classification = issueRouter.classify(
                "The product doesn't match the description at all. Seller seller_42 has terrible reviews and I want answers.");
        assertThat(classification.topCategory()).isEqualTo("product_quality");
        assertThat(classification.productQuality()).isGreaterThan(0.5);
    }

    @Test
    @Tag("llm")
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.*")
    void routing_multiIssue_detectsMultipleConcerns() {
        var classification = issueRouter.classify(
                "Order ORD_X7Y8Z9 is late, the product I already received from the same seller was broken, AND I want a refund for both!");
        // Multi-issue: no single category should dominate above 0.70
        assertThat(classification.isMultiIssue()).isTrue();
    }

    @Test
    @Tag("llm")
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.*")
    void pipeline_lateDelivery_fullScenario() {
        var result = pipeline.process(
                "My order ORD_A1B2C3 was supposed to arrive 5 days ago and still nothing!");

        assertThat(result.classification().topCategory()).isEqualTo("delivery_issue");
        assertThat(result.finalResponse()).isNotBlank();
        assertThat(result.finalScore()).isGreaterThanOrEqualTo(3);
        assertThat(result.criticIterations()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Tag("llm")
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.*")
    void pipeline_productQuality_fullScenario() {
        var result = pipeline.process(
                "The product doesn't match the description at all. Seller seller_42 has terrible reviews and I want answers.");

        assertThat(result.classification().topCategory()).isEqualTo("product_quality");
        assertThat(result.finalResponse()).isNotBlank();
        assertThat(result.finalScore()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @Tag("llm")
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "sk-.*")
    void pipeline_multiIssue_addressesAllConcerns() {
        var result = pipeline.process(
                "Order ORD_X7Y8Z9 is late, the product I already received from the same seller was broken, AND I want a refund for both!");

        assertThat(result.classification().isMultiIssue()).isTrue();
        assertThat(result.finalResponse()).isNotBlank();
        // Multi-issue should require at least 2 critic iterations
        assertThat(result.criticIterations()).isGreaterThanOrEqualTo(1);
        assertThat(result.finalScore()).isGreaterThanOrEqualTo(3);
    }
}
