package org.billa.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.billa.dto.ProductDTO;
import org.billa.entities.Cart;
import org.billa.entities.CartItem;
import org.billa.repositories.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartService {
    private final CartRepository cartRepository;
    @Autowired
    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    CompletableFuture<String> future = new CompletableFuture<>();

    public List<Cart> getAllCarts() {
        try {
            List<Cart> carts = cartRepository.findAll();

// Extract product IDs from the order items
            Set<Long> productIds = carts.stream()
                    .flatMap(cart -> cart.getItems().stream())
                    .map(CartItem::getProductId)
                    .collect(Collectors.toSet());

// Send product IDs to Kafka to retrieve product details
            kafkaTemplate.send("product-list-detail-request", productIds.stream()
                    .map(String::valueOf) // Convert Long to String
                    .collect(Collectors.joining(",")));

// Wait for the product information from Kafka (JSON string)
            String js = future.get(10, TimeUnit.SECONDS);

// Deserialize the JSON string into a list of ProductDTO objects
            ObjectMapper objectMapper = new ObjectMapper();
            ProductDTO[] productArray = objectMapper.readValue(js, ProductDTO[].class);

// Create a map of ProductDTO objects by productId for easy lookup
            Map<Long, ProductDTO> productMap = Arrays.stream(productArray)
                    .collect(Collectors.toMap(ProductDTO::getProductId, product -> product));

// Associate each OrderItem with the corresponding ProductDTO
            for (Cart cart : carts) {
                for (CartItem item : cart.getItems()) {
                    ProductDTO product = productMap.get(item.getProductId());
                    if (product != null) {
                        // Assuming OrderItem has a method to set the product information
                        item.setProductDTO(product);
                    }
                }
            }

// Now the OrderItems have their corresponding ProductDTO embedded


            return carts;
        } catch (Exception e) {
            return null;
        }    }

    public Cart getCartById(Long id) {
        try {
            //List<Cart> carts = cartRepository.findAll();
            Cart cart = cartRepository.findById(id).orElse(null);

// Extract product IDs from the order items
            Set<Long> productIds = cart.getItems().stream()
                    .map(CartItem::getProductId)
                    .collect(Collectors.toSet());

// Send product IDs to Kafka to retrieve product details
            kafkaTemplate.send("product-list-detail-request", productIds.stream()
                    .map(String::valueOf) // Convert Long to String
                    .collect(Collectors.joining(",")));

// Wait for the product information from Kafka (JSON string)
            String js = future.get(10, TimeUnit.SECONDS);

// Deserialize the JSON string into a list of ProductDTO objects
            ObjectMapper objectMapper = new ObjectMapper();
            ProductDTO[] productArray = objectMapper.readValue(js, ProductDTO[].class);

// Create a map of ProductDTO objects by productId for easy lookup
            Map<Long, ProductDTO> productMap = Arrays.stream(productArray)
                    .collect(Collectors.toMap(ProductDTO::getProductId, product -> product));

// Associate each OrderItem with the corresponding ProductDTO
                for (CartItem item : cart.getItems()) {
                    ProductDTO product = productMap.get(item.getProductId());
                    if (product != null) {
                        // Assuming OrderItem has a method to set the product information
                        item.setProductDTO(product);
                    }
            }

// Now the OrderItems have their corresponding ProductDTO embedded


            return cart;
        } catch (Exception e) {
            return null;
        }     }

    public Cart createCart(Cart cart) {
        return cartRepository.save(cart);
    }

    public Cart updateCart(Long id, Cart cart) {
        Cart existingCart = getCartById(id);
        if (existingCart == null) {
            return null;
        }
        existingCart.setItems(cart.getItems());
        existingCart.setUpdatedAt(cart.getUpdatedAt());
        existingCart.setCreatedAt(cart.getCreatedAt());
        existingCart.setStatus(cart.getStatus());
        existingCart.setTotalPrice(cart.getTotalPrice());
        return cartRepository.save(existingCart);
    }

    public void deleteCart(Long id) {
        cartRepository.deleteById(id);
    }

    @KafkaListener(topics = "product-list-detail-response", groupId = "group-3")
    public void receiveProductListDetailsResponse(String json) {
        future.complete(json);
        System.out.println("json = " + json);
    }
}
