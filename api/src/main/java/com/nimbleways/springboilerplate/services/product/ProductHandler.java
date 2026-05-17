package com.nimbleways.springboilerplate.services.product;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;

public interface ProductHandler {
    ProductType supportedType();

    void handle(Product product);
}
