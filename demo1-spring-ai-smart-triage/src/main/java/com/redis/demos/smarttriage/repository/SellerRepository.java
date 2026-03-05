package com.redis.demos.smarttriage.repository;

import com.redis.demos.smarttriage.model.OlistSeller;
import com.redis.om.spring.repository.RedisDocumentRepository;

public interface SellerRepository extends RedisDocumentRepository<OlistSeller, String> {
}
