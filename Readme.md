# Dependency Injection

We’ll start with basic concepts, move through manual dependency management, and end with fully automated injection using `@Autowired`, `@Qualifier`, and custom configuration classes.

---

## 🧠 1. Understanding Dependency Injection

In object-oriented programming, classes often depend on other classes. For example, a `ProductService` may need a `ProductRepository` to retrieve data.
Without DI, we’d manually create dependencies like this:

```java
ProductRepository repo = new ProductRepositoryImpl();
ProductService service = new ProductServiceImpl(repo);
```

This approach **creates tight coupling** — the service directly controls which repository it uses.
If we later want to change the data source (e.g., from a list to a JSON file or a database), we’d have to modify the service class.

**Dependency Injection** solves this problem by allowing **Spring** to manage object creation and wiring.
Classes simply declare what they need, and Spring provides it automatically.

---

## ⚙️ 2. How Spring Boot Manages Dependencies (IoC Container)

Spring uses an **Inversion of Control (IoC) Container**, which creates and manages objects called **beans**.
Beans are registered through annotations like:

* `@Component` – generic Spring bean
* `@Service` – business logic layer
* `@Repository` – persistence layer
* `@Controller` / `@RestController` – web layer

The container automatically detects and injects these beans wherever they are needed.

---

## 🧩 3. Setting Up the Model

Our base entity is a `Product` class (with `id`, `name`, and `price`).
It implements `Cloneable` so we can safely modify copies of products without altering the original data — a good practice for immutability.

---

## 🗄️ 4. Creating a Repository Without Dependency Injection

Let’s first simulate a database repository manually:

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

This repository is functional, but currently it would need to be instantiated manually by any class that uses it — something we’ll soon let Spring handle automatically.

---

## 🧱 5. Decoupling with Interfaces

We define interfaces to **abstract** the implementation.
This lets us replace one repository with another (e.g., JSON-based, database, or API) without modifying business logic.

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

By coding to interfaces, we **decouple** our layers and make testing easier.

---

## 🔌 6. Implementing Dependency Injection in the Service Layer

Now, we let Spring **inject** the repository into the service.
Instead of manually instantiating it, we use constructor injection:

```java
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;
    private final Environment environment;

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
                product.setPrice((long) (p.getPrice() *
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

Spring now automatically provides an instance of `ProductRepository` (specifically the `productJson` bean) and `Environment`.

### 💡 Injection Methods in Spring

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
   Guarantees immutability and makes testing simpler.

---

## 🧬 7. Bean Scopes and Lifecycle

By default, Spring beans follow the **Singleton pattern** — one shared instance per application context.
However, Spring offers more granular control with scopes:

| Scope       | Description                            |
| ----------- | -------------------------------------- |
| `singleton` | One shared instance (default)          |
| `prototype` | A new instance each time it’s injected |
| `request`   | One instance per HTTP request          |
| `session`   | One instance per user session          |

Example:

```java
@Service
@RequestScope
public class RequestScopedService { ... }
```

This is useful for request-specific data like user info or session state.

---

## 🎯 8. Managing Multiple Implementations with @Primary and @Qualifier

If two beans implement the same interface, Spring won’t know which one to inject.
We can solve this in two ways:

* **@Primary**: Marks a default bean to be injected when multiple are available.
* **@Qualifier**: Explicitly specifies which bean to inject.

Example:

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

Here, even though `productList` is marked `@Primary`, we explicitly inject the JSON-based repository using `@Qualifier("productJson")`.

---

## 📂 9. Custom Bean Configuration with @Configuration and @Bean

To add more flexibility, I created a repository that reads data from a **JSON file** stored in `resources/product.json`.

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

Then I registered it as a Spring bean using a **configuration class**:

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

This approach demonstrates **manual bean registration** while keeping full compatibility with DI.

---

## 🌍 10. The Controller Layer and Dependency Injection in Action

The controller layer uses DI to get the service automatically:

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

Spring creates and injects all required components behind the scenes:

* `SomeController` → gets a `ProductServiceImpl`
* `ProductServiceImpl` → gets a `ProductRepositoryJson` and `Environment`

---

## 🧾 11. Summary and Key Takeaways

After completing this section, I’ve learned:

* How **Dependency Injection** decouples components and simplifies maintenance.
* How the **Spring IoC container** manages beans automatically.
* The importance of **interfaces** in flexible architecture.
* Different injection types: field, setter, and constructor.
* The use of **scopes** (`singleton`, `request`, `session`) to control bean lifecycle.
* How **@Primary** and **@Qualifier** resolve multiple bean conflicts.
* How to use **@Configuration** and **@Bean** to define custom beans.

---

## 🚀 Final Thoughts

Dependency Injection is the backbone of Spring Boot.
It lets us focus on **business logic** while Spring handles object creation and lifecycle management.
Once you master it, you can build modular, testable, and scalable applications with ease.

> 💡 **Tip:** Always prefer constructor injection, rely on interfaces, and use qualifiers wisely. These small habits lead to cleaner, more maintainable code.
