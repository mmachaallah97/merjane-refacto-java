package com.nimbleways.springboilerplate.controllers;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.implementations.NotificationService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MyControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Nested
    class NormalProducts {

        @Test
        void decrementsAvailableWhenInStock() throws Exception {
            Product product = saveProduct(new Product(null, 15, 30, ProductType.NORMAL, "USB Cable", null, null, null));

            processOrderFor(product);

            assertEquals(29, reload(product).getAvailable());
            Mockito.verifyNoInteractions(notificationService);
        }

        @Test
        void notifiesDelayWhenOutOfStockAndLeadTimePositive() throws Exception {
            Product product = saveProduct(new Product(null, 10, 0, ProductType.NORMAL, "USB Dongle", null, null, null));

            processOrderFor(product);

            assertEquals(0, reload(product).getAvailable());
            Mockito.verify(notificationService).sendDelayNotification(10, "USB Dongle");
            Mockito.verifyNoMoreInteractions(notificationService);
        }

        @Test
        void doesNothingWhenOutOfStockAndLeadTimeZero() throws Exception {
            Product product = saveProduct(new Product(null, 0, 0, ProductType.NORMAL, "Forgotten Cable", null, null, null));

            processOrderFor(product);

            assertEquals(0, reload(product).getAvailable());
            Mockito.verifyNoInteractions(notificationService);
        }
    }

    @Nested
    class SeasonalProducts {

        @Test
        void decrementsAvailableWhenInSeasonAndInStock() throws Exception {
            Product product = saveProduct(new Product(null, 15, 30, ProductType.SEASONAL, "Watermelon",
                    null, LocalDate.now().minusDays(2), LocalDate.now().plusDays(58)));

            processOrderFor(product);

            assertEquals(29, reload(product).getAvailable());
            Mockito.verifyNoInteractions(notificationService);
        }

        @Test
        void notifiesOutOfStockBeforeSeasonStarts() throws Exception {
            Product product = saveProduct(new Product(null, 15, 30, ProductType.SEASONAL, "Grapes",
                    null, LocalDate.now().plusDays(180), LocalDate.now().plusDays(240)));

            processOrderFor(product);

            assertEquals(30, reload(product).getAvailable());
            Mockito.verify(notificationService).sendOutOfStockNotification("Grapes");
            Mockito.verifyNoMoreInteractions(notificationService);
        }

        @Test
        void marksUnavailableWhenLeadTimeExceedsSeasonEnd() throws Exception {
            Product product = saveProduct(new Product(null, 30, 0, ProductType.SEASONAL, "Strawberry",
                    null, LocalDate.now().minusDays(10), LocalDate.now().plusDays(5)));

            processOrderFor(product);

            assertEquals(0, reload(product).getAvailable());
            Mockito.verify(notificationService).sendOutOfStockNotification("Strawberry");
            Mockito.verifyNoMoreInteractions(notificationService);
        }

        @Test
        void notifiesDelayWhenInSeasonOutOfStockAndLeadTimeFitsSeason() throws Exception {
            Product product = saveProduct(new Product(null, 5, 0, ProductType.SEASONAL, "Tomato",
                    null, LocalDate.now().minusDays(10), LocalDate.now().plusDays(30)));

            processOrderFor(product);

            Mockito.verify(notificationService).sendDelayNotification(5, "Tomato");
            Mockito.verifyNoMoreInteractions(notificationService);
        }
    }

    @Nested
    class ExpirableProducts {

        @Test
        void decrementsAvailableWhenNotExpiredAndInStock() throws Exception {
            Product product = saveProduct(new Product(null, 15, 30, ProductType.EXPIRABLE, "Butter",
                    LocalDate.now().plusDays(26), null, null));

            processOrderFor(product);

            assertEquals(29, reload(product).getAvailable());
            Mockito.verifyNoInteractions(notificationService);
        }

        @Test
        void notifiesExpirationAndZeroesAvailableWhenExpired() throws Exception {
            LocalDate expiry = LocalDate.now().minusDays(2);
            Product product = saveProduct(new Product(null, 90, 6, ProductType.EXPIRABLE, "Milk",
                    expiry, null, null));

            processOrderFor(product);

            assertEquals(0, reload(product).getAvailable());
            Mockito.verify(notificationService).sendExpirationNotification("Milk", expiry);
            Mockito.verifyNoMoreInteractions(notificationService);
        }

        @Test
        void notifiesExpirationWhenNotExpiredButOutOfStock() throws Exception {
            LocalDate expiry = LocalDate.now().plusDays(10);
            Product product = saveProduct(new Product(null, 5, 0, ProductType.EXPIRABLE, "Yogurt",
                    expiry, null, null));

            processOrderFor(product);

            assertEquals(0, reload(product).getAvailable());
            Mockito.verify(notificationService).sendExpirationNotification("Yogurt", expiry);
            Mockito.verifyNoMoreInteractions(notificationService);
        }
    }

    @Test
    void returnsNotFoundWhenOrderDoesNotExist() throws Exception {
        mockMvc.perform(post("/orders/{orderId}/processOrder", 999_999L)
                        .contentType("application/json"))
                .andExpect(status().isNotFound());
    }

    @Test
    void processesMixedOrderEndToEnd() throws Exception {
        Product usbCable = saveProduct(new Product(null, 15, 30, ProductType.NORMAL, "USB Cable", null, null, null));
        Product butter = saveProduct(new Product(null, 15, 30, ProductType.EXPIRABLE, "Butter",
                LocalDate.now().plusDays(26), null, null));
        Product watermelon = saveProduct(new Product(null, 15, 30, ProductType.SEASONAL, "Watermelon",
                null, LocalDate.now().minusDays(2), LocalDate.now().plusDays(58)));

        Order order = orderRepository.save(orderOf(usbCable, butter, watermelon));

        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId())
                        .contentType("application/json"))
                .andExpect(status().isOk());

        assertEquals(29, reload(usbCable).getAvailable());
        assertEquals(29, reload(butter).getAvailable());
        assertEquals(29, reload(watermelon).getAvailable());
        Mockito.verifyNoInteractions(notificationService);
    }

    private Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    private Product reload(Product product) {
        return productRepository.findById(product.getId()).orElseThrow();
    }

    private void processOrderFor(Product product) throws Exception {
        Order order = orderRepository.save(orderOf(product));
        mockMvc.perform(post("/orders/{orderId}/processOrder", order.getId())
                        .contentType("application/json"))
                .andExpect(status().isOk());
    }

    private static Order orderOf(Product... products) {
        Order order = new Order();
        Set<Product> items = new HashSet<>();
        for (Product p : products) {
            items.add(p);
        }
        order.setItems(items);
        return order;
    }
}

