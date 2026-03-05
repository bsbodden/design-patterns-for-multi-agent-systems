package com.redis.demos.researchplanner.repository;

import com.redis.demos.researchplanner.model.OlistOrder;
import com.redis.om.spring.repository.RedisDocumentRepository;

public interface OrderRepository extends RedisDocumentRepository<OlistOrder, String> {
    Iterable<OlistOrder> findByCustomerId(String customerId);
    Iterable<OlistOrder> findByStatus(String status);
}
