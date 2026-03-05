package com.redis.demos.docinsight.repository;

import com.redis.demos.docinsight.model.OlistProduct;
import com.redis.om.spring.repository.RedisDocumentRepository;

public interface ProductRepository extends RedisDocumentRepository<OlistProduct, String> {
}
