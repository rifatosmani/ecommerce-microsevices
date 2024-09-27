package org.code.billa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDiscoveryClient
public class APIGatewayApp {
    public static void main(String[] args) {
        SpringApplication.run(APIGatewayApp.class, args);
    }

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(r -> r.path("/product/**")
                        .uri("lb://product-service"))
                .route(r -> r.path("/cart/**")
                        .uri("lb://cart-service"))
                .route(r -> r.path("/order/**")
                        .uri("lb://order-service"))
                .route(r -> r.path("/user/**")
                        .uri("lb://user-service"))
                .build();
    }
}