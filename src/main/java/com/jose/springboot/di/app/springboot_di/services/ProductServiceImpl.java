package com.jose.springboot.di.app.springboot_di.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jose.springboot.di.app.springboot_di.models.Product;
import com.jose.springboot.di.app.springboot_di.repositories.ProductRepository;

@Component
public class ProductServiceImpl implements ProductService{

    @Autowired
    private ProductRepository repository;

    @Override
    public List<Product> findAll(){
        return repository.findAll().stream().map(p -> {
            // clonamos el producto para no romper
            // el principio de inmutabilidad, implementando
            // la interfaz Cloneable en la clase y sobre escribiendo
            // el método clone()
            Product product = (Product) p.clone();
            product.setPrice((long)(p.getPrice()*1.25d));
            return product;
        }).collect(Collectors.toList());
    }

    @Override
    public Product findById(Long id){
        return repository.findById(id);
    }

}
