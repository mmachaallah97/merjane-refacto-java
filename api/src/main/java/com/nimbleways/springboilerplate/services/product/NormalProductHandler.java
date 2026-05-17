package com.nimbleways.springboilerplate.services.product;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import org.springframework.stereotype.Component;

@Component
public class NormalProductHandler implements ProductHandler {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public NormalProductHandler(ProductRepository productRepository, NotificationService notificationService) {
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    @Override
    public ProductType supportedType() {
        return ProductType.NORMAL;
    }

    @Override
    public void handle(Product product) {
        if (product.getAvailable() > 0) {
            decrementStock(product);
            return;
        }
        if (product.getLeadTime() > 0) {
            notificationService.sendDelayNotification(product.getLeadTime(), product.getName());
            productRepository.save(product);
        }
    }

    private void decrementStock(Product product) {
        product.setAvailable(product.getAvailable() - 1);
        productRepository.save(product);
    }
}
