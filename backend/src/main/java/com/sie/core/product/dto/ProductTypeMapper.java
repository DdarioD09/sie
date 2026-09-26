package com.sie.core.product.dto;

import com.sie.core.product.ProductType;

/**
 * Hand-written Entity <-> DTO mapper. A library like MapStruct removes this boilerplate once you
 * have a dozen entities, but writing it by hand a few times first is worth it so you actually
 * understand what the generated code would do.
 */
public final class ProductTypeMapper {

  private ProductTypeMapper() {}

  public static ProductTypeResponse toResponse(ProductType entity) {
    return new ProductTypeResponse(entity.getId(), entity.getName(), entity.isActive());
  }
}
