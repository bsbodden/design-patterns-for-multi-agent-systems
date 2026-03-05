package com.redis.demos.smarttriage.model;

import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import com.redis.om.spring.annotations.TagIndexed;
import org.springframework.data.annotation.Id;

@Document(value = "Product", indexName = "productIdx")
public class OlistProduct {
    @Id
    private String productId;

    @TagIndexed
    private String category;

    @Indexed
    private Integer weightG;

    @Indexed(sortable = true)
    private Double avgReviewScore;

    public OlistProduct() {}

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getWeightG() { return weightG; }
    public void setWeightG(Integer weightG) { this.weightG = weightG; }
    public Double getAvgReviewScore() { return avgReviewScore; }
    public void setAvgReviewScore(Double avgReviewScore) { this.avgReviewScore = avgReviewScore; }
}
