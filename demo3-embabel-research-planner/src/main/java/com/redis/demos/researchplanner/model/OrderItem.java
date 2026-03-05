package com.redis.demos.researchplanner.model;

import com.redis.om.spring.annotations.Indexed;
import com.redis.om.spring.annotations.TagIndexed;

public class OrderItem {
    @TagIndexed private String sellerId;
    @TagIndexed private String productId;
    @Indexed private Double price;
    @Indexed private Double freightValue;

    public OrderItem() {}

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Double getFreightValue() { return freightValue; }
    public void setFreightValue(Double freightValue) { this.freightValue = freightValue; }
}
