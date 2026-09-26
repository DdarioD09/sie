package com.sie.core.product;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data generates the implementation of this interface at startup - you only declare the
 * queries you need beyond the CRUD methods {@link JpaRepository} already gives you (findAll,
 * findById, save, deleteById, ...).
 */
public interface ProductTypeRepository extends JpaRepository<ProductType, Long> {

  Optional<ProductType> findByNameIgnoreCase(String name);

  boolean existsByNameIgnoreCase(String name);
}
