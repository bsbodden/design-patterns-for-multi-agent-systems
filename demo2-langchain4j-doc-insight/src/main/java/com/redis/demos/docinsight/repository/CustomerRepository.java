package com.redis.demos.docinsight.repository;

import com.redis.demos.docinsight.model.OlistCustomer;
import com.redis.om.spring.repository.RedisDocumentRepository;

public interface CustomerRepository extends RedisDocumentRepository<OlistCustomer, String> {
}
