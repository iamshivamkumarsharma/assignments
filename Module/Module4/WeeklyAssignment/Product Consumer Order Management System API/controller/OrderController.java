package org.nbfc.productwa.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nbfc.productwa.dto.ApiResponse;
import org.nbfc.productwa.dto.OrderRequest;
import org.nbfc.productwa.dto.OrderResponse;
import org.nbfc.productwa.dto.OrderStatusUpdateRequest;
import org.nbfc.productwa.dto.PageResponse;
import org.nbfc.productwa.model.OrderStatus;
import org.nbfc.productwa.service.OrderService;
import org.nbfc.productwa.util.Constants;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders", description = "Order placement and management")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place a new order")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(@Valid @RequestBody OrderRequest request,
                                                                 Authentication authentication) {
        OrderResponse response = orderService.placeOrder(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of("Order placed", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all orders (ADMIN only)")
    public ResponseEntity<PageResponse<OrderResponse>> getAll(
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(orderService.getAllOrders(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an order by id (owner or ADMIN)")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id, Authentication authentication) {
        OrderResponse response = isAdmin(authentication)
                ? orderService.getByIdAsAdmin(id)
                : orderService.getById(authentication.getName(), id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update own order quantity")
    public ResponseEntity<ApiResponse<OrderResponse>> update(@PathVariable Long id,
                                                             @Valid @RequestBody OrderRequest request,
                                                             Authentication authentication) {
        OrderResponse response = orderService.updateOwnOrder(authentication.getName(), id, request);
        return ResponseEntity.ok(ApiResponse.of("Order updated", response));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel own order")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id, Authentication authentication) {
        orderService.cancelOwnOrder(authentication.getName(), id);
        return ResponseEntity.ok(ApiResponse.of("Order cancelled", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete any order (ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.of("Order deleted", null));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update order status (ADMIN only)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequest request) {
        OrderResponse response = orderService.updateStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.of("Order status updated", response));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Search orders by status (ADMIN only)")
    public ResponseEntity<PageResponse<OrderResponse>> getByStatus(
            @PathVariable OrderStatus status,
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(orderService.getByStatus(status, PageRequest.of(page, size)));
    }

    @GetMapping("/user")
    @Operation(summary = "List the authenticated user's own orders")
    public ResponseEntity<PageResponse<OrderResponse>> getMyOrders(
            Authentication authentication,
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(orderService.getMyOrders(authentication.getName(), PageRequest.of(page, size)));
    }

    @GetMapping("/sort")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all orders sorted by a field (ADMIN only)")
    public ResponseEntity<PageResponse<OrderResponse>> sort(
            @RequestParam(defaultValue = "orderDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = Constants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = "10") int size) {
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return ResponseEntity.ok(orderService.getAllOrders(PageRequest.of(page, size, Sort.by(dir, sortBy))));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(Constants.ROLE_ADMIN::equals);
    }
}
