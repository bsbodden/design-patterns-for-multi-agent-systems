package com.redis.demos.researchplanner.service;

import com.redis.demos.researchplanner.model.OlistOrder;
import com.redis.demos.researchplanner.model.OlistSeller;
import com.redis.demos.researchplanner.repository.OrderRepository;
import com.redis.demos.researchplanner.repository.SellerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.StreamSupport;

@Service
public class OlistDataService {

    private final SellerRepository sellerRepo;
    private final OrderRepository orderRepo;

    public OlistDataService(SellerRepository sellerRepo, OrderRepository orderRepo) {
        this.sellerRepo = sellerRepo;
        this.orderRepo = orderRepo;
    }

    public OlistSeller getSellerMetrics(String sellerId) {
        return sellerRepo.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found: " + sellerId));
    }

    public List<OlistOrder> getAffectedOrders(String sellerId) {
        var allOrders = StreamSupport.stream(orderRepo.findAll().spliterator(), false).toList();
        return allOrders.stream()
                .filter(o -> o.getItems() != null && o.getItems().stream()
                        .anyMatch(item -> sellerId.equals(item.getSellerId())))
                .toList();
    }

    public List<OlistSeller> getSellersInRegion(String state) {
        var allSellers = StreamSupport.stream(sellerRepo.findAll().spliterator(), false).toList();
        return allSellers.stream()
                .filter(s -> state.equalsIgnoreCase(s.getState()))
                .toList();
    }

    public String formatSellerSummary(OlistSeller seller) {
        return """
                Seller: %s (%s, %s)
                Rating: %.1f/5 | On-time: %.0f%% | Total orders: %d
                Avg delay: %.1f days | Return rate: %.1f%%
                Revenue: R$%.2f
                Categories: %s
                """.formatted(
                seller.getSellerId(), seller.getCity(), seller.getState(),
                seller.getAvgRating(), seller.getOnTimePct(), seller.getTotalOrders(),
                seller.getDelayAvgDays(), seller.getReturnRate(),
                seller.getRevenueTotal(),
                String.join(", ", seller.getCategories()));
    }

    public String formatAffectedOrdersSummary(List<OlistOrder> orders) {
        double totalValue = orders.stream()
                .mapToDouble(o -> o.getOrderValue() != null ? o.getOrderValue() : 0.0)
                .sum();
        long lateOrders = orders.stream()
                .filter(o -> o.getDeliveredTimestamp() != null && o.getEstimatedDelivery() != null
                        && o.getDeliveredTimestamp().isAfter(o.getEstimatedDelivery()))
                .count();

        return """
                Affected orders: %d
                Total value: R$%.2f
                Late deliveries: %d (%.0f%%)
                """.formatted(orders.size(), totalValue, lateOrders,
                orders.isEmpty() ? 0 : (lateOrders * 100.0) / orders.size());
    }
}
