package com.redis.demos.smarttriage.repository;

import com.redis.demos.smarttriage.model.OlistProduct;
import com.redis.om.spring.repository.RedisDocumentRepository;

public interface ProductRepository extends RedisDocumentRepository<OlistProduct, String> {
}
