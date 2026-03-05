package com.redis.demos.researchplanner.model;

import com.redis.om.spring.annotations.Indexed;
import com.redis.om.spring.annotations.TagIndexed;

public class Payment {
    @TagIndexed private String type;
    @Indexed private Integer installments;
    @Indexed private Double value;

    public Payment() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getInstallments() { return installments; }
    public void setInstallments(Integer installments) { this.installments = installments; }
    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }
}
