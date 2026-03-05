package com.redis.demos.docinsight.model;

import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import com.redis.om.spring.annotations.TagIndexed;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.List;

@Document(value = "Order", indexName = "orderIdx")
public class OlistOrder {
    @Id
    private String orderId;

    @TagIndexed
    private String customerId;

    @TagIndexed
    private String status;

    @Indexed(sortable = true)
    private LocalDateTime purchaseTimestamp;

    @Indexed(sortable = true)
    private LocalDateTime deliveredTimestamp;

    @Indexed(sortable = true)
    private LocalDateTime estimatedDelivery;

    @Indexed
    private List<OrderItem> items;

    @Indexed
    private Payment payment;

    @Indexed
    private Review review;

    @Indexed(sortable = true)
    private Double orderValue;

    public OlistOrder() {}

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getPurchaseTimestamp() { return purchaseTimestamp; }
    public void setPurchaseTimestamp(LocalDateTime purchaseTimestamp) { this.purchaseTimestamp = purchaseTimestamp; }
    public LocalDateTime getDeliveredTimestamp() { return deliveredTimestamp; }
    public void setDeliveredTimestamp(LocalDateTime deliveredTimestamp) { this.deliveredTimestamp = deliveredTimestamp; }
    public LocalDateTime getEstimatedDelivery() { return estimatedDelivery; }
    public void setEstimatedDelivery(LocalDateTime estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public Review getReview() { return review; }
    public void setReview(Review review) { this.review = review; }
    public Double getOrderValue() { return orderValue; }
    public void setOrderValue(Double orderValue) { this.orderValue = orderValue; }
}
