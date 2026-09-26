package com.sie.core.product;

import com.sie.core.common.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic lives here, never in the controller. Controllers translate HTTP <-> Java;
 * services enforce rules (e.g. "no duplicate names"); repositories talk to the database.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductTypeService {

  private final ProductTypeRepository repository;

  public List<ProductType> findAll() {
    return repository.findAll();
  }

  public ProductType findById(Long id) {
    return repository.findById(id).orElseThrow(() -> NotFoundException.forEntity("ProductType", id));
  }

  @Transactional
  public ProductType create(String name) {
    if (repository.existsByNameIgnoreCase(name)) {
      throw new IllegalArgumentException("A product type named '" + name + "' already exists");
    }
    ProductType type = ProductType.builder().name(name).active(true).build();
    return repository.save(type);
  }

  @Transactional
  public ProductType rename(Long id, String newName) {
    ProductType type = findById(id);
    type.setName(newName);
    return type; // no explicit save() needed: entity is managed inside this @Transactional method
  }

  @Transactional
  public void deactivate(Long id) {
    ProductType type = findById(id);
    type.setActive(false);
  }
}
