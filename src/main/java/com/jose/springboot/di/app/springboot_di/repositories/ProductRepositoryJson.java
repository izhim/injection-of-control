package com.jose.springboot.di.app.springboot_di.repositories;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jose.springboot.di.app.springboot_di.models.Product;

public class ProductRepositoryJson implements ProductRepository {

    private List<Product> list;

    @Value("classpath:product.json")
    private Resource resource;

    public ProductRepositoryJson(Resource resource) {
        readValueJson(resource);
    }

    private void readValueJson(Resource resource) {
        ObjectMapper mapper = new ObjectMapper();   
        try {
            list = Arrays.asList(mapper.readValue(resource.getInputStream(), Product[].class));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public List<Product> findAll() {
        return list;
    }

    @Override
    public Product findById(Long id) {
        return list.stream()
                   .filter(p -> p.getId().equals(id))
                   .findFirst()
                   .orElseThrow();
    }

}
