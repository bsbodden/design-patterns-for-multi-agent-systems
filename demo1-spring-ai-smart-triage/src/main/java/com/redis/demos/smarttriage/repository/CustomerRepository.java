package com.redis.demos.smarttriage.repository;

import com.redis.demos.smarttriage.model.OlistCustomer;
import com.redis.om.spring.repository.RedisDocumentRepository;

public interface CustomerRepository extends RedisDocumentRepository<OlistCustomer, String> {
}
