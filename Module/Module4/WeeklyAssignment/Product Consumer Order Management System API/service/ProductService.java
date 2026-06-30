package org.nbfc.productwa.service;

import org.nbfc.productwa.dto.PageResponse;
import org.nbfc.productwa.dto.ProductRequest;
import org.nbfc.productwa.dto.ProductResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductService {

    ProductResponse create(ProductRequest request);

    ProductResponse getById(Long id);

    PageResponse<ProductResponse> getAll(Pageable pageable);

    ProductResponse update(Long id, ProductRequest request);

    void delete(Long id);

    PageResponse<ProductResponse> getByCategory(String category, Pageable pageable);

    PageResponse<ProductResponse> search(String keyword, Pageable pageable);

    PageResponse<ProductResponse> filter(String category, String brand,
                                         BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
}
