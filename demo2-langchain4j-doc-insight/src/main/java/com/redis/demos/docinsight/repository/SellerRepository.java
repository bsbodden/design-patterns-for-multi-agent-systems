package com.redis.demos.docinsight.repository;

import com.redis.demos.docinsight.model.OlistSeller;
import com.redis.om.spring.repository.RedisDocumentRepository;

public interface SellerRepository extends RedisDocumentRepository<OlistSeller, String> {
}
