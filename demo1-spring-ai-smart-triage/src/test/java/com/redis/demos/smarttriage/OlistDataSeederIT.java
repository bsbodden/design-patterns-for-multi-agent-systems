package com.redis.demos.smarttriage;

import com.redis.demos.smarttriage.model.OlistCustomer;
import com.redis.demos.smarttriage.model.OlistOrder;
import com.redis.demos.smarttriage.model.OlistProduct;
import com.redis.demos.smarttriage.model.OlistSeller;
import com.redis.demos.smarttriage.repository.CustomerRepository;
import com.redis.demos.smarttriage.repository.OrderRepository;
import com.redis.demos.smarttriage.repository.ProductRepository;
import com.redis.demos.smarttriage.repository.SellerRepository;
import com.redis.om.spring.search.stream.EntityStream;
import com.redis.om.spring.search.stream.SearchStream;
import com.redis.testcontainers.RedisStackContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.ai.mcp.client.enabled=false",
    "spring.ai.openai.api-key=test-key-not-used",
    "demo.auto-run=false"
})
@Testcontainers
class OlistDataSeederIT {

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
    CustomerRepository customerRepo;

    @Autowired
    ProductRepository productRepo;

    @Autowired
    EntityStream entityStream;

    // --- Seeder Verification Tests ---

    @Test
    void seederPopulatesOrders() {
        assertThat(orderRepo.count()).isGreaterThanOrEqualTo(30);
    }

    @Test
    void seederPopulatesSellers() {
        assertThat(sellerRepo.count()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void seederPopulatesCustomers() {
        assertThat(customerRepo.count()).isGreaterThanOrEqualTo(10);
    }

    @Test
    void seederPopulatesProducts() {
        assertThat(productRepo.count()).isGreaterThanOrEqualTo(10);
    }

    // --- Known Entity Tests (planted data for demo scenarios) ---

    @Test
    void knownOrderExists_ORD_A1B2C3() {
        var order = orderRepo.findById("ORD_A1B2C3");
        assertThat(order).isPresent();
        assertThat(order.get().getCustomerId()).isEqualTo("cust_1234");
        assertThat(order.get().getStatus()).isEqualTo("shipped");
        assertThat(order.get().getItems()).hasSize(1);
        assertThat(order.get().getItems().getFirst().getSellerId()).isEqualTo("seller_42");
    }

    @Test
    void knownSeller42_problemSeller() {
        var seller = sellerRepo.findById("seller_42");
        assertThat(seller).isPresent();
        assertThat(seller.get().getAvgRating()).isLessThan(3.0);
        assertThat(seller.get().getOnTimePct()).isLessThan(70.0);
        assertThat(seller.get().getState()).isEqualTo("SP");
        assertThat(seller.get().getTotalOrders()).isGreaterThan(200);
    }

    @Test
    void knownSeller7_goldStandard() {
        var seller = sellerRepo.findById("seller_7");
        assertThat(seller).isPresent();
        assertThat(seller.get().getAvgRating()).isGreaterThan(4.5);
        assertThat(seller.get().getOnTimePct()).isGreaterThan(95.0);
    }

    @Test
    void knownCustomer1234_repeatComplainant() {
        var customer = customerRepo.findById("cust_1234");
        assertThat(customer).isPresent();
        assertThat(customer.get().getOrderCount()).isEqualTo(9);
        assertThat(customer.get().getAvgReviewScore()).isLessThan(3.0);
    }

    // --- Repository Query Tests ---

    @Test
    void findOrdersByCustomerId() {
        var orders = orderRepo.findByCustomerId("cust_1234");
        var orderList = StreamSupport.stream(orders.spliterator(), false).toList();
        assertThat(orderList).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    void findOrdersByStatus() {
        var shipped = orderRepo.findByStatus("shipped");
        var shippedList = StreamSupport.stream(shipped.spliterator(), false).toList();
        assertThat(shippedList).hasSizeGreaterThanOrEqualTo(2);
    }

    // --- EntityStream Query Tests (validates indexes work) ---

    @Test
    void entityStream_findLateDeliveries() {
        // Find delivered orders where delivery was after estimate
        var allOrders = StreamSupport.stream(orderRepo.findAll().spliterator(), false).toList();
        var lateOrders = allOrders.stream()
                .filter(o -> "delivered".equals(o.getStatus()))
                .filter(o -> o.getDeliveredTimestamp() != null && o.getEstimatedDelivery() != null)
                .filter(o -> o.getDeliveredTimestamp().isAfter(o.getEstimatedDelivery()))
                .toList();
        // We planted multiple late orders
        assertThat(lateOrders).hasSizeGreaterThanOrEqualTo(10);
    }

    @Test
    void entityStream_findSeller42Orders() {
        var allOrders = StreamSupport.stream(orderRepo.findAll().spliterator(), false).toList();
        var seller42Orders = allOrders.stream()
                .filter(o -> o.getItems().stream().anyMatch(i -> "seller_42".equals(i.getSellerId())))
                .toList();
        assertThat(seller42Orders).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    void entityStream_findLowRatedSellers() {
        var allSellers = StreamSupport.stream(sellerRepo.findAll().spliterator(), false).toList();
        var lowRated = allSellers.stream()
                .filter(s -> s.getAvgRating() < 3.0 && s.getTotalOrders() > 50)
                .toList();
        assertThat(lowRated).hasSizeGreaterThanOrEqualTo(2);
        assertThat(lowRated.stream().map(OlistSeller::getSellerId)).contains("seller_42");
    }

    @Test
    void electronicsCategory_lowestReviewScore() {
        var allProducts = StreamSupport.stream(productRepo.findAll().spliterator(), false).toList();
        var electronics = allProducts.stream()
                .filter(p -> "electronics".equals(p.getCategory()))
                .toList();
        var avgScore = electronics.stream()
                .mapToDouble(OlistProduct::getAvgReviewScore)
                .average()
                .orElse(0.0);
        assertThat(avgScore).isLessThan(3.5);
    }
}
