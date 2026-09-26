package com.sie.core.product;

import com.sie.core.product.dto.ProductTypeMapper;
import com.sie.core.product.dto.ProductTypeRequest;
import com.sie.core.product.dto.ProductTypeResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reference implementation for every other controller you write in this project: thin, delegates
 * to the service, maps entities to DTOs before returning them.
 */
@RestController
@RequestMapping("/api/product-types")
@RequiredArgsConstructor
public class ProductTypeController {

  private final ProductTypeService service;

  @GetMapping
  public List<ProductTypeResponse> findAll() {
    return service.findAll().stream().map(ProductTypeMapper::toResponse).toList();
  }

  @GetMapping("/{id}")
  public ProductTypeResponse findById(@PathVariable Long id) {
    return ProductTypeMapper.toResponse(service.findById(id));
  }

  @PostMapping
  public ResponseEntity<ProductTypeResponse> create(@Valid @RequestBody ProductTypeRequest request) {
    ProductType created = service.create(request.name());
    return ResponseEntity.created(URI.create("/api/product-types/" + created.getId()))
        .body(ProductTypeMapper.toResponse(created));
  }

  @PutMapping("/{id}")
  public ProductTypeResponse rename(@PathVariable Long id, @Valid @RequestBody ProductTypeRequest request) {
    return ProductTypeMapper.toResponse(service.rename(id, request.name()));
  }
}
