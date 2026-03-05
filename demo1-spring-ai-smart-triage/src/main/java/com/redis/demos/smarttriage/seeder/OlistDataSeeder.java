package com.redis.demos.smarttriage.seeder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.demos.smarttriage.model.OlistCustomer;
import com.redis.demos.smarttriage.model.OlistOrder;
import com.redis.demos.smarttriage.model.OlistProduct;
import com.redis.demos.smarttriage.model.OlistSeller;
import com.redis.demos.smarttriage.repository.CustomerRepository;
import com.redis.demos.smarttriage.repository.OrderRepository;
import com.redis.demos.smarttriage.repository.ProductRepository;
import com.redis.demos.smarttriage.repository.SellerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@Order(1)
public class OlistDataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(OlistDataSeeder.class);

    private final OrderRepository orderRepo;
    private final SellerRepository sellerRepo;
    private final CustomerRepository customerRepo;
    private final ProductRepository productRepo;
    private final ObjectMapper mapper;

    public OlistDataSeeder(OrderRepository orderRepo, SellerRepository sellerRepo,
                           CustomerRepository customerRepo, ProductRepository productRepo,
                           ObjectMapper mapper) {
        this.orderRepo = orderRepo;
        this.sellerRepo = sellerRepo;
        this.customerRepo = customerRepo;
        this.productRepo = productRepo;
        this.mapper = mapper;
    }

    @Override
    public void run(String... args) throws Exception {
        if (orderRepo.count() > 0) {
            log.info("Data already seeded — skipping. Orders: {}, Sellers: {}",
                    orderRepo.count(), sellerRepo.count());
            return;
        }

        log.info("Seeding Olist operational data...");

        loadAndSave("olist/curated-orders.json", OlistOrder.class, orderRepo);
        loadAndSave("olist/curated-sellers.json", OlistSeller.class, sellerRepo);
        loadAndSave("olist/curated-customers.json", OlistCustomer.class, customerRepo);
        loadAndSave("olist/curated-products.json", OlistProduct.class, productRepo);

        log.info("Seeded {} orders, {} sellers, {} customers, {} products",
                orderRepo.count(), sellerRepo.count(), customerRepo.count(), productRepo.count());
    }

    private <T> void loadAndSave(String path, Class<T> type,
                                  com.redis.om.spring.repository.RedisDocumentRepository<T, String> repo) throws Exception {
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            List<T> items = mapper.readValue(is,
                    mapper.getTypeFactory().constructCollectionType(List.class, type));
            repo.saveAll(items);
            log.info("  Loaded {} records from {}", items.size(), path);
        }
    }
}
