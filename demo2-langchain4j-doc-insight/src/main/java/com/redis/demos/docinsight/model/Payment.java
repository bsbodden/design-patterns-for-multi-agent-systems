package com.redis.demos.docinsight.model;

import com.redis.om.spring.annotations.Indexed;
import com.redis.om.spring.annotations.TagIndexed;

public class Payment {
    @TagIndexed
    private String type;

    @Indexed
    private Integer installments;

    @Indexed
    private Double value;

    public Payment() {}

    public Payment(String type, Integer installments, Double value) {
        this.type = type;
        this.installments = installments;
        this.value = value;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getInstallments() { return installments; }
    public void setInstallments(Integer installments) { this.installments = installments; }
    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }
}
