package com.redis.demos.smarttriage.web;

import com.redis.demos.smarttriage.repository.CustomerRepository;
import com.redis.demos.smarttriage.repository.OrderRepository;
import com.redis.demos.smarttriage.repository.ProductRepository;
import com.redis.demos.smarttriage.repository.SellerRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class DataExplorerController {

    private final OrderRepository orderRepository;
    private final SellerRepository sellerRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public DataExplorerController(OrderRepository orderRepository,
                                  SellerRepository sellerRepository,
                                  CustomerRepository customerRepository,
                                  ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.sellerRepository = sellerRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/all")
    public Map<String, Object> getAllData() {
        var orders = orderRepository.findAll();
        var sellers = sellerRepository.findAll();
        var customers = customerRepository.findAll();
        var products = productRepository.findAll();

        var result = new HashMap<String, Object>();
        result.put("orders", orders);
        result.put("sellers", sellers);
        result.put("customers", customers);
        result.put("products", products);
        result.put("counts", Map.of(
                "orders", orderRepository.count(),
                "sellers", sellerRepository.count(),
                "customers", customerRepository.count(),
                "products", productRepository.count()
        ));
        return result;
    }
}
