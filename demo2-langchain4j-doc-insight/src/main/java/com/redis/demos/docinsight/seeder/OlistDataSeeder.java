package com.redis.demos.docinsight.seeder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.demos.docinsight.model.OlistCustomer;
import com.redis.demos.docinsight.model.OlistOrder;
import com.redis.demos.docinsight.model.OlistProduct;
import com.redis.demos.docinsight.model.OlistSeller;
import com.redis.demos.docinsight.repository.CustomerRepository;
import com.redis.demos.docinsight.repository.OrderRepository;
import com.redis.demos.docinsight.repository.ProductRepository;
import com.redis.demos.docinsight.repository.SellerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class OlistDataSeeder {
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

    @EventListener(ApplicationReadyEvent.class)
    public void seed() throws Exception {
        try {
            if (orderRepo.count() > 0) {
                log.info("Data already seeded — skipping. Orders: {}, Sellers: {}",
                        orderRepo.count(), sellerRepo.count());
                return;
            }
        } catch (Exception e) {
            log.info("Index not ready yet, proceeding with seed: {}", e.getMessage());
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
