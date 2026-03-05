package com.redis.demos.smarttriage.tools;

import com.redis.demos.smarttriage.model.OlistOrder;
import com.redis.demos.smarttriage.model.OlistProduct;
import com.redis.demos.smarttriage.model.OlistSeller;
import com.redis.demos.smarttriage.model.OrderItem;
import com.redis.demos.smarttriage.repository.CustomerRepository;
import com.redis.demos.smarttriage.repository.OrderRepository;
import com.redis.demos.smarttriage.repository.ProductRepository;
import com.redis.demos.smarttriage.repository.SellerRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.StreamSupport;

@Component
public class OlistTools {

    private final OrderRepository orderRepo;
    private final SellerRepository sellerRepo;
    private final CustomerRepository customerRepo;
    private final ProductRepository productRepo;

    public OlistTools(OrderRepository orderRepo, SellerRepository sellerRepo,
                      CustomerRepository customerRepo, ProductRepository productRepo) {
        this.orderRepo = orderRepo;
        this.sellerRepo = sellerRepo;
        this.customerRepo = customerRepo;
        this.productRepo = productRepo;
    }

    @Tool(description = "Look up a customer order by order ID. Returns full order details including status, dates, items, payment, and review.")
    public String lookupOrder(@ToolParam(description = "The order ID to look up") String orderId) {
        var order = orderRepo.findById(orderId);
        if (order.isEmpty()) {
            return "Order not found: " + orderId;
        }
        return formatOrder(order.get());
    }

    @Tool(description = "Check delivery status of an order. Returns estimated date, actual date, delay in days, and whether it is late.")
    public String checkDeliveryStatus(@ToolParam(description = "The order ID to check") String orderId) {
        var order = orderRepo.findById(orderId);
        if (order.isEmpty()) {
            return "Order not found: " + orderId;
        }
        return formatDeliveryStatus(order.get());
    }

    @Tool(description = "Get a customer's order history. Returns list of past orders, average review score, and complaint count.")
    public String getCustomerHistory(@ToolParam(description = "The customer ID") String customerId) {
        var customer = customerRepo.findById(customerId);
        if (customer.isEmpty()) {
            return "Customer not found: " + customerId;
        }

        var orders = orderRepo.findByCustomerId(customerId);
        var orderList = StreamSupport.stream(orders.spliterator(), false).toList();

        var avgReview = orderList.stream()
                .filter(o -> o.getReview() != null)
                .mapToInt(o -> o.getReview().getScore())
                .average().orElse(0.0);

        var complaints = orderList.stream()
                .filter(o -> o.getReview() != null && o.getReview().getScore() <= 2)
                .count();

        var c = customer.get();
        var sb = new StringBuilder();
        sb.append("Customer: %s (%s, %s)\n".formatted(c.getCustomerId(), c.getCity(), c.getState()));
        sb.append("Total orders: %d | Avg review: %.1f | Complaints: %d\n".formatted(orderList.size(), avgReview, complaints));
        sb.append("Total spent: R$%.2f\n\n".formatted(c.getTotalSpent()));
        sb.append("Order history:\n");
        for (var o : orderList) {
            sb.append("  - %s | %s | R$%.2f".formatted(o.getOrderId(), o.getStatus(), o.getOrderValue()));
            if (o.getReview() != null) {
                sb.append(" | Review: %d/5".formatted(o.getReview().getScore()));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "Get seller rating and performance metrics. Returns on-time percentage, average rating, total orders, and categories.")
    public String getSellerRating(@ToolParam(description = "The seller ID") String sellerId) {
        var seller = sellerRepo.findById(sellerId);
        if (seller.isEmpty()) {
            return "Seller not found: " + sellerId;
        }
        return formatSeller(seller.get());
    }

    @Tool(description = "Get product information. Returns category, weight, and average review score.")
    public String getProductInfo(@ToolParam(description = "The product ID") String productId) {
        var product = productRepo.findById(productId);
        if (product.isEmpty()) {
            return "Product not found: " + productId;
        }
        return formatProduct(product.get());
    }

    // --- Formatting helpers ---

    private String formatOrder(OlistOrder o) {
        var sb = new StringBuilder();
        sb.append("Order: %s\n".formatted(o.getOrderId()));
        sb.append("Customer: %s | Status: %s\n".formatted(o.getCustomerId(), o.getStatus()));
        sb.append("Purchased: %s\n".formatted(o.getPurchaseTimestamp()));
        sb.append("Estimated delivery: %s\n".formatted(o.getEstimatedDelivery()));
        if (o.getDeliveredTimestamp() != null) {
            sb.append("Delivered: %s\n".formatted(o.getDeliveredTimestamp()));
        }
        sb.append("Value: R$%.2f\n".formatted(o.getOrderValue()));

        if (o.getItems() != null) {
            sb.append("Items:\n");
            for (OrderItem item : o.getItems()) {
                sb.append("  - Product: %s | Seller: %s | Price: R$%.2f | Freight: R$%.2f\n"
                        .formatted(item.getProductId(), item.getSellerId(), item.getPrice(), item.getFreightValue()));
            }
        }

        if (o.getPayment() != null) {
            sb.append("Payment: %s in %d installments (R$%.2f)\n"
                    .formatted(o.getPayment().getType(), o.getPayment().getInstallments(), o.getPayment().getValue()));
        }

        if (o.getReview() != null) {
            sb.append("Review: %d/5 — \"%s\"\n".formatted(o.getReview().getScore(), o.getReview().getComment()));
        }

        return sb.toString();
    }

    private String formatDeliveryStatus(OlistOrder o) {
        var sb = new StringBuilder();
        sb.append("Delivery Status for %s:\n".formatted(o.getOrderId()));
        sb.append("Estimated delivery: %s\n".formatted(o.getEstimatedDelivery()));

        if (o.getDeliveredTimestamp() != null) {
            sb.append("Delivered: %s\n".formatted(o.getDeliveredTimestamp()));
            long delayDays = ChronoUnit.DAYS.between(o.getEstimatedDelivery(), o.getDeliveredTimestamp());
            if (delayDays > 0) {
                sb.append("Status: LATE — %d days past estimated delivery\n".formatted(delayDays));
            } else {
                sb.append("Status: ON TIME (delivered %d days early)\n".formatted(Math.abs(delayDays)));
            }
        } else {
            sb.append("Delivered: Not yet delivered\n");
            long daysSinceEstimate = ChronoUnit.DAYS.between(o.getEstimatedDelivery(), LocalDateTime.now());
            if (daysSinceEstimate > 0) {
                sb.append("Status: OVERDUE — %d days past estimated delivery and not yet delivered\n".formatted(daysSinceEstimate));
            } else {
                sb.append("Status: IN TRANSIT — delivery expected within %d days\n".formatted(Math.abs(daysSinceEstimate)));
            }
        }

        return sb.toString();
    }

    private String formatSeller(OlistSeller s) {
        var sb = new StringBuilder();
        sb.append("Seller: %s (%s, %s)\n".formatted(s.getSellerId(), s.getCity(), s.getState()));
        sb.append("Rating: %.1f/5 | On-time: %.0f%% | Total orders: %d\n"
                .formatted(s.getAvgRating(), s.getOnTimePct(), s.getTotalOrders()));
        sb.append("Avg delay: %.1f days | Return rate: %.1f%%\n"
                .formatted(s.getDelayAvgDays(), s.getReturnRate()));
        sb.append("Revenue: R$%.2f\n".formatted(s.getRevenueTotal()));
        sb.append("Categories: %s\n".formatted(String.join(", ", s.getCategories())));
        return sb.toString();
    }

    private String formatProduct(OlistProduct p) {
        var sb = new StringBuilder();
        sb.append("Product: %s\n".formatted(p.getProductId()));
        sb.append("Category: %s | Weight: %dg | Avg review: %.1f/5\n"
                .formatted(p.getCategory(), p.getWeightG(), p.getAvgReviewScore()));
        return sb.toString();
    }

    public record DeliveryStatus(String orderId, String estimated, String actual, long delayDays, boolean isLate) {}
}
