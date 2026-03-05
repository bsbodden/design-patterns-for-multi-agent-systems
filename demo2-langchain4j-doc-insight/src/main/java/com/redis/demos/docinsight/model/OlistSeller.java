package com.redis.demos.docinsight.model;

import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import com.redis.om.spring.annotations.TagIndexed;
import org.springframework.data.annotation.Id;

import java.util.List;

@Document(value = "Seller", indexName = "sellerIdx")
public class OlistSeller {
    @Id
    private String sellerId;

    @TagIndexed
    private String city;

    @TagIndexed
    private String state;

    @Indexed(sortable = true)
    private Double avgRating;

    @Indexed(sortable = true)
    private Double onTimePct;

    @Indexed(sortable = true)
    private Integer totalOrders;

    @Indexed
    private Double delayAvgDays;

    @Indexed
    private Double returnRate;

    @Indexed(sortable = true)
    private Double revenueTotal;

    @TagIndexed
    private List<String> categories;

    public OlistSeller() {}

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public Double getAvgRating() { return avgRating; }
    public void setAvgRating(Double avgRating) { this.avgRating = avgRating; }
    public Double getOnTimePct() { return onTimePct; }
    public void setOnTimePct(Double onTimePct) { this.onTimePct = onTimePct; }
    public Integer getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Integer totalOrders) { this.totalOrders = totalOrders; }
    public Double getDelayAvgDays() { return delayAvgDays; }
    public void setDelayAvgDays(Double delayAvgDays) { this.delayAvgDays = delayAvgDays; }
    public Double getReturnRate() { return returnRate; }
    public void setReturnRate(Double returnRate) { this.returnRate = returnRate; }
    public Double getRevenueTotal() { return revenueTotal; }
    public void setRevenueTotal(Double revenueTotal) { this.revenueTotal = revenueTotal; }
    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }
}
