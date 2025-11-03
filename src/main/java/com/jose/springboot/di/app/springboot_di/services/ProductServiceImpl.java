package com.jose.springboot.di.app.springboot_di.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.jose.springboot.di.app.springboot_di.models.Product;
import com.jose.springboot.di.app.springboot_di.repositories.ProductRepository;

@Service
public class ProductServiceImpl implements ProductService{

    // @Autowired
    private Environment environment;

    // @Autowired // Inyección de dependencia vía atributo
    // @Qualifier("productList") // nombre del bean a inyectar
    private ProductRepository repository;
    
    // Inyección de dependencia vía constructor, no hace falta el @Autowired
    // productRepositoryImpl es el nombre del bean que inyectamos, si no se pone
    // se inyecta el que tenga la anotación @Primary
    public ProductServiceImpl(@Qualifier("productJson") ProductRepository repository, Environment environment) {
        this.repository = repository;
        this.environment = environment; 
    }

    @Override
    public List<Product> findAll(){
        return repository.findAll().stream().map(p -> {
            // clonamos el producto para no romper
            // el principio de inmutabilidad, implementando
            // la interfaz Cloneable en la clase y sobre escribiendo
            // el método clone()
            Product product = (Product) p.clone();
            product.setPrice((long)(p.getPrice()*environment.getProperty("config.price.tax", Double.class)));
            return product;
        }).collect(Collectors.toList());
    }

    

    @Override
    public Product findById(Long id){
        return repository.findById(id);
    }

}
