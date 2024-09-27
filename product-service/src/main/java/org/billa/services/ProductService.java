package org.billa.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.billa.entities.Product;
import org.billa.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product product) {
        Product existingProduct = getProductById(id);
        if (existingProduct == null) {
            return null;
        }
        existingProduct.setName(product.getName());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setUpdatedAt(product.getUpdatedAt());
        existingProduct.setCreatedAt(product.getCreatedAt());
        existingProduct.setImageUrl(product.getImageUrl());
        existingProduct.setStockQuantity(product.getStockQuantity());
        return productRepository.save(existingProduct);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    @KafkaListener(topics = "product-list-detail-request", groupId = "group-3")
    public void receiveProductListDetailsRequest(Set<String> productIds) throws JsonProcessingException {
        // Retrieve the product details from the database using JPA or Hibernate
        List<Product> products = new ArrayList<Product>();

        for(String productId: productIds){
            Product product = this.getProductById(Long.valueOf(productId));
            products.add(product);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(products);

        // Send the product details back to the order service using Kafka
        kafkaTemplate.send("product-list-detail-response", json);


    }
}
