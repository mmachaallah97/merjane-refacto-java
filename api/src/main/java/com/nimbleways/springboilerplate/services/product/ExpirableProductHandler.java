package com.nimbleways.springboilerplate.services.product;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ExpirableProductHandler implements ProductHandler {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public ExpirableProductHandler(ProductRepository productRepository, NotificationService notificationService) {
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    @Override
    public ProductType supportedType() {
        return ProductType.EXPIRABLE;
    }

    @Override
    public void handle(Product product) {
        if (product.getAvailable() > 0 && !isExpired(product)) {
            decrementStock(product);
            return;
        }
        notificationService.sendExpirationNotification(product.getName(), product.getExpiryDate());
        product.setAvailable(0);
        productRepository.save(product);
    }

    private boolean isExpired(Product product) {
        return !product.getExpiryDate().isAfter(LocalDate.now());
    }

    private void decrementStock(Product product) {
        product.setAvailable(product.getAvailable() - 1);
        productRepository.save(product);
    }
}
