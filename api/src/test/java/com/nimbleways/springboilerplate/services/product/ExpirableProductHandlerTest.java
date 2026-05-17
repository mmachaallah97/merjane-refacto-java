package com.nimbleways.springboilerplate.services.product;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@UnitTest
class ExpirableProductHandlerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ExpirableProductHandler handler;

    @Test
    void decrementsStockWhenNotExpiredAndAvailable() {
        Product product = new Product(null, 5, 10, ProductType.EXPIRABLE, "Butter",
                LocalDate.now().plusDays(20), null, null);

        handler.handle(product);

        assertEquals(9, product.getAvailable());
        Mockito.verify(productRepository).save(product);
        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    void notifiesExpirationAndZeroesAvailableWhenExpired() {
        LocalDate expiry = LocalDate.now().minusDays(1);
        Product product = new Product(null, 5, 10, ProductType.EXPIRABLE, "Milk",
                expiry, null, null);

        handler.handle(product);

        assertEquals(0, product.getAvailable());
        Mockito.verify(notificationService).sendExpirationNotification("Milk", expiry);
        Mockito.verify(productRepository).save(product);
    }

    @Test
    void notifiesExpirationWhenAvailableIsZeroEvenIfNotExpired() {
        LocalDate expiry = LocalDate.now().plusDays(10);
        Product product = new Product(null, 5, 0, ProductType.EXPIRABLE, "Yogurt",
                expiry, null, null);

        handler.handle(product);

        assertEquals(0, product.getAvailable());
        Mockito.verify(notificationService).sendExpirationNotification("Yogurt", expiry);
        Mockito.verify(productRepository).save(product);
    }
}
