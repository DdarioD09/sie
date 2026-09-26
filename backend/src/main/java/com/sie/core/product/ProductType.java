package com.sie.core.product;

import com.sie.core.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A category of garment ("Sweater" today, "T-Shirt" / "Beanie" later). Kept as its own table
 * instead of a hardcoded enum precisely so you can add new clothing types without a code change.
 *
 * <p>This is the reference/example entity for the module: {@code Product} and
 * {@code ProductVariant} (size + color + SKU) belong in this same package and follow the exact
 * same Entity -> Repository -> Service -> Controller -> DTO shape you see here.
 */
@Entity
@Table(name = "product_types")
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductType extends BaseEntity {

  @Column(nullable = false, unique = true, length = 80)
  private String name;

  @Column(nullable = false)
  private boolean active = true;

  public String getName() {
    return name;
  }

  public boolean isActive() {
    return active;
  }
}
