package com.nimbleways.springboilerplate.services.product;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class SeasonalProductHandler implements ProductHandler {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public SeasonalProductHandler(ProductRepository productRepository, NotificationService notificationService) {
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    @Override
    public ProductType supportedType() {
        return ProductType.SEASONAL;
    }

    @Override
    public void handle(Product product) {
        LocalDate today = LocalDate.now();

        if (isInSeason(product, today) && product.getAvailable() > 0) {
            decrementStock(product);
            return;
        }

        if (leadTimeExceedsSeasonEnd(product, today)) {
            markAsUnavailable(product);
            return;
        }

        if (product.getSeasonStartDate().isAfter(today)) {
            notifyOutOfStock(product);
            return;
        }

        notifyDelay(product);
    }

    private boolean isInSeason(Product product, LocalDate today) {
        return today.isAfter(product.getSeasonStartDate()) && today.isBefore(product.getSeasonEndDate());
    }

    private boolean leadTimeExceedsSeasonEnd(Product product, LocalDate today) {
        return today.plusDays(product.getLeadTime()).isAfter(product.getSeasonEndDate());
    }

    private void decrementStock(Product product) {
        product.setAvailable(product.getAvailable() - 1);
        productRepository.save(product);
    }

    private void markAsUnavailable(Product product) {
        notificationService.sendOutOfStockNotification(product.getName());
        product.setAvailable(0);
        productRepository.save(product);
    }

    private void notifyOutOfStock(Product product) {
        notificationService.sendOutOfStockNotification(product.getName());
        productRepository.save(product);
    }

    private void notifyDelay(Product product) {
        notificationService.sendDelayNotification(product.getLeadTime(), product.getName());
        productRepository.save(product);
    }
}
