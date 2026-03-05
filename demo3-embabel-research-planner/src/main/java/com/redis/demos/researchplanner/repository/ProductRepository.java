package com.redis.demos.researchplanner.repository;

import com.redis.demos.researchplanner.model.OlistProduct;
import com.redis.om.spring.repository.RedisDocumentRepository;

public interface ProductRepository extends RedisDocumentRepository<OlistProduct, String> {
}
