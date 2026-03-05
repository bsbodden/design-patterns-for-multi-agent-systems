package com.redis.demos.researchplanner.model;

import com.redis.om.spring.annotations.Indexed;
import com.redis.om.spring.annotations.Searchable;

public class Review {
    @Indexed private Integer score;
    @Searchable private String comment;

    public Review() {}

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
