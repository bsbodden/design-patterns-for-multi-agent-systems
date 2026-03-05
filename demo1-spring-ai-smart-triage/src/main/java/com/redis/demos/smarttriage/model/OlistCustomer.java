package com.redis.demos.smarttriage.model;

import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import com.redis.om.spring.annotations.TagIndexed;
import org.springframework.data.annotation.Id;

@Document(value = "Customer", indexName = "customerIdx")
public class OlistCustomer {
    @Id
    private String customerId;

    @TagIndexed
    private String city;

    @TagIndexed
    private String state;

    @Indexed
    private Integer orderCount;

    @Indexed(sortable = true)
    private Double avgReviewScore;

    @Indexed(sortable = true)
    private Double totalSpent;

    public OlistCustomer() {}

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public Integer getOrderCount() { return orderCount; }
    public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }
    public Double getAvgReviewScore() { return avgReviewScore; }
    public void setAvgReviewScore(Double avgReviewScore) { this.avgReviewScore = avgReviewScore; }
    public Double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(Double totalSpent) { this.totalSpent = totalSpent; }
}
