package com.jose.springboot.di.app.springboot_di.repositories;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import com.jose.springboot.di.app.springboot_di.models.Product;

@Primary
@Repository("productList")
public class ProductRepositoryImpl implements ProductRepository{

    private List <Product> data;

    public ProductRepositoryImpl() {

        data = Arrays.asList(
            new Product(1L, "RAM Memory", 200L),
            new Product(2L, "Keyboard Razer Mini 60%", 150L),
            new Product(3L, "CPU Intel Core i9", 350L),
            new Product(4L, "MotherBoard Gigabyte", 490L));
    }

    @Override
    public List<Product> findAll(){
        return data;
    }

    @Override
    public Product findById(Long id){
        return data.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
    }

    

}
