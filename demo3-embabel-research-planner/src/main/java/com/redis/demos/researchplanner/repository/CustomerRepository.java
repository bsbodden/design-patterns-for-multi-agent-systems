package com.redis.demos.researchplanner.repository;

import com.redis.demos.researchplanner.model.OlistCustomer;
import com.redis.om.spring.repository.RedisDocumentRepository;

public interface CustomerRepository extends RedisDocumentRepository<OlistCustomer, String> {
}
