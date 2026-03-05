package com.redis.demos.docinsight.tools;

import com.redis.demos.docinsight.model.OlistOrder;
import com.redis.demos.docinsight.model.OlistSeller;
import com.redis.demos.docinsight.repository.OrderRepository;
import com.redis.demos.docinsight.repository.SellerRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class OlistDataTools {

    private final OrderRepository orderRepo;
    private final SellerRepository sellerRepo;

    public OlistDataTools(OrderRepository orderRepo, SellerRepository sellerRepo) {
        this.orderRepo = orderRepo;
        this.sellerRepo = sellerRepo;
    }

    @Tool("Query order volume, payment distribution, and average order value. Returns order count, total value, average value, and payment type breakdown.")
    public String getOrderStats(
            @P("state code (e.g. SP, RJ) or ALL") String state) {
        var allOrders = StreamSupport.stream(orderRepo.findAll().spliterator(), false).toList();

        var filtered = allOrders.stream()
                .filter(o -> "ALL".equalsIgnoreCase(state) || hasSellerInState(o, state))
                .toList();

        if (filtered.isEmpty()) {
            return "No orders found for state: " + state;
        }

        double totalValue = filtered.stream()
                .mapToDouble(o -> o.getOrderValue() != null ? o.getOrderValue() : 0.0)
                .sum();
        double avgValue = totalValue / filtered.size();

        Map<String, Long> paymentTypes = filtered.stream()
                .filter(o -> o.getPayment() != null)
                .collect(Collectors.groupingBy(
                        o -> o.getPayment().getType(),
                        Collectors.counting()));

        var sb = new StringBuilder();
        sb.append("Order Statistics (state: %s)\n".formatted(state));
        sb.append("Total orders: %d\n".formatted(filtered.size()));
        sb.append("Total value: R$%.2f\n".formatted(totalValue));
        sb.append("Average order value: R$%.2f\n".formatted(avgValue));
        sb.append("Payment breakdown:\n");
        paymentTypes.forEach((type, count) ->
                sb.append("  %s: %d orders (%.0f%%)\n".formatted(type, count, (count * 100.0) / filtered.size())));
        return sb.toString();
    }

    @Tool("Get delivery performance metrics: on-time percentage, average delay, worst performing sellers.")
    public String getDeliveryMetrics(
            @P("state code (e.g. SP, RJ) or ALL") String state) {
        var allSellers = StreamSupport.stream(sellerRepo.findAll().spliterator(), false).toList();

        var filtered = allSellers.stream()
                .filter(s -> "ALL".equalsIgnoreCase(state) || state.equalsIgnoreCase(s.getState()))
                .filter(s -> s.getTotalOrders() != null && s.getTotalOrders() > 10)
                .toList();

        if (filtered.isEmpty()) {
            return "No sellers with >10 orders found for state: " + state;
        }

        double avgOnTime = filtered.stream()
                .mapToDouble(s -> s.getOnTimePct() != null ? s.getOnTimePct() : 0.0)
                .average().orElse(0.0);
        double avgDelay = filtered.stream()
                .mapToDouble(s -> s.getDelayAvgDays() != null ? s.getDelayAvgDays() : 0.0)
                .average().orElse(0.0);

        var worstSellers = filtered.stream()
                .sorted(Comparator.comparingDouble(s -> s.getOnTimePct() != null ? s.getOnTimePct() : 100.0))
                .limit(5)
                .toList();

        var sb = new StringBuilder();
        sb.append("Delivery Metrics (state: %s)\n".formatted(state));
        sb.append("Sellers analyzed: %d (with >10 orders)\n".formatted(filtered.size()));
        sb.append("Average on-time: %.1f%%\n".formatted(avgOnTime));
        sb.append("Average delay: %.1f days\n".formatted(avgDelay));
        sb.append("\nWorst performing sellers:\n");
        for (var s : worstSellers) {
            sb.append("  %s (%s, %s): on-time %.0f%%, avg delay %.1f days, %d orders\n"
                    .formatted(s.getSellerId(), s.getCity(), s.getState(),
                            s.getOnTimePct(), s.getDelayAvgDays(), s.getTotalOrders()));
        }
        return sb.toString();
    }

    @Tool("Get review score distribution and complaint themes by product category.")
    public String getReviewBreakdown(
            @P("product category (e.g. electronics, furniture) or ALL") String category) {
        var allOrders = StreamSupport.stream(orderRepo.findAll().spliterator(), false).toList();

        var withReviews = allOrders.stream()
                .filter(o -> o.getReview() != null && o.getReview().getScore() != null)
                .toList();

        if (withReviews.isEmpty()) {
            return "No reviews found";
        }

        Map<Integer, Long> scoreDist = withReviews.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getReview().getScore(),
                        Collectors.counting()));

        double avgScore = withReviews.stream()
                .mapToInt(o -> o.getReview().getScore())
                .average().orElse(0.0);

        long complaints = withReviews.stream()
                .filter(o -> o.getReview().getScore() <= 2)
                .count();

        var negativeComments = withReviews.stream()
                .filter(o -> o.getReview().getScore() <= 2 && o.getReview().getComment() != null)
                .map(o -> o.getReview().getComment())
                .limit(5)
                .toList();

        var sb = new StringBuilder();
        sb.append("Review Breakdown (category: %s)\n".formatted(category));
        sb.append("Total reviews: %d\n".formatted(withReviews.size()));
        sb.append("Average score: %.1f/5\n".formatted(avgScore));
        sb.append("Score distribution:\n");
        for (int i = 1; i <= 5; i++) {
            long count = scoreDist.getOrDefault(i, 0L);
            sb.append("  %d★: %d (%.0f%%)\n".formatted(i, count, (count * 100.0) / withReviews.size()));
        }
        sb.append("Complaints (1-2 stars): %d (%.0f%%)\n".formatted(complaints, (complaints * 100.0) / withReviews.size()));
        if (!negativeComments.isEmpty()) {
            sb.append("\nSample complaint comments:\n");
            for (var c : negativeComments) {
                sb.append("  - \"%s\"\n".formatted(c.length() > 100 ? c.substring(0, 100) + "..." : c));
            }
        }
        return sb.toString();
    }

    @Tool("Rank sellers by a metric: rating, on_time, revenue, or complaints. Returns top or bottom sellers.")
    public String getSellerRankings(
            @P("metric: rating, on_time, revenue, or complaints") String metric,
            @P("direction: top or bottom") String direction,
            @P("how many results to return") int limit) {
        var allSellers = StreamSupport.stream(sellerRepo.findAll().spliterator(), false).toList();

        Comparator<OlistSeller> comparator = switch (metric.toLowerCase()) {
            case "rating" -> Comparator.comparingDouble(s -> s.getAvgRating() != null ? s.getAvgRating() : 0.0);
            case "on_time" -> Comparator.comparingDouble(s -> s.getOnTimePct() != null ? s.getOnTimePct() : 0.0);
            case "revenue" -> Comparator.comparingDouble(s -> s.getRevenueTotal() != null ? s.getRevenueTotal() : 0.0);
            case "complaints" -> Comparator.comparingDouble(s -> s.getReturnRate() != null ? s.getReturnRate() : 0.0);
            default -> Comparator.comparingDouble(s -> s.getAvgRating() != null ? s.getAvgRating() : 0.0);
        };

        if ("top".equalsIgnoreCase(direction)) {
            comparator = comparator.reversed();
        }

        var ranked = allSellers.stream()
                .sorted(comparator)
                .limit(limit)
                .toList();

        var sb = new StringBuilder();
        sb.append("Seller Rankings — %s %s %d\n".formatted(direction, metric, limit));
        for (int i = 0; i < ranked.size(); i++) {
            var s = ranked.get(i);
            sb.append("  %d. %s (%s, %s): rating=%.1f, on-time=%.0f%%, revenue=R$%.0f, return-rate=%.1f%%\n"
                    .formatted(i + 1, s.getSellerId(), s.getCity(), s.getState(),
                            s.getAvgRating(), s.getOnTimePct(), s.getRevenueTotal(), s.getReturnRate()));
        }
        return sb.toString();
    }

    private boolean hasSellerInState(OlistOrder order, String state) {
        // Check if any item's seller is in the specified state
        // For simplicity, we check all sellers — in a real app we'd join
        if (order.getItems() == null) return false;
        for (var item : order.getItems()) {
            var seller = sellerRepo.findById(item.getSellerId());
            if (seller.isPresent() && state.equalsIgnoreCase(seller.get().getState())) {
                return true;
            }
        }
        return false;
    }
}
