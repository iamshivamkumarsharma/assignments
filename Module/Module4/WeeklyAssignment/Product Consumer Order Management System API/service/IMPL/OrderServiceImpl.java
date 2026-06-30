package org.nbfc.productwa.service.impl;

import lombok.RequiredArgsConstructor;
import org.nbfc.productwa.dto.OrderRequest;
import org.nbfc.productwa.dto.OrderResponse;
import org.nbfc.productwa.dto.PageResponse;
import org.nbfc.productwa.exception.BadRequestException;
import org.nbfc.productwa.exception.ResourceNotFoundException;
import org.nbfc.productwa.exception.UnauthorizedException;
import org.nbfc.productwa.mapper.OrderMapper;
import org.nbfc.productwa.model.Order;
import org.nbfc.productwa.model.OrderStatus;
import org.nbfc.productwa.model.Product;
import org.nbfc.productwa.model.User;
import org.nbfc.productwa.repository.OrderRepository;
import org.nbfc.productwa.repository.ProductRepository;
import org.nbfc.productwa.repository.UserRepository;
import org.nbfc.productwa.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponse placeOrder(String userEmail, OrderRequest request) {
        User user = getUser(userEmail);
        Product product = getActiveProduct(request.getProductId());

        if (product.getAvailableQuantity() < request.getQuantity()) {
            throw new BadRequestException("Insufficient stock for product: " + product.getProductName()
                    + ". Available: " + product.getAvailableQuantity());
        }

        product.setAvailableQuantity(product.getAvailableQuantity() - request.getQuantity());
        productRepository.save(product);

        BigDecimal totalPrice = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        Order order = Order.builder()
                .orderDate(LocalDateTime.now())
                .quantity(request.getQuantity())
                .totalPrice(totalPrice)
                .status(OrderStatus.PLACED)
                .user(user)
                .product(product)
                .build();

        Order saved = orderRepository.save(order);
        log.info("User {} placed order id={} for product id={}", userEmail, saved.getId(), product.getId());
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(String userEmail, Long orderId) {
        Order order = getOrder(orderId);
        ensureOwnership(order, userEmail);
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getByIdAsAdmin(Long orderId) {
        return orderMapper.toResponse(getOrder(orderId));
    }

    @Override
    @Transactional
    public OrderResponse updateOwnOrder(String userEmail, Long orderId, OrderRequest request) {
        Order order = getOrder(orderId);
        ensureOwnership(order, userEmail);

        if (order.getStatus() != OrderStatus.PLACED) {
            throw new BadRequestException("Only orders in PLACED status can be updated");
        }

        Product product = order.getProduct();
        int previousQuantity = order.getQuantity();
        int newQuantity = request.getQuantity();
        int delta = newQuantity - previousQuantity;

        if (delta > 0 && product.getAvailableQuantity() < delta) {
            throw new BadRequestException("Insufficient stock to increase order quantity");
        }

        product.setAvailableQuantity(product.getAvailableQuantity() - delta);
        productRepository.save(product);

        order.setQuantity(newQuantity);
        order.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(newQuantity)));
        Order saved = orderRepository.save(order);
        log.info("User {} updated order id={} quantity {}->{}", userEmail, orderId, previousQuantity, newQuantity);
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void cancelOwnOrder(String userEmail, Long orderId) {
        Order order = getOrder(orderId);
        ensureOwnership(order, userEmail);

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already cancelled");
        }
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.SHIPPED) {
            throw new BadRequestException("Cannot cancel an order that is already " + order.getStatus());
        }

        Product product = order.getProduct();
        product.setAvailableQuantity(product.getAvailableQuantity() + order.getQuantity());
        productRepository.save(product);

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("User {} cancelled order id={}", userEmail, orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(String userEmail, Pageable pageable) {
        User user = getUser(userEmail);
        return toPageResponse(orderRepository.findByUserId(user.getId(), pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(Pageable pageable) {
        return toPageResponse(orderRepository.findAll(pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getByStatus(OrderStatus status, Pageable pageable) {
        return toPageResponse(orderRepository.findByStatus(status, pageable));
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus status) {
        Order order = getOrder(orderId);
        order.setStatus(status);
        Order saved = orderRepository.save(order);
        log.info("Admin updated order id={} status to {}", orderId, status);
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = getOrder(orderId);
        orderRepository.delete(order);
        log.info("Admin deleted order id={}", orderId);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private Product getActiveProduct(Long productId) {
        return productRepository.findActiveById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
    }

    private Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private void ensureOwnership(Order order, String userEmail) {
        if (!order.getUser().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("You are not allowed to access this order");
        }
    }

    private PageResponse<OrderResponse> toPageResponse(Page<Order> page) {
        return PageResponse.from(page.map(orderMapper::toResponse));
    }
}
