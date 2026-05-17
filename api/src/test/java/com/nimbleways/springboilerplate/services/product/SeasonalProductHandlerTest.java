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
class SeasonalProductHandlerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private SeasonalProductHandler handler;

    @Test
    void decrementsStockWhenInSeasonAndAvailable() {
        Product product = new Product(null, 5, 10, ProductType.SEASONAL, "Watermelon",
                null, LocalDate.now().minusDays(2), LocalDate.now().plusDays(30));

        handler.handle(product);

        assertEquals(9, product.getAvailable());
        Mockito.verify(productRepository).save(product);
        Mockito.verifyNoInteractions(notificationService);
    }

    @Test
    void marksUnavailableWhenLeadTimeExceedsSeasonEnd() {
        Product product = new Product(null, 30, 0, ProductType.SEASONAL, "Strawberry",
                null, LocalDate.now().minusDays(10), LocalDate.now().plusDays(5));

        handler.handle(product);

        assertEquals(0, product.getAvailable());
        Mockito.verify(notificationService).sendOutOfStockNotification("Strawberry");
        Mockito.verify(productRepository).save(product);
    }

    @Test
    void notifiesOutOfStockBeforeSeasonStarts() {
        Product product = new Product(null, 5, 10, ProductType.SEASONAL, "Grapes",
                null, LocalDate.now().plusDays(60), LocalDate.now().plusDays(120));

        handler.handle(product);

        assertEquals(10, product.getAvailable());
        Mockito.verify(notificationService).sendOutOfStockNotification("Grapes");
        Mockito.verify(productRepository).save(product);
    }

    @Test
    void notifiesDelayWhenInSeasonButOutOfStockAndLeadTimeFits() {
        Product product = new Product(null, 5, 0, ProductType.SEASONAL, "Tomato",
                null, LocalDate.now().minusDays(10), LocalDate.now().plusDays(30));

        handler.handle(product);

        Mockito.verify(notificationService).sendDelayNotification(5, "Tomato");
        Mockito.verify(productRepository).save(product);
    }
}
