package org.nbfc.productwa.mapper;

import org.nbfc.productwa.dto.ProductRequest;
import org.nbfc.productwa.dto.ProductResponse;
import org.nbfc.productwa.model.Product;
import org.springframework.stereotype.Component;

/**
 * Manual mapper between {@link Product} entity and its DTOs.
 */
@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request) {
        return Product.builder()
                .productName(request.getProductName())
                .description(request.getDescription())
                .category(request.getCategory())
                .brand(request.getBrand())
                .price(request.getPrice())
                .availableQuantity(request.getAvailableQuantity())
                .deleted(false)
                .build();
    }

    public void updateEntity(Product product, ProductRequest request) {
        product.setProductName(request.getProductName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setBrand(request.getBrand());
        product.setPrice(request.getPrice());
        product.setAvailableQuantity(request.getAvailableQuantity());
    }

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .productName(product.getProductName())
                .description(product.getDescription())
                .category(product.getCategory())
                .brand(product.getBrand())
                .price(product.getPrice())
                .availableQuantity(product.getAvailableQuantity())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
