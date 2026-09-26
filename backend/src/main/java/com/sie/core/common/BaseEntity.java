package com.sie.core.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Common columns shared by every entity in the system.
 *
 * <p>This is a JPA {@code @MappedSuperclass}: it is NOT a table itself, its columns are folded
 * into every subclass's table. {@code createdAt}/{@code updatedAt} are filled in automatically by
 * Spring Data's auditing listener (enabled in {@link JpaAuditingConfig}) - you never set them by
 * hand.
 *
 * <p>Learning note: extend this from every new entity (Product, StockMovement, SalesOrder, ...) so
 * you get id + audit timestamps for free and stay consistent across the whole domain model.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BaseEntity other)) {
      return false;
    }
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getClass());
  }
}
