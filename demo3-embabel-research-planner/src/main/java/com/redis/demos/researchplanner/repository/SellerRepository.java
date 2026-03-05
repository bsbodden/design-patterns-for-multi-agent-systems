package com.redis.demos.researchplanner.repository;

import com.redis.demos.researchplanner.model.OlistSeller;
import com.redis.om.spring.repository.RedisDocumentRepository;

public interface SellerRepository extends RedisDocumentRepository<OlistSeller, String> {
}
