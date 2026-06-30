package org.nbfc.productwa.config;

import lombok.RequiredArgsConstructor;
import org.nbfc.productwa.model.Order;
import org.nbfc.productwa.model.OrderStatus;
import org.nbfc.productwa.model.Product;
import org.nbfc.productwa.model.Role;
import org.nbfc.productwa.model.User;
import org.nbfc.productwa.repository.OrderRepository;
import org.nbfc.productwa.repository.ProductRepository;
import org.nbfc.productwa.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Seeds sample data on startup when the database is empty.
 */
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Sample data already present - skipping seeding");
            return;
        }

        List<User> users = seedUsers();
        List<Product> products = seedProducts();
        seedOrders(users, products);
        log.info("Sample data seeding completed: {} users, {} products, {} orders",
                userRepository.count(), productRepository.count(), orderRepository.count());
    }

    private List<User> seedUsers() {
        List<User> users = new ArrayList<>();

        users.add(User.builder()
                .firstName("System").lastName("Admin")
                .email("admin@productwa.com")
                .password(passwordEncoder.encode("admin@123"))
                .mobile("9000000000").address("HQ, Bengaluru")
                .role(Role.ROLE_ADMIN).enabled(true)
                .build());

        String[][] data = {
                {"Alice", "Walker", "alice@productwa.com", "9000000001", "12 Park Street, Pune"},
                {"Bob", "Stone", "bob@productwa.com", "9000000002", "44 Lake Road, Mumbai"},
                {"Carol", "Reed", "carol@productwa.com", "9000000003", "9 Hill View, Delhi"},
                {"David", "Cole", "david@productwa.com", "9000000004", "7 Sea Lane, Chennai"},
                {"Eve", "Khan", "eve@productwa.com", "9000000005", "21 Garden Ave, Hyderabad"}
        };
        for (String[] u : data) {
            users.add(User.builder()
                    .firstName(u[0]).lastName(u[1]).email(u[2])
                    .password(passwordEncoder.encode("user@123"))
                    .mobile(u[3]).address(u[4])
                    .role(Role.ROLE_USER).enabled(true)
                    .build());
        }
        return userRepository.saveAll(users);
    }

    private List<Product> seedProducts() {
        List<Product> products = new ArrayList<>();
        Object[][] data = {
                {"iPhone 15", "Apple smartphone", "Electronics", "Apple", "79999.00", 50},
                {"Galaxy S24", "Samsung flagship phone", "Electronics", "Samsung", "69999.00", 40},
                {"MacBook Air", "M3 ultralight laptop", "Computers", "Apple", "114999.00", 25},
                {"Dell XPS 13", "Premium ultrabook", "Computers", "Dell", "99999.00", 30},
                {"Sony WH-1000XM5", "Noise cancelling headphones", "Audio", "Sony", "29999.00", 60},
                {"Kindle Paperwhite", "E-reader", "Books", "Amazon", "13999.00", 80},
                {"Nike Air Max", "Running shoes", "Footwear", "Nike", "8999.00", 100},
                {"Levi's 511", "Slim fit jeans", "Apparel", "Levis", "3499.00", 120},
                {"Instant Pot", "Multi cooker", "Home", "InstantPot", "7999.00", 45},
                {"Logitech MX Master 3", "Wireless mouse", "Accessories", "Logitech", "8499.00", 70}
        };
        for (Object[] p : data) {
            products.add(Product.builder()
                    .productName((String) p[0])
                    .description((String) p[1])
                    .category((String) p[2])
                    .brand((String) p[3])
                    .price(new BigDecimal((String) p[4]))
                    .availableQuantity((Integer) p[5])
                    .deleted(false)
                    .build());
        }
        return productRepository.saveAll(products);
    }

    private void seedOrders(List<User> users, List<Product> products) {
        List<User> consumers = users.stream().filter(u -> u.getRole() == Role.ROLE_USER).toList();
        OrderStatus[] statuses = OrderStatus.values();
        List<Order> orders = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            User user = consumers.get(i % consumers.size());
            Product product = products.get(ThreadLocalRandom.current().nextInt(products.size()));
            int quantity = ThreadLocalRandom.current().nextInt(1, 4);
            BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(quantity));

            orders.add(Order.builder()
                    .orderDate(LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(0, 30)))
                    .quantity(quantity)
                    .totalPrice(total)
                    .status(statuses[ThreadLocalRandom.current().nextInt(statuses.length)])
                    .user(user)
                    .product(product)
                    .build());
        }
        orderRepository.saveAll(orders);
    }
}
