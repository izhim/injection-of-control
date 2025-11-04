# Spring Boot Dependency Injection

This section of my Spring Boot learning journey focuses on **Dependency Injection (DI)**. I’ll explain how it works, why it’s useful, and how I implemented it in this project using different approaches and annotations like `@Autowired`, `@Qualifier`, `@Primary`, and custom configuration classes.

---

## 🧩 What is Dependency Injection?

**Dependency Injection** is a design pattern that allows Spring to *automatically provide* the dependencies (objects) that a class needs, rather than the class creating them manually.
This helps us **decouple** components, improve testability, and make our code more flexible and maintainable.

In short: *you don’t create the dependencies; Spring gives them to you.*

---

## 🏗 Step 1 – Model Creation

First, I created a simple model called `Product` (not shown here for brevity) representing products with fields like `id`, `name`, and `price`.
The model implements `Cloneable` so we can safely clone objects later in the service layer without breaking immutability principles.

---

## 📦 Step 2 – Simulating a Database Repository (No DI Yet)

Before introducing dependency injection, I manually created a repository that simulates database access:

```java
@Repository("productList")
@Primary
public class ProductRepositoryImpl implements ProductRepository {
    private List<Product> data;

    public ProductRepositoryImpl() {
        data = Arrays.asList(
            new Product(1L, "RAM Memory", 200L),
            new Product(2L, "Keyboard Razer Mini 60%", 150L),
            new Product(3L, "CPU Intel Core i9", 350L),
            new Product(4L, "MotherBoard Gigabyte", 490L)
        );
    }

    @Override
    public List<Product> findAll() { return data; }

    @Override
    public Product findById(Long id) {
        return data.stream()
                   .filter(p -> p.getId().equals(id))
                   .findFirst()
                   .orElse(null);
    }
}
```

At this stage, if we wanted to use this repository, we’d have to instantiate it manually inside another class — which tightly couples our code.

---

## 🔗 Step 3 – Decoupling with Interfaces

To avoid tight coupling, I defined two interfaces:

```java
public interface ProductRepository {
    List<Product> findAll();
    Product findById(Long id);
}

public interface ProductService {
    List<Product> findAll();
    Product findById(Long id);
}
```

By programming against **interfaces** instead of concrete implementations, we make our code flexible — we can easily switch to a different repository later (for example, one that reads from a JSON file or a real database) without changing the service or controller logic.

---

## ⚙️ Step 4 – Introducing Dependency Injection

Here’s where the magic starts.
Spring can automatically “inject” a repository into our service class:

```java
@Service
public class ProductServiceImpl implements ProductService {

    private ProductRepository repository;
    private Environment environment;

    // Constructor Injection (recommended)
    public ProductServiceImpl(@Qualifier("productJson") ProductRepository repository,
                              Environment environment) {
        this.repository = repository;
        this.environment = environment;
    }

    @Override
    public List<Product> findAll() {
        return repository.findAll().stream()
            .map(p -> {
                Product product = (Product) p.clone();
                product.setPrice((long)(p.getPrice() *
                        environment.getProperty("config.price.tax", Double.class)));
                return product;
            })
            .collect(Collectors.toList());
    }

    @Override
    public Product findById(Long id) {
        return repository.findById(id);
    }
}
```

### 💡 Types of Dependency Injection in Spring

1. **Field Injection**

   ```java
   @Autowired
   private ProductService service;
   ```

2. **Setter Injection**

   ```java
   @Autowired
   public void setRepository(ProductRepository repository) { ... }
   ```

3. **Constructor Injection (Recommended)**
   The preferred approach since it makes dependencies immutable and easier to test.

---

## 🧠 Step 5 – Understanding Bean Scopes

By default, Spring beans follow the **Singleton pattern** — only one instance of each bean is created and shared across the application.

However, Spring also supports other scopes:

* `@RequestScope`: creates a new bean instance for each HTTP request.
* `@SessionScope`: creates one bean instance per user session.

Example:

```java
@Service
@RequestScope
public class RequestScopedService { ... }
```

This can be useful for storing temporary user data or context-sensitive information.

---

## 🎯 Step 6 – Using @Primary and @Qualifier

When multiple beans implement the same interface, Spring won’t know which one to inject.
To handle this, we can:

* Mark one bean as the default with `@Primary`.
* Or specify the exact bean with `@Qualifier`.

```java
@Primary
@Repository("productList")
public class ProductRepositoryImpl implements ProductRepository { ... }

@Repository("productJson")
public class ProductRepositoryJson implements ProductRepository { ... }

@Service
public class ProductServiceImpl implements ProductService {
    public ProductServiceImpl(@Qualifier("productJson") ProductRepository repository) { ... }
}
```

In this example, even though `ProductRepositoryImpl` is marked as `@Primary`, we override that by explicitly injecting `productJson`.

---

## 🗂 Step 7 – Reading Data from a JSON File with AppConfig

I added a new repository that reads products from a JSON file stored in `resources/product.json`:

```java
public class ProductRepositoryJson implements ProductRepository {
    private List<Product> list;

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
    public List<Product> findAll() { return list; }

    @Override
    public Product findById(Long id) {
        return list.stream()
                   .filter(p -> p.getId().equals(id))
                   .findFirst()
                   .orElseThrow();
    }
}
```

And registered it in a configuration class:

```java
@Configuration
@PropertySource("classpath:config.properties")
public class AppConfig {

    @Value("classpath:product.json")
    private Resource resource;

    @Bean("productJson")
    ProductRepository productRepositoryJson() {
        return new ProductRepositoryJson(resource);
    }
}
```

This demonstrates how we can use a **custom configuration class** with `@Bean` methods to define new beans manually, while still benefiting from dependency injection.

---

## 🌐 Step 8 – Controller and REST Endpoints

Finally, the controller uses dependency injection to access the service layer:

```java
@RestController
@RequestMapping("/api")
public class SomeController {

    @Autowired
    private ProductService service;

    @GetMapping
    public List<Product> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Product show(@PathVariable Long id) {
        return service.findById(id);
    }
}
```

Spring injects `ProductServiceImpl` automatically, which in turn depends on the correct repository.

---

## ✅ Summary

In this section, I’ve learned how to:

* Create and inject dependencies automatically using Spring Boot.
* Decouple components using interfaces.
* Configure custom beans with `@Configuration` and `@Bean`.
* Work with bean scopes (`@Singleton`, `@RequestScope`, `@SessionScope`).
* Use `@Primary` and `@Qualifier` to control which implementation is injected.
* Combine configuration files (`config.properties`) and resources (JSON) for dynamic data.

This example illustrates how Spring Boot handles object management efficiently and promotes clean, modular architecture.
