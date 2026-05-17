package com.nimbleways.springboilerplate.services;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.exceptions.OrderNotFoundException;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.services.product.ProductHandlerRegistry;
import org.springframework.stereotype.Service;

@Service
public class OrderProcessingService {

    private final OrderRepository orderRepository;
    private final ProductHandlerRegistry handlerRegistry;

    public OrderProcessingService(OrderRepository orderRepository, ProductHandlerRegistry handlerRegistry) {
        this.orderRepository = orderRepository;
        this.handlerRegistry = handlerRegistry;
    }

    public Order processOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        for (Product product : order.getItems()) {
            handlerRegistry.handlerFor(product.getType()).handle(product);
        }
        return order;
    }
}
