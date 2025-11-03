package com.jose.springboot.di.app.springboot_di.repositories;

import java.util.Collections;

import org.springframework.stereotype.Repository;

import com.jose.springboot.di.app.springboot_di.models.Product;

@Repository
public class ProductRepositoryFoo implements ProductRepository{

    @Override
    public java.util.List<Product> findAll() {
        return Collections.singletonList(new Product(1L, "Monitor Asus 27", 600L));
    }

    @Override
    public Product findById(Long id) {
        return new Product(id, "Monitor Asus 27", 600L);
    }

}
