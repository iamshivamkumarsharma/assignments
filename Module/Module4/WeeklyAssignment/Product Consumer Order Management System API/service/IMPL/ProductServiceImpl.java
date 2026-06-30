package org.nbfc.productwa.service.impl;

import lombok.RequiredArgsConstructor;
import org.nbfc.productwa.dto.PageResponse;
import org.nbfc.productwa.dto.ProductRequest;
import org.nbfc.productwa.dto.ProductResponse;
import org.nbfc.productwa.exception.ResourceNotFoundException;
import org.nbfc.productwa.mapper.ProductMapper;
import org.nbfc.productwa.model.Product;
import org.nbfc.productwa.repository.ProductRepository;
import org.nbfc.productwa.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        Product saved = productRepository.save(product);
        log.info("Created product {} (id={})", saved.getProductName(), saved.getId());
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        return productMapper.toResponse(findActiveOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAll(Pageable pageable) {
        return toPageResponse(productRepository.findAllActive(pageable));
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findActiveOrThrow(id);
        productMapper.updateEntity(product, request);
        Product saved = productRepository.save(product);
        log.info("Updated product id={}", id);
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = findActiveOrThrow(id);
        product.setDeleted(true);
        productRepository.save(product);
        log.info("Soft-deleted product id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getByCategory(String category, Pageable pageable) {
        return toPageResponse(productRepository.findByCategory(category, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String keyword, Pageable pageable) {
        return toPageResponse(productRepository.searchByKeyword(keyword, pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> filter(String category, String brand,
                                                BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return toPageResponse(productRepository.filter(category, brand, minPrice, maxPrice, pageable));
    }

    private Product findActiveOrThrow(Long id) {
        return productRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private PageResponse<ProductResponse> toPageResponse(Page<Product> page) {
        return PageResponse.from(page.map(productMapper::toResponse));
    }
}
