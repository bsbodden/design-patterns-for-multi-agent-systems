package com.redis.demos.smarttriage.repository;

import com.redis.demos.smarttriage.model.OlistOrder;
import com.redis.om.spring.repository.RedisDocumentRepository;

public interface OrderRepository extends RedisDocumentRepository<OlistOrder, String> {
    Iterable<OlistOrder> findByCustomerId(String customerId);
    Iterable<OlistOrder> findByStatus(String status);
}
